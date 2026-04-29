package ui;

import dao.ContractDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import model.Contract;
import ui.interfaces.Navigable;

import java.time.LocalDate;
import java.util.List;

public class ContractListController implements Navigable {

    private DashboardController dashboard;

    @FXML private TableView<Contract> contractTable;

    @FXML private TableColumn<Contract, String> contractNoCol;
    @FXML private TableColumn<Contract, LocalDate> startDateCol;
    @FXML private TableColumn<Contract, LocalDate> endDateCol;
    @FXML private TableColumn<Contract, Double> valueCol;
    @FXML private TableColumn<Contract, String> statusCol;

    private final ContractDAO contractDAO = new ContractDAO();

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

    @FXML
    public void initialize() {

        contractNoCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getContractNo()));

        startDateCol.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getStartDate()));

        endDateCol.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getEndDate()));

        // Use baseValue instead of contractValue
        valueCol.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getBaseValue()).asObject());

        // status is already String
        statusCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatus()));

        loadData();
    }

    private void loadData() {
        List<Contract> list = contractDAO.findAll();
        contractTable.setItems(FXCollections.observableArrayList(list));
    }
}