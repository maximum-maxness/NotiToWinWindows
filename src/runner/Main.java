package runner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class Main extends Application {
    private final static int STAGE_WIDTH = 900;
    private final static int STAGE_HEIGHT = 600;

    private static Parent configureView, jsonViewer;
    private static Scene configureScene, JSONScene;
    private static Stage primaryStage;

    public static void changeViewToJSON() {
        primaryStage.setScene(JSONScene);
    }

    public static void changeViewToConfig() {
        primaryStage.setScene(configureScene);
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
        configureView = FXMLLoader.load(getClass().getResource("../ui/configureView.fxml"));
        jsonViewer = FXMLLoader.load(getClass().getResource("../ui/jsonviewer.fxml"));
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
