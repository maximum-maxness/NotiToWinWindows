package runner;

import backend.Client;
import backend.JSONConverter;
import backend.PreferenceHelper;
import server.networking.linkHandlers.LANLink;
import server.networking.linkHandlers.LANLinkProvider;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.Preferences;

public class BackgroundThread implements Runnable {
    private final ArrayList<LANLinkProvider> linkProviders = new ArrayList<>();
    private final ConcurrentHashMap<String, Integer> clientIndexMap = new ConcurrentHashMap<>();
    private final ArrayList<Client> clients = new ArrayList<>();
    private final ConcurrentHashMap<String, DeviceListChangedCallback> clientListChangedCallbacks =
            new ConcurrentHashMap<>();
    private final Client.PairingCallback devicePairingCallback =
            new Client.PairingCallback() {
                @Override
                public void incomingRequest(Client client) {
                    onClientListChanged();
                }

                @Override
                public void pairingSuccessful(Client client) {
                    onClientListChanged();
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
                    Client client;
                    try {
                        int index = clientIndexMap.get(clientID);
                        client = clients.get(index);
                    } catch (NullPointerException ignored) {
                        client = null;
                    }
                    if (client != null) {
                        client.addLink(identityPacket, link);
                    } else {
                        client = new Client(identityPacket, link);
                        if (client.isPaired()
                                || client.isPairRequested()
                                || client.isPairRequestedByPeer()
                                || link.linkShouldBeKeptAlive()) {
                            clients.add(client);
                            clientIndexMap.put(clientID, clientIndexMap.size());
                            client.addPairingCallback(devicePairingCallback);
                            Main.updateClientList(clients);
                        } else {
                            client.disconnect(); // TODO Implement decision making on whether to accept the pair or
                            // not, stop it from disconnecting regardless.
                        }
                    }
                    onClientListChanged();
                }

                @Override
                public void onConnectionLost(LANLink link) {
                    Client client = clients.get(clientIndexMap.get(link.getClientID()));
                    if (client != null) {
                        client.removeLink(link);
                        if (!client.isReachable() && !client.isPaired()) {
                            clients.remove(client);
                            client.removePairingCallback(devicePairingCallback);
                        }
                    }
                    onClientListChanged();
                }
            };


    public ArrayList<LANLinkProvider> getLinkProviders() {
        return linkProviders;
    }

    public ArrayList<Client> getClients() {
        return clients;
    }

    public Client getClient(int index) {
        return clients.get(index);
    }

    public void stop() {
        for (LANLinkProvider llp : linkProviders) {
            llp.onStop();
        }
    }

    public void cleanDevices() {
        new Thread(() -> {
            for (Client client : clients) {
                if (!client.isPaired() && !client.isPairRequested() && !client.isPairRequestedByPeer() && !client.deviceShouldBeKeptAlive()) {
                    client.disconnect();
                }
            }
        }).start();
    }

    private void registerLinkProviders() {
        if (linkProviders.isEmpty())
            linkProviders.add(new LANLinkProvider());
    }

    public Client getClient(String id) {
        return clients.get(clientIndexMap.get(id));
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

    private void loadSavedDevicesFromSettings() {
        Preferences trustStore = PreferenceHelper.getDeviceConfigStore();
        String[] trustedDevices = PreferenceHelper.getAllStrings(trustStore);
        for (String clientID : trustedDevices) {
            if (trustStore.getBoolean(clientID, false)) {
                Client client = new Client(clientID);
                clients.add(client);
                clientIndexMap.put(clientID, clientIndexMap.size());
                client.addPairingCallback(devicePairingCallback);
            }
        }
    }

    @Override
    public void run() {
        loadSavedDevicesFromSettings();
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
