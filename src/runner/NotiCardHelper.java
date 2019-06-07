package runner;

import backend.Notification;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.ocpsoft.prettytime.PrettyTime;

import java.awt.*;
import java.util.List;
import java.util.*;

public class NotiCardHelper { // TODO BETTER IMPLEMENTATION PLEASE (FOCUSING ISSUES) Look at
    // https://github.com/goxr3plus/FX-BorderlessScene ?
    private static final int CARD_WIDTH = 350;
    private static final int CARD_HEIGHT = 135;
    private static final int DISTANCE_FROM_SIDES = 25;
    private static final int DISTANCE_FROM_BOTTOM = 45;
    private static Stage tempStage = new Stage(StageStyle.UTILITY);
    private static Stage notiStage;
    private static Scene notiScene;
    private static List<Notification> queue = new ArrayList<>();
    private static boolean timerGoing = false;
    private static boolean hasQueue = false;
    private static Timer timer = new Timer();

    static void initialize(Parent notiParent) {
        //        tempStage.initOwner(Main.primaryStage);
        tempStage.setOpacity(0);
        notiStage = new Stage(StageStyle.UNDECORATED);
        notiStage.initOwner(tempStage);
        notiScene = new Scene(notiParent, CARD_WIDTH, CARD_HEIGHT);
        notiStage.setScene(notiScene);
        notiStage.setAlwaysOnTop(false);
        notiStage.setResizable(true);
        tempStage.focusedProperty().addListener(NotiCardHelper::changed);
        notiStage.focusedProperty().addListener(NotiCardHelper::changed);
    }

    private static void changed(
            ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
        Runnable r =
                () -> {
                    if (Main.primaryStage.isShowing() && !Main.primaryStage.isIconified())
                        Main.primaryStage.requestFocus();
                };
        Platform.runLater(r);
    }

    public static void showNotification(Notification noti) {
        if (!timerGoing && !hasQueue) {
            processNoti(noti, false);
        } else {
            queue.add(noti);
            hasQueue = true;
        }
    }

    private static void processNoti(Notification noti, boolean lastInQueue) {
        NotificationCard card = new NotificationCard(notiScene);
        card.setNotification(noti);
        Dimension screenSize =
                Toolkit.getDefaultToolkit()
                        .getScreenSize(); // TODO Multiple Monitor Option for Notifications
        notiStage.setX(screenSize.getWidth() - (CARD_WIDTH + DISTANCE_FROM_SIDES));
        notiStage.setY(screenSize.getHeight() - (CARD_HEIGHT + DISTANCE_FROM_BOTTOM));
        tempStage.show();
        notiStage.show();
        if (hasQueue && !lastInQueue) {
            notiHideTimer(2000L);
        } else {
            notiHideTimer(5000L);
        }
    }

    private static void notiHideTimer(long ms) {
        TimerTask tt =
                new TimerTask() {
                    @Override
                    public void run() {
                        notiStage.hide();
                        tempStage.hide();
                    }
                };
        TimerTask processQueue =
                new TimerTask() {
                    @Override
                    public void run() {
                        processQueue();
                    }
                };
        TimerTask tt2 =
                new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(tt);
                        timerGoing = false;
                        Platform.runLater(processQueue);
                    }
                };
        if (!timerGoing) {
            timer.schedule(tt2, ms);
            timerGoing = true;
        }
    }

    private static void processQueue() {
        if (hasQueue && queue.size() > 0) {
            Notification noti = queue.get(0);
            if (queue.size() == 1) {
                processNoti(noti, true);
            } else {
                processNoti(noti, false);
            }
            queue.remove(noti);
            if (queue.size() == 0) {
                hasQueue = false;
            }
        }
    }

    static class NotificationCard {
        private Label text, title, appName, time;
        private ImageView icon;
        private Button hideButton, replyButton;
        private Pane replyBar;
        private javafx.scene.control.TextField replyField;
        private TimerTask processQueue =
                new TimerTask() {
                    @Override
                    public void run() {
                        processQueue();
                    }
                };

        NotificationCard(Scene notiCard) {
            text = (Label) notiCard.lookup("#text");
            title = (Label) notiCard.lookup("#title");
            appName = (Label) notiCard.lookup("#appName");
            time = (Label) notiCard.lookup("#time");
            icon = (ImageView) notiCard.lookup("#icon");
            hideButton = (Button) notiCard.lookup("#hideButton");
            replyBar = (Pane) notiCard.lookup("#replyBar");
            replyButton = (Button) notiCard.lookup("#replyButton");
            replyField = (TextField) notiCard.lookup("#replyField");

            hideButton.setOnAction(
                    actionEvent -> {
                        timer.purge();
                        notiStage.hide();
                        tempStage.hide();
                        timerGoing = false;
                        Platform.runLater(processQueue);
                    });

        }

        void setNotification(Notification noti) {
            text.setText(noti.getText());
            title.setText(noti.getTitle());
            appName.setText(noti.getAppName());
            PrettyTime pt = new PrettyTime();
            String time = pt.format(new Date(noti.getTimeStamp()));
            this.time.setText(time);
            icon.setImage(new Image(noti.getIconInputStream()));
            if (noti.isRepliable()) {
                replyBar.setVisible(true);
                replyBar.setDisable(false);
                replyButton.setOnAction(actionEvent -> {
                    timer.purge();
                    notiStage.hide();
                    tempStage.hide();
                    timerGoing = false;
                    Platform.runLater(processQueue);

                });
            } else {
                replyBar.setVisible(false);
                replyBar.setDisable(true);
            }
        }
    }
}
