package ui.nav;

import javafx.scene.Parent;

public class ViewState {
    public final String fxml;
    public final Parent view;

    public ViewState(String fxml, Parent view) {
        this.fxml = fxml;
        this.view = view;
    }
}

