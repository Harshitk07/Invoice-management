package ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.UserRole;
import service.DatabaseBackupService;
import ui.interfaces.Navigable;
import ui.interfaces.RoleAware;

import javafx.scene.input.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RestoreDatabaseController implements Navigable, RoleAware {

    private DashboardController dashboard;
    private UserRole role;

    @FXML
    private Label recommendationLabel;


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
        loadBackups();
        showRecommendation();
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
                    setGraphic(null);
                    return;
                }

                int index = getIndex() + 1;
                var health = DatabaseBackupService.getHealth(item);

                Label icon = new Label("●");
                icon.setStyle(
                        "-fx-font-size:14;" +
                                "-fx-font-weight:bold;" +
                                switch (health) {
                                    case HEALTHY -> "-fx-text-fill:#15803d;"; // darker green
                                    case STALE   -> "-fx-text-fill:#a16207;"; // amber
                                    case CORRUPT -> "-fx-text-fill:#b91c1c;"; // deep red
                                }
                );

                Label text = new Label(
                        String.format(
                                "%02d. %s",
                                index,
                                DatabaseBackupService.prettyBackupName(item)
                        )
                );


                text.setStyle("-fx-font-size:13; -fx-text-fill:#111827;");

                HBox row = new HBox(10, icon, text);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setPadding(new javafx.geometry.Insets(4, 8, 4, 8));

                Tooltip tip = new Tooltip(
                        DatabaseBackupService.prettyBackupName(item)
                );
                Tooltip.install(row, tip);
                setGraphic(row);
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

        // 🔒 Block restoring active DB
        if (selected.toAbsolutePath()
                .equals(DatabaseBackupService.getCurrentDbPath())) {

            new Alert(Alert.AlertType.ERROR,
                    "You cannot restore the currently active database.")
                    .showAndWait();
            return;
        }

        // ================================
        // ✅ STEP 5 — METADATA PRESENCE CHECK
        // ================================
        Path metaFile = selected.resolveSibling(
                selected.getFileName().toString() + ".meta"
        );

        if (!Files.exists(metaFile)) {
            new Alert(Alert.AlertType.ERROR,
                    "This backup has no metadata and cannot be restored.\n\n" +
                            "Possible reasons:\n" +
                            "• Old backup (pre-upgrade)\n" +
                            "• File was copied manually\n" +
                            "• Backup is incomplete")
                    .showAndWait();
            return;
        }

        // ================================
        // 🔔 ONLY NOW show confirmation
        // ================================
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

            // 🔒 LOCK UI
            // 🔒 LOCK UI (FX thread)
            System.out.println("LOCK UI");
            dashboard.lockUIForRestore();

// 🔁 Run restore in BACKGROUND thread
            new Thread(() -> {
                try {
                    System.out.println("START RESTORE");

                    DatabaseBackupService.restore(selected);

                    // ✅ Exit app AFTER restore completes
                    Platform.runLater(() -> {
                        System.out.println("EXIT APP");
                        System.exit(0);
                    });

                } catch (Exception e) {
                    e.printStackTrace();

                    Platform.runLater(() -> {
                        dashboard.unlockUIAfterRestore();
                        new Alert(Alert.AlertType.ERROR,
                                "Restore failed:\n" + e.getMessage()).showAndWait();
                    });
                }
            }).start();


            // ================================
            // 🔥 FINAL: SERVICE CALL
            // ===============================

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

    private void loadBackups() {
        backupList.getItems().clear();

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
    }

    public void refreshAfterBackup() {
        loadBackups();
        showRecommendation();
    }


    private void showRecommendation() {

        Path recommended = DatabaseBackupService.getRecommendedRestore();

        if (recommended == null) {
            recommendationLabel.setText("⚠ No healthy backup available");
            recommendationLabel.setStyle("-fx-text-fill:#b91c1c;");
            return;
        }

        recommendationLabel.setText(
                "✅ Recommended: " + DatabaseBackupService.prettyBackupName(recommended)
        );
        recommendationLabel.setStyle(
                "-fx-text-fill:#166534; -fx-font-weight:bold;"
        );
    }

    @FXML
    private void cleanupManual() {

        if (role != UserRole.ADMIN) {
            new Alert(Alert.AlertType.ERROR,
                    "Admin access required").showAndWait();
            return;
        }

        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Manual Backup Cleanup");
        dialog.setHeaderText(null);

        ButtonType cleanupBtn =
                new ButtonType("Cleanup", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(cleanupBtn, ButtonType.CANCEL);

        // ===== Card Container =====
        VBox card = new VBox(14);
        card.setPadding(new javafx.geometry.Insets(22));
        card.setStyle("""
        -fx-background-color:white;
        -fx-background-radius:12;
        -fx-border-radius:12;
        -fx-border-color:#e5e7eb;
    """);

        Label title = new Label("Cleanup Old Manual Backups");
        title.setStyle("""
        -fx-font-size:16;
        -fx-font-weight:bold;
        -fx-text-fill:#111827;
    """);

        Label desc = new Label(
                "Keep only the most recent manual backups.\n" +
                        "All older manual backups will be permanently deleted."
        );
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill:#475569;");

        HBox inputRow = new HBox(10);
        inputRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label keepLabel = new Label("Backups to keep:");
        keepLabel.setStyle("-fx-font-weight:bold;");

        TextField keepField = new TextField("10");
        keepField.setPrefWidth(80);

        inputRow.getChildren().addAll(keepLabel, keepField);

        Label warning = new Label("⚠ This action cannot be undone");
        warning.setStyle("-fx-text-fill:#b91c1c;");

        card.getChildren().addAll(title, desc, inputRow, warning);
        dialog.getDialogPane().setContent(card);

        // ===== Validation =====
        Node okButton = dialog.getDialogPane().lookupButton(cleanupBtn);
        okButton.setDisable(true);

        keepField.textProperty().addListener((obs, o, n) -> {
            okButton.setDisable(!n.matches("\\d+") || Integer.parseInt(n) < 1);
        });

        dialog.setResultConverter(bt -> {
            if (bt == cleanupBtn) {
                return Integer.parseInt(keepField.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(keep -> {
            int deleted =
                    DatabaseBackupService.cleanupManualBackups(keep);

            new Alert(Alert.AlertType.INFORMATION,
                    "Deleted " + deleted + " old manual backups.")
                    .showAndWait();

            refreshAfterBackup();
        });
    }



    @FXML
    private void dangerHoverIn(MouseEvent e) {
        Button b = (Button) e.getSource();
        b.setStyle(
                "-fx-background-color:#b91c1c;" +
                        "-fx-text-fill:white;" +
                        "-fx-background-radius:8;" +
                        "-fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.3),10,0.2,0,3);"
        );
    }

    @FXML
    private void restoreBaseStyle(MouseEvent e) {
        Button b = (Button) e.getSource();
        b.setStyle(
                "-fx-background-color:#ef4444;" +
                        "-fx-text-fill:white;" +
                        "-fx-background-radius:8;"
        );
    }


    @FXML
    private void close() {
        dashboard.goBack();
    }
}
