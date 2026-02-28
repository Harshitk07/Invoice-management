package app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class AppPaths {

    private static final String APP_ROOT = "ShreeUmaInvoice";

    private AppPaths() {}

    public static Path getBaseDir() {
        return Paths.get(
                System.getProperty("user.home"),
                "AppData",
                "Local",
                APP_ROOT
        );
    }

    public static Path getDataDir() {
        return getBaseDir().resolve("data");
    }

    public static Path getBackupDir() {
        return getBaseDir().resolve("backups");
    }

    public static Path getDatabasePath() {
        return getDataDir().resolve("shree_uma_invoice.db");
    }

    public static void ensureDirectories() throws Exception {
        Files.createDirectories(getDataDir());
        Files.createDirectories(getBackupDir());
    }
}