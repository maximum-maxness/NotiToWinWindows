package backend;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import server.Networking.ClientCommunicator;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client {
    private SimpleObjectProperty<InetAddress> ip;
    private SimpleBooleanProperty confirmed, hasThread;
    private SimpleStringProperty name;
    private List<Notification> notifications;
    private ClientCommunicator clientCommunicator;

    public Client(InetAddress ip) {
        this.ip = new SimpleObjectProperty<InetAddress>(ip);
        this.confirmed = new SimpleBooleanProperty(false);
        this.hasThread = new SimpleBooleanProperty(false);
        this.notifications = new ArrayList<Notification>();
    }

    public SimpleObjectProperty<InetAddress> ipProperty() {
        return ip;
    }

    public SimpleBooleanProperty confirmedProperty() {
        return confirmed;
    }

    public SimpleBooleanProperty hasThreadProperty() {
        return hasThread;
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public InetAddress getIp() {
        return ip.getValue();
    }

    public void setIp(InetAddress ip) {
        this.ip.set(ip);
    }

    public String getName() {
        return name.getValue();
    }

    public void setName(String name) {
        if (this.name != null) {
            this.name.set(name);
        } else {
            this.name = new SimpleStringProperty(name);
        }
    }

    public boolean isConfirmed() {
        return confirmed.get();
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed.set(confirmed);
    }

    public boolean isHasThread() {
        return hasThread.get();
    }

    public void setHasThread(boolean hasThread) {
        this.hasThread.set(hasThread);
    }

    public ClientCommunicator getClientCommunicator() {
        return clientCommunicator;
    }

    public void setClientCommunicator(ClientCommunicator clientCommunicator) {
        this.clientCommunicator = clientCommunicator;
    }

    public Notification[] getNotifications() {
        return (Notification[]) notifications.toArray();
    }

    public void setNotifications(Notification[] notifications) {
        this.notifications.clear();
        this.notifications.addAll(Arrays.asList(notifications));
    }

    public List<Notification> getNotificationList() {
        return notifications;
    }

    public void addNoti(Notification noti) {
        boolean match = false;
        for (Notification notification : notifications) {
            if (notification.getId().equals(noti.getId())) {
                match = true;
            }
        }
        if (!match)
            notifications.add(noti);
        else {
            System.out.println("Already Have that Notification!");
        }
    }

    public void clearNotifications() {
        notifications.clear();
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
