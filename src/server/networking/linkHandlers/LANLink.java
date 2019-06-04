package server.networking.linkHandlers;

import backend.Client;
import backend.JSONConverter;
import backend.PacketType;
import backend.RSAHelper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
            ConnectionStarted connectionSource) {
        this.clientID = clientID;
        this.linkProvider = linkProvider;
        this.connectionSource = connectionSource;
        this.socket = socket;
        callback = linkProvider;
    }

    public String getName() {
        return "LanLink";
    } //TODO Socket is always null, fix.

    public LANLinkHandler getPairingHandler(
            Client device, LANLinkHandler.PairingHandlerCallback callback) {
        return new LANLinkHandler(device, callback);
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
                        while (true) {
                            String packet;
                            try {
                                packet = reader.readUTF();
                            } catch (SocketTimeoutException e) {
                                continue;
                            }
//                            }
                            if (packet.isEmpty()) {
                                continue;
                            }
                            System.out.println("Data Received:\n" + packet);
                            JSONConverter json = JSONConverter.unserialize(packet);
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
            final ServerSocket server; //TODO Implement DataLoad Sending

            if (key != null) {
                System.out.println("Encrypting Packet...");
                json = RSAHelper.encrypt(json, key);
                System.out.println("Packet encrypted successfully!");
            }

            try {
                DataOutputStream writer = new DataOutputStream(socket.getOutputStream());
                writer.writeUTF(json.serialize());
                writer.flush();
//                writer.close();
            } catch (Exception e) {
                disconnect(); // main socket is broken, disconnect
                e.printStackTrace();
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
        packageReceived(json);
    }

    private void packageReceived(JSONConverter json) {
        System.out.println("Sending Packet to all Receivers!");
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
