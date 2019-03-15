package runner;

import backend.Client;
import javafx.application.Application;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.util.ArrayList;

public class Main extends Application {
    private final static int STAGE_WIDTH = 900;
    private final static int STAGE_HEIGHT = 600;

    public static Scene configureScene, JSONScene;
    private static Stage primaryStage;

    public static void changeViewToJSON() {
        primaryStage.setScene(JSONScene);
    }

    public static void changeViewToConfig() {
        primaryStage.setScene(configureScene);
    }

    public static void updateClientList(ArrayList<Client> clientList) {
        ListView clientView = (ListView) configureScene.lookup("#clientList");
        ListProperty clientProperty = new SimpleListProperty();
        clientView.setItems(clientProperty);
        clientProperty.set(FXCollections.observableArrayList(clientList));
    }

    public static void updateServerConnectionStatus(boolean b) {
        Label status1 = (Label) configureScene.lookup(("#serverStatusLabel"));
        Label status2 = (Label) JSONScene.lookup("#serverStatusLabel2");
        String connected = "Connected";
        String disconnected = "Disconnected";
        if (b) {
            status1.setText(connected);
            status2.setText(connected);
        } else {
            status1.setText(disconnected);
            status2.setText(disconnected);
        }
    }

    @Override
    public void start(Stage primaryStageIn) throws Exception {
        primaryStage = primaryStageIn;
        Parent configureView = FXMLLoader.load(getClass().getResource("../ui/configureView.fxml"));
        Parent jsonViewer = FXMLLoader.load(getClass().getResource("../ui/jsonviewer.fxml"));
        configureScene = new Scene(configureView, STAGE_WIDTH, STAGE_HEIGHT);
        JSONScene = new Scene(jsonViewer, STAGE_WIDTH, STAGE_HEIGHT);
        primaryStage.setTitle("NotiToWin");
        primaryStage.setScene(configureScene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
