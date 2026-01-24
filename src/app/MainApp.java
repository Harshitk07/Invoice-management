package app;

import context.AppMode;
import context.CapabilityGate;
import context.CompanyContext;
import context.UserContext;
import dao.DB;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import service.DatabaseBackupService;
import service.DefaultSystemStatusProvider;
import service.StaticCompanyProfileProvider;
import service.StaticUserContextProvider;
import ui.DashboardController;
import ui.interfaces.SystemStatusProvider;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // ================= BOOTSTRAP (NON-NEGOTIABLE) =================
        DB.init();
        DatabaseBackupService.autoBackupIfNeeded();

// ================= CONTEXT BOOTSTRAP =================
        CompanyContext.init(new StaticCompanyProfileProvider());
        UserContext.init(new StaticUserContextProvider());

// OWNER install for now (full access)
        CapabilityGate.setMode(AppMode.OWNER);


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
