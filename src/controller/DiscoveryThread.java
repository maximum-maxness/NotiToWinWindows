package controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

class DiscoveryThread implements Runnable {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final String className = getClass().getName();
    private DatagramSocket socket;

    static DiscoveryThread getInstance() {
        return DiscoveryThreadHolder.INSTANCE;
    }

    @Override
    public void run() {
        System.out.println("Server Thread Started!");
        running.set(true);
        try {
            socket = new DatagramSocket(8657, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);

            while (running.get()) {
                System.out.println(className + ":::   Ready to receive packet!");

                byte[] receiverBuffer = new byte[15000];
                DatagramPacket packet = new DatagramPacket(receiverBuffer, receiverBuffer.length);
                socket.receive(packet);
                System.out.println("Whats this");

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
            System.out.println("Socket Closed");
        }
    }

    void stop() {
        running.set(false);
        socket.close();
    }

    private static class DiscoveryThreadHolder {
        private static final DiscoveryThread INSTANCE = new DiscoveryThread();
    }

}
