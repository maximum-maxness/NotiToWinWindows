package backend;

import server.Networking.ClientCommunicator;

import java.net.InetAddress;

public class Client {
    private InetAddress ip;
    private boolean confirmed, hasThread;
    private String name;
    private ClientCommunicator clientCommunicator;
    private Notification[] notifications;

    public Client(InetAddress ip) {
        this.ip = ip;
        this.confirmed = false;
        this.hasThread = false;
        this.notifications = new Notification[1];
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

    public ClientCommunicator getClientCommunicator() {
        return clientCommunicator;
    }

    public void setClientCommunicator(ClientCommunicator clientCommunicator) {
        this.clientCommunicator = clientCommunicator;
    }

    public Notification[] getNotifications() {
        return notifications;
    }

    public void setNotifications(Notification[] notifications) {
        this.notifications = notifications;
    }

    public void addNoti(Notification noti) {
        Notification[] newArr = new Notification[getNotifications().length + 1];
        for (int i = 0; i < getNotifications().length; i++) {
            newArr[i] = getNotifications()[i];
        }
        newArr[getNotifications().length] = noti;
        setNotifications(newArr);
    }

    @Override
    public String toString() {
        StringBuilder returnString = new StringBuilder();
        returnString.append("Client Name: ").append(getName()).append("\n");
        returnString.append("Client IP: ").append(getIp()).append("\n");
        returnString.append("Client is Confirmed? ").append(isConfirmed()).append("\n");
        returnString.append("Client has thread? ").append(isHasThread()).append("\n");
        returnString.append("Client Notifications:").append("\n");
        for (Notification noti : this.notifications) {
            if (noti != null) returnString.append(noti.toString()).append("\n");
        }
        return returnString.toString();
    }
}
