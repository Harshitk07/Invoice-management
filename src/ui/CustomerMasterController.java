package ui;

import context.security.CapabilityContext;
import dao.CustomerDAO;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.GridPane;
import model.Customer;
import ui.interfaces.Navigable;
import context.Capability;


public class CustomerMasterController implements Navigable{

    private DashboardController dashboard;


    private void applyCapabilityUI() {

        boolean canEditCustomers =
                CapabilityContext.get().has(Capability.CUSTOMER_MASTER_EDIT);

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
        addressCol.setCellFactory(col -> new TableCell<>() {

            private final Label label = new Label();

            {
                label.setWrapText(true);
                label.setStyle(
                        "-fx-font-size:13.5;" +
                                "-fx-text-fill:#111827;"
                );

                label.setMaxWidth(Double.MAX_VALUE);
            }

            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);

                if (empty || text == null) {
                    setGraphic(null);
                } else {
                    label.setText(text);
                    setGraphic(label);
                }
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                // preserve selection color
                label.setTextFill(
                        selected ? javafx.scene.paint.Color.WHITE
                                : javafx.scene.paint.Color.web("#111827")
                );
            }
        });

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

                btn.setOnAction(e -> {
                    Customer c = getTableView()
                            .getItems()
                            .get(getIndex());

                    if (!CapabilityContext.get().has(Capability.CUSTOMER_MASTER_EDIT)) {
                        new Alert(Alert.AlertType.WARNING,
                                "You do not have permission to edit customers.")
                                .showAndWait();
                        return;
                    }

                    openEditDialog(c);
                });
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

        if (!CapabilityContext.get().has(Capability.CUSTOMER_MASTER_EDIT)) {
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

        // ===== Fields =====
        TextField name = new TextField();
        TextField gst = new TextField();
        TextField state = new TextField();
        TextField code = new TextField();

        TextArea address = new TextArea();
        address.setWrapText(true);
        address.setPrefRowCount(4);

        name.setPromptText("Customer Name");
        gst.setPromptText("GST Number");
        state.setPromptText("State");
        code.setPromptText("State Code");
        address.setPromptText("Full Address");

        // Uniform sizing
        for (Control ctrl : new Control[]{name, gst, state, code}) {
            ctrl.setPrefHeight(34);
            ctrl.setStyle("-fx-font-size:13.5;");
        }

        address.setStyle("-fx-font-size:13.5;");

        // ===== Layout =====
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        grid.add(new Label("Name *"), 0, 0);
        grid.add(name, 1, 0);

        grid.add(new Label("GST No"), 0, 1);
        grid.add(gst, 1, 1);

        grid.add(new Label("State"), 0, 2);
        grid.add(state, 1, 2);

        grid.add(new Label("State Code"), 0, 3);
        grid.add(code, 1, 3);

        grid.add(new Label("Address"), 0, 4);
        grid.add(address, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // ===== Validation + Result =====
        dialog.setResultConverter(bt -> {
            if (bt == saveBtn) {
                if (name.getText().isBlank()) {
                    return null;
                }

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

        // ===== Fields =====
        TextField name = new TextField(c.getName());
        TextField gst = new TextField(c.getGstNo());
        TextField state = new TextField(c.getState());
        TextField code = new TextField(c.getStateCode());

        TextArea address = new TextArea(c.getAddress());
        address.setWrapText(true);
        address.setPrefRowCount(4);

        // Uniform sizing
        for (Control ctrl : new Control[]{name, gst, state, code}) {
            ctrl.setStyle("-fx-font-size:13.5;");
            ctrl.setPrefHeight(34);
        }

        address.setStyle("-fx-font-size:13.5;");

        // ===== Layout =====
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        grid.add(new Label("Name"), 0, 0);
        grid.add(name, 1, 0);

        grid.add(new Label("GST No"), 0, 1);
        grid.add(gst, 1, 1);

        grid.add(new Label("State"), 0, 2);
        grid.add(state, 1, 2);

        grid.add(new Label("State Code"), 0, 3);
        grid.add(code, 1, 3);

        grid.add(new Label("Address"), 0, 4);
        grid.add(address, 1, 4);

        GridPane.setColumnSpan(address, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt == saveBtn) {
                c.setName(name.getText().trim());
                c.setAddress(address.getText().trim());
                c.setGstNo(gst.getText().trim());
                c.setState(state.getText().trim());
                c.setStateCode(code.getText().trim());
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
