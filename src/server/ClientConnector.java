package server;

import controller.Client;
import controller.JSONConverter;
import controller.Notification;
import controller.PacketType;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;

import static server.NewDiscoveryThread.clients;
import static server.NewDiscoveryThread.notifications;

public class ClientConnector implements Runnable {
    private InetAddress ip;
    private int port;
    private Client client;
    private DatagramSocket socket;
    private boolean firstRun = true;

    public ClientConnector(Client client) throws UnknownHostException, SocketException {
        this.client = client;
        this.port = client.getPort();
        this.ip = client.getIp();
        this.socket = new DatagramSocket(8657, InetAddress.getByName("0.0.0.0"));
        this.socket.setBroadcast(true);
    }

    @Override
    public void run() {
        if ((this.port != 0) && (this.ip != null)) {
            while (true) {
                try {
                    if (firstRun) {
                        sendReady();
                        firstRun = false;
                    }
                    String message = recievePacket();
                    switch(message){
                        case PacketType.NOTI_REQUEST:
                            sendReady();
                            break;
                        case PacketType.UNPAIR_CMD:
                            notifications.clear();
                            clients.remove(client);
                            Thread.currentThread().interrupt();
                            break;
                        default:
                            if(message.endsWith("}")){
                                processJson(message);
                            } else {
                                System.err.println("Packet: " + message + " is invalid.");
                            }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processJson(String jsonString){
        JSONConverter json = JSONConverter.unserialize(jsonString);
        if(json.getType().equals(PacketType.NOTI_REQUEST)){
            Notification noti = Notification.jsonToNoti(json);
            notifications.add(noti);
        } else {
            System.err.println("Json type: " + json.getType() + "is unrecognized.");
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
