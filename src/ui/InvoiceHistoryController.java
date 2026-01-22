package ui;

import dao.InvoiceDAO;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Invoice;
import model.InvoiceCopyType;
import model.InvoiceItem;
import ui.interfaces.Navigable;

import java.util.List;

public class InvoiceHistoryController implements Navigable {

    @FXML private TableView<Invoice> table;
    private DashboardController dashboard;

    @FXML private TableColumn<Invoice, Integer> colNo;
    @FXML private TableColumn<Invoice, String> colDate;
    @FXML private TableColumn<Invoice, String> colBuyer;
    @FXML private TableColumn<Invoice, Double> colTotal;

    @FXML
    public void initialize() {

        // ---------- Column bindings (READ-ONLY) ----------
        colNo.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getInvoiceNo()));

        colDate.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getInvoiceDateFormatted()));

        colBuyer.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getBuyerName()));

        colTotal.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getGrandTotal()));

        // ---------- Load data ----------
        table.getItems().setAll(InvoiceDAO.listInvoices());

        // ---------- UX defaults ----------
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /* ================= ACTIONS ================= */


    @FXML
    private void preview() {

        Invoice selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Select an invoice");
            return;
        }

        int invoiceNo = selected.getInvoiceNo();

        // 🔑 ALWAYS reload FULL invoice for preview/print
        Invoice fullInvoice = InvoiceDAO.findInvoiceByNo(invoiceNo);

        List<InvoiceItem> items =
                InvoiceDAO.findItemsByInvoiceNo(invoiceNo);

        if (items == null || items.isEmpty()) {
            showInfo("Invoice items not found");
            return;
        }

        InvoiceController.openPreviewWindow(
                fullInvoice,
                items,
                InvoiceCopyType.ORIGINAL
        );
    }

    public void setDashboard(DashboardController dashboard) {
        this.dashboard = dashboard;
    }

    @FXML
    private void close() {
        dashboard.goBack();
    }


    /* ================= UTIL ================= */

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
