package controller;

import backend.Client;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Paint;
import javafx.util.Callback;
import runner.Main;
import server.Networking.ClientDiscoverer;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SuppressWarnings("WeakerAccess")
public class ConfigureViewController {

    private Executor discoveryThread;
    static ClientDiscoverer discovery;
    private ListProperty<Client> clientProperty = new SimpleListProperty<>();

    @FXML
    private Button startServerButton, stopServerButton, toJSONButton, printClients, sendReadyButton;

    @FXML
    private ListView<Client> clientList;

    @FXML
    private Label serverStatusLabel;

    @FXML
    private TextArea logOutput;

    @FXML
    private URL location;

    @FXML
    private ResourceBundle resources;

    public ConfigureViewController() {

    }

    @FXML
    private void initialize() {
        discovery = ClientDiscoverer.getInstance();
        discoveryThread = Executors.newFixedThreadPool(1);
        clientList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        clientList.setCellFactory(new Callback<ListView<Client>, ListCell<Client>>() {
            @Override
            public ListCell<Client> call(ListView param) {
                ListCell<Client> cell = new ListCell<Client>() {
                    @Override
                    protected void updateItem(Client item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setText(item.getName());
                        } else {
                            setText("");
                        }
                    }
                };
                return cell;
            }
        });
        clientList.setItems(clientProperty);
        clientProperty.set(FXCollections.observableArrayList(discovery.clients));
        Console console = new Console(logOutput);
        PrintStream ps = new PrintStream(console, true);
        System.setOut(ps);
//        System.setErr(ps);
    }

    @FXML
    private void startServer() {
        System.out.println("Started the Server.");
        discoveryThread.execute(discovery);
        startServerButton.setDisable(true);
        stopServerButton.setDisable(false);
        serverStatusLabel.setText("Started");
        serverStatusLabel.setTextFill(Paint.valueOf("GREEN"));
    }

    @FXML
    private void stopServer() {
        try {
            discovery.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
        startServerButton.setDisable(false);
        stopServerButton.setDisable(true);
        serverStatusLabel.setText("Stopped");
        serverStatusLabel.setTextFill(Paint.valueOf("RED"));
        System.out.println("Stopped the Server.");
    }

    @FXML
    private void changeViewToJSON() {
        Main.changeViewToJSON();
    }

    @FXML
    private void printClients() {
        int index = clientList.getSelectionModel().getSelectedIndex();
        if (index != -1)
            System.out.println(discovery.clients.get(index));
        else System.out.println("Nothing Selected!");
    }

    @FXML
    private void sendReady() throws IOException {
        int index = clientList.getSelectionModel().getSelectedIndex();
        if (index != -1)
            discovery.clients.get(index).getClientCommunicator().sendReady();
        else System.out.println("Nothing Selected!");
    }

}
