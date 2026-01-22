package ui;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import model.UserRole;
import ui.interfaces.RoleAware;
import ui.interfaces.Navigable;
import ui.interfaces.SystemStatusSnapshot;

import java.util.HashSet;
import java.util.Set;

public class HomeController implements Navigable, RoleAware {

    private DashboardController dashboard;
    private UserRole role;
    private final Set<String> animatedOnce = new HashSet<>();



    @FXML private VBox root;

    @FXML private VBox systemStatusCard;
    @FXML private VBox coreOpsCard;
    @FXML private VBox analyticsCard;
    @FXML private VBox personalToolsCard;

    @FXML private Label backupStatusLabel;
    @FXML private Label dbSizeLabel;
    @FXML private Label invoiceCountLabel;

    @Override
    public void setDashboard(DashboardController dashboard) {
        this.dashboard = dashboard;
    }

    @Override
    public void setRole(UserRole role) {
        this.role = role;
        applyRoleVisibility();
    }

    @Override
    public void onNavigateTo() {
        refreshSystemStatus();
    }



    @FXML
    public void initialize() {

        playEntranceAnimation();
        playCardCascade(
                systemStatusCard,
                coreOpsCard,
                analyticsCard,
                personalToolsCard
        );

        for (Node n : root.lookupAll(".button")) {
            installPressEffect((Button) n);
            installHover(n, 1.04, 14);
        }

        installHover(systemStatusCard, 1.015, 18);
        installHover(coreOpsCard, 1.015, 22);
        installHover(analyticsCard, 1.015, 16);
        installHover(personalToolsCard, 1.015, 14);
    }


    private void refreshSystemStatus() {

        SystemStatusSnapshot s =
                dashboard.getSystemStatusProvider().fetchStatus();

        backupStatusLabel.setText(
                s.autoBackupEnabled()
                        ? s.lastBackupText()
                        : "Auto backup disabled"
        );

        dbSizeLabel.setText(formatMb(s.dbSizeBytes()));
        invoiceCountLabel.setText(String.valueOf(s.invoiceCount()));
    }

    private String formatMb(long bytes) {
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }




    private void playEntranceAnimation() {
        root.setOpacity(0);
        root.setTranslateY(18);

        FadeTransition fade =
                new FadeTransition(Duration.millis(260), root);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide =
                new TranslateTransition(Duration.millis(260), root);
        slide.setFromY(18);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    private void applyRoleVisibility() {

        if (role == UserRole.STAFF) {
            personalToolsCard.setManaged(false);
            personalToolsCard.setVisible(false);
        }

        if (role == UserRole.PERSONAL) {
            analyticsCard.setManaged(false);
            analyticsCard.setVisible(false);
        }
    }

    private void installHover(Node node, double scale, double shadowRadius) {

        var shadow = new javafx.scene.effect.DropShadow();
        shadow.setRadius(shadowRadius);
        shadow.setSpread(0.15);
        shadow.setOffsetY(2);
        shadow.setColor(javafx.scene.paint.Color.rgb(0, 0, 0, 0.18));

        ScaleTransition scaleUp =
                new ScaleTransition(javafx.util.Duration.millis(140), node);
        scaleUp.setToX(scale);
        scaleUp.setToY(scale);

        ScaleTransition scaleDown =
                new ScaleTransition(javafx.util.Duration.millis(140), node);
        scaleDown.setToX(1);
        scaleDown.setToY(1);

        node.setOnMouseEntered(e -> {
            node.setEffect(shadow);
            scaleUp.playFromStart();
        });

        node.setOnMouseExited(e -> {
            node.setEffect(null);
            scaleDown.playFromStart();
        });
    }

    private void playCardCascade(VBox... cards) {

        double delay = 0;

        for (VBox card : cards) {

            card.setOpacity(0);
            card.setTranslateY(14);

            FadeTransition fade =
                    new FadeTransition(Duration.millis(220), card);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setDelay(Duration.millis(delay));

            TranslateTransition slide =
                    new TranslateTransition(Duration.millis(220), card);
            slide.setFromY(14);
            slide.setToY(0);
            slide.setDelay(Duration.millis(delay));

            ParallelTransition p = new ParallelTransition(fade, slide);
            p.setInterpolator(Interpolator.EASE_OUT);

            p.play();

            delay += 70; // tighter = more native
        }
    }


    private void installPressEffect(Button b) {
        b.setOnMousePressed(e -> {
            b.setScaleX(0.97);
            b.setScaleY(0.97);
        });
        b.setOnMouseReleased(e -> {
            b.setScaleX(1);
            b.setScaleY(1);
        });
    }




    // ===== BUTTON ACTIONS =====

    @FXML
    private void newInvoice() {
        dashboard.openInvoice();
    }

    @FXML
    private void reprint() {
        dashboard.openReprintDialog();
    }

    @FXML
    private void history() {
        dashboard.openInvoiceHistory();
    }

    @FXML
    private void itemMaster() {
        dashboard.openItemMaster();
    }

    @FXML
    private void customerMaster() {
        dashboard.openCustomerMaster();
    }
}
