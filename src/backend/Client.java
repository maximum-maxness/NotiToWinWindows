package backend;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import server.networking.linkHandlers.LANLink;
import server.networking.linkHandlers.LANLinkHandler;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static backend.PreferenceHelper.*;

public class Client implements LANLink.PacketReceiver {

    private final CopyOnWriteArrayList<LANLink> links = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<PairingCallback> pairingCallback = new CopyOnWriteArrayList<>();
    private final Map<String, LANLinkHandler> pairingHandlers = new HashMap<>();
    private final SendPacketStatusCallback defaultCallback =
            new SendPacketStatusCallback() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onFailure(Throwable e) {
                    e.printStackTrace();
                }
            };
    private final String clientID;
    public PublicKey publicKey;
    private final Preferences settings;
    public Certificate certificate;

    public String getName() {
        return name.get();
    }

    private SimpleStringProperty name;
    private SimpleObjectProperty<PairStatus> pairStatus;
    private SimpleBooleanProperty trusted;
    private String osName, osVersion;
    private List<Notification> notifications = new ArrayList<>();

    public Client(JSONConverter json, LANLink link) {
        this.clientID = json.getString("clientID");
        this.name = new SimpleStringProperty("Unknown");
        this.pairStatus = new SimpleObjectProperty<>(PairStatus.NotPaired);
        this.publicKey = null;
        settings = PreferenceHelper.getDeviceConfigNode(clientID);
        trusted = new SimpleBooleanProperty(false);
        addLink(json, link);
    }

    public Client(String clientID) {
        this.clientID = clientID;
        this.pairStatus = new SimpleObjectProperty<>(PairStatus.Paired);
        settings = PreferenceHelper.getDeviceConfigNode(clientID);
        name = new SimpleStringProperty(settings.get("clientName", "Unknown"));
        osName = settings.get("osName", "Unknown");
        osVersion = settings.get("osVer", "xx");
        trusted = new SimpleBooleanProperty(true);
    }

    public PairStatus getPairStatus() {
        return pairStatus.get();
    }

    public SimpleBooleanProperty trustedProperty() {
        return trusted;
    }

    public SimpleObjectProperty<PairStatus> pairStatusProperty() {
        return pairStatus;
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    @Override
    public void onPacketReceived(JSONConverter json) {
        System.out.println("Received Packet of Type: " + json.getType());
        if (PacketType.PAIR_REQUEST.equals(json.getType())) {
            System.out.println("Pair Packet!");
            for (LANLinkHandler llh : pairingHandlers.values()) {
                try {
                    llh.packageReceived(json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (isPaired()) {
            if (PacketType.NOTIFICATION.equals(json.getType())) {
                System.out.println("Notification Packet!");
                Notification notification = Notification.jsonToNoti(json, getClientID());
                addNoti(notification);
            } else {
                System.err.println("No.");
            }

            //Case while paired, will implement later.
        } else {
            unpair();
        }
    }

    public String getClientID() {
        return this.clientID;
    }

    public boolean isPaired() {
        return this.pairStatus.get() == PairStatus.Paired;
    }

    public boolean isReachable() {
        return !links.isEmpty();
    }

    public boolean isConnected() {
        boolean b = false;
        for (LANLink link : links) {
            System.out.println("Link is connected?" + (b = link.isConnected()));
        }
        return b;
    }

    public void acceptPairing() {
        for (LANLinkHandler llh : pairingHandlers.values()) {
            llh.acceptPairing();
        }
    }

    public void requestPairing() {
        System.out.println("Requesting to pair to " + name.getName());

        if (isPaired()) {
            for (PairingCallback cb : pairingCallback) {
                cb.pairingFailed("Already Paired!");
            }
            return;
        }

        if (!isReachable()) {
            for (PairingCallback cb : pairingCallback) {
                cb.pairingFailed("Not Reachable.");
            }
            return;
        }

        for (LANLinkHandler llh : pairingHandlers.values()) {
            llh.requestPairing();
        }

    }

    public void unpair() {

        for (LANLinkHandler llh : pairingHandlers.values()) {
            llh.unpair();
        }
        unpairInternal(); // Even if there are no pairing handlers, unpair
    }

    public void disconnect() {
        for (LANLink link : links) {
            link.disconnect();
        }
    }

    public boolean deviceShouldBeKeptAlive() {
        if (PreferenceHelper.contains(getTrustedDeviceNode(), getClientID(), false)) return true;
        for (LANLink l : links) {
            if (l.linkShouldBeKeptAlive()) {
                return true;
            }
        }
        return false;
    }


    public void addLink(JSONConverter identityPacket, LANLink link) {
        System.out.println("Adding Link to Client ID: " + clientID);
        if (identityPacket.has("clientName")) {
            this.name.set(identityPacket.getString("clientName"));
            settings.put("clientName", this.name.getValue());
        }
        if (identityPacket.has("certificate")) {
            System.out.println("Packet has a certificate!");
            String certificateString = identityPacket.getString("certificate");
            try {
                System.out.println("Parsing Certificate...");
                byte[] certificateBytes = Base64.getDecoder().decode(certificateString);
                certificate = SSLHelper.parseCertificate(certificateBytes);
            } catch (Exception e) {
                System.err.println("Error Parsing Certificate.");
                e.printStackTrace();
            }
        }

        links.add(link);

        try {
            System.out.println("Setting Private Key..");
            PrivateKey privateKey = RSAHelper.getPrivateKey();
            link.setPrivateKey(privateKey);
            System.out.println("Set Private Key Successfully!");
        } catch (Exception e) {
            System.err.println("Error setting private key!");
            e.printStackTrace();
        }

        if (!pairingHandlers.containsKey(link.getName())) {
            LANLinkHandler.PairingHandlerCallback callback =
                    new LANLinkHandler.PairingHandlerCallback() {
                        @Override
                        public void incomingRequest() {
                            for (PairingCallback pb : pairingCallback) {
                                pb.incomingRequest(Client.this);
                            }
                        }

                        @Override
                        public void pairingDone() {
                            Client.this.pairingDone();
                        }

                        @Override
                        public void pairingFailed(String error) {
                            for (PairingCallback pb : pairingCallback) {
                                pb.pairingFailed(error);
                            }
                        }

                        @Override
                        public void unpaired() {
                            unpairInternal();
                        }
                    };
            pairingHandlers.put(link.getName(), link.getPairingHandler(this, callback));
        }
        link.addPacketReceiver(this);
    }

    public void removeLink(LANLink link) {
        System.out.println("Removing link: " + link.getName());
        boolean linkExists = false;
        for (LANLink llink : links) {
            if (llink.getName().equals(link.getName())) {
                linkExists = true;
                break;
            }
        }
        if (!linkExists) {
            pairingHandlers.remove(link.getName());
        }

        link.removePacketReceiver(this);
        links.remove(link);
    }

    private void pairingDone() {
        System.out.println("Pairing was a success!!!");
        pairStatus.set(PairStatus.Paired);
        Preferences trustStore = getTrustedDeviceNode();
        trustStore.putBoolean(clientID, true);
        applyChanges(trustStore);

        trusted.set(true);
        Preferences deviceStore = getDeviceConfigNode(clientID);
        deviceStore.put("clientName", name.getValue());
        applyChanges(deviceStore);
        for (PairingCallback pb : pairingCallback) {
            pb.pairingSuccessful(this);
        }
    }

    private void unpairInternal() {
        System.out.println("Forcing an Unpair..");
        pairStatus.set(PairStatus.NotPaired);

        Preferences trustStore = getTrustedDeviceNode();
        trustStore.remove(clientID);
        applyChanges(trustStore);

        Preferences deviceStore = getDeviceConfigNode(clientID);
        try {
            deviceStore.clear();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
        applyChanges(deviceStore);

        for (PairingCallback pb : pairingCallback) {
            pb.unpaired();
        }
    }

    public void sendPacket(JSONConverter json) {
        sendPacket(json, defaultCallback);
    }

    public void sendPacket(final JSONConverter json, final SendPacketStatusCallback callback) {
        new Thread(() -> sendPacketBlocking(json, callback)).start();
    }

    public boolean sendPacketBlocking(final JSONConverter json, final SendPacketStatusCallback callback) {
        System.out.println("Sending Packet to all Applicable Links!");
        boolean shouldUseEncryption = (!json.getType().equals(PacketType.PAIR_REQUEST) && isPaired());
        boolean success = false;
        for (final LANLink link : links) {
            if (link == null) continue;
            if (shouldUseEncryption) {
                success = link.sendPacket(json, callback, publicKey);
            } else {
                success = link.sendPacket(json, callback);
            }
            if (success) break;
        }
        System.out.println("Packet send success? " + success);
        return success;
    }

    public boolean isPairRequested() {
        boolean pairRequested = false;
        for (LANLinkHandler llh : pairingHandlers.values()) {
            pairRequested = pairRequested || llh.isPairRequested();
        }
        System.out.println("Is pair Requested? " + pairRequested);
        return pairRequested;
    }

    public boolean isPairRequestedByPeer() {
        boolean pairRequestedByPeer = false;
        boolean paired = false;
        for (LANLinkHandler llh : pairingHandlers.values()) {
            pairRequestedByPeer = pairRequestedByPeer || llh.isPairRequestedByPeer();
            paired = paired || llh.isPaired();
        }
        System.out.println("Is pair requested by peer? " + pairRequestedByPeer + ", is Paired? " + paired);
        return pairRequestedByPeer;
    }

    public void addPairingCallback(PairingCallback callback) {
        pairingCallback.add(callback);
    }

    public void removePairingCallback(PairingCallback callback) {
        pairingCallback.remove(callback);
    }

    public String getOsName() {
        return osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public enum PairStatus {
        NotPaired,
        Paired
    }

    @Override
    public String toString() {
        StringBuilder returnString = new StringBuilder();
        returnString.append("Client Name: ").append(getName()).append("\n");
        returnString.append("Client ID: ").append(getClientID()).append("\n");
        returnString.append("Pair Status: ").append(getPairStatus()).append("\n");
        returnString.append("Links: ").append("\n");
        for (LANLink link : links) {
            returnString.append("  - Name: ").append(link.getName()).append("\n    ");
            returnString.append("  Connected: ").append(link.isConnected()).append("\n");
        }
        returnString.append("Client Notifications:").append("\n");
        for (Notification noti : this.notifications) {
            if (noti != null) returnString.append("  ").append(noti.toString()).append("\n");
        }
        return returnString.toString();
    }

    public abstract static class SendPacketStatusCallback {
        public abstract void onSuccess();

        public abstract void onFailure(Throwable e);

        public void onProgressChanged(int percent) {
        }
    }

    public Notification[] getNotifications() {
        return (Notification[]) notifications.toArray();
    }

    public void setNotifications(Notification[] notifications) {
        this.notifications.clear();
        this.notifications.addAll(Arrays.asList(notifications));
    }

    public List<Notification> getNotificationList() {
        return notifications;
    }

    public void addNoti(Notification noti) {
        boolean match = false;
        for (Notification notification : notifications) {
            if (notification.getId().equals(noti.getId())
                    && notification.getText().equals(noti.getText())) {
                match = true;
                noti.setIcon(notification.getIcon());
            }
        }
        if (!match) {
            notifications.add(noti);
        } else {
            System.out.println("Already Have that Notification!");
        }
        Runnable r = noti::display;
        Platform.runLater(r);
    }

    public interface PairingCallback {
        void incomingRequest(Client client);

        void pairingSuccessful(Client client);

        void pairingFailed(String error);

        void unpaired();
    }

}
