package server.old;

import backend.Client;
import backend.JSONConverter;
import backend.Notification;
import backend.PacketType;
import java.io.IOException;
import java.net.*;

public class ClientConnector implements Runnable {
    private InetAddress ip;
    private int port;
    private Client client;
    private DatagramSocket socket;
    private boolean firstRun = true;

    public ClientConnector(Client client, DatagramSocket socket) {
        this.client = client;
        this.ip = client.getIp();
        this.socket = socket;
        System.out.println("Client connector has been created!");
    }

    @Override
    public void run() {
        if ((this.port != 0) && (this.ip != null)) {
            System.out.println("ClientConnector is running!");
            while (true) {
                try {
                    if (firstRun) {
                        sendReady();
                        firstRun = false;
                    }
                    String message = recievePacket();

                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    private void sendReady() throws IOException {
        byte[] sendData = PacketType.READY_RESPONSE.getBytes();
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, ip, port);
        socket.send(packet);
        System.out.println("Sent Ready!");
    }

    private String recievePacket() throws IOException {
        byte[] recieveBuff = new byte[15000];
        DatagramPacket packet = new DatagramPacket(recieveBuff, recieveBuff.length, ip, port);
        socket.receive(packet);
        return new String(packet.getData()).trim();
    }

}
