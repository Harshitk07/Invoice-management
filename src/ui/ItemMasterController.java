package ui;

import context.Capability;
import context.security.CapabilityContext;
import dao.ItemDAO;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Item;
import ui.interfaces.Navigable;


public class ItemMasterController implements Navigable{

    private DashboardController dashboard;


    @FXML private TextField name, hsn, rate;
    @FXML private ComboBox<Double> gstCombo;
    @FXML private ComboBox<String> unitCombo;


    @FXML private TableView<Item> table;
    @FXML private TableColumn<Item, String> nameCol, hsnCol;
    @FXML private TableColumn<Item, Double> rateCol, gstCol;
    @FXML private TableColumn<Item, Boolean> activeCol;
    @FXML private TableColumn<Item, String> unitCol;


    private final ObservableList<Item> data =
            FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        applyCapabilityUI();

        // ---------- Table setup ----------
        table.setEditable(true);

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        hsnCol.setCellValueFactory(new PropertyValueFactory<>("hsn"));
        rateCol.setCellValueFactory(new PropertyValueFactory<>("rate"));
        gstCol.setCellValueFactory(new PropertyValueFactory<>("gstPercent"));
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));

        activeCol.setCellFactory(CheckBoxTableCell.forTableColumn(activeCol));
        activeCol.setEditable(true);

        activeCol.setCellValueFactory(cellData -> {
            Item item = cellData.getValue();

            SimpleBooleanProperty prop =
                    new SimpleBooleanProperty(item.isActive());

            prop.addListener((obs, oldVal, newVal) -> {
                item.setActive(newVal);
                ItemDAO.updateActive(item.getId(), newVal);
                table.refresh(); // 🔑 force re-style
            });

            return prop;
        });

        activeCol.setCellFactory(CheckBoxTableCell.forTableColumn(activeCol));



        unitCombo.getItems().setAll(
                "Nos",
                "Kg",
                "Ltr",
                "Box",
                "Packet",
                "Meter"
        );

        unitCombo.getSelectionModel().selectFirst();


        // ---------- GST master ----------
        gstCombo.getItems().setAll(0.0, 5.0, 12.0, 18.0, 28.0);
        gstCombo.getSelectionModel().select(1); // default 5%

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setStyle("");
                } else if (!item.isActive()) {
                    setStyle(
                            "-fx-background-color: #f5f5f5;" +
                                    "-fx-text-fill: #9e9e9e;"
                    );
                } else {
                    setStyle("");
                }
            }
        });

        table.setRowFactory(tv -> {
            TableRow<Item> row = new TableRow<>();

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openEditDialog(row.getItem());
                }
            });

            return row;
        });



        data.setAll(ItemDAO.findAll());
        table.setItems(data);
    }

    @FXML
    public void save() {

        if (name.getText().isBlank()) {
            alert("Item name required");
            return;
        }

        Item i = new Item();
        i.setName(name.getText().trim());
        i.setHsn(hsn.getText().trim());
        i.setUnit(unitCombo.getValue());
        i.setRate(Double.parseDouble(rate.getText()));
        i.setGstPercent(gstCombo.getValue());
        i.setActive(true);

        ItemDAO.save(i);
        data.add(i);

        clearForm();
    }

    private void openEditDialog(Item item) {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ui/fxml/EditItemDialog.fxml")
            );

            Parent root = loader.load();
            EditItemController controller = loader.getController();
            controller.setItem(item);

            Stage stage = new Stage();
            stage.initOwner(table.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Edit Item");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            table.refresh(); // reflect updated values

        } catch (Exception e) {
            e.printStackTrace();
            alert("Unable to open edit dialog");
        }
    }


    private void clearForm() {
        name.clear();
        hsn.clear();
        rate.clear();
        gstCombo.getSelectionModel().select(1);
        unitCombo.getSelectionModel().selectFirst();
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    @Override
    public void setDashboard(DashboardController dashboard) {
        this.dashboard = dashboard;
    }


    @Override
    public void onNavigateTo() {
        refreshItems();
        applyCapabilityUI();
    }



    private void applyCapabilityUI() {

        boolean canEditItems =
                CapabilityContext.get().has(Capability.ITEM_MASTER_EDIT);

        name.setDisable(!canEditItems);
        hsn.setDisable(!canEditItems);
        rate.setDisable(!canEditItems);
        unitCombo.setDisable(!canEditItems);
        gstCombo.setDisable(!canEditItems);

        table.setEditable(canEditItems);
    }


    private void refreshItems() {
        data.setAll(ItemDAO.findAll());
    }

}

