package controller;

import backend.Client;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Paint;
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

    @FXML
    private Button startServerButton, stopServerButton, toJSONButton, printClients;

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
        for (Client client : discovery.clients) {
            System.out.println(client);
        }
    }

}
