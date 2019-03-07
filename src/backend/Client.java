package backend;

import java.net.InetAddress;

public class Client {
    private InetAddress ip;
    private boolean confirmed, hasThread;
    private String name;

    public Client(InetAddress ip) {
        this.ip = ip;
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
