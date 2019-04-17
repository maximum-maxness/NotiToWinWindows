package server.Networking;

import backend.Client;
import backend.JSONConverter;
import backend.PacketType;
import runner.Main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;

public class ClientDiscoverer extends DiscoveryThread {

  private final boolean showPrints = false;

  public static ClientDiscoverer getInstance() {
    return ClientDiscovererHolder.INSTANCE;
  }

  @Override
  public void run() {
    running.set(true);
    try {
      initializeSocket();
      while (running.get()) {
        try {
          String packetMessage = receiveMessage();
          if (showPrints) System.out.println("Got message: " + packetMessage);
          processMessage(packetMessage);
        } catch (SocketException e) {
          System.err.println("Socket Closed!");
          break;
        }
      }
      stop();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public boolean processMessage(String message) throws IOException {
    if (!message.startsWith("{")) return false;
    JSONConverter json = JSONConverter.unserialize(message);
    if (json.getType().equals(PacketType.CLIENT_PAIR_REQUEST)) {
      initialPair(json, true);
    } else if (json.getType().equals(PacketType.CLIENT_PAIR_CONFIRM)) {
      confirmPair(json);
    } else {
      System.err.println("Packet: " + message + " is not recognized on this port.");
    }
    return true;
  }

  private void confirmPair(JSONConverter json) throws IOException {
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
      initialPair(json, false);
      confirmPair(json);
    }
  }

  private void initialPair(JSONConverter json, boolean initial) throws IOException {
    Client client = new Client(getIP());
    if (initial) {
      String name = json.getMainBody().getString("deviceName");
      client.setName(name);
    } else {
      String name = "<no name>";
      client.setName(name);
    }
    checkClientList(client);
    if (initial) {
      JSONConverter readyJSON = PacketType.makeIdentityPacket();
      sendMessage(readyJSON.serialize(), getPort());
    }
  }

  private void checkClientList(Client client1) throws IOException {
    boolean b = false;
    for (Client client : clients) {
      if (client.getIp().equals(client1.getIp())) { // Check if the client has already
        b = true; // been added to the list
      }
    }
    if (!b) {
      clients.add(client1); // if the client isn't isn't on the list, add it
      Main.updateClientList(this.clients);
    }
    if (showPrints) System.out.println("Client is on list? " + b);
  }

  private int findIndxClient(DatagramPacket packet) {
    for (int count = 0; count < clients.size(); count++) {
      if (clients
          .get(count)
          .getIp()
          .equals(packet.getAddress())) // if the ip and port match up to a client
      { // on the client list, get the index of the
        return count + 1; // client
      }
    }
    return 0;
  }

  private static class ClientDiscovererHolder {
    private static final ClientDiscoverer INSTANCE = new ClientDiscoverer();
  }
}
