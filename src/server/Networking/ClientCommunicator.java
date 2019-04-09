package server.Networking;

import backend.Client;
import backend.JSONConverter;
import backend.Notification;
import backend.PacketType;
import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ClientCommunicator extends CommunicationThread {
    private static int MAX_FAILS = 5;
    private int failCount = 0;

    ClientCommunicator(@NotNull Client client) {
        super(client);
    }

    @Override
    public void run() {
        if (failCount < MAX_FAILS) {
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
                    messageScanner();
                    stop();
                } else {
                    System.err.println("Connection from IP: \"" + getSocket().getInetAddress() + "\" Does not match set IP: \"" + getIP() + "\" Trying Again...");
                    getSocket().close();
                    this.run();
                }
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    getSocket().close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                getClient().setConfirmed(false);
                failCount++;
                this.run();
            }
        } else {
            System.err.println("Too many failed connections!");
            try {
                stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void messageScanner() {
        String message;
        try {
            while ((message = receiveMessage()) != null) {
                if (!processMessage(message)) break;
            }
            System.err.println("Error message null!");
        } catch (EOFException e) {
            System.err.println("Client Disconnected Unexpectedly!");
            messageScanner();
            e.printStackTrace();
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

    public void recieveDataLoad(long size, String name) { //TODO
        try {
            int bytesRead;
            OutputStream output = new FileOutputStream(name);
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytesRead = getDataInputStream().read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendReady() throws IOException {
        sendMessage(new JSONConverter(PacketType.READY_RESPONSE).serialize(), getPort());
    }

    public void sendChoice(boolean b) throws IOException {
        JSONConverter json = new JSONConverter(PacketType.DATALOAD_REQUEST);
        json.set("dataLoadRequest", b);
        sendMessage(json.serialize(), getPort());
    }
}
