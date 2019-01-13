package controller;

import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

class DiscoveryThread implements Runnable {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final String className = getClass().getName();
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
                    int count;
                    for (count = 0; count < clients.size(); count++) {
                        if (clients.get(count).getIp().equals(packet.getAddress())
                                && clients.get(count).getPort() == packet.getPort()) {
                            break;
                        }
                    }
                    while (true) {
                        socket.receive(packet);
                        message = new String(packet.getData());
                        message = message.trim();
                        JSONConverter json = JSONConverter.unserialize(message);
                        if (json.getType().equals(PacketType.NOTI_REQUEST)) {
                            Notification noti = new Notification();
                            JSONObject jsonOb = json.getJSONObject("body");
                            Iterator<String> iter = jsonOb.keys();
                            while (iter.hasNext()) {
                                String key = iter.next();
                                switch (key) {
                                    case "id":
                                        noti.setId((String) jsonOb.get(key));
                                        break;
                                    case "isClearable":
                                        noti.setClearable((boolean) jsonOb.get(key));
                                        break;
                                    case "appName":
                                        noti.setAppName((String) jsonOb.get(key));
                                        break;
                                    case "time":
                                        noti.setTimeStamp((String) jsonOb.get(key));
                                        break;
                                    case "title":
                                        noti.setTitle((String) jsonOb.get(key));
                                        break;
                                    case "text":
                                        noti.setText((String) jsonOb.get(key));
                                        break;
                                    case "isRepliable":
                                        noti.setRepliable((boolean) jsonOb.get(key));
                                        break;
                                    case "requestReplyId":
                                        noti.setRequestReplyId((String) jsonOb.get(key));
                                        break;
                                    case "hasDataLoad":
                                        noti.setHasDataLoad((boolean) jsonOb.get(key));
                                        break;
                                    case "dataLoadHash":
                                        noti.setDataLoadHash((String) jsonOb.get(key));
                                        break;
                                    default:
                                        System.err.println("Key: \"" + key + "\" isn't a notification key.");
                                }
                                notifications.add(noti);
                            }
                        } else if (json.getType().equals(PacketType.UNPAIR_CMD)) {
                            notifications.clear();
                            clients.clear();
                            break;
                        }
                    }
                }

            }
            System.out.println("about to close socket");
        } catch (IOException e) {
            System.out.println("Socket Closed");
        }
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
