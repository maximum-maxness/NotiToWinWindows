package server;

import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommunicationThread implements Runnable {

    final AtomicBoolean running = new AtomicBoolean(false);
    Socket socket;
    InetAddress ip;
    int port;


    @Override
    public void run() {

    }
}
