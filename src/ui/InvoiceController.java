package ui;

import dao.CustomerDAO;
import dao.InvoiceDAO;
import dao.ItemDAO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;
import model.*;
import print.PrintInvoiceBuilder;
import ui.interfaces.Navigable;
import ui.interfaces.Refreshable;
import ui.interfaces.RoleAware;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvoiceController implements Refreshable, RoleAware, Navigable {

    @Override
    public void refresh() {
        reloadCustomers();
    }

    private DashboardController dashboard;
    private UserRole role;

    @Override
    public void setDashboard(DashboardController dashboard) {
        this.dashboard = dashboard;
    }

    @Override
    public void setRole(UserRole role) {
        this.role = role;
    }

    @Override
    public void onNavigateTo() {
        // Called every time InvoiceView is shown
        // Good place to reset UI / refresh state if needed
    }

    @Override
    public void onNavigateFrom() {
        // Called before navigating away
        // Cleanup if required
    }


    /* ================= CONSTANTS ================= */

    private static final int SELLER_STATE_CODE = 37;
    private static final double DEFAULT_ZOOM = 1.25; // 125%
    private final ObservableList<Customer> customers =
            FXCollections.observableArrayList();
    @FXML private ScrollPane root;


    /* ================= HEADER ================= */

    @FXML private TextField invoiceNoField;
    @FXML private DatePicker invoiceDatePicker;

    /* ================= BUYER ================= */

    @FXML private ComboBox<Customer> customerBox;
    @FXML private TextArea buyerAddressArea;
    @FXML private TextField buyerGstField;
    @FXML private TextField buyerStateField;
    @FXML private TextField buyerStateCodeField;

    /* ================= CONSIGNEE ================= */

    @FXML private CheckBox sameAsBuyerCheck;
    @FXML private TextField consigneeNameField;
    @FXML private TextArea consigneeAddressArea;
    @FXML private TextField consigneeGstField;
    @FXML private TextField consigneeStateField;
    @FXML private TextField consigneeStateCodeField;

    /* ================= TABLE ================= */

    @FXML private TableView<InvoiceItem> table;
    @FXML private TableColumn<InvoiceItem, Number> slNo;
    @FXML private TableColumn<InvoiceItem, String> itemCol;
    @FXML private TableColumn<InvoiceItem, String> hsnCol;
    @FXML private TableColumn<InvoiceItem, String> unitCol;
    @FXML private TableColumn<InvoiceItem, Double> qtyCol;
    @FXML private TableColumn<InvoiceItem, Double> rateCol;
    @FXML private TableColumn<InvoiceItem, Double> gstCol;
    @FXML private TableColumn<InvoiceItem, Double> amountCol;
    @FXML
    private TableColumn<InvoiceItem, Void> deleteCol;


    /* ================= TOTALS ================= */

    @FXML private Label taxableAmountLabel;
    @FXML private Label cgstLabel;
    @FXML private Label sgstLabel;
    @FXML private Label igstLabel;
    @FXML private Label cgstTextLabel;
    @FXML private Label sgstTextLabel;
    @FXML private Label igstTextLabel;
    @FXML private Label roundOffLabel;
    @FXML private Label totalLabel;
    @FXML private CheckBox roundOffCheck;
    @FXML private TextField roundOffField;


    /* ================= META ================= */

    @FXML private TextField poNoField;
    @FXML private DatePicker poDatePicker;
    @FXML private TextField dcNoField;
    @FXML private TextField dispatchThroughField;
    @FXML private TextField ewayBillField;
    @FXML private DatePicker dcDatePicker;
    @FXML private Button savePrintButton;
    @FXML private Button saveButton;
    @FXML private Button printBtn;
    @FXML private Button previewButton;


    /* ================= STATE ================= */

    private final ObservableList<InvoiceItem> rows = FXCollections.observableArrayList();
    private List<Item> masterItems;
    private ObservableList<String> itemNames;
    private Boolean lockedIntraState = null;
    private boolean isDraft = true;
    private Customer previousCustomer = null;


    /* ================= CALCULATED TOTALS ================= */

    private double subtotal = 0;
    private double cgst = 0;
    private double sgst = 0;
    private double igst = 0;
    private double grandTotal = 0;


    /* ================= INIT ================= */

    @FXML
    public void initialize() {

        invoiceDatePicker.setValue(LocalDate.now());
        invoiceNoField.setText("AUTO");
        invoiceNoField.setEditable(false);
        roundOffField.setDisable(true);

        roundOffCheck.selectedProperty().addListener((obs, o, isOn) -> {
            roundOffField.setDisable(!isOn);
            recalc();
        });

        roundOffField.textProperty().addListener((obs, o, n) -> recalc());


        customers.setAll(CustomerDAO.findActive());
        customerBox.setItems(customers);

        customerBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Customer c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText(null);
                } else {
                    setText(c.getName());
                    setDisable(!c.isActive());
                    setOpacity(c.isActive() ? 1.0 : 0.4);
                }
            }
        });

        customerBox.valueProperty().addListener((obs, oldVal, newVal) -> {

            // 🔒 HARD BLOCK: do not allow change if items exist
            if (hasActualItems()) {
                Platform.runLater(() -> customerBox.setValue(previousCustomer));
                info("Remove all items to change customer");
                return;
            }

            previousCustomer = newVal;

            if (newVal == null) return;

            buyerAddressArea.setText(newVal.getAddress());
            buyerGstField.setText(newVal.getGstNo());
            buyerStateField.setText(newVal.getState());
            buyerStateCodeField.setText(newVal.getStateCode());

            if (sameAsBuyerCheck.isSelected()) {
                copyBuyerToConsignee(newVal);
            }

            recalc();
        });

        invoiceDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> {

            if (!isDraft) {
                Platform.runLater(() ->
                        invoiceDatePicker.setValue(oldDate)
                );
                info("Invoice date cannot be changed after saving");
            }
        });



        customerBox.setEditable(false);
        setupTable();
        addItemRow();
    }


    /* ================= TABLE ================= */

    private void setupTable() {

        // ---------- MASTER DATA ----------
        masterItems = ItemDAO.findActiveItems();
        itemNames = FXCollections.observableArrayList(
                masterItems.stream()
                        .map(Item::getName)
                        .toList()
        );


        table.setItems(rows);
        table.setEditable(true);

        // ---------- COLUMN RESIZE POLICY ----------
        // Absolutely mandatory for print-style tables
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // ---------- FIX TABLE WIDTH BEHAVIOR ----------
        table.setPrefWidth(Control.USE_COMPUTED_SIZE);
        table.setMinWidth(Control.USE_PREF_SIZE);
        table.setMaxWidth(Control.USE_PREF_SIZE);

        // ---------- FIX ROW HEIGHT ----------
        table.setFixedCellSize(24);

        // ---------- SL NO ----------
        slNo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });


        // ---------- ITEM COLUMN ----------
        itemCol.setCellValueFactory(cell -> cell.getValue().itemNameProperty());

        itemCol.setCellFactory(col ->
                new ComboBoxTableCell<>(
                        FXCollections.observableArrayList(itemNames)
                )
        );


        itemCol.setOnEditCommit(e -> {
            InvoiceItem row = e.getRowValue();
            if (row == null) return;

            String name = e.getNewValue();
            if (name == null) return;

            masterItems.stream()
                    .filter(i -> i.getName().equals(name))
                    .findFirst()
                    .ifPresent(i -> {
                        row.setItemName(i.getName());
                        row.setHsn(i.getHsn());
                        row.setUnit(i.getUnit());
                        row.setRate(i.getRate());
                        row.setGstPercent(i.getGstPercent()); // 🔑 MISSING LINE

                        if (row.getQty() <= 0) row.setQty(1);
                    });


            recalc();
            updateCustomerLock();

        });

        // ---------- HSN ----------
        hsnCol.setCellValueFactory(cell -> cell.getValue().hsnProperty());

        // ---------- QTY ----------
        qtyCol.setCellValueFactory(cell -> cell.getValue().qtyProperty().asObject());
        qtyCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        qtyCol.setOnEditCommit(e -> {
            if (e.getRowValue() == null) return;
            e.getRowValue().setQty(e.getNewValue());
            recalc();
        });

        // ---------- UNIT ----------
        unitCol.setCellValueFactory(cell -> cell.getValue().unitProperty());


        // ---------- RATE ----------
        rateCol.setCellValueFactory(cell -> cell.getValue().rateProperty().asObject());
        rateCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        rateCol.setOnEditCommit(e -> {
            if (e.getRowValue() == null) return;
            e.getRowValue().setRate(e.getNewValue());
            recalc();
        });

        // ---------- GST ----------
        ObservableList<Double> gstSlabs =
                FXCollections.observableArrayList(0.0, 5.0, 12.0, 18.0, 28.0);

        gstCol.setCellValueFactory(cell -> cell.getValue().gstPercentProperty().asObject());
        gstCol.setCellFactory(ComboBoxTableCell.forTableColumn(gstSlabs));
        gstCol.setOnEditCommit(e -> {
            if (e.getRowValue() == null) return;
            e.getRowValue().setGstPercent(e.getNewValue());
            recalc();
        });

        // ---------- AMOUNT (READ ONLY) ----------
        amountCol.setCellValueFactory(cell -> cell.getValue().amountProperty().asObject());
        amountCol.setEditable(false);

        // ---------- DELETE ROW COLUMN ----------
        deleteCol.setCellFactory(col -> new TableCell<>() {

            private final Button btn = new Button("✕");

            {
                btn.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-text-fill: red;" +
                                "-fx-font-weight: bold;"
                );

                btn.setOnAction(e -> {

                    InvoiceItem item = getTableView()
                            .getItems()
                            .get(getIndex());

                    if (rows.size() > 1) {
                        rows.remove(item);
                    } else {
                        clearInvoiceItem(item);   // 🔑 CLEAR LAST ROW
                    }

                    recalc();
                    updateCustomerLock();
                });

            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

    }

    private void updateCustomerLock() {
        customerBox.setDisable(hasActualItems());
    }


    /* ================= ACTIONS ================= */

    @FXML
    private void addItemRow() {
        InvoiceItem item = new InvoiceItem();
        item.setQty(1);
        item.setGstPercent(0);
        rows.add(item);
        int index = rows.size() - 1;
        table.getSelectionModel().clearAndSelect(index);
        table.scrollTo(index);

        updateCustomerLock();

    }


    @FXML
    private void addNewItem() {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ui/fxml/AddItemDialog.fxml")
            );

            Parent root = loader.load();
            AddItemController controller = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Item Master");
            stage.setScene(new Scene(root));

            // UX polish
            stage.setMinWidth(420);
            stage.setMinHeight(320);

            stage.showAndWait();

            // Refresh item master only if something changed
            if (controller.isDataChanged()) {
                masterItems = ItemDAO.findActiveItems();
                itemNames.setAll(
                        masterItems.stream()
                                .map(Item::getName)
                                .toList()
                );
            }



        } catch (Exception e) {
            e.printStackTrace();
            error("Unable to open Add Item dialog");
        }
    }

    private boolean hasActualItems() {
        return rows.stream()
                .anyMatch(r ->
                        r.getItemName() != null &&
                                !r.getItemName().isBlank()
                );
    }




    @FXML
    private void addNewCustomer() {

        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Add New Customer");
        dialog.setHeaderText("Enter customer details");

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        // -------- Fields --------
        TextField nameField = new TextField();
        nameField.setPromptText("Customer Name");

        TextArea addressField = new TextArea();
        addressField.setPromptText("Address");
        addressField.setPrefRowCount(3);

        TextField gstField = new TextField();
        gstField.setPromptText("GST No (optional)");

        TextField stateField = new TextField();
        stateField.setPromptText("State");

        TextField stateCodeField = new TextField();
        stateCodeField.setPromptText("State Code");

        // -------- Layout --------
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Name*"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("Address*"), 0, 1);
        grid.add(addressField, 1, 1);

        grid.add(new Label("GST No"), 0, 2);
        grid.add(gstField, 1, 2);

        grid.add(new Label("State"), 0, 3);
        grid.add(stateField, 1, 3);

        grid.add(new Label("State Code"), 0, 4);
        grid.add(stateCodeField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // -------- Validation --------
        Node saveButton = dialog.getDialogPane().lookupButton(saveBtn);
        saveButton.setDisable(true);

        nameField.textProperty().addListener((obs, o, n) ->
                saveButton.setDisable(n == null || n.trim().isEmpty())
        );

        // -------- Result --------
        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                Customer c = new Customer();
                c.setName(nameField.getText().trim());
                c.setAddress(addressField.getText().trim());
                c.setGstNo(gstField.getText().trim());
                c.setState(stateField.getText().trim());
                c.setStateCode(stateCodeField.getText().trim());
                return c;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(customer -> {
            CustomerDAO.save(customer);
            reloadCustomers();
            customerBox.setValue(customer);
        });
    }

    @FXML
    private void editCustomer() {

        Customer selected = customerBox.getValue();
        if (selected == null) {
            info("Select a customer to edit");
            return;
        }

        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Edit Customer");

        ButtonType saveBtn =
                new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(saveBtn, ButtonType.CANCEL);

        TextField nameField = new TextField(selected.getName());
        TextArea addressField = new TextArea(selected.getAddress());
        TextField gstField = new TextField(selected.getGstNo());
        TextField stateField = new TextField(selected.getState());
        TextField stateCodeField = new TextField(selected.getStateCode());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Name"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Address"), 0, 1);
        grid.add(addressField, 1, 1);
        grid.add(new Label("GST No"), 0, 2);
        grid.add(gstField, 1, 2);
        grid.add(new Label("State"), 0, 3);
        grid.add(stateField, 1, 3);
        grid.add(new Label("State Code"), 0, 4);
        grid.add(stateCodeField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt == saveBtn) {
                selected.setName(nameField.getText());
                selected.setAddress(addressField.getText());
                selected.setGstNo(gstField.getText());
                selected.setState(stateField.getText());
                selected.setStateCode(stateCodeField.getText());
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            CustomerDAO.update(updated);
            reloadCustomers();
            customerBox.setValue(updated);
        });
    }


    void reloadCustomers() {
        customers.setAll(CustomerDAO.findActive());

    }


    private void copyBuyerToConsignee(Customer c) {

        consigneeNameField.setText(c.getName());
        consigneeAddressArea.setText(c.getAddress());
        consigneeGstField.setText(c.getGstNo());
        consigneeStateField.setText(c.getState());
        consigneeStateCodeField.setText(c.getStateCode());
    }


    private void setConsigneeFieldsDisabled(boolean disabled) {

        consigneeNameField.setDisable(disabled);
        consigneeAddressArea.setDisable(disabled);
        consigneeGstField.setDisable(disabled);
        consigneeStateField.setDisable(disabled);
        consigneeStateCodeField.setDisable(disabled);
    }


    @FXML
    private void saveInvoice() {

        if (!isDraft) {
            info("Invoice already saved");
            return;
        }

        validateBeforeSave();

        if (customerBox.getValue() == null) {
            info("Select a customer");
            return;
        }

        for (InvoiceItem r : rows) {
            if (r.getItemName() == null || r.getItemName().isBlank()) {
                info("Remove empty item rows before saving");
                return;
            }
        }

        Invoice invoice = buildInvoiceFromUI();
        List<InvoiceItem> items = new ArrayList<>(rows);

        int invoiceNo = persistInvoice(invoice, items);


        info("Invoice saved successfully. Invoice No: " + invoiceNo);
        isDraft = false;
        printBtn.setDisable(false);

        resetInvoice();
        addItemRow();
    }


    @FXML
    private void previewInvoice() {

        if (!isDraft) {
            info("Preview is available only for draft invoices");
            return;
        }

        Invoice invoice = buildInvoiceFromUI(); // draft invoice
        List<InvoiceItem> items = new ArrayList<>(rows);

        PrintInvoiceBuilder builder = new PrintInvoiceBuilder();
        Parent page = builder.build(invoice, items, null);

        // 🔑 Force layout for accurate preview
        page.applyCss();
        page.layout();

        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.setPannable(true);

        // ---------- CLOSE BUTTON ----------
        Button closeBtn = new Button("CLOSE");
        closeBtn.setStyle(
                "-fx-background-color:#c62828;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-weight:bold;" +
                        "-fx-padding:8 20;"
        );

        HBox footer = new HBox(closeBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 20, 10, 20));

        BorderPane root = new BorderPane();
        root.setCenter(sp);
        root.setBottom(footer);

        Stage stage = new Stage();
        stage.setTitle("Invoice Preview");
        stage.setScene(new Scene(root, 900, 800));
        stage.setMaximized(true);

        closeBtn.setOnAction(e -> stage.close());

        stage.show();
    }



    @FXML
    private void saveAndPrint() {

        if (!isDraft) {
            printStatic(Integer.parseInt(invoiceNoField.getText()),
                    InvoiceCopyType.ORIGINAL);
            return;
        }


        validateBeforeSave();

        if (customerBox.getValue() == null) {
            info("Select a customer");
            return;
        }

        for (InvoiceItem r : rows) {
            if (r.getItemName() == null || r.getItemName().isBlank()) {
                info("Remove empty item rows before saving");
                return;
            }
        }

        Invoice invoice = buildInvoiceFromUI();
        List<InvoiceItem> items = new ArrayList<>(rows);

        int invoiceNo = persistInvoice(invoice, items);
        printStatic(invoiceNo, InvoiceCopyType.ORIGINAL);


        resetInvoice();
        addItemRow();
    }

    @FXML
    private void printCurrentInvoice() {

        if (isDraft) {
            info("Save invoice before printing");
            return;
        }

        int invoiceNo = Integer.parseInt(invoiceNoField.getText());

        // 🔑 PRINT CURRENT INVOICE DIRECTLY
        printStatic(invoiceNo, InvoiceCopyType.ORIGINAL);
    }


    @FXML
    private void openReprintDialog() {

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Reprint Invoice");

        ButtonType reprintBtn = new ButtonType("Reprint", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(reprintBtn, ButtonType.CANCEL);

        // ---------- Fields ----------
        TextField invoiceNoField = new TextField();
        invoiceNoField.setPromptText("Invoice Number");

        ComboBox<InvoiceCopyType> copyTypeBox = new ComboBox<>();
        copyTypeBox.getItems().addAll(InvoiceCopyType.values());
        copyTypeBox.setValue(InvoiceCopyType.ORIGINAL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Invoice No:"), 0, 0);
        grid.add(invoiceNoField, 1, 0);
        grid.add(new Label("Copy Type:"), 0, 1);
        grid.add(copyTypeBox, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // ---------- Validation ----------
        Node reprintButton = dialog.getDialogPane().lookupButton(reprintBtn);
        reprintButton.setDisable(true);

        invoiceNoField.textProperty().addListener((obs, o, n) ->
                reprintButton.setDisable(!n.matches("\\d+"))
        );

        reprintButton.addEventFilter(ActionEvent.ACTION, e -> {
            int invoiceNo = Integer.parseInt(invoiceNoField.getText());

            if (!InvoiceDAO.invoiceExists(invoiceNo)) {
                info("Invoice not found");
                e.consume();
                return;
            }

            printStatic(invoiceNo, copyTypeBox.getValue());
        });

        dialog.showAndWait();
    }

    @FXML
    private void reprintInvoice() {
        openReprintDialog();
    }


    public static void printStatic(int invoiceNo, InvoiceCopyType copyType) {

        Invoice invoice = InvoiceDAO.findInvoiceByNo(invoiceNo);
        List<InvoiceItem> items = InvoiceDAO.findItemsByInvoiceNo(invoiceNo);

        if (invoice == null || items == null || items.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Invoice data not found")
                    .showAndWait();
            return;
        }

        PrintInvoiceBuilder builder = new PrintInvoiceBuilder();
        Parent page = (Parent) builder.build(invoice, items, copyType);

        page.applyCss();
        page.layout();

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) return;

        // 🔑 THIS IS WHAT YOU WERE MISSING
        boolean proceed = job.showPrintDialog(null);
        if (!proceed) return;   // user clicked Cancel

        PageLayout layout = job.getPrinter().createPageLayout(
                Paper.A4,
                PageOrientation.PORTRAIT,
                Printer.MarginType.HARDWARE_MINIMUM
        );

        double scaleX = layout.getPrintableWidth() / page.getBoundsInLocal().getWidth();
        double scaleY = layout.getPrintableHeight() / page.getBoundsInLocal().getHeight();
        double scale = Math.min(scaleX, scaleY);

        page.getTransforms().add(new Scale(scale, scale));

        page.applyCss();
        page.layout();

        boolean printed = job.printPage(layout, page);

        page.getTransforms().clear();

        if (printed) job.endJob();
    }


    public static void openPreviewWindow(
            Invoice invoice,
            List<InvoiceItem> items,
            InvoiceCopyType copyType
    ) {

        PrintInvoiceBuilder builder = new PrintInvoiceBuilder();
        Parent page = (Parent) builder.build(invoice, items, copyType);

        // Ensure preview matches print exactly
        page.applyCss();
        page.layout();

        // ---------- SCROLLABLE PREVIEW ----------
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setPannable(true);

        // ---------- ACTION BAR (ALWAYS VISIBLE) ----------
        Button printBtn = new Button("Print");
        printBtn.setPrefWidth(140);
        printBtn.setStyle(
                "-fx-background-color: #2e7d32;" +   // green
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;"
        );
        printBtn.setOnAction(e ->
                printStatic(invoice.getInvoiceNo(), copyType)
        );

        Button closeBtn = new Button("Close");
        closeBtn.setPrefWidth(120);
        closeBtn.setOnAction(e ->
                ((Stage) closeBtn.getScene().getWindow()).close()
        );

        HBox actionBar = new HBox(12, printBtn, closeBtn);
        actionBar.setStyle(
                "-fx-padding:12;" +
                        "-fx-alignment: center-right;" +
                        "-fx-background-color: #f5f5f5;" +
                        "-fx-border-color: #d0d0d0;" +
                        "-fx-border-width: 1 0 0 0;"
        );

        // ---------- ROOT LAYOUT ----------
        VBox root = new VBox();
        root.getChildren().addAll(actionBar, scroll);
        VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);

        Stage stage = new Stage();
        stage.setTitle("Invoice Preview - " + invoice.getInvoiceNo());
        stage.setScene(new Scene(root, 1000, 900));
        stage.show();
    }



    private Invoice buildInvoiceFromUI() {

        Invoice inv = new Invoice();

        // -------- Invoice meta --------
        inv.setInvoiceDate(resolveInvoiceDate());
        inv.setTermsOfPayment("CREDIT");
        inv.setPoNo(poNoField.getText());
        inv.setPoDate(poDatePicker.getValue());
        inv.setDcNo(dcNoField.getText());
        inv.setDcDate(dcDatePicker.getValue());
        inv.setDispatchThrough(dispatchThroughField.getText());
        inv.setEwayBillNo(ewayBillField.getText());

        // -------- Buyer --------
        Customer buyer = customerBox.getValue();
        if (buyer != null) {
            inv.setBuyerName(buyer.getName());
            inv.setBuyerAddress(buyerAddressArea.getText());
            inv.setBuyerGst(buyerGstField.getText());
            inv.setBuyerState(buyerStateField.getText());
            inv.setBuyerStateCode(buyerStateCodeField.getText());
        }

        // -------- Consignee --------
        inv.setConsigneeName(consigneeNameField.getText());
        inv.setConsigneeAddress(consigneeAddressArea.getText());
        inv.setConsigneeGst(consigneeGstField.getText());
        inv.setConsigneeState(consigneeStateField.getText());
        inv.setConsigneeStateCode(consigneeStateCodeField.getText());

        // -------- Totals (already calculated) --------
        inv.setTaxableAmount(parse(taxableAmountLabel.getText()));
        inv.setCgstTotal(parse(cgstLabel.getText()));
        inv.setSgstTotal(parse(sgstLabel.getText()));
        inv.setIgstTotal(parse(igstLabel.getText()));
        inv.setRoundOff(parse(roundOffLabel.getText()));
        inv.setGrandTotal(parse(totalLabel.getText()));

        return inv;
    }



    private double parse(String s) {
        return s == null || s.isBlank() ? 0.0 : Double.parseDouble(s);
    }

    private void enterDraftMode() {
        isDraft = true;

        table.setDisable(false);
        saveButton.setDisable(false);
        savePrintButton.setDisable(false);
        previewButton.setDisable(false);
        invoiceDatePicker.setDisable(false);
        invoiceDatePicker.setValue(LocalDate.now());
        invoiceNoField.setText("AUTO");
    }

    private void enterFinalMode(int invoiceNo) {
        table.setDisable(true);
        saveButton.setDisable(true);
        savePrintButton.setDisable(true);
        previewButton.setDisable(true);
        invoiceNoField.setText(String.valueOf(invoiceNo));
        invoiceDatePicker.setDisable(true);

    }

    @FXML
    private void onSameAsBuyerToggle() {

        if (sameAsBuyerCheck.isSelected()) {

            Customer buyer = customerBox.getValue();
            if (buyer == null) return;

            copyBuyerToConsignee(buyer);
            setConsigneeFieldsDisabled(true);

        } else {
            clearConsigneeFields();
            setConsigneeFieldsDisabled(false);
        }
    }


    /* ================= CORE LOGIC ================= */

    private void recalc() {

        if (!hasActualItems()) {
            lockedIntraState = null;
            resetTableEditingState();
        }

        double taxableTotal = 0;   // EXCLUSIVE of GST
        double totalGst = 0;

        cgst = 0;
        sgst = 0;
        igst = 0;

        boolean currentIntra = isIntraState();

        if (lockedIntraState == null && hasActualItems()) {
            lockedIntraState = currentIntra;
        }

        if (lockedIntraState != null && lockedIntraState != currentIntra) {
            info("Buyer state cannot be changed after items are added");
            buyerStateCodeField.setText(
                    lockedIntraState ? String.valueOf(SELLER_STATE_CODE) : ""
            );
            currentIntra = lockedIntraState;
        }

        boolean intraState = lockedIntraState != null
                ? lockedIntraState
                : currentIntra;

        // ================= CALCULATION =================
        for (InvoiceItem item : rows) {

            double taxable = item.getAmount(); // ALREADY EXCLUSIVE OF GST
            double gst = taxable * item.getGstPercent() / 100.0;

            taxableTotal += taxable;
            totalGst += gst;
        }


        // ================= GST SPLIT =================
        if (intraState) {
            cgst = totalGst / 2;
            sgst = totalGst / 2;
            igst = 0;
        } else {
            igst = totalGst;
            cgst = 0;
            sgst = 0;
        }

        // ================= TOTAL =================
        double grandTotal = taxableTotal + totalGst;
        double payable;
        double roundOff = 0;

        if (roundOffCheck.isSelected()) {

            if (roundOffField.getText() == null || roundOffField.getText().isBlank()) {
                // AUTO ROUND
                double rounded = Math.round(grandTotal);
                roundOff = rounded - (grandTotal);
                payable = rounded;
            } else {
                // MANUAL ROUND
                try {
                    roundOff = Double.parseDouble(roundOffField.getText());
                } catch (Exception e) {
                    roundOff = 0;
                }
                payable = grandTotal + roundOff;
            }

        } else {
            // ROUND OFF DISABLED
            payable = taxableTotal + totalGst;
            roundOff = 0;
        }


        // ================= UI =================
        taxableAmountLabel.setText(fmt(taxableTotal));   // ✅ TAXABLE AMOUNT
        cgstLabel.setText(fmt(cgst));
        sgstLabel.setText(fmt(sgst));
        igstLabel.setText(fmt(igst));
        roundOffLabel.setText(fmt(roundOff));
        totalLabel.setText(fmt(payable));
    }


    private boolean isIntraState() {
        try {
            return Integer.parseInt(buyerStateCodeField.getText().trim()) == SELLER_STATE_CODE;
        } catch (Exception e) {
            return false;
        }
    }

    private LocalDate resolveInvoiceDate() {

        LocalDate selected = invoiceDatePicker.getValue();

        LocalDate finalDate = (selected != null)
                ? selected
                : LocalDate.now();

        if (finalDate.isAfter(LocalDate.now())) {
            throw new RuntimeException("Invoice date cannot be in the future");
        }

        return finalDate;
    }


    // TODO: JavaFX TableView editor state leak after invoice reset
// Fix by recreating TableView node when starting a new invoice


    private void resetInvoice() {
        rows.clear();
        lockedIntraState = null;
        previousCustomer = null;
        updateCustomerLock();
        table.getSelectionModel().clearSelection();
    }


    private int persistInvoice(Invoice invoice, List<InvoiceItem> items) {

        int invoiceNo = InvoiceDAO.saveInvoice(invoice, items);

        isDraft = false;                    // 🔑 first
        enterFinalMode(invoiceNo);          // 🔑 lock UI
        invoiceDatePicker.setDisable(true); // 🔑 hard lock

        return invoiceNo;
    }






    /* ================= UTILS ================= */

    private void validateBeforeSave() {

        if (customerBox.getValue() == null) {
            throw new RuntimeException("Select a customer");
        }

        for (InvoiceItem r : rows) {
            if (r.getItemName() == null || r.getItemName().isBlank()) {
                throw new RuntimeException("Remove empty item rows");
            }
        }
    }

    @FXML
    private void deleteSelectedRow() {

        InvoiceItem selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (rows.size() > 1) {
            rows.remove(selected);
        } else {
            clearInvoiceItem(selected);  // 🔑 CLEAR, NOT BLOCK
        }

        recalc();
        updateCustomerLock();
    }


    private void clearInvoiceItem(InvoiceItem item) {

        item.setItemName("");
        item.setHsn("");
        item.setUnit("");
        item.setQty(1);
        item.setRate(0);
        item.setGstPercent(0);

        // amount auto-recalculates via listeners
    }

    private void resetTableEditingState() {
        table.edit(-1, null);              // cancel any active edit
        table.getSelectionModel().clearSelection();
        table.requestFocus();
    }


    private void clearConsigneeFields() {
        consigneeNameField.clear();
        consigneeAddressArea.clear();
        consigneeGstField.clear();
        consigneeStateField.clear();
        consigneeStateCodeField.clear();
    }


    private String fmt(double v) {
        return String.format("%.2f", v);
    }

    private void info(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void error(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }


    /* ============ HOVER=================*/



}
