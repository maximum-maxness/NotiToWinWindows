package runner;

import backend.Client;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static runner.Main.primaryStage;

public class PopupHelper {
    private static Stage popup = new Stage();
    private static boolean firstTime = true;

    private static void initStage() {
        Image image = new Image(String.valueOf(Main.class.getResource("/ui/res/ic_launcher_round.png")));
        popup.getIcons().add(image);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(primaryStage);
        firstTime = false;
    }

    public static void createPairPopup(Client client, Client.SendPacketStatusCallback callback) {
        if (firstTime) {
            initStage();
        }

        Parent popupParent = null;
        try {
            popupParent = FXMLLoader.load(Main.class.getResource("/ui/fxml/pairPopup.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Scene popupScene = new Scene(popupParent, 300, 200);
        Button acceptButton = (Button) popupScene.lookup("#acceptButton");
        Button rejectButton = (Button) popupScene.lookup("#rejectButton");
        Label timer = (Label) popupScene.lookup("#timer");
        Label prompt = (Label) popupScene.lookup("#prompt");
        prompt.setText("Accept pairing for " + client.getName() + "?");
        timer.setText("30");
        Timer timerT = new Timer();
        new Thread(() -> {
            for (int i = 30; i > 0; i--) {
                String time;
                if (i < 10) {
                    time = "00:0" + i;
                } else {
                    time = "00:" + i;
                }
                Platform.runLater(() -> timer.setText(time));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        timerT.schedule(new TimerTask() {
            @Override
            public void run() {
                callback.onFailure(new Exception());
                Platform.runLater(popup::hide);
            }
        }, 30000);

        acceptButton.setOnAction(event -> {
            callback.onSuccess();
            timerT.cancel();
            popup.hide();
        });
        rejectButton.setOnAction(event -> {
            callback.onFailure(new Exception());
            popup.hide();
        });

        popup.setScene(popupScene);
        popup.setOnCloseRequest(windowEvent -> {
            callback.onFailure(new Exception());
            Platform.runLater(popup::hide);
        });
        popup.setX((primaryStage.getWidth() / 2) - popup.getWidth());
        popup.setY((primaryStage.getHeight() / 2) - popup.getHeight());
        popup.show();
        popup.requestFocus();
    }
}
