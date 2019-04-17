package backend;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

public class PacketType {
    public static final String CLIENT_PAIR_REQUEST = "CLIENT_PAIR_REQUEST";
    public static final String SERVER_PAIR_RESPONSE = "SERVER_PAIR_RESPONSE";
    public static final String CLIENT_PAIR_CONFIRM = "CLIENT_PAIR_CONFIRM";
    public static final String NOTI_REQUEST = "ANDR_NOTI_RECEIVE";
    public static final String NOTIFICATION = "NOTIFICATION";
    public static final String SERVER_ERROR = "WIN_ERROR";
    public static final String SEND_REPLY = "WIN_REPLY_RECEIVE";
    public static final String DATALOAD_REQUEST = "ANDR_DATALOAD_RECEIVE";
    public static final String READY_RESPONSE = "WIN_READY";
    public static final String UNPAIR_CMD = "DISCONNECT_UNPAIR";

    public static Set<String> protocolPacketTypes = new HashSet<String>() {{
        add(CLIENT_PAIR_REQUEST);
        add(SERVER_PAIR_RESPONSE);
        add(CLIENT_PAIR_CONFIRM);
        add(NOTI_REQUEST);
        add(SERVER_ERROR);
        add(SEND_REPLY);
        add(DATALOAD_REQUEST);
        add(READY_RESPONSE);
        add(UNPAIR_CMD);
    }};

    public static JSONConverter makeIdentityPacket() throws UnknownHostException {
        JSONConverter json = new JSONConverter(SERVER_PAIR_RESPONSE);
        InetAddress myHost = InetAddress.getLocalHost();
        String deviceName = myHost.getHostName();
        String osName = System.getProperty("os.name");
        String osVer = System.getProperty("os.version");
        json.set("deviceName", deviceName);
        json.set("osName", osName);
    json.set("osVersion", osVer);
        return json;
    }
}
