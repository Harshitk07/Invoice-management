package ui;

import dao.CustomerDAO;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import model.Customer;

public class AddCustomerDialog {

    private AddCustomerDialog() {}

    public static void open(TableView<Customer> table,
                            ObservableList<Customer> backingList) {

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
        TextField stateCode = new TextField();

        name.setPromptText("Customer Name");
        address.setPromptText("Address");
        address.setPrefRowCount(3);
        gst.setPromptText("GST No");
        state.setPromptText("State");
        stateCode.setPromptText("State Code");

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
        grid.add(stateCode, 1, 4);

        dialog.getDialogPane().setContent(grid);

        Node saveButton = dialog.getDialogPane().lookupButton(saveBtn);
        saveButton.setDisable(true);

        name.textProperty().addListener((obs, o, n) ->
                saveButton.setDisable(n == null || n.trim().isEmpty())
        );

        dialog.setResultConverter(bt -> {
            if (bt == saveBtn) {
                Customer c = new Customer();
                c.setName(name.getText().trim());
                c.setAddress(address.getText().trim());
                c.setGstNo(gst.getText().trim());
                c.setState(state.getText().trim());
                c.setStateCode(stateCode.getText().trim());
                c.setActive(true);
                return c;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(c -> {
            CustomerDAO.save(c);
            backingList.setAll(CustomerDAO.findAll());
            table.refresh();
        });
    }
}
