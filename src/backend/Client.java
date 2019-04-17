package backend;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import server.Networking.ClientCommunicator;

import java.io.File;
import java.io.IOException;
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
      if (notification.getId().equals(noti.getId())
          && notification.getText().equals(noti.getText())) {
        match = true;
        noti.setIcon(notification.getIcon());
      }
    }
    try {
      clientCommunicator.sendChoice(!match);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (!match) {
      try {
        getIconFromNetwork(noti);
      } catch (IOException e) {
        e.printStackTrace();
      }
      notifications.add(noti);
    } else {
      System.out.println("Already Have that Notification!");
    }
    Runnable r = noti::display;
    Platform.runLater(r);
  }

  //    private int timesFailed = 0;

  private void getIconFromNetwork(Notification noti) throws IOException {
    String name = noti.getAppName() + noti.getTimeStamp();
    File icon = clientCommunicator.recieveDataLoad(noti.getDataLoadSize(), name);
    noti.setIcon(icon);
    //        String hash = DataLoad.getChecksum(noti.getIconInputStream().readAllBytes());
    //        if (!noti.getDataLoadHash().equals(hash) && timesFailed < 5) {
    //            clientCommunicator.sendChoice(true);
    //            timesFailed++;
    //            System.out.println("Hashes don't match, trying again!");
    //            getIconFromNetwork(noti);
    //        } else if (timesFailed >= 5) {
    //            noti.setIcon(new File("src/ui/res/x.png"));
    //        } else {
    //            timesFailed = 0;
    //            System.out.println("Hashes Match!");
    //            clientCommunicator.sendChoice(false);
    //        }
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
