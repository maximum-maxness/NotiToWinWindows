package server;

import controller.Client;
import controller.JSONConverter;
import controller.Notification;
import controller.PacketType;
import java.io.IOException;
import java.net.*;

public class ClientConnector implements Runnable {
    private InetAddress ip;
    private int port;
    private Client client;
    private DatagramSocket socket;
    private boolean firstRun = true;

    public ClientConnector(Client client, DatagramSocket socket) throws UnknownHostException, SocketException {
        this.client = client;
        this.port = client.getPort();
        this.ip = client.getIp();
        this.socket = socket;
        this.socket.setBroadcast(true);
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
                    switch(message){
                        case PacketType.NOTI_REQUEST:
                            System.out.println("Noti Request!");
                            Thread.sleep(100);
                            sendReady();
                            sendReady();

                            sendReady();

                            break;
                        case PacketType.UNPAIR_CMD:
                            System.out.println("Unpair Command!");
                            DiscoveryThread.notifications.clear();
                            DiscoveryThread.clients.remove(client);
                            Thread.currentThread().interrupt();
                            break;
                        default:
                            if(message.endsWith("}")){
                                processJson(message);
                                sendReady();
                            } else {
                                System.err.println("Packet: " + message + " is invalid.");
                            }
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processJson(String jsonString){
        System.out.println("JSON Detected!");
        JSONConverter json = JSONConverter.unserialize(jsonString);
        if(json.getType().equals(PacketType.NOTI_REQUEST)){
            System.out.println("JSON Type is Noti Request!");
            Notification noti = Notification.jsonToNoti(json);
            DiscoveryThread.notifications.add(noti);
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
