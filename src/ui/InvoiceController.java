package ui;

import context.CompanyContext;
import dao.CustomerDAO;
import dao.InvoiceDAO;
import dao.ItemDAO;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
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
import javafx.util.Duration;
import javafx.util.converter.DoubleStringConverter;
import model.*;
import print.PrintInvoiceBuilder;
import ui.interfaces.Navigable;
import ui.interfaces.Refreshable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvoiceController implements Refreshable,Navigable {

    @Override
    public void refresh() {
        reloadCustomers();
    }

    private DashboardController dashboard;

    @Override
    public void setDashboard(DashboardController dashboard) {
        this.dashboard = dashboard;
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
    @FXML private Label companyNameLabel;
    @FXML private Label companyDescriptionLabel;
    @FXML private Label companyGstLabel;
    @FXML private Label companyContactLabel;


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

    private static final int MAX_VISIBLE_ROWS = 6;



    /* ================= TOTALS ================= */
    @FXML private VBox invoiceSummaryBox;
    @FXML private Label taxableAmountLabel;
    @FXML private Label cgstLabel;
    @FXML private Label sgstLabel;
    @FXML private Label igstLabel;
    @FXML private Label roundOffLabel;
    @FXML private CheckBox roundOffCheck;
    @FXML private TextField roundOffField;
    @FXML private HBox stickyTotalBar;
    @FXML private Label roundOffTextLabel;
    @FXML private Label roundOffValueLabel;

    @FXML private HBox summaryTotalSlot;
    @FXML private HBox stickyTotalSlot;

    private Label sharedTotalLabel;
    private boolean stickyVisible = false;
    @FXML private HBox grandTotalRow;



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
    private Invoice draftSnapshot; // seller snapshot cache


    /* ================= CALCULATED TOTALS ================= */

    private double subtotal = 0;
    private double cgst = 0;
    private double sgst = 0;
    private double igst = 0;
    private double grandTotal = 0;


    @FXML private VBox pageRoot;

    private double baseFontSize = 14;
    private double fontStep = 1;
    private double minFont = 12;
    private double maxFont = 20;

    @FXML
    private void increaseFont() {
        setFont(baseFontSize + fontStep);
    }

    @FXML
    private void decreaseFont() {
        setFont(baseFontSize - fontStep);
    }

    private void setFont(double size) {
        size = Math.max(minFont, Math.min(maxFont, size));
        baseFontSize = size;
        pageRoot.setStyle(
                "-fx-font-size: " + baseFontSize + "px;"
        );
    }



    /* ================= INIT ================= */

    @FXML
    public void initialize() {

        // In initialize, listen to both scroll AND layout changes
        root.vvalueProperty().addListener((obs, o, n) -> updateStickyBySummaryVisibility());

// CRITICAL: Listen to height changes of the content (pageRoot)
// This triggers when rows are added/removed
        pageRoot.heightProperty().addListener((obs, o, n) -> {
            // Wait for one layout pulse so bounds are updated
            Platform.runLater(this::updateStickyBySummaryVisibility);
        });

        sharedTotalLabel = new Label("0.00");
        sharedTotalLabel.setStyle("""
    -fx-font-size:22px;
    -fx-font-weight:800;
    -fx-text-fill:#2563EB;
""");

        stickyTotalBar.setMouseTransparent(true);
        sharedTotalLabel.setMinWidth(150);
        sharedTotalLabel.setAlignment(Pos.CENTER_RIGHT);

// Start INSIDE summary
        summaryTotalSlot.getChildren().add(sharedTotalLabel);
        invoiceDatePicker.setValue(LocalDate.now());
        invoiceNoField.setText("AUTO");
        invoiceNoField.setEditable(false);
        roundOffField.setDisable(true);

        roundOffCheck.selectedProperty().addListener((obs, o, isOn) -> {
            roundOffField.setDisable(!isOn);
            recalc();
            updateStickyBySummaryVisibility();

        });

        roundOffField.textProperty().addListener((obs, o, n) -> recalc());
        roundOffField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().matches("-?([0-9]*\\.?[0-9]*)?")) {
                return change;
            }
            return null;
        }));


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
            updateStickyBySummaryVisibility();
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
        Platform.runLater(() -> {
            addItemRowIfNeeded();
        });

        loadDraftCompanyHeader();
        addHoverEffect(savePrintButton);
        addHoverEffect(saveButton);
        addHoverEffect(previewButton);
        Platform.runLater(this::updateStickyBySummaryVisibility);
        stickyTotalBar.setVisible(false);
        stickyTotalBar.setOpacity(1);
        stickyTotalBar.setTranslateY(0);



    }

    /* ========== company profile =========*/

    private void loadDraftCompanyHeader() {
        CompanyProfile c = CompanyContext.get();

        companyNameLabel.setText(c.getLegalName());
        companyDescriptionLabel.setText(c.getDescription());
        companyGstLabel.setText("GST No: " + c.getGstin());
        companyContactLabel.setText(
                "Phone: " + c.getPhoneNo() + " | Email: " + c.getEmail()
        );
    }


    /* ================= TABLE ================= */

    private void setupTable() {
        // ---------- MASTER DATA ----------
        masterItems = ItemDAO.findActiveItems();
        itemNames = FXCollections.observableArrayList(
                masterItems.stream().map(Item::getName).toList()
        );

        table.setItems(rows);
        table.setEditable(true);
        table.setFocusTraversable(false); // 🔑 Fixes scroll jitter
        table.setPlaceholder(new Label("No items added to this invoice."));

        // ---------- ZOHO-STYLE ROW HEIGHT & SMOOTH SCROLL ----------
        // Zoho uses slightly taller rows for better readability
        double rowHeight = 50.0;
        table.setFixedCellSize(rowHeight);

        // Precise height binding to prevent the TableView from showing its own scrollbars
        table.prefHeightProperty().bind(
                Bindings.createDoubleBinding(() -> {
                    double headerHeight = 40.0; // Standard header height
                    double totalRowsHeight = Math.max(1, rows.size()) * rowHeight;
                    return totalRowsHeight + headerHeight + 5; // +5 for border/fudge factor
                }, rows)
        );

        // ---------- COLUMN RESIZE POLICY ----------
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // ---------- MODERN HEADER STYLING (Zoho Aesthetic) ----------
        table.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            Node header = table.lookup(".column-header-background");
            if (header != null) {
                header.setStyle("""
                -fx-background-color: #F9FAFB; 
                -fx-border-color: #E5E7EB; 
                -fx-border-width: 0 0 1 0;
            """);
            }
            // Style individual headers
            table.lookupAll(".column-header").forEach(n -> {
                n.setStyle("-fx-background-color: transparent; -fx-padding: 12 8;");
                Node label = n.lookup(".label");
                if (label != null) {
                    label.setStyle("-fx-text-fill: #4B5563; -fx-font-weight: 700; -fx-font-size: 12px;");
                }
            });
        });

        // ---------- ROW FACTORY (Minimalist Selection) ----------
        table.setRowFactory(tv -> {
            TableRow<InvoiceItem> row = new TableRow<>();
            row.setStyle("-fx-background-color: white; -fx-border-color: #F3F4F6; -fx-border-width: 0 0 1 0;");

            row.hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                if (isNowHovered && !row.isEmpty()) {
                    row.setStyle("-fx-background-color: #F9FAFB; -fx-border-color: #F3F4F6; -fx-border-width: 0 0 1 0;");
                } else {
                    row.setStyle("-fx-background-color: white; -fx-border-color: #F3F4F6; -fx-border-width: 0 0 1 0;");
                }
            });
            return row;
        });

        // ---------- SL NO ----------
        slNo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                    setStyle("-fx-alignment: CENTER; -fx-text-fill: #9CA3AF;");
                }
            }
        });

        // ---------- ITEM COLUMN (ComboBox) ----------
        itemCol.setCellValueFactory(cell -> cell.getValue().itemNameProperty());
        itemCol.setCellFactory(col -> new ComboBoxTableCell<>(itemNames) {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    // Mimics Zoho's "click to edit" text field look
                    setStyle("-fx-padding: 5 10; -fx-alignment: CENTER_LEFT;");
                }
            }
        });

        itemCol.setOnEditCommit(e -> {
            InvoiceItem row = e.getRowValue();
            if (row == null || e.getNewValue() == null) return;

            masterItems.stream()
                    .filter(i -> i.getName().equals(e.getNewValue()))
                    .findFirst()
                    .ifPresent(i -> {
                        row.setItemName(i.getName());
                        row.setHsn(i.getHsn());
                        row.setUnit(i.getUnit());
                        row.setRate(i.getRate());
                        row.setGstPercent(i.getGstPercent()); // ✅ Verified Missing Line Fix
                        if (row.getQty() <= 0) row.setQty(1);
                    });
            recalcAndRefresh();
        });

        // ---------- HSN ----------
        hsnCol.setCellValueFactory(cell -> cell.getValue().hsnProperty());
        hsnCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) { setStyle("-fx-alignment: CENTER; -fx-text-fill: #6B7280;"); setText(item); }
                else setText(null);
            }
        });

        // ---------- QTY ----------
        qtyCol.setCellValueFactory(cell -> cell.getValue().qtyProperty().asObject());
        qtyCol.setCellFactory(col -> new TextFieldTableCell<>(new DoubleStringConverter()) {
            @Override
            public void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setStyle("-fx-alignment: CENTER; -fx-padding: 5;");
            }
        });
        qtyCol.setOnEditCommit(e -> {
            if (e.getRowValue() != null) {
                e.getRowValue().setQty(e.getNewValue());
                recalcAndRefresh();
            }
        });

        // ---------- UNIT ----------
        unitCol.setCellValueFactory(cell -> cell.getValue().unitProperty());
        unitCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) { setStyle("-fx-alignment: CENTER; -fx-text-fill: #6B7280;"); setText(item); }
                else setText(null);
            }
        });

        // ---------- RATE ----------
        rateCol.setCellValueFactory(cell -> cell.getValue().rateProperty().asObject());
        rateCol.setCellFactory(col -> new TextFieldTableCell<>(new DoubleStringConverter()) {
            @Override
            public void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setStyle("-fx-alignment: CENTER_RIGHT; -fx-padding: 0 10 0 0;");
            }
        });
        rateCol.setOnEditCommit(e -> {
            if (e.getRowValue() != null) {
                e.getRowValue().setRate(e.getNewValue());
                recalcAndRefresh();
            }
        });

        // ---------- GST ----------
        ObservableList<Double> gstSlabs = FXCollections.observableArrayList(0.0, 5.0, 12.0, 18.0, 28.0);
        gstCol.setCellValueFactory(cell -> cell.getValue().gstPercentProperty().asObject());
        gstCol.setCellFactory(ComboBoxTableCell.forTableColumn(gstSlabs));
        gstCol.setOnEditCommit(e -> {
            if (e.getRowValue() != null) {
                e.getRowValue().setGstPercent(e.getNewValue());
                recalcAndRefresh();
            }
        });

        // ---------- AMOUNT (Zoho Bold Style) ----------
        amountCol.setCellValueFactory(cell -> cell.getValue().amountProperty().asObject());
        amountCol.setEditable(false);
        amountCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    setText(String.format("%.2f", item));
                    setStyle("-fx-alignment: CENTER_RIGHT; -fx-padding: 0 15 0 0; -fx-font-weight: 700; -fx-text-fill: #111827;");
                }
            }
        });

        // ---------- COLUMN WIDTHS ----------
        slNo.setPrefWidth(45);
        hsnCol.setPrefWidth(90);
        qtyCol.setPrefWidth(80);
        unitCol.setPrefWidth(80);
        rateCol.setPrefWidth(110);
        gstCol.setPrefWidth(90);
        deleteCol.setPrefWidth(40);
        amountCol.setPrefWidth(140);

        // ITEM details column absorbs all extra space
        itemCol.prefWidthProperty().bind(table.widthProperty().subtract(680));

        // ---------- DELETE COLUMN ----------
        deleteCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("✕");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-font-size: 14px; -fx-cursor: hand;");
                btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #EF4444;"));
                btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9CA3AF;"));
                btn.setOnAction(e -> {
                    InvoiceItem item = getTableView().getItems().get(getIndex());
                    if (rows.size() > 1) rows.remove(item);
                    else clearInvoiceItem(item);
                    recalcAndRefresh();
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    /** * Helper to update everything consistently after table edits
     */
    private void recalcAndRefresh() {
        recalc();
        updateCustomerLock();
        updateStickyBySummaryVisibility();
    }


    private void updateCustomerLock() {
        customerBox.setDisable(hasActualItems());
    }


    /* ================= ACTIONS ================= */

    private void addItemRowIfNeeded() {
        if (rows.isEmpty()) {
            rows.add(createEmptyRow());
            return;
        }

        InvoiceItem last = rows.get(rows.size() - 1);
        if (isRowValid(last)) {
            rows.add(createEmptyRow());
            table.scrollTo(rows.size() - 1);
        }
    }

    @FXML
    private void forceAddEmptyRow() {
        rows.add(createEmptyRow());
        table.scrollTo(rows.size() - 1);
    }



    private InvoiceItem createEmptyRow() {
        InvoiceItem item = new InvoiceItem();
        item.setQty(1);
        item.setGstPercent(0);
        return item;
    }

    private boolean isRowValid(InvoiceItem item) {
        return item.getItemName() != null &&
                !item.getItemName().isBlank();
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
        try {
            validateBeforeSave();
        } catch (RuntimeException ex) {
            info(ex.getMessage());
            return;
        }


        Invoice invoice = buildInvoiceFromUI();
        List<InvoiceItem> items = new ArrayList<>(rows);

        int invoiceNo = persistInvoice(invoice, items);


        info("Invoice saved successfully. Invoice No: " + invoiceNo);
        isDraft = false;
        printBtn.setDisable(false);

        resetInvoice();
        addItemRowIfNeeded();
    }


    @FXML
    private void previewInvoice() {

        if (!isDraft) {
            info("Preview is available only for draft invoices");
            return;
        }

        table.edit(-1, null);
        recalc();

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
        addItemRowIfNeeded();
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

    /* ================= NEW INVOICE / RESET ACTION ================= */

    @FXML
    private void startNewInvoice() {
        // 1. Reset Internal State
        isDraft = true;
        lockedIntraState = null;
        previousCustomer = null;
        draftSnapshot = null;

        // 2. Clear Header & Meta Fields
        customerBox.setValue(null);
        customerBox.setDisable(false);
        invoiceNoField.setText("AUTO");
        invoiceDatePicker.setValue(LocalDate.now());
        invoiceDatePicker.setDisable(false);

        poNoField.clear();
        poDatePicker.setValue(null);
        dcNoField.clear();
        dcDatePicker.setValue(null);
        ewayBillField.clear();
        dispatchThroughField.clear();

        // 3. Reset Consignee
        sameAsBuyerCheck.setSelected(false);
        clearConsigneeFields();
        setConsigneeFieldsDisabled(false);

        // 4. Clear Table
        rows.clear();
        table.setEditable(true);
        addItemRowIfNeeded(); // Adds the first default empty row

        // 5. Reset Footer Buttons
        saveButton.setDisable(false);
        savePrintButton.setDisable(false);
        previewButton.setDisable(false);
        printBtn.setDisable(true);

        // 6. Refresh UI totals to 0.00
        recalc();

        // Optional: Provide feedback to user
        // info("Form cleared for new invoice.");
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

        if (draftSnapshot == null) {
            CompanyProfile c = CompanyContext.get();

            draftSnapshot = new Invoice();
            draftSnapshot.setSellerName(c.getLegalName());
            draftSnapshot.setSellerDescription(c.getDescription());
            draftSnapshot.setSellerAddress(c.getAddress());
            draftSnapshot.setSellerGst(c.getGstin());
            draftSnapshot.setSellerPhone(c.getPhoneNo());
            draftSnapshot.setSellerEmail(c.getEmail());
            draftSnapshot.setSellerBankName(c.getBankName());
            draftSnapshot.setSellerAccountNo(c.getAccountNo());
            draftSnapshot.setSellerIfsc(c.getIfsc());
        }

// 🔒 COPY SNAPSHOT (NOT CONTEXT)
        inv.setSellerName(draftSnapshot.getSellerName());
        inv.setSellerDescription(draftSnapshot.getSellerDescription());
        inv.setSellerAddress(draftSnapshot.getSellerAddress());
        inv.setSellerGst(draftSnapshot.getSellerGst());
        inv.setSellerPhone(draftSnapshot.getSellerPhone());
        inv.setSellerEmail(draftSnapshot.getSellerEmail());
        inv.setSellerBankName(draftSnapshot.getSellerBankName());
        inv.setSellerAccountNo(draftSnapshot.getSellerAccountNo());
        inv.setSellerIfsc(draftSnapshot.getSellerIfsc());

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
        inv.setGrandTotal(parse(sharedTotalLabel.getText()));

        return inv;
    }



    private double parse(String s) {
        if (s == null || s.isBlank()) return 0.0;

        // Remove common non-numeric artifacts if any (like currency symbols)
        String clean = s.trim().replace(",", "");

        // If it's just a sign or a dot, it's not a number yet
        if (clean.equals("-") || clean.equals("+") || clean.equals(".")) {
            return 0.0;
        }

        try {
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            // Return 0.0 instead of crashing the UI Thread
            return 0.0;
        }
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
        isDraft = false;
        table.setEditable(false); // Lock the table
        saveButton.setDisable(true);
        savePrintButton.setDisable(true);
        invoiceDatePicker.setDisable(true);
        customerBox.setDisable(true);

        invoiceNoField.setText(String.valueOf(invoiceNo));
        printBtn.setDisable(false);
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
        // 1. Safety check: Reset if empty
        if (!hasActualItems()) {
            lockedIntraState = null;
            resetTableEditingState();
        }

        // 2. Initialize local accumulators to ensure zero-start every time
        double totalTaxable = 0;
        double totalGstAmount = 0;

        // 3. Determine Tax Type
        boolean currentIntra = isIntraState();
        if (lockedIntraState == null && hasActualItems()) {
            lockedIntraState = currentIntra;
        }

        // Lock logic (keeps the UI consistent)
        if (lockedIntraState != null && lockedIntraState != currentIntra) {
            buyerStateCodeField.setText(lockedIntraState ? String.valueOf(SELLER_STATE_CODE) : "");
            currentIntra = lockedIntraState;
        }
        boolean intraState = (lockedIntraState != null) ? lockedIntraState : currentIntra;

        // 4. Loop through rows and sum up
        for (InvoiceItem item : rows) {
            if (item.getItemName() == null || item.getItemName().isBlank()) continue;

            // Model's getAmount() returns (Qty * Rate) based on your provided code
            double lineTaxable = item.getAmount();
            double lineGst = lineTaxable * (item.getGstPercent() / 100.0);

            totalTaxable += lineTaxable;
            totalGstAmount += lineGst;
        }

        // 5. Assign to Global Variables (used for DB saving later)
        this.subtotal = totalTaxable;
        if (intraState) {
            this.cgst = totalGstAmount / 2.0;
            this.sgst = totalGstAmount / 2.0;
            this.igst = 0;
        } else {
            this.igst = totalGstAmount;
            this.cgst = 0;
            this.sgst = 0;
        }

        // 6. Final Calculation
        double rawTotal = totalTaxable + this.cgst + this.sgst + this.igst;
        double roundOff = 0;
        double finalPayable = rawTotal;

        // 7. Zoho-Style Rounding Logic
        if (roundOffCheck.isSelected()) {
            String manualVal = roundOffField.getText();
            if (manualVal == null || manualVal.isBlank()) {
                // Auto-round to nearest whole number
                finalPayable = Math.round(rawTotal);
                roundOff = finalPayable - rawTotal;
            } else {
                // Manual adjustment
                roundOff = parse(manualVal);
                finalPayable = rawTotal + roundOff;
            }
        }
        this.grandTotal = finalPayable;

        // 8. Push to UI
        // 8. Push to UI
        taxableAmountLabel.setText(fmt(totalTaxable));
        cgstLabel.setText(fmt(this.cgst));
        sgstLabel.setText(fmt(this.sgst));
        igstLabel.setText(fmt(this.igst));

// --- ROUND OFF VISIBILITY LOGIC ---
        roundOffLabel.setText(fmt(roundOff)); // for DB / print

        boolean showRoundOff = roundOffCheck.isSelected() && Math.abs(roundOff) > 0.0001;

        roundOffTextLabel.setVisible(showRoundOff);
        roundOffTextLabel.setManaged(showRoundOff);

        roundOffValueLabel.setVisible(showRoundOff);
        roundOffValueLabel.setManaged(showRoundOff);

        if (showRoundOff) {
            roundOffValueLabel.setText(fmt(roundOff));
        }

// --- TOTAL ---
        sharedTotalLabel.setText(fmt(this.grandTotal));

        updateStickyBySummaryVisibility();

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
        draftSnapshot = null; // 🔑 CRITICAL
        enterDraftMode();
        rows.clear();
        lockedIntraState = null;
        previousCustomer = null;
        updateCustomerLock();
        table.getSelectionModel().clearSelection();
    }


    private int persistInvoice(Invoice invoice, List<InvoiceItem> items) {
        try {
            int invoiceNo = InvoiceDAO.saveInvoice(invoice, items);

            isDraft = false;
            enterFinalMode(invoiceNo);
            loadCompanyHeader(invoice);

            // Disable the Save buttons so they can't double-click
            saveButton.setDisable(true);
            savePrintButton.setDisable(true);
            printBtn.setDisable(false); // Enable the standalone print button

            return invoiceNo;
        } catch (Exception e) {
            error("Database Error: Could not save invoice. " + e.getMessage());
            return -1;
        }
    }


    private void loadCompanyHeader(Invoice inv) {

        if (inv == null || inv.getSellerName() == null || inv.getSellerName().isBlank()) {
            // 🔴 HARD FAIL – legacy / corrupt invoice
            companyNameLabel.setText("UNKNOWN SELLER");
            companyDescriptionLabel.setText("Legacy invoice – seller snapshot missing");
            companyGstLabel.setText("");
            companyContactLabel.setText("");
            return;
        }

        // ✅ SNAPSHOT ONLY
        companyNameLabel.setText(inv.getSellerName());
        companyDescriptionLabel.setText(inv.getSellerDescription());
        companyGstLabel.setText("GST No: " + inv.getSellerGst());
        companyContactLabel.setText(
                "Phone: " + inv.getSellerPhone() +
                        " | Email: " + inv.getSellerEmail()
        );
    }


    private void addHoverEffect(Button btn) {
        btn.setOnMouseEntered(e ->
                btn.setStyle(btn.getStyle() +
                        ";-fx-effect:dropshadow(gaussian, rgba(37,99,235,0.35), 10, 0, 0, 2);")
        );

        btn.setOnMouseExited(e ->
                btn.setStyle(btn.getStyle().replaceAll("-fx-effect:.*?;", ""))
        );
    }



    private void updateStickyBySummaryVisibility() {
        if (!hasActualItems() || grandTotalRow == null || grandTotal <= 0) {
            toggleSticky(false);
            return;
        }

        // 1. Get the screen bounds of the real summary row
        Bounds summaryBounds = grandTotalRow.localToScene(grandTotalRow.getBoundsInLocal());

        // 2. Get the screen bounds of the ScrollPane's viewport
        Bounds scrollBounds = root.localToScene(root.getBoundsInLocal());

        // THE MAGIC TRIGGER:
        // We show the sticky bar ONLY when the summary row's bottom
        // is below the viewport's bottom edge.
        boolean shouldBeSticky = summaryBounds.getMaxY() > scrollBounds.getMaxY();

        toggleSticky(shouldBeSticky);
    }

    private void toggleSticky(boolean showSticky) {
        if (stickyVisible == showSticky) return;
        stickyVisible = showSticky;

        if (showSticky) {
            // DETACH from summary, ATTACH to sticky bar
            summaryTotalSlot.getChildren().remove(sharedTotalLabel);
            if (!stickyTotalSlot.getChildren().contains(sharedTotalLabel)) {
                stickyTotalSlot.getChildren().add(sharedTotalLabel);
            }

            stickyTotalBar.setVisible(true);
            stickyTotalBar.setManaged(true);

            // Subtle "Slide Up" to make it feel like it's sticking to the bottom
            stickyTotalBar.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(80), stickyTotalBar);
            ft.setToValue(1);
            ft.play();

        } else {
            // DETACH from sticky bar, ATTACH back to summary
            stickyTotalBar.setVisible(false);
            stickyTotalBar.setManaged(false);

            stickyTotalSlot.getChildren().remove(sharedTotalLabel);
            if (!summaryTotalSlot.getChildren().contains(sharedTotalLabel)) {
                summaryTotalSlot.getChildren().add(sharedTotalLabel);
            }
        }
    }

    private boolean isNodeFullyVisible(Node node, ScrollPane sp) {
        if (node == null || sp == null || node.getScene() == null) return false;

        // Get the screen bounds of the grandTotalRow
        Bounds nodeBounds = node.localToScene(node.getBoundsInLocal());
        // Get the screen bounds of the ScrollPane's viewport
        Bounds scrollBounds = sp.localToScene(sp.getBoundsInLocal());

        // We only care about the vertical axis for a sticky footer
        // The row is "out of view" if its top is below the scrollpane's bottom
        return scrollBounds.contains(nodeBounds.getMinX(), nodeBounds.getMinY()) &&
                scrollBounds.contains(nodeBounds.getMaxX(), nodeBounds.getMaxY());
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
        updateStickyBySummaryVisibility();

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
