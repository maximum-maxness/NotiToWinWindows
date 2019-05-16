package runner;

import backend.Client;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static runner.Main.primaryStage;

public class PopupHelper {
    public static void createPairPopup(Client client, Client.SendPacketStatusCallback callback) throws IOException {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(primaryStage);
        Parent popupParent = FXMLLoader.load(Main.class.getResource("/ui/fxml/pairPopup.fxml"));
        Scene popupScene = new Scene(popupParent, 300, 200);
        Button acceptButton = (Button) popupScene.lookup("#acceptButton");
        Button rejectButton = (Button) popupScene.lookup("#rejectButton");
        Label timer = (Label) popupScene.lookup("#timer");
        Label prompt = (Label) popupScene.lookup("#prompt");

        Timer timerT = new Timer();
        timerT.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String time = timer.getText().substring(2);
                int count = Integer.parseInt(time);
                count--;
                timer.setText("00:" + count);
            }
        }, 1000, 30000);
        timerT.schedule(new TimerTask() {
            @Override
            public void run() {
                callback.onFailure(new Exception());
                popup.hide();
            }
        }, 30000);

        acceptButton.setOnAction(event -> {
            callback.onSuccess();
            timerT.cancel();
        });
        rejectButton.setOnAction(event -> callback.onFailure(new Exception()));

        popup.setScene(popupScene);
        popup.setOnCloseRequest(windowEvent -> {
            callback.onFailure(new Exception());
            popup.hide();
        });
        popup.setX((primaryStage.getWidth() / 2) - popup.getWidth());
        popup.setY((primaryStage.getHeight() / 2) - popup.getHeight());
        popup.show();
        popup.requestFocus();
    }
}
