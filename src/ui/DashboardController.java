package ui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import model.InvoiceCopyType;
import model.UserRole;
import service.DatabaseBackupService;
import ui.interfaces.RoleAware;
import ui.interfaces.Navigable;
import ui.interfaces.SystemStatusProvider;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public class DashboardController {

    private UserRole currentRole = UserRole.ADMIN; // later load from login
    private static final Set<String> NON_CACHED_VIEWS = Set.of(
            "InvoiceView.fxml",
            "RestoreDatabaseView.fxml"
    );

    private SystemStatusProvider systemStatusProvider;

    public DashboardController(SystemStatusProvider systemStatusProvider) {
        this.systemStatusProvider = systemStatusProvider;
    }

    public SystemStatusProvider getSystemStatusProvider() {
        if (systemStatusProvider == null) {
            throw new IllegalStateException(
                    "SystemStatusProvider not injected into DashboardController"
            );
        }
        return systemStatusProvider;
    }

    @FXML
    private ImageView logoImage;

    private DropShadow logoGlow;
    private Timeline idlePulse;

    @FXML private StackPane mainLayer;
    @FXML private VBox restoreOverlay;

    // idle detection
    private static final Duration IDLE_TIMEOUT = Duration.seconds(8);

    private PauseTransition idleTimer;
    private boolean isIdle = false;





    private static final Map<String, String> BREADCRUMB_LABELS = Map.of(
            "HomeView.fxml", "Home",
            "InvoiceView.fxml", "New Invoice",
            "InvoiceHistoryView.fxml", "Invoice History",
            "ItemMasterView.fxml", "Item Master",
            "CustomerMaster.fxml", "Customer Master"
    );


    /* ================= FXML ================= */

    @FXML private StackPane contentPane;
    @FXML private Label autoBackupStatusLabel;
    @FXML private HBox breadcrumbBar;
    @FXML private Button backButton;

    /* ================= NAV STATE ================= */

    private final Deque<ViewState> backStack = new ArrayDeque<>();
    private final Map<String, Parent> viewCache = new HashMap<>();
    private final Map<String, Navigable> controllerCache = new HashMap<>();


    /* ================= SCROLL STATE ================= */

    private final Map<String, Double> scrollV = new HashMap<>();
    private final Map<String, Double> scrollH = new HashMap<>();

    private static final Duration TRANSITION = Duration.millis(220);

    /* ================= INIT ================= */



    @FXML
    public void initialize() {

        refreshSystemStatus();

        // Clear nav state completely
        backStack.clear();
        viewCache.clear();
        controllerCache.clear();

        // Load HOME as root (no animation, no history push)
        loadRootView("HomeView.fxml");
        setupLogoGlow();
        setupLogoHover();
        setupIdleDetection();

        backButton.setOnAction(e -> goBack());
        updateBackButton();
    }

    private void loadRootView(String fxml) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/ui/fxml/" + fxml));

            Parent view = loader.load();
            Object raw = loader.getController();

            if (!(raw instanceof Navigable controller)) {
                throw new IllegalStateException(
                        fxml + " controller must implement Navigable"
                );
            }

            injectContext(controller);

            backStack.clear();
            backStack.push(new ViewState(fxml, view));

            contentPane.getChildren().setAll(view);

            updateBreadcrumb();
            updateBackButton();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load root view", e);
        }
    }

    private void setupLogoGlow() {

        logoGlow = new DropShadow();
        logoGlow.setColor(Color.rgb(59, 130, 246, 0.35)); // calm blue
        logoGlow.setRadius(36);
        logoGlow.setSpread(0.12);

        logoImage.setEffect(logoGlow);

        idlePulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(logoGlow.radiusProperty(), 34),
                        new KeyValue(logoGlow.colorProperty(),
                                Color.rgb(59,130,246,0.28))
                ),
                new KeyFrame(Duration.seconds(3.2),
                        new KeyValue(logoGlow.radiusProperty(), 44),
                        new KeyValue(logoGlow.colorProperty(),
                                Color.rgb(59,130,246,0.45))
                )
        );

        idlePulse.setAutoReverse(true);
        idlePulse.setCycleCount(Animation.INDEFINITE);
        idlePulse.play();
    }

    private void setupLogoHover() {

        logoImage.setOnMouseEntered(e -> {
            idlePulse.stop();

            Timeline hoverPulse = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(logoGlow.radiusProperty(), 46),
                            new KeyValue(logoGlow.colorProperty(),
                                    Color.rgb(96,165,250,0.65)) // brighter blue
                    ),
                    new KeyFrame(Duration.seconds(1.4),
                            new KeyValue(logoGlow.radiusProperty(), 56),
                            new KeyValue(logoGlow.colorProperty(),
                                    Color.rgb(147,197,253,0.85))
                    )
            );

            hoverPulse.setAutoReverse(true);
            hoverPulse.setCycleCount(Animation.INDEFINITE);
            hoverPulse.play();

            logoImage.setUserData(hoverPulse);
        });

        logoImage.setOnMouseExited(e -> {
            Timeline hoverPulse = (Timeline) logoImage.getUserData();
            if (hoverPulse != null) hoverPulse.stop();

            idlePulse.play();
        });
    }



    private void injectContext(Navigable controller) {

        controller.setDashboard(this);

        if (controller instanceof RoleAware r) {
            r.setRole(currentRole);
        }

        controller.onNavigateTo();
    }






    /* ================= PUBLIC NAV METHODS (UNCHANGED SIGNATURES) ================= */

    public void openInvoice() {
        navigate("InvoiceView.fxml", true);
    }

    public void openInvoiceHistory() {
        navigate("InvoiceHistoryView.fxml", true);
    }

    public void openItemMaster() {
        navigate("ItemMasterView.fxml", true);
    }

    public void openCustomerMaster() {
        navigate("CustomerMaster.fxml", true);
    }

    @FXML
    private void openRestoreDatabase() {
        navigate("RestoreDatabaseView.fxml", true);
    }


    /* ================= REPRINT (UNCHANGED) ================= */

    public void openReprintDialog() {

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Reprint Invoice");

        ButtonType printBtn =
                new ButtonType("Print", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(printBtn, ButtonType.CANCEL);

        TextField invoiceNoField = new TextField();
        invoiceNoField.setPromptText("Invoice Number");

        ComboBox<InvoiceCopyType> copyTypeBox = new ComboBox<>();
        copyTypeBox.getItems().addAll(InvoiceCopyType.values());
        copyTypeBox.setValue(InvoiceCopyType.ORIGINAL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Invoice No:"), invoiceNoField);
        grid.addRow(1, new Label("Copy Type:"), copyTypeBox);

        dialog.getDialogPane().setContent(grid);

        Node printButton =
                dialog.getDialogPane().lookupButton(printBtn);
        printButton.setDisable(true);

        invoiceNoField.textProperty().addListener(
                (obs, o, n) -> printButton.setDisable(!n.matches("\\d+"))
        );

        dialog.setResultConverter(bt -> {
            if (bt == printBtn) {
                InvoiceController.printStatic(
                        Integer.parseInt(invoiceNoField.getText()),
                        copyTypeBox.getValue()
                );
            }
            return null;
        });

        dialog.showAndWait();
    }

    /* ================= BACKUP ================= */

    @FXML
    private void backupNow() {
        try {
            Path p = DatabaseBackupService.manualBackup("user");

            new Alert(Alert.AlertType.INFORMATION,
                    "Backup created:\n" + p.getFileName()).showAndWait();

            refreshSystemStatus();

            // ✅ TELL CURRENT VIEW
            notifyBackupCompleted();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Backup failed:\n" + e.getMessage()).showAndWait();
        }
    }


    /* ================= NAV CORE ================= */

    private void navigate(String fxml, boolean pushHistory) {

        // 1️⃣ No-op guard (same view)
        if (pushHistory
                && !backStack.isEmpty()
                && backStack.peek().fxml.equals(fxml)) {
            return;
        }

        try {
            boolean cacheable = !NON_CACHED_VIEWS.contains(fxml);

            Parent view;
            Navigable controller;

            // 2️⃣ Load or fetch from cache
            if (cacheable && viewCache.containsKey(fxml)) {

                view = viewCache.get(fxml);
                controller = controllerCache.get(fxml);

            } else {

                FXMLLoader loader =
                        new FXMLLoader(getClass().getResource("/ui/fxml/" + fxml));

                view = loader.load();
                Object rawController = loader.getController();

                // 3️⃣ HARD ENFORCEMENT
                if (!(rawController instanceof Navigable nav)) {
                    throw new IllegalStateException(
                            fxml + " controller must implement Navigable"
                    );
                }

                controller = nav;

                // Always cache controller
                controllerCache.put(fxml, controller);

// Cache view only if allowed
                if (cacheable) {
                    viewCache.put(fxml, view);
                }

            }

            // 4️⃣ Lifecycle: leaving current view
            if (!backStack.isEmpty()) {
                Navigable current =
                        controllerCache.get(backStack.peek().fxml);
                if (current != null) {
                    current.onNavigateFrom();
                }
                saveScroll(backStack.peek().fxml, backStack.peek().view);
            }

            // 5️⃣ Push history
            if (pushHistory || backStack.isEmpty()) {
                backStack.push(new ViewState(fxml, view));
            }

            // 6️⃣ Inject + enter lifecycle
            injectContext(controller);

            // 7️⃣ Transition + restore scroll
            animateTransition(view, true);
            restoreScroll(fxml, view);

            // 8️⃣ UI sync
            updateBreadcrumb();
            updateBackButton();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Unable to load view:\n" + fxml).showAndWait();
        }
    }



    public void goBack() {
        if (backStack.size() <= 1) return;

        ViewState current = backStack.pop();

        Navigable ctrl = controllerCache.get(current.fxml);
        if (ctrl != null) {
            ctrl.onNavigateFrom();
        }

        saveScroll(current.fxml, current.view);

        ViewState prev = backStack.peek();

        animateTransition(prev.view, false);
        restoreScroll(prev.fxml, prev.view);

        updateBreadcrumb();
        updateBackButton();
    }

    public void lockUIForRestore() {
        restoreOverlay.setVisible(true);
        restoreOverlay.setManaged(true);

        mainLayer.setDisable(true);
        restoreOverlay.setDisable(false);
    }

    public void unlockUIAfterRestore() {
        restoreOverlay.setVisible(false);
        restoreOverlay.setManaged(false);

        mainLayer.setDisable(false);
    }


    /* ================= TRANSITION ================= */

    private void animateTransition(Parent next, boolean forward) {
        Parent old = contentPane.getChildren().isEmpty()
                ? null
                : (Parent) contentPane.getChildren().get(0);

        contentPane.getChildren().setAll(next);

        next.setOpacity(0);
        next.setTranslateX(forward ? 40 : -40);

        FadeTransition fade =
                new FadeTransition(TRANSITION, next);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide =
                new TranslateTransition(TRANSITION, next);
        slide.setToX(0);

        ParallelTransition p = new ParallelTransition(fade, slide);
        p.setInterpolator(Interpolator.EASE_OUT);
        p.play();


        if (old != null) old.setOpacity(1);
    }

    /* ================= BREADCRUMB ================= */

    private void updateBreadcrumb() {
        breadcrumbBar.getChildren().clear();

        List<ViewState> list = new ArrayList<>(backStack);
        Collections.reverse(list); // Home → Current

        for (int i = 0; i < list.size(); i++) {
            ViewState v = list.get(i);

            Label crumb = new Label(pretty(v.fxml));
            crumb.setStyle(
                    "-fx-font-size:13;" +
                            "-fx-text-fill:" + (v == backStack.peek() ? "#000000" : "#1565c0") + ";" +
                            "-fx-font-weight:" + (v == backStack.peek() ? "bold" : "normal") + ";"
            );

            if (v != backStack.peek()) {
                crumb.setOnMouseClicked(e -> jumpTo(v.fxml));
                crumb.setCursor(javafx.scene.Cursor.HAND);
            }

            breadcrumbBar.getChildren().add(crumb);

            if (i < list.size() - 1) {
                Label sep = new Label("›");
                sep.setStyle("-fx-text-fill:#888; -fx-padding:0 4;");
                breadcrumbBar.getChildren().add(sep);
            }
        }
    }



    private void jumpTo(String fxml) {

        while (backStack.size() > 1 &&
                !backStack.peek().fxml.equals(fxml)) {

            ViewState pop = backStack.pop();
            saveScroll(pop.fxml, pop.view);
        }

        ViewState target = backStack.peek();

        animateTransition(target.view, false);
        restoreScroll(target.fxml, target.view);

        updateBreadcrumb();
        updateBackButton();
    }


    private String pretty(String fxml) {
        return BREADCRUMB_LABELS.getOrDefault(
                fxml,
                fxml.replace("View.fxml", "").replace(".fxml", "")
        );
    }

    /* ================= SCROLL ================= */

    private void saveScroll(String key, Parent view) {
        ScrollPane sp = findScroll(view);
        if (sp != null) {
            scrollV.put(key, sp.getVvalue());
            scrollH.put(key, sp.getHvalue());
        }
    }

    private void restoreScroll(String key, Parent view) {
        ScrollPane sp = findScroll(view);
        if (sp != null) {
            Platform.runLater(() -> {
                sp.setVvalue(scrollV.getOrDefault(key, 0.0));
                sp.setHvalue(scrollH.getOrDefault(key, 0.0));
            });
        }
    }

    private ScrollPane findScroll(Node n) {
        if (n instanceof ScrollPane) return (ScrollPane) n;
        if (n instanceof Parent p) {
            for (Node c : p.getChildrenUnmodifiable()) {
                ScrollPane s = findScroll(c);
                if (s != null) return s;
            }
        }
        return null;
    }

    private void refreshSystemStatus() {
        autoBackupStatusLabel.setText(
                "🛡 " + systemStatusProvider.fetchStatus().lastBackupText()
        );
    }


    /* ================= HOVER EFFECTS (UNCHANGED) ================= */

    @FXML
    private void primaryHoverIn(MouseEvent e) {
        ((Button) e.getSource()).setStyle(
                "-fx-background-color:#1e88e5;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:15;" +
                        "-fx-font-weight:bold;" +
                        "-fx-background-radius:10;" +
                        "-fx-effect:dropshadow(gaussian, rgba(30,136,229,0.35),10,0.2,0,3);" +
                        "-fx-cursor:hand;"
        );
    }

    @FXML
    private void primaryHoverOut(MouseEvent e) {
        ((Button) e.getSource()).setStyle(
                "-fx-background-color:#1565c0;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:15;" +
                        "-fx-font-weight:bold;" +
                        "-fx-background-radius:10;"
        );
    }

    @FXML
    private void secondaryHoverIn(MouseEvent e) {
        ((Button) e.getSource()).setStyle(
                "-fx-background-color:#455a64;" +
                        "-fx-text-fill:white;" +
                        "-fx-background-radius:10;" +
                        "-fx-cursor:hand;"
        );
    }


    @FXML
    private void restoreBaseStyle(MouseEvent e) {
        Button b = (Button) e.getSource();
        Object base = b.getUserData();
        if (base instanceof String css) {
            b.setStyle(css);
        }
    }

    @FXML
    private void dangerHoverIn(MouseEvent e) {
        ((Button) e.getSource()).setStyle(
                "-fx-background-color:#b71c1c;" +
                        "-fx-text-fill:white;" +
                        "-fx-background-radius:10;" +
                        "-fx-cursor:hand;"
        );
    }

    @FXML
    private void backHoverIn(MouseEvent e) {
        ((Button) e.getSource()).setStyle(
                "-fx-background-color:#e2e8f0;" +
                        "-fx-text-fill:#0f172a;" +
                        "-fx-background-radius:8;" +
                        "-fx-cursor:hand;"
        );
    }

    /* =========== IDLE DETECTION ==================*/

    private void setupIdleDetection() {

        idleTimer = new PauseTransition(IDLE_TIMEOUT);
        idleTimer.setOnFinished(e -> enterIdleMode());

        // Any interaction resets idle timer
        mainLayer.addEventFilter(javafx.scene.input.MouseEvent.ANY, e -> userActive());
        mainLayer.addEventFilter(javafx.scene.input.KeyEvent.ANY, e -> userActive());
        mainLayer.addEventFilter(javafx.scene.input.ScrollEvent.ANY, e -> userActive());

        // Start timer immediately
        idleTimer.playFromStart();
    }

    private void userActive() {

        // If returning from idle → stop pulse
        if (isIdle) {
            exitIdleMode();
        }

        idleTimer.playFromStart();
    }

    private void enterIdleMode() {
        if (isIdle) return;

        isIdle = true;
        startIdlePulse();
    }

    private void exitIdleMode() {
        isIdle = false;
        stopIdlePulse();
    }

    private void startIdlePulse() {

        idlePulse.stop();

        idlePulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(logoGlow.radiusProperty(), 36),
                        new KeyValue(logoGlow.colorProperty(),
                                Color.rgb(59, 130, 246, 0.40)) // brighter base
                ),
                new KeyFrame(Duration.seconds(2.4),
                        new KeyValue(logoGlow.radiusProperty(), 64), // wider glow
                        new KeyValue(logoGlow.colorProperty(),
                                Color.rgb(96, 165, 250, 0.75)) // clearly visible but calm
                )
        );

        idlePulse.setAutoReverse(true);
        idlePulse.setCycleCount(Animation.INDEFINITE);
        idlePulse.play();
    }


    private void stopIdlePulse() {

        idlePulse.stop();

        logoGlow.setRadius(36);
        logoGlow.setColor(Color.rgb(59, 130, 246, 0.35)); // calm default
    }




    /* ================= UTILS ================= */

    private void updateBackButton() {
        if (backButton != null) {
            backButton.setDisable(backStack.size() <= 1);
        }
    }

    private static final class ViewState {
        final String fxml;
        final Parent view;
        ViewState(String fxml, Parent view) {
            this.fxml = fxml;
            this.view = view;
        }
    }

    public void notifyBackupCompleted() {
        if (backStack.isEmpty()) return;

        String currentFxml = backStack.peek().fxml;
        Navigable controller = controllerCache.get(currentFxml);

        if (controller instanceof RestoreDatabaseController restoreCtrl) {
            restoreCtrl.refreshAfterBackup();
        }
    }

}
