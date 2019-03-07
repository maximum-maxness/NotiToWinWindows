package server;

import backend.Client;
import backend.PacketType;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DiscoveryThread implements Runnable{

    final static int port = 8657;
    final AtomicBoolean running = new AtomicBoolean(false);
    public static ArrayList<Client> clients = new ArrayList<>();
    public static Executor notiThreadPool = Executors.newWorkStealingPool();
    DatagramSocket socket;
    InetAddress receivedIP;


    @Override
    public void run() {
        running.set(true);
        try{
            socket = new DatagramSocket((8657, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);

            while(running.get()){
                byte[] receiveBuffer = new byte[15000];
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(receivePacket);
                this.receivedIP = receivePacket.getAddress();
                String packetMessage = new String(receivePacket.getData()).trim();

                switch(packetMessage){
                    case PacketType.CLIENT_PAIR_REQUEST:
                        Client client = new Client(receivePacket.getAddress());
                        checkClientList(client);
                        sendMessage(PacketType.SERVER_PAIR_RESPONSE, port);
                        break;
                    case PacketType.CLIENT_PAIR_CONFIRM:
                        int index = findIndxClient(receivePacket);
                        if(index != 0){
                            Client client1 = clients.get(index - 1);
                            client1.setConfirmed(true);
                            if(!client1.isHasThread()){
                                CommunicationThread ct = new CommunicationThread(client1);
                                notiThreadPool.execute(ct);
                                client1.setHasThread(true);
                            }
                        } else {
                            System.err.println("Client is not Confirmed!");
                        }
                        break;
                    default:
                        System.err.println("Packet: " + packetMessage + " is not recognized on this port.");

                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkClientList(Client client1) throws IOException {
        boolean b = false;
        for (Client client : clients) {
            if (client.getIp().equals(client1.getIp())) { //Check if the client has already
                b = true;                                       //been added to the list
            }
        }
        if (!b) {
            clients.add(client1); //if the client isn't isn't on the list, add it
        }
        System.out.println("Client is on list? " + b);
    }

    private void sendMessage(String packetType, int port1) throws IOException {
        byte[] sendData = packetType.getBytes(); //Turn the string into a byte array
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, this.receivedIP, port1);
        socket.send(packet); //put into a packet and send
        System.out.println("Sent: " + packetType);
    }

    private int findIndxClient(DatagramPacket packet) {
        for (int count = 0; count < clients.size(); count++) {
            if (clients.get(count).getIp().equals(packet.getAddress())) //if the ip and port match up to a client
            {                                                           // on the client list, get the index of the
                return count + 1;                                       // client
            }
        }
        return 0;
    }
}