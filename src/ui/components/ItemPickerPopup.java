package ui.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import model.Item;

import java.util.List;
import java.util.function.Consumer;

public class ItemPickerPopup extends PopupControl {

    private final TextField searchField = new TextField();
    private final ListView<Item> listView = new ListView<>();
    private final ObservableList<Item> filtered = FXCollections.observableArrayList();

    private Consumer<Item> onItemSelected;
    private Runnable onAddNew;

    public ItemPickerPopup(List<Item> items) {

        filtered.setAll(items);
        listView.setItems(filtered);
        listView.setFixedCellSize(48);

        // ---------- Search ----------
        searchField.setPromptText("Search items...");
        searchField.textProperty().addListener((obs, o, text) -> {
            filtered.setAll(
                    items.stream()
                            .filter(i -> i.getName().toLowerCase().contains(text.toLowerCase()))
                            .toList()
            );
            addAddNewRow();
        });

        // ---------- Cell Rendering ----------
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                if ("__ADD_NEW__".equals(item.getName())) {
                    Label add = new Label("+ Add New Item");
                    add.setStyle("-fx-text-fill:#2563EB; -fx-font-weight:800;");
                    setGraphic(add);
                    return;
                }

                Label name = new Label(item.getName());
                name.setStyle("-fx-font-weight:700; -fx-text-fill:#111827;");

                Label meta = new Label(
                        "₹ " + item.getRate() + "  |  GST " + item.getGstPercent() + "%"
                );
                meta.setStyle("-fx-font-size:12; -fx-text-fill:#6B7280;");

                VBox box = new VBox(2, name, meta);
                box.setPadding(new Insets(6, 10, 6, 10));
                setGraphic(box);
            }
        });

        // ---------- Selection ----------
        listView.setOnMouseClicked(e -> select());
        listView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) select();
            if (e.getCode() == KeyCode.ESCAPE) hide();
        });

        // ---------- Layout ----------
        VBox root = new VBox(8, searchField, listView);
        root.setPadding(new Insets(10));
        root.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #E5E7EB;
            -fx-border-radius: 10;
            -fx-background-radius: 10;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 6);
        """);

        root.setPrefWidth(360);
        listView.setPrefHeight(6 * listView.getFixedCellSize());

        getScene().setRoot(root);
        addAddNewRow();
    }

    private void addAddNewRow() {
        filtered.removeIf(i -> "__ADD_NEW__".equals(i.getName()));
        Item add = new Item();
        add.setName("__ADD_NEW__");
        filtered.add(add);
    }

    private void select() {
        Item selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if ("__ADD_NEW__".equals(selected.getName())) {
            if (onAddNew != null) onAddNew.run();
            hide();
            return;
        }

        if (onItemSelected != null) onItemSelected.accept(selected);
        hide();
    }

    public void setOnItemSelected(Consumer<Item> consumer) {
        this.onItemSelected = consumer;
    }

    public void setOnAddNew(Runnable r) {
        this.onAddNew = r;
    }
}
