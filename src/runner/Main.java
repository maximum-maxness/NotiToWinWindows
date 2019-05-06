package runner;

import backend.Client;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import server.networking.helpers.RSAHelper;
import server.networking.helpers.SSLHelper;

import java.awt.*;
import java.util.Collection;

public class Main extends Application {
  private static final int STAGE_WIDTH = 900;
  private static final int STAGE_HEIGHT = 600;

  static Stage primaryStage;
  private static Scene configureScene, JSONScene;
  public static BackgroundThread backgroundThread;

  public static void changeViewToJSON() {
    primaryStage.setScene(JSONScene);
  }

  public static void changeViewToConfig() {
    primaryStage.setScene(configureScene);
  }

  public static void updateClientList(Collection<Client> clientList) {
    TableView clientView = (TableView) configureScene.lookup("#clientList");
    ChoiceBox<Client> clientBox = (ChoiceBox<Client>) JSONScene.lookup("#clientList");
    ListProperty clientProperty = new SimpleListProperty();
    if (clientBox != null) clientBox.setItems(clientProperty);
    if (clientView != null) clientView.setItems(clientProperty);
    clientProperty.set(FXCollections.observableArrayList(clientList));
  }

  private static void createTaskbarIcon() {
    TrayIcon trayIcon;
    if (SystemTray.isSupported()) {
      SystemTray tray = SystemTray.getSystemTray();
      Image image =
          Toolkit.getDefaultToolkit()
              .getImage("src/ui/res/x.png")
              .getScaledInstance(tray.getTrayIconSize().width, tray.getTrayIconSize().height, 0);
      PopupMenu popupMenu = new PopupMenu();
      MenuItem defaultItem = new MenuItem("Show");
      defaultItem.addActionListener(
          e ->
              Platform.runLater(
                  () -> {
                    if (primaryStage.isIconified()) primaryStage.setIconified(false);
                    if (!primaryStage.isShowing()) primaryStage.show();
                  }));
      MenuItem quitItem = new MenuItem("Quit");
      quitItem.addActionListener(e -> System.exit(0));
      MenuItem hideItem = new MenuItem("Hide");
      hideItem.addActionListener(
          e ->
              Platform.runLater(
                  () -> {
                    if (primaryStage.isShowing()) primaryStage.hide();
                  }));
      popupMenu.add(defaultItem);
      popupMenu.add(hideItem);
      popupMenu.add(quitItem);
      trayIcon = new TrayIcon(image, "Test", popupMenu);
      trayIcon.addActionListener(
          e ->
              Platform.runLater(
                  () -> {
                    if (!primaryStage.isShowing()) {
                      primaryStage.show();
                      primaryStage.toFront();
                      primaryStage.requestFocus();
                    } else if (primaryStage.isIconified()) {
                      primaryStage.setIconified(false);
                      primaryStage.toFront();
                      primaryStage.requestFocus();
                    } else {
                      primaryStage.hide();
                    }
                  }));
      try {
        tray.add(trayIcon);
      } catch (AWTException e) {
        System.err.println("Couldn't add trayIcon. " + e.getLocalizedMessage());
      }
    } else {

    }
  }

  public static void initSecurity() {
    RSAHelper.initKeys();
    SSLHelper.initCertificate();
  }

  public static void main(String[] args) {
    initSecurity();
    backgroundThread = new BackgroundThread();
    launch(args);
  }

  @Override
  public void start(Stage primaryStageIn) throws Exception {
    primaryStage = primaryStageIn;
    Platform.setImplicitExit(false);
    Parent configureView = FXMLLoader.load(getClass().getResource("../ui/fxml/configureView.fxml"));
    Parent jsonViewer = FXMLLoader.load(getClass().getResource("../ui/fxml/jsonviewer.fxml"));
    configureScene = new Scene(configureView, STAGE_WIDTH, STAGE_HEIGHT);
    JSONScene = new Scene(jsonViewer, STAGE_WIDTH, STAGE_HEIGHT);
    primaryStage.setTitle("NotiToWin");
    primaryStage.setScene(configureScene);
    primaryStage.setScene(JSONScene);
    primaryStage.setScene(configureScene);
    primaryStage.show();
    primaryStage.setOnCloseRequest(windowEvent -> primaryStage.hide());
    primaryStage.requestFocus();
    Parent notiParent = FXMLLoader.load(getClass().getResource("../ui/fxml/notificationCard.fxml"));
    NotiCardHelper.initialize(notiParent);
    createTaskbarIcon();
  }
}
