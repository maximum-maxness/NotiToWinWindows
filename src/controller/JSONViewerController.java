package controller;

import backend.Client;
import backend.Notification;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.ocpsoft.prettytime.PrettyTime;
import runner.Main;
import server.Processing.NotiToTree;

import java.util.Date;

import static controller.ConfigureViewController.discovery;

public class JSONViewerController {

    private ListProperty<Client> clientProperty = new SimpleListProperty<>();
    private int currentClient;

    @FXML
    private Button toConfigButton;

    @FXML
    private ListView<Notification> notiList;

    @FXML
    private ChoiceBox<Client> clientList;

    @FXML
    private TreeView<String> jsonTree;

    @FXML
    private ImageView iconView;
    //
    //    JSONViewerController(){
    //
    //    }

    @FXML
    private void initialize() {
        initList();
        initChoiceBox();
    }

    private void initChoiceBox() {
        clientList.setConverter(
                new StringConverter<Client>() {
                    @Override
                    public String toString(Client object) {
                        return object.getName();
                    }

                    @Override
                    public Client fromString(String string) {
                        return null;
                    }
        });
        clientList.setItems(clientProperty);
        clientProperty.set(FXCollections.observableArrayList(discovery.clients));

        clientList
                .getSelectionModel()
                .selectedIndexProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (newValue.intValue() != -1) updateNotiList(newValue.intValue());
                            System.err.println("Client at index: " + newValue.intValue() + " Selected!");
                        });
    }

    private void updateNotiList(int index) {
        currentClient = index;
        ListProperty notiListProp = new SimpleListProperty();

        notiList.setItems(notiListProp);
        notiListProp.set(
                FXCollections.observableArrayList(
                        discovery.clients.get(currentClient).getNotificationList()));

        notiList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        notiList
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            getSelectedNoti(newValue);
                        });
    }

    public void refreshNotis() {
        if (currentClient != -1) updateNotiList(currentClient);
    }

    private void getSelectedNoti(Notification noti) {
        if (noti != null) {
            iconView.setImage(new Image(noti.getIconInputStream()));
            //            noti.display();
            TreeItem<String> root = NotiToTree.convert(noti);
            NotiToTree.expandTreeView(root);
            jsonTree.setRoot(root);
        }
    }

    private void initList() {
        notiList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        notiList.setCellFactory(
                new Callback<ListView<Notification>, ListCell<Notification>>() {
                    @Override
                    public ListCell<Notification> call(ListView param) {
                        ListCell<Notification> cell =
                                new ListCell<Notification>() {
                                    @Override
                                    protected void updateItem(Notification item, boolean empty) {
                                        super.updateItem(item, empty);
                                        if (item != null) {
                                            PrettyTime pt = new PrettyTime();
                                            setText(item.getAppName() + " - " + pt.format(new Date(item.getTimeStamp())));
                                        } else {
                                            setText("");
                                        }
                                    }
                };
                        return cell;
                    }
        });
    }

    @FXML
    private void changeBoxSelection() {
    }

    @FXML
    private void changeViewToConfig() {
        Main.changeViewToConfig();
        notiList.setItems(FXCollections.emptyObservableList());
    }
}
