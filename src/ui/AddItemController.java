package ui;

import dao.ItemDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Item;

public class AddItemController {

    @FXML private TextField nameField;
    @FXML private TextField hsnField;
    @FXML private TextField unitField;
    @FXML private TextField rateField;
    @FXML private ComboBox<Double> gstCombo;


    private Stage stage;
    private boolean dataChanged = false;

    @FXML
    private void initialize() {

        gstCombo.getItems().setAll(
                0.0,
                5.0,
                12.0,
                18.0,
                28.0
        );

        // Default selection (important UX)
        gstCombo.getSelectionModel().select(1); // 5%
    }


    /* =======================
       LIFECYCLE
       ======================= */

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Optional: prefill name when invoked from invoice screen
     */
    public void prefillName(String name) {
        if (name == null || name.isBlank()) return;
        nameField.setText(name.trim());
        nameField.requestFocus();
        nameField.positionCaret(nameField.getText().length());
    }

    /* =======================
       ACTIONS
       ======================= */

    @FXML
    private void save() {

        String name = nameField.getText().trim();
        String hsn  = hsnField.getText().trim();
        String unit = unitField.getText().trim();

        if (name.isEmpty()) {
            error("Item name is required");
            return;
        }

        double rate;

        try {
            rate = Double.parseDouble(rateField.getText().trim());
        } catch (NumberFormatException e) {
            error("Rate must be a valid number");
            return;
        }

        Double gst = gstCombo.getValue();
        if (gst == null) {
            error("Select GST rate");
            return;
        }


        if (rate <= 0) {
            error("Rate must be greater than zero");
            return;
        }

        Item item = new Item();
        item.setName(name);
        item.setHsn(hsn);
        item.setUnit(unit);
        item.setRate(rate);
        item.setGstPercent(gst);

        try {
            ItemDAO.save(item);
            dataChanged = true;
            close();

        } catch (Exception e) {
            e.printStackTrace();
            error("Item already exists or could not be saved");
        }
    }

    @FXML
    private void close() {
        if (stage == null) {
            stage = (Stage) nameField.getScene().getWindow();
        }
        stage.close();
    }

    /* =======================
       UTIL
       ======================= */

    private void error(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    public boolean isDataChanged() {
        return dataChanged;
    }
}
