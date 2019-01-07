package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleSocketServer {
    private int port;
    public String reply;
    private ServerSocket serverSocket;
    private Socket clientSocket;

    public SimpleSocketServer(int port) {
        this.port = port;

    }

    public void startServer() {
        try {
            this.serverSocket = new ServerSocket(this.port);
            this.clientSocket = serverSocket.accept();
            System.out.println("Got connection!!!");
            this.reply = serverListener();
            System.out.println("Reply gotten!");
            System.out.println(this.reply);
        } catch (IOException e) {
            System.err.println("Socket Closed, Which means the server is stopped!");
            e.printStackTrace();
        }
    }

    public void stopServer() {
        try {
            if(clientSocket != null && !this.clientSocket.isClosed()) {
                this.clientSocket.close();
            }
            if(!this.serverSocket.isClosed()) {
                this.serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String serverListener() throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(this.clientSocket.getInputStream());
        BufferedReader in = new BufferedReader(inputStreamReader);
        String s = in.readLine();
        return s;
    }

}
