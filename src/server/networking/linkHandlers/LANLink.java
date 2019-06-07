package server.networking.linkHandlers;

import backend.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.NotYetConnectedException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

public class LANLink {

    private final String clientID;
    private final ArrayList<PacketReceiver> receivers = new ArrayList<>();
    private final LinkDisconnectedCallback callback;
    private PrivateKey privKey;
    private ConnectionStarted connectionSource;
    private LANLinkProvider linkProvider;
    private volatile Socket socket = null;

    public LANLink(
            String clientID,
            LANLinkProvider linkProvider,
            Socket socket,
            ConnectionStarted connectionSource) throws IOException {
        this.clientID = clientID;
        this.linkProvider = linkProvider;
        this.connectionSource = connectionSource;
        callback = linkProvider;
        reset(socket, connectionSource);
    }

    public String getName() {
        return "LanLink";
    } //TODO Socket is always null, fix.

    public LANLinkHandler getPairingHandler(
            Client device, LANLinkHandler.PairingHandlerCallback callback) {
        return new LANLinkHandler(device, callback);
    }

    public ConnectionStarted getConnectionSource() {
        return connectionSource;
    }

    public Socket reset(Socket newSocket, ConnectionStarted connectionSource) throws IOException {
        System.err.println("Reset Method Invoked!");
        Socket oldSocket = socket;
        socket = newSocket;

        this.connectionSource = connectionSource;

        if (oldSocket != null) {
            System.out.println("Closing Old Socket!");
            oldSocket.close();
        }

        new Thread(
                () -> {
                    try {
                        DataInputStream reader =
                                new DataInputStream(newSocket.getInputStream());
                        int errCount = 0;
                        while (true) {
                            String packet;
                            try {
                                packet = reader.readUTF();
                            } catch (SocketTimeoutException e) {
                                continue;
                            }
                            if (packet == null) {
                                throw new IOException("End of Stream");
                            }
                            if (packet.isEmpty()) {
                                continue;
                            }
                            System.out.println("Data Received:\n" + packet);
                            JSONConverter json = JSONConverter.unserialize(packet);
                            errCount = 0;
                            receivedNetworkPacket(json);
                        }
                    } catch (IOException e) {
                        System.out.println("Socket closed: " + newSocket.hashCode() + ". Reason: " + e.getMessage());
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException ignored) {
                        } // Wait a bit because we might receive a new socket meanwhile
                        boolean thereIsaANewSocket = (newSocket != socket);
                        if (!thereIsaANewSocket) {
                            callback.linkDisconnected(LANLink.this);
                        }
                    }
                }).start();

        return oldSocket;
    }

    private boolean sendPacketProtected(JSONConverter json, final Client.SendPacketStatusCallback callback, PublicKey key) {
        if (socket == null) {
            callback.onFailure(new NotYetConnectedException());
            return false;
        }

        try {
            final ServerSocket server;
            if (json.getBoolean("hasDataLoad", false)) {
                server = LANLinkProvider.openTCPServerOnFreePort();
                json.put("dataLoadPort", server.getLocalPort());
            } else {
                server = null;
            }

            if (key != null) { //FIXME We dont need to encrypt once we have sslsockets working properly.
                System.out.println("Encrypting Packet...");
                json = RSAHelper.encrypt(json, key);
                System.out.println("Packet encrypted successfully!");
            }
            System.err.println("InputStream Available? " + socket.getInputStream().available());
            try {
                DataOutputStream writer = new DataOutputStream(this.socket.getOutputStream());
                writer.writeUTF(json.serialize());
                writer.flush();
//                writer.close();
            } catch (Exception e) {
                disconnect(); // main socket is broken, disconnect
                e.printStackTrace();
            }

            if (server != null) {
                Socket payloadSocket = null;
                OutputStream outputStream = null;
                InputStream inputStream;
                try {
                    server.setSoTimeout(10000);

                    payloadSocket = server.accept();

//                    payloadSocket = SSLHelper.convertToSSLSocket(payloadSocket, getClientID(), true, false);

                    outputStream = payloadSocket.getOutputStream();
                    inputStream = json.getDataLoad().getInputStream();

                    System.out.println("Sending DataLoad!");
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long size = json.getDataLoad().getSize();
                    long progress = 0;
                    long timeSinceLastUpdate = -1;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        progress += bytesRead;
                        outputStream.write(buffer, 0, bytesRead);
                        if (size > 0) {
                            if (timeSinceLastUpdate + 500 < System.currentTimeMillis()) {
                                long percent = ((100 * progress) / size);
                                callback.onProgressChanged((int) percent);
                                timeSinceLastUpdate = System.currentTimeMillis();
                            }
                        }
                    }
                    outputStream.flush();
                    System.out.println("Finished Sending DataLoad, " + progress + " bytes written.");
                } finally {
                    try {
                        server.close();
                    } catch (Exception ignored) {
                    }
                    try {
                        payloadSocket.close();
                    } catch (Exception ignored) {
                    }
                    json.getDataLoad().getInputStream().close();
                    try {
                        outputStream.close();
                    } catch (Exception ignored) {
                    }
                }
            }


            callback.onSuccess();
            return true;
        } catch (Exception e) {
            if (callback != null) {
                callback.onFailure(e);
            }
            return false;
        }
    }

    public boolean sendPacket(JSONConverter json, Client.SendPacketStatusCallback callback) {
        System.out.println("Sending Packet, no need to Encrypt.");
        return sendPacketProtected(json, callback, null);
    }

    public boolean sendPacket(JSONConverter json, Client.SendPacketStatusCallback callback, PublicKey key) {
        System.out.println("Sending Packet, going to Encrypt");
        return sendPacketProtected(json, callback, key);
    }

    private void receivedNetworkPacket(JSONConverter json) {
        System.out.println("Received Packet of Type: " + json.getType());
        if (json.getType().equals(PacketType.ENCRYPTED_PACKET)) {
            try {
                System.out.println("Trying to Decrypt Packet...");
                json = RSAHelper.decrypt(json, privKey);
                System.out.println("Packet decrypted successfully!");
            } catch (Exception e) {
                System.err.println("Error Decrypting Packet.");
                e.printStackTrace();
            }
        }

        if (json.getBoolean("hasDataLoad", false)) {
            System.out.println("Receiving DataLoad!");
            Socket dataLoadSocket = new Socket();
            try {
                int tcpPort = json.getInt("dataLoadPort");
                InetSocketAddress deviceAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
                System.out.println("Connecting to DataLoad server on port " + tcpPort);
                dataLoadSocket.connect(new InetSocketAddress(deviceAddress.getAddress(), tcpPort));
                System.out.println("Connected!");
//                dataLoadSocket = SSLHelper.convertToSSLSocket(dataLoadSocket, getClientID(), true, true);
//                System.out.println("Converted!");
                json.setDataLoad(new DataLoad(dataLoadSocket.getInputStream(), json.getLong("dataLoadSize")));
            } catch (Exception e) {
                try {
                    dataLoadSocket.close();
                } catch (Exception ignored) {
                }
                e.printStackTrace();
            }

        }

        packageReceived(json);
    }

    private void packageReceived(JSONConverter json) {
        System.out.println("Sending Packet to all Receivers! (Size = " + receivers.size() + ")");
        for (PacketReceiver pr : receivers) {
            pr.onPacketReceived(json);
        }
    }

    public void disconnect() {
        System.out.println("Disconnecting Socket.");
        linkProvider.connectionLost(this);
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getClientID() {
        return this.clientID;
    }

    public void setPrivateKey(PrivateKey key) {
        this.privKey = key;
    }

    public LANLinkProvider getLinkProvider() {
        return this.linkProvider;
    }

    public boolean linkShouldBeKeptAlive() {
        return true;
    } //TODO Remedy Temp Fix (Should be false)

    public void addPacketReceiver(PacketReceiver pr) {
        receivers.add(pr);
    }

    public void removePacketReceiver(PacketReceiver pr) {
        receivers.remove(pr);
    }

    public boolean isConnected() {
        return socket.isConnected();
    }

    public enum ConnectionStarted {
        Locally,
        Remotely
    }

    public interface PacketReceiver {
        void onPacketReceived(JSONConverter json);
    }

    public interface LinkDisconnectedCallback {
        void linkDisconnected(LANLink broken);
    }
}
