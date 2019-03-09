package server;

import backend.*;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommunicationThread implements NetworkThread, Runnable {

    final AtomicBoolean running = new AtomicBoolean(false);
    private Socket socket;
    private Client client;
    private InputStream inputStream;
    private OutputStream outputStream;
    private InputStreamReader inputStreamReader;
    private OutputStreamWriter outputStreamWriter;

    private InetAddress ip;
    private int port;

    public CommunicationThread(Client client) {
        this.client = client;
        setIP(client.getIp());
        setPort(NetworkThread.COMMNICATION_PORT);
    }


    @Override
    public void run() {
        try {
            ServerSocket ss = new ServerSocket(getPort());
            System.out.println("Wating for client to connect to socket...");
            this.socket = ss.accept();
            ss.close();
            System.out.println("Connection from IP: \"" + this.socket.getInetAddress() + "\"");
            if (this.socket.getInetAddress().toString().equals(getIP().toString())) {
                System.out.println("IP Matches set IP!");
                this.running.set(true);
                openStreams();
                while (running.get()) {
                    String message = recieveMessage();
                    switch (message) {
                        case PacketType.NOTI_REQUEST:
                            System.out.println("Noti Request!");
                            sendMessage(PacketType.READY_RESPONSE, getPort());
                            break;
                        case PacketType.UNPAIR_CMD:
                            System.out.println("Unpair Command!");

                            Thread.currentThread().interrupt();
                            break;
                        default:
                            if (message.endsWith("}")) {
                                processJson(message);
                                sendMessage(PacketType.READY_RESPONSE, getPort());
                            } else {
                                System.err.println("Packet: " + message + " is invalid.");
                            }
                    }
                }
            } else {
                System.err.println("Connection from IP: \"" + this.socket.getInetAddress() + "\" Does not match set IP: \"" + getIP() + "\" Trying Again...");
                this.run();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void processJson(String jsonString) {
        System.out.println("JSON Detected!");
        JSONConverter json = JSONConverter.unserialize(jsonString);
        if (json.getType().equals(PacketType.NOTI_REQUEST)) {
            System.out.println("JSON Type is Noti Request!");
            Notification noti = Notification.jsonToNoti(json);
        } else {
            System.err.println("Json type: " + json.getType() + "is unrecognized.");
        }

    }

    private DataLoad recieveDataLoad() { //TODO
        return null;
    }

    @Override
    public void sendMessage(String message, int port) throws IOException {
        openWriter();
        this.outputStreamWriter.write(message);
        closeWriter();
    }

    @Override
    public String recieveMessage() throws IOException {
        openReader();
        BufferedReader br = new BufferedReader(this.inputStreamReader);
        String message = br.readLine();
        br.close();
        closeReader();
        return message;
    }

    @Override
    public InetAddress getIP() {
        return this.ip;
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

    public void stop() throws IOException {
        closeStreams();
        this.socket.close();
        Thread.currentThread().interrupt();
    }

    private void openReader() {
        this.inputStreamReader = new InputStreamReader(this.inputStream);
    }

    private void closeReader() throws IOException {
        if (this.inputStreamReader != null)
            this.inputStreamReader.close();
    }

    private void openWriter() {
        this.outputStreamWriter = new OutputStreamWriter(this.outputStream);
    }

    private void closeWriter() throws IOException {
        if (this.outputStreamWriter != null)
            this.outputStreamWriter.close();

    }

    private void openStreams() throws IOException {
        this.inputStream = this.socket.getInputStream();
        this.outputStream = this.socket.getOutputStream();
    }

    private void closeStreams() throws IOException {
        closeReader();
        this.inputStream.close();
        closeWriter();
        this.outputStream.close();
    }

}
