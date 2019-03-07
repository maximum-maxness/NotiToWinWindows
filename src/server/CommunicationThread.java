package server;

import backend.Client;
import backend.DataLoad;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommunicationThread implements Runnable {

    final AtomicBoolean running = new AtomicBoolean(false);
    Socket socket;
    private InetAddress ip;
    public static final int port = 9856;
    private Client client;

    public CommunicationThread(Client client) {
        this.client = client;
        this.ip = client.getIp();
    }


    @Override
    public void run() {
        try {
            ServerSocket ss = new ServerSocket(port);
            System.out.println("Wating for client to connect to socket...");
            this.socket = ss.accept();
            System.out.println("Connection from IP: \"" + this.socket.getInetAddress() + "\"");
            if (this.socket.getInetAddress().toString().equals(this.ip.toString())) {
                System.out.println("IP Matches set IP!");
                this.running.set(true);
                while (running.get()) {
                    listenForTextPackets();
                }
            } else {
                System.err.println("Connection from IP: \"" + this.socket.getInetAddress() + "\" Does not match set IP: \"" + this.ip + "\" Trying Again...");
                this.run();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void openStreams() {

    }

    private void closeStreams() {

    }

    private String listenForTextPackets() {
        return null;
    }

    private DataLoad recieveDataLoad() {
        return null;
    }

    private void stop() {

    }
}
