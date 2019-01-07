package runner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private final static int STAGE_WIDTH = 900;
    private final static int STAGE_HEIGHT = 600;

    private static Parent configureView, jsonViewer;
    private static Stage primaryStage;

    @Override
    public void start(Stage primaryStageIn) throws Exception {
        primaryStage = primaryStageIn;
        configureView = FXMLLoader.load(getClass().getResource("../ui/configureView.fxml"));
        jsonViewer = FXMLLoader.load(getClass().getResource("../ui/jsonviewer.fxml"));
        primaryStage.setTitle("NotiToWin");
        primaryStage.setScene(new Scene(configureView, STAGE_WIDTH, STAGE_HEIGHT));
        primaryStage.show();
    }

    public static void changeViewToJSON() {
        primaryStage.setScene(new Scene(jsonViewer, STAGE_WIDTH, STAGE_HEIGHT));
    }

    public static void changeViewToConfig() {
        primaryStage.setScene(new Scene(configureView, STAGE_WIDTH, STAGE_HEIGHT));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
