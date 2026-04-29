package ui;

import dao.PurchaseOrderDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import model.PurchaseOrder;
import ui.interfaces.Navigable;

import java.time.LocalDate;
import java.util.List;

public class PurchaseOrderListController implements Navigable {

    private DashboardController dashboard;

    @FXML private TableView<PurchaseOrder> poTable;

    @FXML private TableColumn<PurchaseOrder, String> poNoCol;
    @FXML private TableColumn<PurchaseOrder, LocalDate> poDateCol;
    @FXML private TableColumn<PurchaseOrder, LocalDate> deliveryDateCol;
    @FXML private TableColumn<PurchaseOrder, Double> totalCol;
    @FXML private TableColumn<PurchaseOrder, String> statusCol;

    private final PurchaseOrderDAO poDAO = new PurchaseOrderDAO();

    @FXML
    public void initialize() {

        poNoCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getPoNo()));

        poDateCol.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getPoDate()));

        deliveryDateCol.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getDeliveryByDate()));

        totalCol.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getGrandTotal()).asObject());

        statusCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatus().name()));

        loadData();
    }

    private void loadData() {
        List<PurchaseOrder> list = poDAO.findAll();
        poTable.setItems(FXCollections.observableArrayList(list));
    }

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