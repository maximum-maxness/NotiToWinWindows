package controller;

import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

class DiscoveryThread implements Runnable { //TODO Restructure entirely

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final String className = getClass().getName();
    private int count;
    public static ArrayList<Client> clients = new ArrayList<>();
    public static ArrayList<Notification> notifications = new ArrayList<>();
    private DatagramSocket socket;

    static DiscoveryThread getInstance() {
        return DiscoveryThreadHolder.INSTANCE;
    }

    @Override
    public void run() {
        System.out.println("Server Thread Started!");
        running.set(true);
        try {
            socket = new DatagramSocket(8657, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);

            while (running.get()) {
                System.out.println(className + ":::   Ready to receive packet!");
//                Main.updateServerConnectionStatus(true);
                byte[] receiverBuffer = new byte[15000];
                DatagramPacket packet = new DatagramPacket(receiverBuffer, receiverBuffer.length);
                socket.receive(packet);
                System.out.println("Whats this");

                System.out.println(className + ":::   Packet received from: " + packet.getAddress().getHostAddress());
                String message = new String(packet.getData());
                System.out.println(className + ":::   Packet Data: " + message);
                message = message.trim();
                if (message.equals(PacketType.CLIENT_PAIR_REQUEST)) {
                    byte[] sendData = PacketType.SERVER_PAIR_RESPONSE.getBytes();
                    Client client1 = new Client(packet.getAddress(), packet.getPort());
                    boolean b = false;
                    for (Client client : clients) {
                        if (client.getIp().equals(client1.getIp())
                                && client.getPort() == client1.getPort()) {
                            b = true;
                        }
                    }
                    if (!b) {
                        clients.add(client1);
                    }
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                    socket.send(sendPacket);
                    System.out.println(className + ":::   Packet Sent to: " + sendPacket.getAddress().getHostAddress());
                } else if (message.equals(PacketType.CLIENT_PAIR_CONFIRM)) {
                    for (count = 0; count < clients.size(); count++) {
                        if (clients.get(count).getIp().equals(packet.getAddress())
                                && clients.get(count).getPort() == packet.getPort()) {
                            break;
                        }
                    }
                    InetAddress ip = clients.get(count).getIp();
                    int port = clients.get(count).getPort();
                    while (true) {
                        message = recievePacket(ip, port);
                        System.out.println(message);
                        if (message.equals(PacketType.NOTI_REQUEST)) {
                            sendReady(ip, port);
                            message = recievePacket(ip, port);
                            System.out.println("2: " + message);
                            if (message.endsWith("}")) {
                                JSONConverter json = JSONConverter.unserialize(message);
                                if (json.getType().equals(PacketType.NOTI_REQUEST)) {
                                    Notification noti = Notification.jsonToNoti(json);
                                    notifications.add(noti);
                                } else if (json.getType().equals(PacketType.UNPAIR_CMD)) {
                                    notifications.clear();
                                    clients.clear();
                                    break;
                                }
                            } else {
                                sendReady(packet.getAddress(), packet.getPort());
                            }
                        } else {
                            System.out.println("Nope");
                        }
                    }
                }

            }
            System.out.println("about to close socket");
        } catch (IOException e) {
            System.out.println("Socket Closed");
        }
    }


    private String recievePacket(InetAddress ip, int port) throws IOException {
        byte[] recieveBuff = new byte[15000];
        DatagramPacket packet = new DatagramPacket(recieveBuff, recieveBuff.length, ip, port);
        socket.receive(packet);
        return new String(packet.getData()).trim();
    }

    private void sendReady(InetAddress ip, int port) throws IOException {
        byte[] sendData = PacketType.READY_RESPONSE.getBytes();
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, ip, port);
        socket.send(packet);
        System.out.println("Sent Ready!");
    }

    public void sendReadyPacket() throws IOException {

        byte[] sendData = PacketType.READY_RESPONSE.getBytes();
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, clients.get(count).getIp(), clients.get(count).getPort());
        socket.send(packet);
        System.out.println("Sent Ready!");
    }

    void stop() {
        running.set(false);
        socket.close();
        //   Main.updateServerConnectionStatus(false);
    }

    private static class DiscoveryThreadHolder {
        private static final DiscoveryThread INSTANCE = new DiscoveryThread();
    }

}
