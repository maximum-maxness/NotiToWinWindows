package server;

import backend.Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class DiscoveryThread implements NetworkThread, Runnable {


    private InetAddress ip;
    final AtomicBoolean running = new AtomicBoolean(false);
    public static ArrayList<Client> clients = new ArrayList<>();
    public static Executor notiThreadPool = Executors.newWorkStealingPool();


    DatagramSocket socket;
    InetAddress receivedIP;

    DiscoveryThread() {
        try {
            setIP(InetAddress.getByName("0.0.0.0"));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        setPort(NetworkThread.DISCOVERY_PORT);
    }

    @Override
    public void sendMessage(String packetType, int port1) throws IOException {
        byte[] sendData = packetType.getBytes(); //Turn the string into a byte array
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, this.receivedIP, port1);
        socket.send(packet); //put into a packet and send
        System.out.println("Sent: " + packetType);
    }

    @Override
    public String receiveMessage() {
        return null;
    }

    @Override
    public InetAddress getIP() {
        return null;
    }

    @Override
    public void setIP(InetAddress ip) {

    }

    @Override
    public int getPort() {
        return 0;
    }



    @Override
    public void setPort(int port) {

    }

    @Override
    public void stop() throws IOException {
        running.set(false);
        socket.close();
    }

    public AtomicBoolean isRunning() {
        return running;
    }

    public DatagramSocket getSocket() {
        return socket;
    }
}
