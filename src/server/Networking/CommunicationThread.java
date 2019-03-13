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
    private OutputStream outputStream;


    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
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
        this.printWriter.write(message);
    }

    @Override
    public String receiveMessage() throws IOException {
        return this.bufferedReader.readLine();
    }

    @Override
    public InetAddress getIP() {
        return this.ip;
    }

    Socket getSocket() {
        return this.socket;
    }

    public BufferedReader getBufferedReader() {
        return bufferedReader;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public PrintWriter getPrintWriter() {
        return printWriter;
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
        Thread.currentThread().interrupt();
    }

    private void openReader() {
        this.bufferedReader = new BufferedReader(new InputStreamReader(this.inputStream));
    }

    private void closeReader() throws IOException {
        if (this.bufferedReader != null)
            this.bufferedReader.close();
    }

    private void openWriter() {
        this.printWriter = new PrintWriter(this.outputStream);
    }

    private void closeWriter() throws IOException {
        if (this.printWriter != null)
            this.printWriter.close();

    }

    public void openStreams() throws IOException {
        this.inputStream = this.socket.getInputStream();
        openReader();
        this.outputStream = this.socket.getOutputStream();
        openWriter();
    }

    public void closeStreams() throws IOException {
        closeReader();
        this.inputStream.close();
        closeWriter();
        this.outputStream.close();
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
