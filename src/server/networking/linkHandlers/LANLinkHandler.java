package server.networking.linkHandlers;

import backend.Client;
import backend.JSONConverter;
import backend.PacketType;
import backend.PreferenceHelper;

import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;

public class LANLinkHandler { // TODO Finish and Implement

    private final PairingHandlerCallback pairingHandlerCallback;
    private final Client client;
    private PairStatus pairStatus;
    private Timer pairingTimer;

    public LANLinkHandler(Client client, PairingHandlerCallback callback) {
        this.pairingHandlerCallback = callback;
        this.client = client;
        if (client.isPaired()) {
            pairStatus = PairStatus.Paired;
        } else {
            pairStatus = PairStatus.NotPaired;
        }
    }

    public void requestPairing() {
        System.out.println("Sending Pair Packet!");
        Client.SendPacketStatusCallback callback =
                new Client.SendPacketStatusCallback() {

                    @Override
                    public void onSuccess() {
                        pairingTimer = new Timer();
                        pairingTimer.schedule(
                                new TimerTask() {
                                    @Override
                                    public void run() {
                                        pairingHandlerCallback.pairingFailed("Timed Out");
                                        pairStatus = PairStatus.NotPaired;
                                    }
                                },
                                30000);
                        pairStatus = PairStatus.Requested;
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        pairingHandlerCallback.pairingFailed("Cant send packet.");
                    }
                };
        client.sendPacket(createPairPacket(), callback);
    }

    public void unpair() {
        System.out.println("UnPairing Client ID: " + client.getClientID());
        pairStatus = PairStatus.NotPaired;
        JSONConverter json = new JSONConverter(PacketType.PAIR_REQUEST);
        json.set("pair", false);
        client.sendPacket(json);
    }

    public void acceptPairing() {
        System.out.println("Accepting the Pair for Client ID: " + client.getClientID());
        Client.SendPacketStatusCallback callback =
                new Client.SendPacketStatusCallback() {

                    @Override
                    public void onSuccess() {
                        pairingDone();
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        pairingHandlerCallback.pairingFailed("Not Reachable.");
                    }
                };
        client.sendPacket(createPairPacket(), callback);
    }

    public void rejectPairing() {
        System.out.println("Rejecting Pair for Client ID: " + client.getClientID());
        pairStatus = PairStatus.NotPaired;
        JSONConverter json = new JSONConverter(PacketType.PAIR_REQUEST);
        json.set("pair", false);
        client.sendPacket(json);
    }

    public boolean isPaired() {
        return pairStatus == PairStatus.Paired;
    }

    public boolean isPairRequested() {
        return pairStatus == PairStatus.Requested;
    }

    public boolean isPairRequestedByPeer() {
        return pairStatus == PairStatus.RequestedByPeer;
    }

    private JSONConverter createPairPacket() {
        JSONConverter json = new JSONConverter(PacketType.PAIR_REQUEST);
        json.set("pair", true);
//        String pubKeyStr = null;
//        try {
//            PublicKey pubKey = RSAHelper.getPublicKey();
//            pubKeyStr = pubKey.toString();
//        } catch (Exception e) {
//            e.printStackTrace();
//            pubKeyStr = "";
//        }
//        String publicKeyFormatted =
//                "-----BEGIN PUBLIC KEY-----\n" + pubKeyStr.trim() + "\n-----END PUBLIC KEY-----\n";
//        json.set("publicKey", publicKeyFormatted);
        return json;
    }

    public void packageReceived(JSONConverter json) {
        System.out.println("Package Received for Client ID: " + client.getClientID());
        boolean wantsToPair = json.getBoolean("pair");

        if (wantsToPair == isPaired()) {
            if (pairStatus == PairStatus.Requested) {
                pairStatus = PairStatus.NotPaired;
                pairingHandlerCallback.pairingFailed("Nope");
            }
            return;
        }

        if (wantsToPair) {
            System.out.println("Client wants to Pair.");
            try {
                String publicKeyStr = json.getString("publicKey").replace("-----BEGIN PUBLIC KEY-----\n", "").replace("-----END PUBLIC KEY-----\n", "");
                byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyStr);
                client.publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            } catch (Exception ignored) {
            }

            if (pairStatus == PairStatus.Requested) {
                pairingDone();
            } else {
                if (client.isPaired()) {
                    acceptPairing();
                    return;
                }

                //TODO Implement Choosing Whether or not to accept pair

                pairStatus = PairStatus.RequestedByPeer;
                pairingHandlerCallback.incomingRequest();
            }
        } else {
            System.out.println("Unpair Request from client ID: " + client.getClientID());
            if (pairStatus == PairStatus.Requested) {
                pairingHandlerCallback.pairingFailed("Cancelled");
            } else if (pairStatus == PairStatus.Paired) {
                pairingHandlerCallback.unpaired();
            }

            pairStatus = PairStatus.NotPaired;
        }
    }

    private void pairingDone() {
        Preferences editor = PreferenceHelper.getDeviceConfigNode(client.getClientID());
        try {
            String encodedCertificate = Base64.getEncoder().encodeToString(client.certificate.getEncoded());
            editor.put("certificate", encodedCertificate);
        } catch (NullPointerException n) {
            System.err.println("No Certificate Exists!");
        } catch (Exception e) {
            e.printStackTrace();
        }
        PreferenceHelper.applyChanges(editor);
        pairStatus = PairStatus.Paired;
        pairingHandlerCallback.pairingDone();
    }

    protected enum PairStatus {
        NotPaired,
        Requested,
        RequestedByPeer,
        Paired
    }

    public interface PairingHandlerCallback {
        void incomingRequest();

        void pairingDone();

        void pairingFailed(String error);

        void unpaired();
    }
}
