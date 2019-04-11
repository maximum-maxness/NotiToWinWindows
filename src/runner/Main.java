package runner;

import backend.Client;
import controller.NotiCardHelper;
import javafx.application.Application;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.util.ArrayList;

public class Main extends Application {
    private final static int STAGE_WIDTH = 900;
    private final static int STAGE_HEIGHT = 600;

    public static Scene configureScene, JSONScene;
    public static Stage primaryStage, secondaryStage;

    public static void changeViewToJSON() {
        secondaryStage.setScene(JSONScene);
    }

    public static void changeViewToConfig() {
        secondaryStage.setScene(configureScene);
    }

    public static void updateClientList(ArrayList<Client> clientList) {
        TableView clientView = (TableView) configureScene.lookup("#clientList");
        ChoiceBox<Client> clientBox = (ChoiceBox<Client>) JSONScene.lookup("#clientList");
        ListProperty clientProperty = new SimpleListProperty();
        if (clientBox != null)
            clientBox.setItems(clientProperty);
        if (clientView != null)
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
        secondaryStage = new Stage();
        Parent configureView = FXMLLoader.load(getClass().getResource("../ui/fxml/configureView.fxml"));
        Parent jsonViewer = FXMLLoader.load(getClass().getResource("../ui/fxml/jsonviewer.fxml"));
        configureScene = new Scene(configureView, STAGE_WIDTH, STAGE_HEIGHT);
        JSONScene = new Scene(jsonViewer, STAGE_WIDTH, STAGE_HEIGHT);
        secondaryStage.setTitle("NotiToWin");
        secondaryStage.setScene(configureScene);
        secondaryStage.setScene(JSONScene);
        secondaryStage.setScene(configureScene);
        secondaryStage.show();
        secondaryStage.requestFocus();
        primaryStage.hide();
        Parent notiParent = FXMLLoader.load(getClass().getResource("../ui/fxml/notificationCard.fxml"));
        NotiCardHelper.initialize(notiParent);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
