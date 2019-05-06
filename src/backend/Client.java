package backend;

import javafx.beans.property.SimpleStringProperty;
import server.networking.helpers.PacketType;
import server.networking.helpers.RSAHelper;
import server.networking.helpers.SSLHelper;
import server.networking.linkHandlers.LANLink;
import server.networking.linkHandlers.LANLinkHandler;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

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
    public Certificate certificate;
    public Thread requestThread = null;
    private PairStatus pairStatus;
    private SimpleStringProperty name;
    private List<Notification> notifications;

    public Client(JSONConverter json, LANLink link) {
        this.clientID = json.getString("clientID");
        this.name = new SimpleStringProperty("Unknown");
        this.pairStatus = PairStatus.NotPaired;
        this.publicKey = null;
        addLink(json, link);
    }

//    public Client (String clientID){
//        this.clientID = clientID;
//        this.pairStatus = PairStatus.Paired;
//    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    @Override
    public void onPacketReceived(JSONConverter json) {
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
            //Case while paired, will implement later.
        } else {
            unpair();
        }
    }

    public String getClientID() {
        return this.clientID;
    }

    public boolean isPaired() {
        return this.pairStatus == PairStatus.Paired;
    }

    public boolean isReachable() {
        return !links.isEmpty();
    }

    public void acceptPairing() {
        for (LANLinkHandler llh : pairingHandlers.values()) {
            llh.acceptPairing();
        }
    }

    public void requestPairing() {


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
            this.name = identityPacket.getString("clientName");
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
                                pb.incomingRequest();
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

//        link.addPacketReceiver(this);
//        requestThread = new Thread(() -> {
//            String sendTestString = Main.getDecision("Request Pairing? Y/N");
//            sendTestString = sendTestString.toUpperCase();
//            if (sendTestString.equals("Y")) {
//                requestPairing();
//            } else {
//                unpair();
//            }
//        });
//        requestThread.start();
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
        pairStatus = PairStatus.Paired;
        for (PairingCallback pb : pairingCallback) {
            pb.pairingSuccessful();
        }
    }

    private void unpairInternal() {
        System.out.println("Forcing an Unpair..");
        pairStatus = PairStatus.NotPaired;
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

    public enum PairStatus {
        NotPaired,
        Paired
    }

    public interface PairingCallback {
        void incomingRequest();

        void pairingSuccessful();

        void pairingFailed(String error);

        void unpaired();
    }

    public abstract static class SendPacketStatusCallback {
        public abstract void onSuccess();

        public abstract void onFailure(Throwable e);

        public void onProgressChanged(int percent) {
        }
    }


}
