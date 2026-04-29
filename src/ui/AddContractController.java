package ui;

import dao.ContractDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Contract;
import ui.interfaces.Navigable;

public class AddContractController implements Navigable {

    @FXML private TextField contractNoField;
    @FXML private TextField descriptionField;
    @FXML private TextField baseQtyField;
    @FXML private TextField variationField;
    @FXML private TextField maxQtyField;
    @FXML private TextField baseValueField;
    @FXML private TextField maxValueField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    private final ContractDAO contractDAO = new ContractDAO();

    private DashboardController dashboard;

    @Override
    public void setDashboard(DashboardController dashboard) {
        this.dashboard = dashboard;
    }

    @Override
    public void onNavigateTo() {
        // optional: reset form fields here if needed
    }

    @Override
    public void onNavigateFrom() {
        // optional cleanup
    }

    @FXML
    public void initialize() {

        baseQtyField.textProperty().addListener((obs,o,n) -> recalc());
        variationField.textProperty().addListener((obs,o,n) -> recalc());
        baseValueField.textProperty().addListener((obs,o,n) -> recalc());
    }

    private void recalc() {

        double baseQty = parse(baseQtyField.getText());
        double variation = parse(variationField.getText());
        double baseValue = parse(baseValueField.getText());

        double maxQty = baseQty * (1 + variation / 100.0);
        double maxVal = baseValue * (1 + variation / 100.0);

        maxQtyField.setText(String.format("%.2f", maxQty));
        maxValueField.setText(String.format("%.2f", maxVal));
    }

    @FXML
    private void onSave() {

        if (contractNoField.getText().isBlank()) {
            alert("Contract number required");
            return;
        }

        try {

            Contract c = new Contract();

            c.setContractNo(contractNoField.getText().trim());
            c.setDescription(descriptionField.getText());
            c.setBaseQuantity(parse(baseQtyField.getText()));
            c.setVariationPercent(parse(variationField.getText()));
            c.setMaxQuantity(parse(maxQtyField.getText()));
            c.setBaseValue(parse(baseValueField.getText()));
            c.setMaxValue(parse(maxValueField.getText()));
            c.setStartDate(startDatePicker.getValue());
            c.setEndDate(endDatePicker.getValue());
            c.setStatus("ACTIVE");

            contractDAO.save(c);

            close();

        } catch (Exception e) {
            alert("Failed to save contract");
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) contractNoField.getScene().getWindow();
        stage.close();
    }

    private double parse(String s) {
        if (s == null || s.isBlank()) return 0;
        return Double.parseDouble(s);
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}