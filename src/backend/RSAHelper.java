package backend;

import org.json.JSONArray;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.prefs.Preferences;

import static javax.crypto.Cipher.*;

public class RSAHelper {

    public static void initKeys(boolean force) {
        Preferences generalStore = PreferenceHelper.getGeneralConfig();
        if (!PreferenceHelper.contains(generalStore, "publicKey") || !PreferenceHelper.contains(generalStore, "privateKey") || force) {
            KeyPair keyPair = generateKeyPair();
            if (keyPair == null) return;
            byte[] publicKey = keyPair.getPublic().getEncoded();
            byte[] privateKey = keyPair.getPrivate().getEncoded();
            generalStore.put("publicKey", Base64.getEncoder().encodeToString(publicKey));
            generalStore.put("privateKey", Base64.getEncoder().encodeToString(privateKey));
            PreferenceHelper.applyChanges(generalStore);
        } else {
            System.out.println("Already have Keys!");
        }
    }

    public static PublicKey getPublicKey() throws GeneralSecurityException {
        Preferences generalStore = PreferenceHelper.getGeneralConfig();
        byte[] publicKeyBytes = Base64.getDecoder().decode(generalStore.get("publicKey", ""));
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
    }

    public static PrivateKey getPrivateKey() throws GeneralSecurityException {
        Preferences generalStore = PreferenceHelper.getGeneralConfig();
        byte[] privateKeyBytes = Base64.getDecoder().decode(generalStore.get("privateKey", ""));
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
    }

    public static JSONConverter encrypt(JSONConverter json, PublicKey pubKey) throws GeneralSecurityException {
        String jsonStr = json.serialize();

        Cipher cipher = getInstance("RSA/ECB/PKCS1PADDING");
        cipher.init(ENCRYPT_MODE, pubKey);

        int fragmentSize = 128; //The size of each encrypted fragment in amount of characters

        JSONArray arr = new JSONArray();
        while (jsonStr.length() > 0) {
            if (jsonStr.length() < fragmentSize) {
                fragmentSize = jsonStr.length();
            }
            String fragment = jsonStr.substring(0, fragmentSize);
            jsonStr = jsonStr.substring(fragmentSize);
            byte[] fragmentBytes = fragment.getBytes();
            byte[] encryptedFragment = cipher.doFinal(fragmentBytes); //encrypt fragment
            arr.put(Base64.getEncoder().encodeToString(encryptedFragment)); //add to the array of encrypted fragments
        }

        JSONConverter encryptedJson = new JSONConverter(PacketType.ENCRYPTED_PACKET);
        encryptedJson.set("data", arr);
        return encryptedJson;
    }

    public static JSONConverter decrypt(JSONConverter json, PrivateKey prvKey) throws GeneralSecurityException {
        JSONArray fragments = json.getJSONArray("data");

        Cipher cipher = getInstance("RSA/ECB/PKCS1PADDING");
        cipher.init(DECRYPT_MODE, prvKey);

        StringBuilder decryptedStr = new StringBuilder();
        for (int i = 0; i < fragments.length(); i++) {
            byte[] encryptedFragment = Base64.getDecoder().decode(fragments.getString(i));
            String decryptedFragment = new String(cipher.doFinal(encryptedFragment));
            decryptedStr.append(decryptedFragment);
        }

        return JSONConverter.unserialize(decryptedStr.toString());
    }

    private static KeyPair generateKeyPair() {
        KeyPair keyPair = null;
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            keyPair = kpg.generateKeyPair();
            return keyPair;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return keyPair;
        }
    }

}
