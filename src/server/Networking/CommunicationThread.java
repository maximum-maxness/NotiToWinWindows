package server.Networking;

import backend.Client;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class CommunicationThread implements NetworkThread {

    private Socket socket;
    private InputStream inputStream;
    private DataInputStream dataInputStream;
    private OutputStream outputStream;
    private DataOutputStream dataOutputStream;

    private Client client;
    private InetAddress ip;
    private int port;

    CommunicationThread(@NotNull Client client) {
        setClient(client);
        setIP(client.getIp());
        setPort(NetworkThread.COMMUNICATION_PORT);
    }

    void waitForConnection() throws IOException {
        ServerSocket ss = new ServerSocket(this.getPort());
        System.out.println("Wating for client to connect to socket...");
        this.socket = ss.accept();
        ss.close();
    }

    @Override
    public void sendMessage(String message, int port) throws IOException {
        this.dataOutputStream.writeUTF(message);
        this.dataOutputStream.flush();
        System.out.println("Wrote message: " + message + " to outputstream!");
    }

    @Override
    public String receiveMessage() throws IOException {
        System.out.println("Waiting for packet...");
        String message = this.dataInputStream.readUTF();
        System.out.println("Received Message: " + message);
        return message;
    }

    @Override
    public InetAddress getIP() {
        return this.ip;
    }

    Socket getSocket() {
        return this.socket;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public void setIP(InetAddress ip) {
        this.ip = ip;
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
        closeStreams();
        this.socket.close();
        this.client.setConfirmed(false);
        this.client.setHasThread(false);
        this.client.clearNotifications();
        Thread.currentThread().interrupt();
    }

    public void openStreams() throws IOException {
        this.inputStream = this.socket.getInputStream();
        this.outputStream = this.socket.getOutputStream();
        this.dataInputStream = new DataInputStream((this.inputStream));
        this.dataOutputStream = new DataOutputStream(this.outputStream);
    }

    public void closeStreams() throws IOException {
        this.dataInputStream.close();
        this.dataOutputStream.close();
        this.inputStream.close();
        this.outputStream.close();
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
