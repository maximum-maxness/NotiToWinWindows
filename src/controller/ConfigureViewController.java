package controller;

import backend.Client;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Paint;
import javafx.util.Callback;
import runner.Main;

import java.io.PrintStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SuppressWarnings("WeakerAccess")
public class ConfigureViewController {

  private TableColumn nameCol = new TableColumn("Name");
  private TableColumn pairCol = new TableColumn("Pair Status");
//  private TableColumn confirmedCol = new TableColumn("Is Confirmed?");
//  private TableColumn hasThreadCol = new TableColumn("Has Thread?");
  private ListProperty<Client> clientProperty = new SimpleListProperty<>();
  private Executor executor;

  @FXML
  private Button startServerButton, stopServerButton, toJSONButton, printClients, yesPairButton, noPairButton;

  @FXML private TableView<Client> clientList;

  @FXML private Label serverStatusLabel;

  @FXML private TextArea logOutput;

  @FXML private URL location;

  @FXML private ResourceBundle resources;

  public ConfigureViewController() {}

  @FXML
  private void initialize() {
    initTable();
    Console console = new Console(logOutput);
    PrintStream ps = new PrintStream(console, true);
    executor = Executors.newSingleThreadExecutor();
//    System.setOut(ps);
    //        System.setErr(ps);
  }

  private void initTable() {
    clientList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

    nameCol.setCellValueFactory(
        (Callback<TableColumn.CellDataFeatures<Client, String>, ObservableValue<String>>)
            param -> {
              if (param.getValue() != null) {
                return param.getValue().nameProperty();
              } else {
                return new SimpleStringProperty("<no name>");
              }
            });
    pairCol.setCellValueFactory(
            (Callback<TableColumn.CellDataFeatures<Client, Client.PairStatus>, ObservableValue<Client.PairStatus>>)
                    param -> {
                      if (param.getValue() != null) {
                        return param.getValue().pairStatusProperty();
                      } else {
                        return new SimpleObjectProperty<Client.PairStatus>(Client.PairStatus.NotPaired);
                      }
                    });
/*
    confirmedCol.setCellValueFactory(
        (Callback<TableColumn.CellDataFeatures<ClientOLD, Boolean>, ObservableValue<Boolean>>)
            param -> {
              if (param.getValue() != null) {
                return param.getValue().confirmedProperty();
              } else {
                return new SimpleBooleanProperty(false);
              }
            });
    hasThreadCol.setCellValueFactory(
        (Callback<TableColumn.CellDataFeatures<ClientOLD, Boolean>, ObservableValue<Boolean>>)
            param -> {
              if (param.getValue() != null) {
                return param.getValue().hasThreadProperty();
              } else {
                return new SimpleBooleanProperty(false);
              }
            });
*/


    clientList.setItems(clientProperty);
    clientProperty.set(FXCollections.observableArrayList(Main.backgroundThread.getClients()));
    clientList.getColumns().addAll(nameCol, pairCol);
    clientList.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.intValue() >= 0) {
        yesPairButton.setDisable(false);
        noPairButton.setDisable(false);
      }
    });
  }

  @FXML
  private void startServer() {
    executor.execute(Main.backgroundThread);
    System.out.println("Started the Server.");
    startServerButton.setDisable(true);
    stopServerButton.setDisable(false);
    serverStatusLabel.setText("Started");
    serverStatusLabel.setTextFill(Paint.valueOf("GREEN"));
  }

  @FXML
  private void stopServer() {
    Main.backgroundThread.stop();
    startServerButton.setDisable(false);
    stopServerButton.setDisable(true);
    serverStatusLabel.setText("Stopped");
    serverStatusLabel.setTextFill(Paint.valueOf("RED"));
    System.out.println("Stopped the Server.");
  }

  @FXML
  private void changeViewToJSON() {
    Main.changeViewToJSON();
    Main.updateClientList(Main.backgroundThread.getClients());
  }

  @FXML
  private void printClients() {
    int index = clientList.getSelectionModel().getSelectedIndex();
    if (index != -1) System.out.println(Main.backgroundThread.getClient(index));
    else System.out.println("Nothing Selected!");
  }

  @FXML
  private void requestPairAction() {
    Client client = clientList.getSelectionModel().getSelectedItem();
    if (client == null) return;
    client.requestPairing();
  }

  @FXML
  private void unpairAction() {
    Client client = clientList.getSelectionModel().getSelectedItem();
    if (client == null) return;
    client.unpair();
  }
}
