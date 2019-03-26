package server.Networking;

import backend.Client;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class DiscoveryThread implements NetworkThread {


    final AtomicBoolean running = new AtomicBoolean(false);
    public ArrayList<Client> clients = new ArrayList<>();


    Executor notiThreadPool = Executors.newWorkStealingPool();
    private DatagramSocket socket;
    private InetAddress receivedIP;
    private int port;


    private DatagramPacket currentPacket;

    DiscoveryThread() {
        try {
            setIP(InetAddress.getByName("0.0.0.0"));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        setPort(NetworkThread.DISCOVERY_PORT);
    }

    void initializeSocket() throws SocketException {
        socket = new DatagramSocket(null);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(getIP(), getPort()));
        socket.setBroadcast(true);
    }

    @Override
    public void sendMessage(String packetType, int port1) throws IOException {
        byte[] sendData = packetType.getBytes(); //Turn the string into a byte array
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, this.receivedIP, port1);
        socket.send(packet); //put into a packet and send
        System.out.println("Sent: " + packetType);
    }

    @Override
    public String receiveMessage() throws IOException {
        byte[] receiveBuffer = new byte[15000];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        socket.receive(receivePacket);
        setIP(receivePacket.getAddress());
        setCurrentPacket(receivePacket);
        return new String(receivePacket.getData()).trim();
    }

    DatagramPacket getCurrentPacket() {
        return currentPacket;
    }

    private void setCurrentPacket(DatagramPacket currentPacket) {
        this.currentPacket = currentPacket;
    }

    @Override
    public InetAddress getIP() {
        return this.receivedIP;
    }

    @Override
    public void setIP(InetAddress ip) {
        this.receivedIP = ip;
    }

    @Override
    public int getPort() {
        return this.port;
    }



    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void stop() throws IOException {
        running.set(false);
        if (!socket.isClosed())
            socket.close();
//        System.err.println("Socket is Bound? " + socket.isBound());
    }

    public AtomicBoolean isRunning() {
        return running;
    }

    public DatagramSocket getSocket() {
        return socket;
    }
}
