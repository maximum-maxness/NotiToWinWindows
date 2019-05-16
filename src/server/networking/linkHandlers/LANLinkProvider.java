package server.networking.linkHandlers;

import backend.Client;
import backend.JSONConverter;
import runner.Main;
import server.networking.helpers.PacketType;
import server.networking.helpers.SSLHelper;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class LANLinkProvider implements LANLink.LinkDisconnectedCallback {

    static final int DATALOAD_TRANSFER_MIN_PORT = 1936;
    private static final int MAX_PORT = 1958;
    private static final int MIN_PORT = 1927;
    private final HashMap<String, LANLink> visibleClients = new HashMap<>();
    private final CopyOnWriteArrayList<ConnectionReceiver> connectionReceivers = new CopyOnWriteArrayList<>();

    private ServerSocket tcpServer;
    private DatagramSocket udpServer;

    private boolean listening = false;

    public LANLinkProvider() {

    }

    static ServerSocket openTCPServerOnFreePort() throws IOException {
        System.out.println("Opening TCP Server On Free Port.");
        int tcpPort = LANLinkProvider.MIN_PORT;
        while (tcpPort <= MAX_PORT) {
            try {
                ServerSocket possibleServer = new ServerSocket();
                possibleServer.bind(new InetSocketAddress(tcpPort));
                return possibleServer;
            } catch (IOException e) {
                tcpPort++;
                if (tcpPort == MAX_PORT) {
                    throw e;
                }
            }
        }
        throw new RuntimeException("Bad.");
    }

    public void addConnectionReceiver(ConnectionReceiver cr) {
        connectionReceivers.add(cr);
    }

    public boolean removeConnectionReceiver(ConnectionReceiver cr) {
        return connectionReceivers.remove(cr);
    }

    @Override
    public void linkDisconnected(LANLink broken) {
        System.out.println("Link Disconnected for Client ID: " + broken.getClientID());
        String clientID = broken.getClientID();
        visibleClients.remove(clientID);
        connectionLost(broken);
    }

    private void TCPPacketReceived(Socket socket) {
        System.out.println("TCP Packet Has Been Received!");
        JSONConverter json;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message = reader.readLine();
//            System.out.println("Read message: " + message);
            json = JSONConverter.unserialize(message);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        assert json != null;
        System.out.println("JSON Type is.. " + json.getType());
        if (!json.getType().equals(PacketType.IDENTITY_PACKET)) {
            return;
        }

        identityPacketReceived(json, socket, LANLink.ConnectionStarted.Locally);
    }

    private void UDPPacketReceived(DatagramPacket packet) {
        System.out.println("UDP Packet Received!");
        final InetAddress address = packet.getAddress();
        try {
            String message = new String(packet.getData());
//            System.out.println("Received Message: " + message);
            final JSONConverter json = JSONConverter.unserialize(message);
            assert json != null;
            System.out.println("JSON Type is: " + json.getType());
            final String clientID = json.getString("clientID");
            if (!json.getType().equals(PacketType.IDENTITY_PACKET)) {
                return;
            } else {
                InetAddress myHost = InetAddress.getLocalHost();
                String ownID = myHost.getHostName();
                if (ownID.equals(clientID)) {
                    return;
                }
            }

            int tcpPort = json.getInt("tcpPort", MIN_PORT);

            SocketFactory sf = SocketFactory.getDefault();
            System.out.println("Attempting to create a TCP Connection with Address: " + address);
            Socket socket = sf.createSocket(address, tcpPort);
            configureSocket(socket);

            OutputStream out = socket.getOutputStream();
            JSONConverter ownIdentity = PacketType.makeIdentityPacket();
            System.out.println("Sending Identity Packet to address: " + address);
            out.write(ownIdentity.serialize().getBytes());
            out.flush();

            identityPacketReceived(json, socket, LANLink.ConnectionStarted.Remotely);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configureSocket(Socket socket) {
        try {
            socket.setKeepAlive(true);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void identityPacketReceived(final JSONConverter json, final Socket socket, final LANLink.ConnectionStarted connectionStarted) {
        String myID = PacketType.getDeviceID();
        final String clientID = json.getString("clientID");
        System.out.println("Identity Packet Received from Client ID: " + clientID);
        if (clientID.equals(myID)) {
            System.err.println("The client ID matches my own ID!");
            return;
        }

        final boolean clientMode = connectionStarted == LANLink.ConnectionStarted.Locally;

        try {
            final SSLSocket sslSocket = SSLHelper.convertToSSLSocket(socket, clientID, SSLHelper.certificateIsStored(clientID), clientMode);
            sslSocket.addHandshakeCompletedListener(event -> {
                String mode = clientMode ? "client" : "server";
                try {
                    Certificate certificate = event.getPeerCertificates()[0];
                    json.set("certificate", Base64.getEncoder().encodeToString(certificate.getEncoded()));
                    System.out.println("Handshake as " + mode + " successful with " + json.getString("clientName") + " secured with " + event.getCipherSuite());
                    addLink(json, sslSocket, connectionStarted);
                } catch (Exception e) {
                    System.err.println("Handshake as " + mode + " failed with " + json.getString("clientName"));
                    e.printStackTrace();
                    Client client = Main.backgroundThread.getClient(clientID);
                    if (client == null) return;
                    client.unpair();
                }
            });

            new Thread(() -> {
                try {
                    synchronized (this) {
                        System.out.println("Starting SSL Handshake...");
                        sslSocket.startHandshake();
                    }
                } catch (Exception e) {
                    System.err.println("Handshake failed with " + json.getString("clientName"));
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addLink(final JSONConverter json, Socket socket, LANLink.ConnectionStarted connectionStarted) throws IOException {
        String clientID = json.getString("clientID");
        LANLink currentLink = visibleClients.get(clientID);
        if (currentLink != null) {
            System.out.println("Re Using Same Link for Client ID: " + clientID);
            final Socket oldSocket = currentLink.reset(socket, connectionStarted);
        } else {
            System.out.println("Creating a new Link for Client ID: " + clientID);
            LANLink link = new LANLink(clientID, this, socket, connectionStarted);
            visibleClients.put(clientID, link);
            connectionAccepted(json, link);
        }
    }

    private void setupUDPListener() {
        System.out.println("Creating the UDP Listener Server...");
        try {
            udpServer = new DatagramSocket(MIN_PORT);
            udpServer.setReuseAddress(true);
            udpServer.setBroadcast(true);
        } catch (SocketException e) {
            System.err.println("Error Creating the UDP Server.");
            e.printStackTrace();
            return;
        }
        new Thread(() -> {
            while (listening) {
                final int bufferSize = 524288;
                byte[] data = new byte[bufferSize];
                DatagramPacket packet = new DatagramPacket(data, bufferSize);
                try {
                    udpServer.receive(packet);
                    UDPPacketReceived(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Stopping UDP Listener...");
        }).start();
    }

    private void setupTCPListener() {
        System.out.println("Creating the TCP Listener Server...");
        try {
            tcpServer = openTCPServerOnFreePort();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        new Thread(() -> {
            while (listening) {
                try {
                    Socket socket = tcpServer.accept();
                    configureSocket(socket);
                    TCPPacketReceived(socket);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Stopping TCP Listener...");
        }).start();
    }

    private void broadcastUdpPacket() {
        System.out.println("Broadcasting Identity Packet to 255.255.255.255");
        new Thread(() -> {
            String broadcast = "255.255.255.255";

            JSONConverter passportPacket = PacketType.makeIdentityPacket();
            int port = (tcpServer == null || !tcpServer.isBound()) ? MIN_PORT : tcpServer.getLocalPort();
            passportPacket.set("tcpPort", port);
            DatagramSocket socket = null;
            byte[] bytes = null;
            try {
                socket = new DatagramSocket();
                socket.setReuseAddress(true);
                socket.setBroadcast(true);
                bytes = passportPacket.serialize().getBytes();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (bytes != null) {
                try {
                    InetAddress client = InetAddress.getByName(broadcast);
                    socket.send(new DatagramPacket(bytes, bytes.length, client, MIN_PORT));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (socket != null) {
                socket.close();
            }
        }).start();
    }

    void connectionAccepted(JSONConverter json, LANLink link) {
        for (ConnectionReceiver cr : connectionReceivers) {
            cr.onConnectionReceived(json, link);
        }
    }

    void connectionLost(LANLink link) {
        for (ConnectionReceiver cr : connectionReceivers) {
            cr.onConnectionLost(link);
        }
    }

    public void onStart() {
        System.out.println("Starting LANLINK PROVIDER");
        if (!listening) {
            listening = true;
            setupTCPListener();
            setupUDPListener();
            broadcastUdpPacket();
        }
    }

    public void onStop() {
        System.out.println("Stopping LANLINK PROVIDER");
        listening = false;
        try {
            tcpServer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            udpServer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface ConnectionReceiver {
        void onConnectionReceived(JSONConverter identityPacket, LANLink link);

        void onConnectionLost(LANLink link);
    }
}
