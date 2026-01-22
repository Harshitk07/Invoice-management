package app;

import dao.DB;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import service.DatabaseBackupService;
import service.DefaultSystemStatusProvider;
import ui.DashboardController;
import ui.interfaces.SystemStatusProvider;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // ================= BOOTSTRAP (NON-NEGOTIABLE) =================
        DB.init();
        DatabaseBackupService.autoBackupIfNeeded();

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

        stage.setTitle("Shree Uma Associates - Invoice");
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
