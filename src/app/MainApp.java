package app;

import context.Capability;
import context.CompanyContext;
import context.UserContext;
import context.security.CapabilityContext;
import context.security.StaticCapabilityGate;
import dao.DB;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import service.*;
import ui.DashboardController;
import ui.interfaces.SystemStatusProvider;

import java.util.Set;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // ================= BOOTSTRAP (NON-NEGOTIABLE) =================
        // 1️⃣ Migrate old data (if exists)
//        MigrationUtil.migrateIfNeeded();
//
        DB.init();
        DatabaseBackupService.autoBackupIfNeeded();

// ================= CONTEXT BOOTSTRAP =================
        CompanyContext.init(new StaticCompanyProfileProvider());
        UserContext.init(new StaticUserContextProvider());

// OWNER install for now (full access)
        CapabilityContext.init(
                new StaticCapabilityGate(
                        Set.of(
                                Capability.CUSTOMER_MASTER_EDIT,
                                Capability.ITEM_MASTER_EDIT,
                                Capability.SYSTEM_RESTORE,
                                Capability.VIEW_ANALYTICS,
                                Capability.PERSONAL_TOOLS,
                                Capability.VIEW_HISTORY,
                                Capability.NEW_INVOICE,
                                Capability.SYSTEM_BACKUP_CLEANUP
                        )
                )
        );




        // ================= APP SERVICES =================
        SystemStatusProvider systemStatusProvider =
                new DefaultSystemStatusProvider();

        // ================= LOAD DASHBOARD =================
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ui/fxml/DashboardView.fxml")
        );

        // 🔑 CONSTRUCTOR INJECTION — SINGLE SOURCE OF TRUTH
        DashboardController dashboardController =
                new DashboardController(systemStatusProvider);

        loader.setController(dashboardController);

        Parent root = loader.load();

        // ================= STAGE =================
        Scene scene = new Scene(root);

        stage.setTitle(ProductInfo.WINDOW_TITLE);
        stage.setScene(scene);
        stage.setMaximized(true);

        stage.setOnCloseRequest(e ->
                DatabaseBackupService.autoBackupIfNeeded()
        );

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
