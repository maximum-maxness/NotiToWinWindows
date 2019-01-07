package controller;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import runner.Main;

import java.io.PrintStream;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class ConfigureViewController {

    //    private Thread discoveryThread;
    private SimpleSocketServer simpleSocketServer;

    @FXML
    private Button startServerButton, stopServerButton, toJSONButton;

    @FXML
    private Label serverStatusLabel;

    @FXML
    private TextArea logOutput;

    @FXML
    private TextField portField;

    @FXML
    private URL location;

    @FXML
    private ResourceBundle resources;

    public ConfigureViewController() {

    }

    @FXML
    private void initialize() {
//        discoveryThread = new Thread(DiscoveryThread.getInstance());
        Console console = new Console(logOutput);
        PrintStream ps = new PrintStream(console, true);
        System.setOut(ps);
//        System.setErr(ps);
    }

    @FXML
    private void startServer() {
        System.out.println("Started the Server.");
        if (simpleSocketServer == null) {
            int port = Integer.parseInt(portField.getText());
            simpleSocketServer = new SimpleSocketServer(port);
        }
        Thread thread = new Thread() {
            @Override
            public void run() {
                simpleSocketServer.startServer();

            }
        };
        thread.start();
//        discoveryThread.start();
        startServerButton.setDisable(true);
        stopServerButton.setDisable(false);
        serverStatusLabel.setText("Started");
        serverStatusLabel.setTextFill(Paint.valueOf("GREEN"));
    }

    @FXML
    private void stopServer() {
//        DiscoveryThreadShutdown.shutdown();
//        discoveryThread.interrupt();
        simpleSocketServer.stopServer();
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

}
