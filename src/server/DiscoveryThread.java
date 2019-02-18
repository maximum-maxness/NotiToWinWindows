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

public class DiscoveryThread implements Runnable {

    final AtomicBoolean running = new AtomicBoolean(false);
    public static Executor notiThreadPool = Executors.newWorkStealingPool();
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
                System.out.println("Waiting for packet...");
                socket.receive(receivedPacket); //Wait to receive a packet from a potential client...
                System.out.println("Received Packet!");
                this.recievedIP = receivedPacket.getAddress();
                this.recievedPort = receivedPacket.getPort();


                String receivedPacketMessage = new String(receivedPacket.getData());
                receivedPacketMessage = receivedPacketMessage.trim();
                System.out.println("Packet Message: " + receivedPacketMessage);
                switch (receivedPacketMessage) {
                    case PacketType.CLIENT_PAIR_REQUEST: //Client Initially Requests to Pair
                        System.out.println("Client Pair Request!");
                        Client client1 = new Client(receivedPacket.getAddress(), receivedPacket.getPort());
                        checkClientList(client1); //See if client is already on the list, if not add it
                        sendMessage(PacketType.SERVER_PAIR_RESPONSE); //Respond to client
                        break;
                    case PacketType.CLIENT_PAIR_CONFIRM: //Client Confirms it Wants to Pair
                        System.out.println("Client Pair Confirm!");
                        int index = findIndxClient(receivedPacket);
                        if(index != 0){
                            clients.get(index - 1).setConfirmed(true);
                        }
                        break;
                    case PacketType.NOTI_REQUEST: //Client is Wanting to Send a Notification
                        System.out.println("Noti Request!");
                        Client client = findClientFromPacket(receivedPacket);
                        if(client != null) {
                            if (client.isConfirmed() && !(client.isHasThread())){ //Check if client has gone through the pairing process
                                ClientConnector cc = new ClientConnector(client, socket);
                                notiThreadPool.execute(cc); //create a new thread listening to notifications
                                client.setHasThread(true);
                            } else {
                                System.err.println("Hasn't gone through pairing process yet, or already has thread.");
                            }
                        } else {
                            System.err.println("Client hasn't connected before...");
                        }
                        break;
                    case PacketType.UNPAIR_CMD: //client wants to unpair
                        System.out.println("Unpair Command!");
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
        byte[] sendData = packetType.getBytes(); //Turn the string into a byte array
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, this.recievedIP, this.recievedPort);
        socket.send(packet); //put into a packet and send
        System.out.println("Sent: " + packetType);
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
        System.out.println("Client is on list? " + b);
    }

    private Client findClientFromPacket(DatagramPacket packet){
        int index = findIndxClient(packet);
        if(index != 0) return clients.get(index - 1);
        else return null;
    }

    private int findIndxClient(DatagramPacket packet) {
        for (int count = 0; count < clients.size(); count++) {
            if (clients.get(count).getIp().equals(packet.getAddress()) //if the ip and port match up to a client
                    && clients.get(count).getPort() == packet.getPort()) { // on the client list, get the index of the
                return count + 1;                                       // client
            }
        }
        return 0;
    }

    public void stop() {

        running.set(false); //stop the while loop
        socket.close(); // close the socket, causing a connection exception
        //   Main.updateServerConnectionStatus(false);
    }

    public static DiscoveryThread getInstance() {
        return DiscoveryThreadHolder.INSTANCE;
    }

    private static class DiscoveryThreadHolder {
        private static final DiscoveryThread INSTANCE = new DiscoveryThread();
    }
}
