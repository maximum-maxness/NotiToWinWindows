package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeView;
import runner.Main;

public class JSONViewerController {
    @FXML
    private Button toConfigButton;

    @FXML
    private TreeView jsonTree;

    @FXML
    private void changeViewToConfig() {
        Main.changeViewToConfig();
    }


}
