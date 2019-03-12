package server;

import java.io.IOException;
import java.net.InetAddress;

public interface NetworkThread {

    int DISCOVERY_PORT = 8657;
    int COMMUNICATION_PORT = 9856;

    void sendMessage(String message, int port) throws IOException;

    String receiveMessage() throws IOException;

    InetAddress getIP();

    void setIP(InetAddress ip);

    int getPort();

    void setPort(int port);

    void stop() throws IOException;

}
