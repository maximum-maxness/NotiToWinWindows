package server.Networking;


import backend.Client;
import backend.PacketType;

import java.io.IOException;
import java.net.DatagramPacket;

public class ClientDiscoverer extends DiscoveryThread {
    public static ClientDiscoverer getInstance() {
        return ClientDiscovererHolder.INSTANCE;
    }

    @Override
    public void run() {
        running.set(true);
        try {
            initializeSocket();
            while (running.get()) {
                String packetMessage = receiveMessage();
                processMessage(packetMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean processMessage(String message) throws IOException {
        if (message.startsWith(PacketType.CLIENT_PAIR_REQUEST)) {
            String name = message.split(PacketType.CLIENT_PAIR_REQUEST)[1];
            Client client = new Client(getIP());
            client.setName(name);
            checkClientList(client);
            sendMessage(PacketType.SERVER_PAIR_RESPONSE, getPort());
        } else if (PacketType.CLIENT_PAIR_CONFIRM.equals(message)) {
            int index = findIndxClient(getCurrentPacket());
            if (index != 0) {
                Client client1 = clients.get(index - 1);
                if (!client1.isHasThread()) {
                    client1.setClientCommunicator(new ClientCommunicator(client1));
                    notiThreadPool.execute(client1.getClientCommunicator());
                    client1.setHasThread(true);
                }
            } else {
                System.err.println("Client is not Confirmed!");
            }
        }
        System.err.println("Packet: " + message + " is not recognized on this port.");
        return true;
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


    private static class ClientDiscovererHolder {
        private static final ClientDiscoverer INSTANCE = new ClientDiscoverer();
    }
}
