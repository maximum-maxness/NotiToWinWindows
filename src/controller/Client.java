package controller;

import java.net.InetAddress;

public class Client {
    private InetAddress ip;
    private boolean confirmed, hasThread;
    private String name;
    private int port;

    public Client(InetAddress ip, int port) {
        this.ip = ip;
        this.port = port;
        this.confirmed = false;
        this.hasThread = false;
    }

    public InetAddress getIp() {
        return ip;
    }

    public void setIp(InetAddress ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public boolean isHasThread() {
        return hasThread;
    }

    public void setHasThread(boolean hasThread) {
        this.hasThread = hasThread;
    }
}
