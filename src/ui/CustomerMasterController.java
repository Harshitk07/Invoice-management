package ui;

import dao.CustomerDAO;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.GridPane;
import model.Customer;
import ui.interfaces.Navigable;
import context.Capability;
import context.CapabilityGate;


public class CustomerMasterController implements Navigable{

    private DashboardController dashboard;


    private void applyCapabilityUI() {

        boolean canEditCustomers =
                CapabilityGate.allowed(Capability.CUSTOMER_MASTER_EDIT);

        table.setEditable(canEditCustomers);
        activeCol.setEditable(canEditCustomers);
    }



    @Override
    public void setDashboard(DashboardController dashboard) {
        this.dashboard = dashboard;
    }

    @FXML private TableView<Customer> table;
    @FXML private TableColumn<Customer, String> nameCol;
    @FXML private TableColumn<Customer, String> addressCol;
    @FXML private TableColumn<Customer, String> stateCol;
    @FXML private TableColumn<Customer, String> gstCol;
    @FXML private TableColumn<Customer, Boolean> activeCol;
    @FXML private TableColumn<Customer, Void> editCol;

    private final ObservableList<Customer> data =
            FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        table.setFixedCellSize(60);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);


        // ---------- TABLE ----------
        table.setEditable(true);
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        addressCol.setCellValueFactory(c -> c.getValue().addressProperty());
        stateCol.setCellValueFactory(c -> c.getValue().stateProperty());
        gstCol.setCellValueFactory(c -> c.getValue().gstNoProperty());
        nameCol.setStyle("-fx-alignment: CENTER-LEFT;");
        addressCol.setStyle("-fx-alignment: CENTER-LEFT;");
        stateCol.setStyle("-fx-alignment: CENTER-LEFT;");
        gstCol.setStyle("-fx-alignment: CENTER-LEFT;");

        // ---------- ACTIVE TOGGLE (CORRECT WAY) ----------
        activeCol.setEditable(true);
        activeCol.setStyle("-fx-alignment: CENTER;");
        activeCol.setCellValueFactory(c -> {
            Customer cust = c.getValue();

            cust.activeProperty().addListener((obs, oldVal, newVal) -> {
                CustomerDAO.updateActive(cust.getId(), newVal);
            });

            return cust.activeProperty();
        });

        activeCol.setCellFactory(CheckBoxTableCell.forTableColumn(activeCol));



        // ---------- EDIT BUTTON ----------
        editCol.setCellFactory(col -> new TableCell<>() {

            private final Button btn = new Button("Edit");

            {
                btn.setStyle(
                        "-fx-padding:4 10;" +
                                "-fx-font-size:12;"
                );
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                    setAlignment(Pos.CENTER);
                }
            }
        });



        // ---------- VISUAL GREY FOR INACTIVE ----------
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Customer c, boolean empty) {
                super.updateItem(c, empty);
                if (c == null || empty) {
                    setStyle("");
                } else if (!c.isActive()) {
                    setStyle("-fx-opacity:0.45;");
                } else {
                    setStyle("");
                }
            }
        });

        reload();
        table.setItems(data);
        applyCapabilityUI();

    }


    // ================= ADD CUSTOMER =================

    @FXML
    private void openAddCustomerDialog() {

        if (!CapabilityGate.allowed(Capability.CUSTOMER_MASTER_EDIT)) {
            new Alert(Alert.AlertType.WARNING,
                    "You do not have permission to add customers.")
                    .showAndWait();
            return;
        }


        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Add Customer");

        ButtonType saveBtn =
                new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(saveBtn, ButtonType.CANCEL);

        TextField name = new TextField();
        TextArea address = new TextArea();
        TextField gst = new TextField();
        TextField state = new TextField();
        TextField code = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Name*"), 0, 0);
        grid.add(name, 1, 0);
        grid.add(new Label("Address"), 0, 1);
        grid.add(address, 1, 1);
        grid.add(new Label("GST No"), 0, 2);
        grid.add(gst, 1, 2);
        grid.add(new Label("State"), 0, 3);
        grid.add(state, 1, 3);
        grid.add(new Label("State Code"), 0, 4);
        grid.add(code, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt == saveBtn && !name.getText().isBlank()) {
                Customer c = new Customer();
                c.setName(name.getText().trim());
                c.setAddress(address.getText().trim());
                c.setGstNo(gst.getText().trim());
                c.setState(state.getText().trim());
                c.setStateCode(code.getText().trim());
                c.setActive(true);
                return c;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(c -> {
            CustomerDAO.save(c);
            reload();
        });
    }

    // ================= EDIT CUSTOMER =================

    private void openEditDialog(Customer c) {

        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Edit Customer");

        ButtonType saveBtn =
                new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(saveBtn, ButtonType.CANCEL);

        TextField name = new TextField(c.getName());
        TextArea address = new TextArea(c.getAddress());
        TextField gst = new TextField(c.getGstNo());
        TextField state = new TextField(c.getState());
        TextField code = new TextField(c.getStateCode());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Name"), 0, 0);
        grid.add(name, 1, 0);
        grid.add(new Label("Address"), 0, 1);
        grid.add(address, 1, 1);
        grid.add(new Label("GST No"), 0, 2);
        grid.add(gst, 1, 2);
        grid.add(new Label("State"), 0, 3);
        grid.add(state, 1, 3);
        grid.add(new Label("State Code"), 0, 4);
        grid.add(code, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt == saveBtn) {
                c.setName(name.getText());
                c.setAddress(address.getText());
                c.setGstNo(gst.getText());
                c.setState(state.getText());
                c.setStateCode(code.getText());
                return c;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            CustomerDAO.update(updated);
            reload();
        });
    }

    // ================= UTIL =================

    private void reload() {
        data.setAll(CustomerDAO.findAll());
    }

    @Override
    public void onNavigateTo() {
        reload();
        applyCapabilityUI();
    }


}
