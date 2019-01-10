package controller;

import javafx.scene.control.TextArea;

import java.io.OutputStream;

class Console extends OutputStream {

    private final TextArea output;

    public Console(TextArea ta) {
        this.output = ta;
    }

    @Override
    public void write(int i) {
        output.appendText(String.valueOf((char) i));
    }
}
