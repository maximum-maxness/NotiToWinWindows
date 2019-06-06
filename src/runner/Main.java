package runner;

import backend.Client;
import backend.PreferenceHelper;
import backend.RSAHelper;
import backend.SSLHelper;
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
import javafx.scene.image.Image;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;

public class Main extends Application {
  private static final int STAGE_WIDTH = 900;
  private static final int STAGE_HEIGHT = 600;

  private static final boolean FORCE_KEY_CERT_REFRESH = true;

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

  private static void createTaskbarIcon() throws IOException {
    TrayIcon trayIcon;
    if (SystemTray.isSupported()) {
      SystemTray tray = SystemTray.getSystemTray();
      BufferedImage image = ImageIO.read(Main.class.getResource("/ui/res/ic_launcher_round.png"));
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
      trayIcon = new TrayIcon(image, "NotiToWin", popupMenu);
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
      trayIcon.setImageAutoSize(true);
      try {
        tray.add(trayIcon);
      } catch (AWTException e) {
        System.err.println("Couldn't add trayIcon. " + e.getLocalizedMessage());
      }
    } else {

    }
  }

  public static void initSecurity() {
    RSAHelper.initKeys(FORCE_KEY_CERT_REFRESH);
    SSLHelper.initCertificate(FORCE_KEY_CERT_REFRESH);
  }

  public static void main(String[] args) {
    PreferenceHelper.initPreferences();
    initSecurity();
    backgroundThread = new BackgroundThread();
    launch(args);
  }

  @Override
  public void start(Stage primaryStageIn) throws Exception {
    primaryStage = primaryStageIn;
    Platform.setImplicitExit(false);
    Parent configureView = FXMLLoader.load(getClass().getResource("/ui/fxml/configureView.fxml"));
    Parent jsonViewer = FXMLLoader.load(getClass().getResource("/ui/fxml/jsonviewer.fxml"));
    configureScene = new Scene(configureView, STAGE_WIDTH, STAGE_HEIGHT);
    JSONScene = new Scene(jsonViewer, STAGE_WIDTH, STAGE_HEIGHT);
    primaryStage.setTitle("NotiToWin");
    primaryStage.setScene(configureScene);
    primaryStage.setScene(JSONScene);
    primaryStage.setScene(configureScene);
    Image image = new Image(String.valueOf(Main.class.getResource("/ui/res/ic_launcher_round.png")));
    primaryStage.getIcons().add(image);
    primaryStage.show();
    primaryStage.setOnCloseRequest(windowEvent -> primaryStage.hide());
    primaryStage.requestFocus();
    Parent notiParent = FXMLLoader.load(getClass().getResource("/ui/fxml/notificationCard.fxml"));
    NotiCardHelper.initialize(notiParent);
    createTaskbarIcon();
  }
}
