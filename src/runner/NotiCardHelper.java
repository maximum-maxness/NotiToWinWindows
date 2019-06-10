package runner;

import backend.Client;
import backend.JSONConverter;
import backend.Notification;
import backend.PacketType;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.ocpsoft.prettytime.PrettyTime;

import java.awt.*;
import java.util.List;
import java.util.*;

public class NotiCardHelper { // TODO BETTER IMPLEMENTATION (FOCUSING ISSUES) Look at https://github.com/goxr3plus/FX-BorderlessScene ?
    private static final int CARD_WIDTH = 350;
    private static final int CARD_HEIGHT = 162;
    private static final int DISTANCE_FROM_SIDES = 25;
    private static final int DISTANCE_FROM_BOTTOM = 45;
    private static Stage tempStage = new Stage(StageStyle.UTILITY);
    private static Stage notiStage;
    private static Scene notiScene;
    private static List<Notification> queue = new ArrayList<>();
    private static Notification NOTI_PLACEHOLDER = new Notification();
    private static Notification currentlyDisplayedNoti = NOTI_PLACEHOLDER;
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
//        tempStage.focusedProperty().addListener(NotiCardHelper::changed);
//        notiStage.focusedProperty().addListener(NotiCardHelper::changed);
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
            if (!queue.contains(noti) && currentlyDisplayedNoti != noti) {
                queue.add(noti);
                hasQueue = true;
            }
        }
    }

    private static void processNoti(Notification noti, boolean lastInQueue) {
        currentlyDisplayedNoti = noti;
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
                        while (true)
                            if (!NotificationCard.replying) {
                                Platform.runLater(tt);
                                currentlyDisplayedNoti = NOTI_PLACEHOLDER;
                                timerGoing = false;
                                Platform.runLater(processQueue);
                                break;
                            } else {
                                try {
                                    Thread.sleep(500L);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                    }
                };
        if (!timerGoing) {
            try {
                timer.schedule(tt2, ms);
            } catch (IllegalStateException e) {
                timer = new Timer();
                timer.schedule(tt2, ms);
            }
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

        public static boolean replying = false;

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
                        replyField.setText("");
                        timerGoing = false;
                        replying = false;
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
                System.out.println("Making repliable notification card!");
                replyBar.setVisible(true);
                replyBar.setDisable(false);
                replyField.setOnMouseClicked(event -> {
                    replying = true;
                    System.out.println("Reply field clicked!");
                });
                replyButton.setOnAction(actionEvent -> replyAction(noti));
                replyField.setOnKeyPressed(ke -> {
                    if (ke.getCode().equals(KeyCode.ENTER)) {
                        replyAction(noti);
                    }
                });
                replyField.requestFocus();
            } else {
                replyBar.setVisible(false);
                replyBar.setDisable(true);
            }
        }

        private void replyAction(Notification noti) {
            replying = false;
            Client.SendPacketStatusCallback callback = new Client.SendPacketStatusCallback() {
                @Override
                public void onSuccess() {
                    currentlyDisplayedNoti = NOTI_PLACEHOLDER;
                    notiStage.hide();
                    tempStage.hide();
                    replyField.clear();
                }

                @Override
                public void onFailure(Throwable e) {
                    e.printStackTrace();
                }
            };
            sendReply(noti.getRequestReplyId(), replyField.getText(), noti.getClientID(), callback);
        }

        void sendReply(String replyID, String message, String clientID, Client.SendPacketStatusCallback callback) {
            JSONConverter packet = new JSONConverter(PacketType.NOTIFICATION_ACTION);
            packet.set("requestReplyId", replyID);
            packet.set("message", message);
            Platform.runLater(() -> {
                Main.backgroundThread.getClient(clientID).sendPacket(packet, callback);
            });
        }
    }
}
