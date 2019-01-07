package controller;

import java.io.IOException;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscoveryThread implements Runnable {

    DatagramSocket socket;
    private final String className = getClass().getName();

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(8657, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);

            while (!Thread.interrupted()) {
                System.out.println(className + ":::   Ready to receive packet!");

                byte[] receiverBuffer = new byte[15000];
                DatagramPacket packet = new DatagramPacket(receiverBuffer, receiverBuffer.length);
                socket.receive(packet);
                System.out.println("Whats this");
                if (Thread.interrupted()) {
                    socket.close();
                    System.out.println("INTERUPTTED!");
                }

                System.out.println(className + ":::   Packet received from: " + packet.getAddress().getHostAddress());
                String message = new String(packet.getData());
                System.out.println(className + ":::   Packet Data: " + message);
                message = message.trim();

                if (message.equals("PAIR_NOTISERVER_REQUEST")) {
                    byte[] sendData = "PAIR_NOTISERVER_RESPONSE".getBytes();

                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                    socket.send(sendPacket);

                    System.out.println(className + ":::   Packet Sent to: " + sendPacket.getAddress().getHostAddress());
                }

            }
            System.out.println("about to close socket");
        } catch (IOException e) {
            Logger.getLogger(DiscoveryThread.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public static DiscoveryThread getInstance() {
        return DiscoveryThreadHolder.INSTANCE;
    }

    public static class DiscoveryThreadHolder {
        private static final DiscoveryThread INSTANCE = new DiscoveryThread();
    }

}
