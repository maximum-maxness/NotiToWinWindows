package runner;

import backend.Client;
import backend.JSONConverter;
import server.networking.linkHandlers.LANLink;
import server.networking.linkHandlers.LANLinkProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class BackgroundThread implements Runnable {
  private final ArrayList<LANLinkProvider> linkProviders = new ArrayList<>();
  private final ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<>();
  private  final ConcurrentHashMap<Integer, String> clientIndexMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, DeviceListChangedCallback> clientListChangedCallbacks =
      new ConcurrentHashMap<>();
  private final Client.PairingCallback devicePairingCallback =
      new Client.PairingCallback() {
        @Override
        public void incomingRequest() {
//          for (Client client : clients.values()) {
//            if (client.requestThread != null) {
//              client.requestThread.interrupt();
//            }
//          }
//          String decision = runner.Main.getDecision("Accept Pairing? Y/N");
//          if (decision.equals("Y")) {
//            for (Client client : clients.values()) {
//              client.acceptPairing();
//            }
//          }
        }

        @Override
        public void pairingSuccessful() {
//          String decision = runner.Main.getDecision("Choose A message to send!");
//          JSONConverter json = new JSONConverter(PacketType.NOTIFICATION);
//          json.put("message", decision);
//          for (Client client : clients.values()) {
//            client.sendPacket(json);
//          }
        }

        @Override
        public void pairingFailed(String error) {
          onClientListChanged();
        }

        @Override
        public void unpaired() {
          onClientListChanged();
        }
      };
  private final LANLinkProvider.ConnectionReceiver deviceListener =
      new LANLinkProvider.ConnectionReceiver() {
        @Override
        public void onConnectionReceived(JSONConverter identityPacket, LANLink link) {
          String clientID = identityPacket.getString("clientID");
          Client client = clients.get(clientID);
          if (client != null) {
            client.addLink(identityPacket, link);
          } else {
            client = new Client(identityPacket, link);
            if (client.isPaired()
                || client.isPairRequested()
                || client.isPairRequestedByPeer()
                || link.linkShouldBeKeptAlive()) {
              clients.put(clientID, client);
              clientIndexMap.put(clientIndexMap.size(), clientID);
              client.addPairingCallback(devicePairingCallback);
            } else {
              client
                  .disconnect(); // TODO Implement decision making on whether to accept the pair or
                                 // not, stop it from disconnecting regardless.
            }
          }

          onClientListChanged();
        }

        @Override
        public void onConnectionLost(LANLink link) {
          Client client = clients.get(link.getClientID());
          if (client != null) {
            client.removeLink(link);
            if (!client.isReachable() && !client.isPaired()) {
              clients.remove(link.getClientID());
              client.removePairingCallback(devicePairingCallback);
            }
          }
          onClientListChanged();
        }
      };

  public ArrayList<LANLinkProvider> getLinkProviders() {
    return linkProviders;
  }

  public ConcurrentHashMap<String, Client> getClientsHash() {
    return clients;
  }

  public Collection<Client> getClients() {
    return clients.values();
  }

  public Client getClient(int index){
    return clients.get(clientIndexMap.get(index));
  }

  public void stop() {
    for (LANLinkProvider llp : linkProviders) {
      llp.onStop();
    }
  }

  private void registerLinkProviders() {
    linkProviders.add(new LANLinkProvider());
  }

  public Client getClient(String id) {
    return clients.get(id);
  }

  private void onClientListChanged() {
    for (DeviceListChangedCallback callback : clientListChangedCallbacks.values()) {
      callback.onDeviceListChanged();
    }
  }

  public void addConnectionListener(LANLinkProvider.ConnectionReceiver cr) {
    for (LANLinkProvider llp : linkProviders) {
      llp.addConnectionReceiver(cr);
    }
  }

  public void removeConnectionListener(LANLinkProvider.ConnectionReceiver cr) {
    for (LANLinkProvider llp : linkProviders) {
      llp.removeConnectionReceiver(cr);
    }
  }

  public void addClientListChangedCallback(String key, DeviceListChangedCallback callback) {
    clientListChangedCallbacks.put(key, callback);
  }

  public void removeClientListChangedCallback(String key) {
    clientListChangedCallbacks.remove(key);
  }

  @Override
  public void run() {
    registerLinkProviders();
    addConnectionListener(deviceListener);
    for (LANLinkProvider llp : linkProviders) {
      llp.onStart();
    }
  }

  public interface DeviceListChangedCallback {
    void onDeviceListChanged();
  }

  public interface InstanceCallback {
    void onServiceStart(runner.Main service);
  }
}
