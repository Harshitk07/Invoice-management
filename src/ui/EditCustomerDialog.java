package ui;

import dao.CustomerDAO;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import model.Customer;

public class EditCustomerDialog {

    private EditCustomerDialog() {}

    public static void open(Customer c, TableView<Customer> table) {

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
        TextField stateCode = new TextField(c.getStateCode());

        address.setPrefRowCount(3);

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
        grid.add(stateCode, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt == saveBtn) {
                c.setName(name.getText().trim());
                c.setAddress(address.getText().trim());
                c.setGstNo(gst.getText().trim());
                c.setState(state.getText().trim());
                c.setStateCode(stateCode.getText().trim());
                return c;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            CustomerDAO.update(updated);
            table.refresh();
        });
    }
}

