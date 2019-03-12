package server;

import backend.Client;
import backend.DataLoad;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class CommunicationThread implements NetworkThread, Runnable {

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;


    private BufferedReader bufferedReader;
    private PrintWriter printWriter;

    private InetAddress ip;
    private int port;

    CommunicationThread(@NotNull Client client) {
        setIP(client.getIp());
        setPort(NetworkThread.COMMUNICATION_PORT);
    }

    void waitForConnection() throws IOException {
        ServerSocket ss = new ServerSocket(this.getPort());
        System.out.println("Wating for client to connect to socket...");
        this.socket = ss.accept();
        ss.close();
    }

    public DataLoad recieveDataLoad() { //TODO
        return null;
    }

    @Override
    public void sendMessage(String message, int port) throws IOException {
        this.printWriter.write(message);
    }

    @Override
    public String receiveMessage() throws IOException {
        String message = this.bufferedReader.readLine();
        return message;
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

}
