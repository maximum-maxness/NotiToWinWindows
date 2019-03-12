package server;


import backend.Client;
import backend.PacketType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ClientDiscoverer extends DiscoveryThread {
    public static ClientDiscoverer getInstance() {
        return ClientDiscovererHolder.INSTANCE;
    }

    @Override
    public void run() {
        running.set(true);
        try {
            socket = new DatagramSocket(getPort(), getIP());
            socket.setBroadcast(true);

            while (running.get()) {
                byte[] receiveBuffer = new byte[15000];
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(receivePacket);
                this.receivedIP = receivePacket.getAddress();
                String packetMessage = new String(receivePacket.getData()).trim();

                switch (packetMessage) {
                    case PacketType.CLIENT_PAIR_REQUEST:
                        Client client = new Client(receivePacket.getAddress());
                        checkClientList(client);
                        sendMessage(PacketType.SERVER_PAIR_RESPONSE, getPort());
                        break;
                    case PacketType.CLIENT_PAIR_CONFIRM:
                        int index = findIndxClient(receivePacket);
                        if (index != 0) {
                            Client client1 = clients.get(index - 1);
                            client1.setConfirmed(true);
                            if (!client1.isHasThread()) {
                                ClientCommunicator cc = new ClientCommunicator(client1);
                                notiThreadPool.execute(cc);
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

    private int findIndxClient(DatagramPacket packet) {
        for (int count = 0; count < clients.size(); count++) {
            if (clients.get(count).getIp().equals(packet.getAddress())) //if the ip and port match up to a client
            {                                                           // on the client list, get the index of the
                return count + 1;                                       // client
            }
        }
        return 0;
    }

    public static class ClientDiscovererHolder {
        private static final ClientDiscoverer INSTANCE = new ClientDiscoverer();
    }
}
