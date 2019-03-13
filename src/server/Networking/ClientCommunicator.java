package server.Networking;

import backend.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
                String message;
                while ((message = receiveMessage()) != null) {
                    if (!processMessage(message)) break;
                }
                stop();
            } else {
                System.err.println("Connection from IP: \"" + getSocket().getInetAddress() + "\" Does not match set IP: \"" + getIP() + "\" Trying Again...");
                this.run();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean processMessage(@NotNull String message) throws IOException {
        switch (message) {
            case PacketType.UNPAIR_CMD:
                System.out.println("Unpair Command!");
                return false;
            case PacketType.NOTI_REQUEST:
                System.out.println("Noti Request!");
                sendReady();
                return true;
            default:
                if (message.endsWith("}")) {
                    Notification noti = processJson(message);
                    getClient().addNoti(noti);
                    sendReady();
                } else {
                    System.err.println("Packet: " + message + " is invalid.");
                }
                return true;
        }
    }

    public DataLoad recieveDataLoad() { //TODO
        return null;
    }

    @Nullable
    private Notification processJson(String jsonString) {
        System.out.println("JSON Detected!");
        JSONConverter json = JSONConverter.unserialize(jsonString);
        if (json.getType().equals(PacketType.NOTI_REQUEST)) {
            System.out.println("JSON Type is Noti Request!");
            return Notification.jsonToNoti(json);
        } else {
            System.err.println("Json type: " + json.getType() + "is unrecognized.");
            return null;
        }

    }


    private void sendReady() throws IOException {
        sendMessage(PacketType.READY_RESPONSE, getPort());
    }
}
