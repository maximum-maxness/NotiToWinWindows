package server;

import controller.Client;
import controller.Notification;
import controller.PacketType;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class NewDiscoveryThread implements Runnable {

    final AtomicBoolean running = new AtomicBoolean(false);
    Executor notiThreadPool = Executors.newWorkStealingPool();
    DatagramSocket socket;
    InetAddress recievedIP;
    int recievedPort;
    public static ArrayList<Client> clients = new ArrayList<>();
    public static ArrayList<Notification> notifications = new ArrayList<>();


    @Override
    public void run() {
        running.set(true);
        try {
            socket = new DatagramSocket(8657, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);

            while (running.get()) {
                byte[] receiveBuffer = new byte[15000];
                DatagramPacket receivedPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(receivedPacket); //Wait to receive a packet from a potential client...
                this.recievedIP = receivedPacket.getAddress();
                this.recievedPort = receivedPacket.getPort();


                String receivedPacketMessage = new String(receivedPacket.getData());
                receivedPacketMessage = receivedPacketMessage.trim();
                switch (receivedPacketMessage) {
                    case PacketType.CLIENT_PAIR_REQUEST: //Client Initially Requests to Pair
                        Client client1 = new Client(receivedPacket.getAddress(), receivedPacket.getPort());
                        checkClientList(client1); //See if client is already on the list, if not add it
                        sendMessage(PacketType.SERVER_PAIR_RESPONSE); //Respond to client
                        break;
                    case PacketType.CLIENT_PAIR_CONFIRM: //Client Confirms it Wants to Pair
                        int index = findIndxClient(receivedPacket);
                        if(index != 0){
                            clients.get(index - 1).setConfirmed(true);
                        }
                        break;
                    case PacketType.NOTI_REQUEST: //Client is Wanting to Send a Notification
                        Client client = findClientFromPacket(receivedPacket);
                        if(client != null) {
                            if (client.isConfirmed()){ //Check if client has gone through the pairing process
                                ClientConnector cc = new ClientConnector(client, socket);
                                notiThreadPool.execute(cc); //create a new thread listening to notifications
                            } else {
                                System.err.println("Hasn't gone through pairing process yet.");
                            }
                        } else {
                            System.err.println("Client hasn't connected before...");
                        }
                        break;
                    case PacketType.UNPAIR_CMD: //client wants to unpair
                        notifications.clear(); //clear notifications
                        Client client2 = findClientFromPacket(receivedPacket); //find the client in the list
                        if(client2 != null) {
                            clients.remove(client2); //remove the client from the list
                        } else {
                            System.err.println("Client hasn't connected before, nothing to do.");
                        }
                        break;
                    default: //Other message contained within packet
                        System.err.println("Packet: " + receivedPacketMessage + " is not recognized.");
                }
            }
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String packetType) throws IOException {
        byte[] sendData = packetType.getBytes();
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, this.recievedIP, this.recievedPort);
        socket.send(packet);
    }

    private void checkClientList(Client client1) throws IOException {
        boolean b = false;
        for (Client client : clients) {
            if (client.getIp().equals(client1.getIp())
                    && client.getPort() == client1.getPort()) { //Check if the client has already
                b = true;                                       //been added to the list
            }
        }
        if (!b) {
            clients.add(client1); //if the client isn't isn't on the list, add it
        }
    }

    private Client findClientFromPacket(DatagramPacket packet){
        int index = findIndxClient(packet);
        if(index != 0) return clients.get(index - 1);
        else return null;
    }

    private int findIndxClient(DatagramPacket packet) {
        for (int count = 0; count < clients.size(); count++) {
            if (clients.get(count).getIp().equals(packet.getAddress())
                    && clients.get(count).getPort() == packet.getPort()) {
                return count + 1;
            }
        }
        return 0;
    }

    static NewDiscoveryThread getInstance() {
        return DiscoveryThreadHolder.INSTANCE;
    }

    private static class DiscoveryThreadHolder {
        private static final NewDiscoveryThread INSTANCE = new NewDiscoveryThread();
    }
}
