package backend;

import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class PreferenceHelper {

    private static final String ROOT_NODE = "ca/NotiToWin/prefs";
    private static final String GENERAL_CONFIG = "config";
    private static final String TRUSTED_DEVICES = "trusted_devices";
    private static final String DEVICE_STORE = "devices";

    private static Preferences rootNode;
    private static Preferences genConfig;
    private static Preferences trustedDeviceNode;
    private static Preferences deviceConfigStore;

    public static void initPreferences() {
        rootNode = Preferences.userRoot().node(ROOT_NODE);
        genConfig = rootNode.node(GENERAL_CONFIG);
        trustedDeviceNode = rootNode.node(TRUSTED_DEVICES);
        deviceConfigStore = trustedDeviceNode.node(DEVICE_STORE);
    }

    public static Preferences getRootNode() {
        return rootNode;
    }

    public static Preferences getTrustedDeviceNode() {
        return trustedDeviceNode;
    }

    public static Preferences getDeviceConfigStore() {
        return deviceConfigStore;
    }

    public static Preferences getDeviceConfigNode(String clientID) {
        return deviceConfigStore.node(clientID);
    }

    public static Preferences getGeneralConfig() {
        return genConfig;
    }

    public static void applyChanges(Preferences node) {
        try {
            node.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    public static void addClientToStore(Client client, boolean isTrusted) {
        Preferences clientNode = deviceConfigStore.node(client.getClientID());
        clientNode.put("deviceName", client.getName());
        clientNode.put("osName", client.getOsName());
        clientNode.put("osVer", client.getOsVersion());
        if (isTrusted) {
            setClientTrusted(client, true);
        }
    }

    public static void setClientTrusted(Client client, boolean isTrusted) {
        setClientTrusted(client.getClientID(), isTrusted);
    }

    public static void setClientTrusted(String clientID, boolean isTrusted) {
        trustedDeviceNode.putBoolean(clientID, isTrusted);
    }

    public static String[] getAllStrings(Preferences node) {
        try {
            String[] keys = node.keys();
            ArrayList<String> returnArr = new ArrayList<String>();
            for (String key : keys) {
                String s = node.get(key, "");
                if (!s.isEmpty()) returnArr.add(s);
            }
            return returnArr.toArray(new String[0]);
        } catch (BackingStoreException e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    public static String[] getAllKeys(Preferences node) {
        try {
            return node.keys();
        } catch (BackingStoreException e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    public static Boolean[] getAllBooleans(Preferences node) {
        try {
            String[] keys = node.keys();
            ArrayList<Boolean> returnArr = new ArrayList<>();
            for (String key : keys) {
                boolean b;
                b = node.getBoolean(key, false);
                returnArr.add(b);
            }
            return returnArr.toArray(new Boolean[0]);
        } catch (BackingStoreException e) {
            e.printStackTrace();
            return new Boolean[0];
        }
    }

    public static String[] getChildNames(Preferences node) {
        try {
            return node.childrenNames();
        } catch (BackingStoreException e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    public static boolean contains(Preferences node, String search) {
        return contains(node, search, false);
    }

    public static boolean contains(Preferences node, String search, boolean def) {
        return !node.get(search, "null").equals("null") || def;
    }

}
