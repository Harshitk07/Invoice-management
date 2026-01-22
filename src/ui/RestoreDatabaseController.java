package ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.UserRole;
import service.DatabaseBackupService;
import ui.interfaces.Navigable;
import ui.interfaces.RoleAware;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class RestoreDatabaseController implements Navigable, RoleAware {

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
        // optional: refresh UI, warnings, etc
    }

    @Override
    public void onNavigateFrom() {
        // optional cleanup
    }

    private static final Path BACKUP_ROOT =
            Paths.get(System.getProperty("user.dir")).resolve("backups");


    @FXML
    private ListView<Path> backupList;

    @FXML
    private Button restoreButton;


    @FXML
    public void initialize() {

        restoreButton.disableProperty()
                .bind(backupList.getSelectionModel()
                        .selectedItemProperty()
                        .isNull());


        if (!Files.exists(BACKUP_ROOT)) return;

        try {
            Files.walk(BACKUP_ROOT, 2)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".db"))
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(b)
                                    .compareTo(Files.getLastModifiedTime(a));
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .forEach(backupList.getItems()::add);

        } catch (Exception e) {
            e.printStackTrace();
        }

        backupList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String type = item.getParent().getFileName().toString().toUpperCase();
                    setText("[" + type + "] " + item.getFileName());
                }
            }
        });
    }



    @FXML
    private void restore() {

        Path selected = backupList.getSelectionModel().getSelectedItem();

        if (selected == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Please select a backup file").showAndWait();
            return;
        }

        // 🔒 BLOCK restoring the active DB
        if (selected.toAbsolutePath()
                .equals(DatabaseBackupService.getCurrentDbPath())) {

            new Alert(Alert.AlertType.ERROR,
                    "You cannot restore the currently active database.")
                    .showAndWait();
            return;
        }

        try {
            long sizeBytes = Files.size(selected);
            String sizeMB = String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0));

            String modified =
                    Files.getLastModifiedTime(selected)
                            .toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime()
                            .format(java.time.format.DateTimeFormatter.ofPattern(
                                    "dd MMM yyyy, HH:mm"));

            int invoiceCount = getInvoiceCount(selected);

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Restore");
            confirm.setHeaderText("⚠ Restore Database Backup");

            confirm.setContentText(
                    "Backup File : " + selected.getFileName() + "\n" +
                            "Invoices    : " + (invoiceCount >= 0 ? invoiceCount : "Unknown") + "\n" +
                            "Size        : " + sizeMB + "\n" +
                            "Modified    : " + modified + "\n\n" +
                            "A safety backup of the current database will be created.\n" +
                            "The application will close after restore.\n\n" +
                            "This action CANNOT be undone."
            );

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }

            DatabaseBackupService.restore(selected);

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Restore failed:\n" + e.getMessage()).showAndWait();
        }
    }


    private int getInvoiceCount(Path db) {
        String url = "jdbc:sqlite:" + db.toAbsolutePath();
        try (var con = java.sql.DriverManager.getConnection(url);
             var st = con.createStatement();
             var rs = st.executeQuery("SELECT COUNT(*) FROM invoices")) {

            return rs.next() ? rs.getInt(1) : 0;

        } catch (Exception e) {
            return -1;
        }
    }



    @FXML
    private void close() {
        dashboard.goBack();
    }
}
