package ui;

import dao.ItemDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Item;

public class EditItemController {

    @FXML private TextField nameField;
    @FXML private TextField hsnField;
    @FXML private ComboBox<String> unitCombo;
    @FXML private TextField rateField;
    @FXML private ComboBox<Double> gstCombo;

    private Item item;

    public void setItem(Item item) {
        this.item = item;

        nameField.setText(item.getName());
        hsnField.setText(item.getHsn());
        rateField.setText(String.valueOf(item.getRate()));
        unitCombo.setValue(item.getUnit());
        gstCombo.setValue(item.getGstPercent());
    }

    @FXML
    private void initialize() {
        unitCombo.getItems().setAll("Nos", "Kg", "Ltr", "Box", "Packet", "Meter");
        gstCombo.getItems().setAll(0.0, 5.0, 12.0, 18.0, 28.0);
    }

    @FXML
    private void update() {
        try {
            item.setHsn(hsnField.getText().trim());
            item.setUnit(unitCombo.getValue());
            item.setRate(Double.parseDouble(rateField.getText()));
            item.setGstPercent(gstCombo.getValue());

            ItemDAO.update(item);
            close();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Invalid data or update failed").showAndWait();
        }
    }

    @FXML
    private void close() {
        ((Stage) nameField.getScene().getWindow()).close();
    }
}

