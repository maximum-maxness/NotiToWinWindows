package controller;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.io.OutputStream;

class Console extends OutputStream {

    private final TextArea output;

    public Console(TextArea ta) {
        this.output = ta;
    }

    @Override
    public void write(final int i) throws IOException {
        Platform.runLater(() -> output.appendText(String.valueOf((char) i)));
    }
}
