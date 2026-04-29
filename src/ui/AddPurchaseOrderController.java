package ui;

import dao.ContractDAO;
import dao.ItemDAO;
import dao.PurchaseOrderDAO;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import model.*;
import ui.interfaces.Navigable;

import java.time.LocalDate;

public class AddPurchaseOrderController implements Navigable {

    @FXML private TextField poNoField;
    @FXML private DatePicker poDatePicker;
    @FXML private ComboBox<Contract> contractBox;

    @FXML private TableView<PurchaseOrderItem> itemTable;
    @FXML private TableColumn<PurchaseOrderItem, Item> itemNameCol;
    @FXML private TableColumn<PurchaseOrderItem, String> hsnCol;
    @FXML private TableColumn<PurchaseOrderItem, String> unitCol;
    @FXML private TableColumn<PurchaseOrderItem, Double> qtyCol;
    @FXML private TableColumn<PurchaseOrderItem, Double> rateCol;
    @FXML private TableColumn<PurchaseOrderItem, Double> gstCol;
    @FXML private TableColumn<PurchaseOrderItem, Double> amountCol;

    @FXML private Label subtotalLabel;
    @FXML private Label gstLabel;
    @FXML private Label grandTotalLabel;

    private final ObservableList<PurchaseOrderItem> rows =
            FXCollections.observableArrayList();

    private final ObservableList<Item> itemMaster =
            FXCollections.observableArrayList(ItemDAO.findActiveItems());

    private final PurchaseOrderDAO poDAO = new PurchaseOrderDAO();
    private final ContractDAO contractDAO = new ContractDAO();

    @FXML
    public void initialize() {

        itemMaster.setAll(ItemDAO.findActiveItems());

        poDatePicker.setValue(LocalDate.now());
        itemTable.setEditable(true);
        itemTable.setItems(rows);

        // ITEM COLUMN (ComboBox)
        itemNameCol.setCellValueFactory(c ->
                c.getValue().itemProperty());

        itemNameCol.setCellFactory(ComboBoxTableCell.forTableColumn(
                new StringConverter<>() {
                    @Override
                    public String toString(Item item) {
                        return item == null ? "" : item.getName();
                    }
                    @Override
                    public Item fromString(String s) { return null; }
                },
                itemMaster
        ));

        itemNameCol.setOnEditCommit(e -> {
            PurchaseOrderItem row = e.getRowValue();
            Item selected = e.getNewValue();

            row.setItem(selected);

            if (selected != null) {
                row.setRate(selected.getRate());
                row.setGstPercent(selected.getGstPercent());
            }

            updateTotals();
        });

        // HSN + UNIT
        hsnCol.setCellValueFactory(c ->
                Bindings.createStringBinding(
                        () -> {
                            Item item = c.getValue().getItem();
                            return item != null ? item.getHsn() : "";
                        },
                        c.getValue().itemProperty()
                )
        );

        unitCol.setCellValueFactory(c ->
                Bindings.createStringBinding(
                        () -> {
                            Item item = c.getValue().getItem();
                            return item != null ? item.getUnit() : "";
                        },
                        c.getValue().itemProperty()
                )
        );

        // QTY
        qtyCol.setCellValueFactory(c ->
                c.getValue().qtyProperty().asObject());

        qtyCol.setCellFactory(TextFieldTableCell.forTableColumn(
                new DoubleStringConverter()));

        qtyCol.setOnEditCommit(e -> {
            e.getRowValue().setQty(e.getNewValue());
            updateTotals();
        });

        // RATE
        rateCol.setCellValueFactory(c ->
                c.getValue().rateProperty().asObject());

        rateCol.setCellFactory(TextFieldTableCell.forTableColumn(
                new DoubleStringConverter()));

        rateCol.setOnEditCommit(e -> {
            e.getRowValue().setRate(e.getNewValue());
            updateTotals();
        });

        // GST
        gstCol.setCellValueFactory(c ->
                c.getValue().gstPercentProperty().asObject());

        gstCol.setCellFactory(TextFieldTableCell.forTableColumn(
                new DoubleStringConverter()));

        gstCol.setOnEditCommit(e -> {
            e.getRowValue().setGstPercent(e.getNewValue());
            updateTotals();
        });

        // AMOUNT
        amountCol.setCellValueFactory(c ->
                Bindings.createObjectBinding(
                        () -> c.getValue().calculateTaxable(),
                        c.getValue().qtyProperty(),
                        c.getValue().rateProperty()
                ));

        addRow();
    }

    @FXML
    private void addRow() {
        rows.add(new PurchaseOrderItem());
    }

    private void updateTotals() {

        double subtotal = 0;
        double gstTotal = 0;

        for (PurchaseOrderItem item : rows) {
            double taxable = item.calculateTaxable();
            double gst = item.calculateGstAmount();

            subtotal += taxable;
            gstTotal += gst;
        }

        subtotalLabel.setText(String.format("%.2f", subtotal));
        gstLabel.setText(String.format("%.2f", gstTotal));
        grandTotalLabel.setText(String.format("%.2f", subtotal + gstTotal));
    }

    @FXML
    private void savePO() {

        if (poNoField.getText().isBlank()) {
            alert("PO number required");
            return;
        }

        if (rows.stream().anyMatch(r -> r.getItem() == null || r.getQty() <= 0)) {
            alert("All rows must have item and qty > 0");
            return;
        }

        try {

            PurchaseOrder po = new PurchaseOrder();

            po.setPoNo(poNoField.getText().trim());
            po.setPoDate(poDatePicker.getValue());
            po.setStatus(POStatus.OPEN);

            double subtotal = rows.stream()
                    .mapToDouble(PurchaseOrderItem::calculateTaxable)
                    .sum();

            double gstTotal = rows.stream()
                    .mapToDouble(PurchaseOrderItem::calculateGstAmount)
                    .sum();

            po.setSubtotal(subtotal);
            po.setGstTotal(gstTotal);
            po.setGrandTotal(subtotal + gstTotal);
            po.setItems(rows);

            poDAO.save(po);

            alert("PO Saved");

        } catch (Exception e) {
            e.printStackTrace();
            alert("Save failed");
        }
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private DashboardController dashboard;

    @Override
    public void setDashboard(DashboardController dashboard) {
        this.dashboard = dashboard;
    }

    @Override
    public void onNavigateTo() {
        Navigable.super.onNavigateTo();
    }

    @Override
    public void onNavigateFrom() {
        Navigable.super.onNavigateFrom();
    }
}