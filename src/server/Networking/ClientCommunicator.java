package server.Networking;

import backend.*;
import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;

public class ClientCommunicator extends CommunicationThread {

    ClientCommunicator(@NotNull Client client) {
        super(client);
    }

    @Override
    public void run() {
        try {
            waitForConnection();
            System.out.println("Connection from IP: \"" + getSocket().getInetAddress() + "\"");
            if (getSocket().getInetAddress().toString().equals(getIP().toString())) {
                System.out.println("IP Matches set IP!");
                openStreams();
                getClient().setConfirmed(true);
                System.out.println("Sending Ready...");
                sendReady();
                System.out.println("Sent Ready!");
                String message;
                try {
                    while ((message = receiveMessage()) != null) {
                        if (!processMessage(message)) break;
                    }
                    System.err.println("Error message null!");
                } catch (EOFException e) {
                    System.err.println("Client Disconnected Unexpectedly!");
                } finally {
                    getSocket().close();
                    getClient().setConfirmed(false);
                    this.run();
                }
                stop();
            } else {
                System.err.println("Connection from IP: \"" + getSocket().getInetAddress() + "\" Does not match set IP: \"" + getIP() + "\" Trying Again...");
                getSocket().close();
                this.run();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean processMessage(@NotNull String message) throws IOException {
        JSONConverter json = JSONConverter.unserialize(message);
        switch (json.getType()) {
            case PacketType.UNPAIR_CMD:
                System.out.println("Unpair Command!");
                return false;
            case PacketType.NOTI_REQUEST:
                System.out.println("Noti Request!");
                sendReady();
                return true;
            case PacketType.NOTIFICATION:
                Notification noti = Notification.jsonToNoti(json);
                getClient().addNoti(noti);
                sendReady();
                return true;
            default:
                System.err.println("Packet: " + message + " is invalid.");
                System.err.println("JSON Type of: " + json.getType());
                return true;
        }
    }

    public DataLoad recieveDataLoad() { //TODO
        return null;
    }

    public void sendReady() throws IOException {
        sendMessage(new JSONConverter(PacketType.READY_RESPONSE).serialize(), getPort());
    }
}
