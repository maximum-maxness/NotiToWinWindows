package controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DiscoveryThreadShutdown {


    public static void shutdown() {
        DatagramSocket socket;

        {
            try {
                socket = new DatagramSocket(8657, InetAddress.getByName("127.0.0.1"));
                byte[] shutdown = "shutdown".getBytes();
                DatagramPacket packet = new DatagramPacket(shutdown, shutdown.length);
                socket.send(packet);
            } catch (IOException e) {

            }
        }
    }
}