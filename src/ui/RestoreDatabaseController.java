package ui;

import context.security.CapabilityContext;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import model.BackupHealth;
import model.BackupType;
import service.DatabaseBackupService;
import ui.interfaces.Navigable;
import context.Capability;

import javafx.scene.input.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class RestoreDatabaseController implements Navigable{

    private DashboardController dashboard;

    @FXML
    private Label recommendationLabel;

    @FXML
    private ComboBox<BackupType> filterBox;

    private java.util.Map<Path, java.nio.file.attribute.FileTime> timeCache;


    @Override
    public void setDashboard(DashboardController dashboard) {
        this.dashboard = dashboard;
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

    private static final String RESTORE_BASE = """
    -fx-background-color:#dc2626;
    -fx-text-fill:white;
    -fx-font-weight:bold;
    -fx-background-radius:8;
    -fx-padding:6 18;
""";

    private static final String RESTORE_HOVER = """
    -fx-background-color:#b91c1c;
    -fx-text-fill:white;
    -fx-font-weight:bold;
    -fx-background-radius:8;
    -fx-padding:6 18;
    -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.25),8,0.2,0,2);
""";


    @FXML
    private ListView<Path> backupList;

    @FXML
    private Button restoreButton;

    @FXML
    private ComboBox<String> sortBox;



    @FXML
    public void initialize() {

        restoreButton.disableProperty().bind(
                backupList.getSelectionModel().selectedItemProperty().isNull()
        );

        if (!CapabilityContext.get().has(Capability.SYSTEM_RESTORE)) {
            restoreButton.setDisable(true);
        }

        restoreButton.setStyle(RESTORE_BASE);

        restoreButton.setOnMouseEntered(e ->
                restoreButton.setStyle(RESTORE_HOVER)
        );

        restoreButton.setOnMouseExited(e ->
                restoreButton.setStyle(RESTORE_BASE)
        );


        backupList.setCellFactory(lv -> new ListCell<>() {

            private final Region accentBar = new Region();
            private String leftBarColor = "#16a34a";
            private final Button deleteBtn = new Button("Delete");


            private void applyBackground(String bg) {
                setStyle("-fx-background-color: " + bg + ";");
            }



            {
                deleteBtn.setVisible(false);
                deleteBtn.setManaged(false);

                this.hoverProperty().addListener((obs, oldVal, isHover) -> {

                    Path current = getItem();

                    if (current == null || isEmpty()) {
                        deleteBtn.setVisible(false);
                        deleteBtn.setManaged(false);
                        return;
                    }

                    boolean canDelete = DatabaseBackupService.canDelete(current);

                    deleteBtn.setVisible(canDelete && isHover);
                    deleteBtn.setManaged(canDelete && isHover);
                    deleteBtn.setDisable(!canDelete);
                });
                // Hover
                setOnMouseEntered(e -> {
                    if (!isEmpty() && !isSelected()) {
                        applyBackground("#f8fafc");
                    }
                });

                setOnMouseExited(e -> {
                    if (!isEmpty() && !isSelected()) {
                        applyBackground("white");
                    }
                });

                // Selection
                selectedProperty().addListener((obs, oldVal, selected) -> {
                    if (!isEmpty()) {
                        if (selected) {
                            applyBackground("#dbeafe"); // light blue selection
                            accentBar.setPrefWidth(6);
                            accentBar.setStyle(
                                    "-fx-background-color:#2563eb;" +  // blue bar on select
                                            "-fx-background-radius:3;"
                            );
                            setStyle("-fx-background-color:#dbeafe; -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.08),8,0,0,2);");
                        } else {
                            applyBackground("white");
                            accentBar.setPrefWidth(4);
                            accentBar.setStyle(
                                    "-fx-background-color:" + leftBarColor + ";" +
                                            "-fx-background-radius:2;"
                            );
                            setStyle("-fx-background-color:white;");
                        }
                    }
                });




            }


            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    deleteBtn.setVisible(false);
                    deleteBtn.setManaged(false);
                    setGraphic(null);
                    setStyle("");
                    return;
                }

                BackupHealth health = DatabaseBackupService.getHealth(item);

                leftBarColor = switch (health) {
                    case HEALTHY -> "#16a34a";
                    case STALE   -> "#ca8a04";
                    case CORRUPT -> "#dc2626";
                };

                boolean selected = isSelected();

                // ===== STRONG SELECTION VISUAL =====
                String background = selected ? "#2563eb" : "white";
                String textColor  = selected ? "white" : "#111827";
                String metaColor  = selected ? "rgba(255,255,255,0.85)" : "#475569";
                String barColor   = selected ? "#1e40af" : leftBarColor;
                double barWidth   = selected ? 6 : 4;

                setStyle("-fx-background-color:" + background + ";");

                // ===== Accent Bar =====
                accentBar.setPrefWidth(barWidth);
                accentBar.setMinWidth(barWidth);
                accentBar.setMaxWidth(barWidth);
                accentBar.setStyle(
                        "-fx-background-color:" + barColor + ";" +
                                "-fx-background-radius:3;"
                );

                VBox barWrapper = new VBox(accentBar);
                barWrapper.setPadding(new Insets(6, 0, 6, 0));
                barWrapper.setAlignment(Pos.CENTER_LEFT);

                // ===== HEALTH =====
                Region healthCircle = new Region();
                healthCircle.setPrefSize(12, 12);
                healthCircle.setStyle(
                        "-fx-background-color:" + (selected ? "white" : leftBarColor) + ";" +
                                "-fx-background-radius:6;"
                );

                Label healthLabel = new Label(
                        health.name().charAt(0) +
                                health.name().substring(1).toLowerCase()
                );
                healthLabel.setStyle(
                        "-fx-font-size:11;" +
                                "-fx-font-weight:bold;" +
                                "-fx-text-fill:" + (selected ? "white" : leftBarColor) + ";"
                );

                HBox healthBlock = new HBox(6, healthCircle, healthLabel);
                healthBlock.setAlignment(Pos.CENTER_LEFT);
                healthBlock.setMinWidth(90);

                // ===== DATE =====
                String dateTime;
                try {
                    dateTime = Files.getLastModifiedTime(item)
                            .toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime()
                            .format(java.time.format.DateTimeFormatter.ofPattern(
                                    "dd MMM yyyy, hh:mm a"
                            ));
                } catch (Exception e) {
                    dateTime = "Unknown date";
                }

                Label dateLabel = new Label(dateTime);
                dateLabel.setStyle(
                        "-fx-font-size:13;" +
                                "-fx-font-weight:bold;" +
                                "-fx-text-fill:" + textColor + ";"
                );

                // ===== INVOICE COUNT =====
                int invoiceCount = getInvoiceCount(item);

                Label invoiceLabel = new Label(
                        invoiceCount >= 0
                                ? invoiceCount + " invoices"
                                : "Unknown invoices"
                );

                invoiceLabel.setStyle(
                        "-fx-font-size:12;" +
                                "-fx-text-fill:" + metaColor + ";"
                );

                // ===== TYPE BADGE =====
                BackupType type = DatabaseBackupService.getType(item);

                Label typeBadge = new Label(type.name());
                typeBadge.setStyle(
                        "-fx-font-size:10;" +
                                "-fx-padding:2 8;" +
                                "-fx-background-radius:12;" +
                                "-fx-background-color:" + (selected ? "rgba(255,255,255,0.2)" : "#f1f5f9") + ";" +
                                "-fx-text-fill:" + (selected ? "white" : "#475569") + ";"
                );

                HBox metaRow = new HBox(10, invoiceLabel, typeBadge);
                metaRow.setAlignment(Pos.CENTER_LEFT);

                VBox textBlock = new VBox(4, dateLabel, metaRow);

                deleteBtn.setStyle("""
    -fx-background-color:#ef4444;
    -fx-text-fill:white;
    -fx-background-radius:6;
""");
                SVGPath trashIcon = new SVGPath();
                trashIcon.setContent(
                        "M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14"
                );
                trashIcon.setStyle("-fx-fill:white;");

                deleteBtn.setGraphic(trashIcon);
                deleteBtn.setText(null);

                deleteBtn.setOnAction(e -> confirmDelete(item, type));

                HBox content = new HBox(18, healthBlock, textBlock);
                content.setAlignment(Pos.CENTER_LEFT);
                content.setPadding(new Insets(10, 14, 10, 10));

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox fullRow = new HBox(barWrapper, content, spacer, deleteBtn);
                fullRow.setAlignment(Pos.CENTER_LEFT);

                setGraphic(fullRow);
            }
        });

        Label empty = new Label("No backups found");
        empty.setStyle("""
    -fx-text-fill:#94a3b8;
    -fx-font-size:14;
""");
        backupList.setPlaceholder(empty);

        backupList.setStyle("-fx-background-color: transparent;");
        backupList.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            var selectionModel = backupList.getSelectionModel();
            int index = backupList.getSelectionModel()
                    .getSelectedIndex();

            if (index >= 0) {
                ListCell<?> cell = getCellAt(e);
                if (cell == null || cell.getIndex() == index) {
                    selectionModel.clearSelection();
                    e.consume();
                }
            }
        });


        sortBox.getItems().addAll(
                "Newest first",
                "Oldest first",
                "By Health"
        );

        sortBox.setValue("Newest first");

        filterBox.getItems().add(null);
        filterBox.getItems().addAll(BackupType.values());
        filterBox.setValue(null);

        filterBox.valueProperty().addListener((obs,o,n) -> applyFilterAndSort());
        sortBox.valueProperty().addListener((obs,o,n) -> applyFilterAndSort());

        filterBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(BackupType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null) {
                    setText("All");
                } else {
                    setText(item.name().replace("_", " "));
                }
            }
        });

        filterBox.setButtonCell(filterBox.getCellFactory().call(null));

    }

    private ListCell<?> getCellAt(MouseEvent e) {
        Node node = e.getPickResult().getIntersectedNode();
        while (node != null && !(node instanceof ListCell)) {
            node = node.getParent();
        }
        return (ListCell<?>) node;
    }

    private void applyFilterAndSort() {

        List<Path> all = new java.util.ArrayList<>(
                DatabaseBackupService.listAllBackups()
        );

        // ===== BUILD TIMESTAMP CACHE ONCE =====
        timeCache = new java.util.HashMap<>();

        for (Path p : all) {
            try {
                timeCache.put(p, Files.getLastModifiedTime(p));
            } catch (Exception e) {
                timeCache.put(p, java.nio.file.attribute.FileTime.fromMillis(0));
            }
        }

        BackupType selected = filterBox.getValue();

        // FILTER
        if (selected != null) {
            all = all.stream()
                    .filter(p -> DatabaseBackupService.getType(p) == selected)
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        }

        // SORT
        switch (sortBox.getValue()) {

            case "Oldest first" ->
                    all.sort(this::compareByTimeAsc);

            case "By Health" ->
                    all.sort((a,b) -> {
                        int h = Integer.compare(healthRank(a), healthRank(b));
                        if (h != 0) return h;
                        return compareByTimeDesc(a,b); // newest inside same health
                    });

            default -> // Newest first
                    all.sort(this::compareByTimeDesc);
        }

        backupList.getItems().setAll(all);
    }

    private void confirmDelete(Path file, BackupType type) {

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);

        if (type == BackupType.BEFORE_RESTORE) {

            confirm.setTitle("Delete Safety Backup");
            confirm.setHeaderText("⚠ Delete Before-Restore Backup");
            confirm.setContentText(
                    "This backup was created before a restore.\n" +
                            "Deleting it removes rollback protection.\n\n" +
                            "Are you absolutely sure?"
            );

        } else {

            confirm.setTitle("Delete Backup");
            confirm.setHeaderText("Delete Backup");
            confirm.setContentText(
                    "This action cannot be undone.\n\n" +
                            "Are you sure?"
            );
        }

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {

            var result = DatabaseBackupService.deleteBackup(file);

            if (!result.success()) {
                new Alert(Alert.AlertType.WARNING, result.message())
                        .showAndWait();
                return;
            }

            refreshAfterBackup();
        }
    }

//    private void showUndoSnackbar(Path trashedFile) {
//
//        HBox snackbar = new HBox(15);
//        snackbar.setStyle("""
//        -fx-background-color:#1f2937;
//        -fx-padding:12 20;
//        -fx-background-radius:8;
//    """);
//
//        Label msg = new Label("Backup deleted");
//        msg.setStyle("-fx-text-fill:white;");
//
//        Button undo = new Button("Undo");
//        undo.setStyle("""
//        -fx-background-color:transparent;
//        -fx-text-fill:#60a5fa;
//    """);
//
//        snackbar.getChildren().addAll(msg, undo);
//        snackbar.setAlignment(Pos.CENTER_LEFT);
//
//        dashboard.getRootPane().getChildren().add(snackbar);
//
//        undo.setOnAction(e -> {
//            DatabaseBackupService.restoreFromTrash(trashedFile);
//            dashboard.getRootPane().getChildren().remove(snackbar);
//            refreshAfterBackup();
//        });
//
//        new Thread(() -> {
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException ignored) {}
//
//            Platform.runLater(() -> {
//                dashboard.getRootPane().getChildren().remove(snackbar);
//                DatabaseBackupService.permanentlyDelete(trashedFile);
//            });
//        }).start();
//    }

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
        applyFilterAndSort();
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

        if (!CapabilityContext.get().has(Capability.SYSTEM_BACKUP_CLEANUP)) {
            new Alert(Alert.AlertType.ERROR,
                    "You do not have permission to perform this action.")
                    .showAndWait();
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

    private int compareByTimeAsc(Path a, Path b) {
        return timeCache.get(a).compareTo(timeCache.get(b));
    }

    private int compareByTimeDesc(Path a, Path b) {
        return timeCache.get(b).compareTo(timeCache.get(a));
    }

    private int typeRank(Path p) {
        return switch (DatabaseBackupService.getType(p)) {
            case BEFORE_RESTORE -> 0;
            case MANUAL -> 1;
            case AUTO -> 2;
        };
    }

    private int healthRank(Path p) {
        return switch (DatabaseBackupService.getHealth(p)) {
            case HEALTHY -> 0;
            case STALE -> 1;
            case CORRUPT -> 2;
        };
    }



    @FXML
    private void close() {
        dashboard.goBack();
    }
}
