package service;

import app.AppPaths;

import java.io.IOException;
import java.nio.file.*;

public final class MigrationUtil {

    private MigrationUtil() {}

    public static void migrateIfNeeded() {

        try {

            Path oldDb = Paths.get("data", "shree_uma_invoice.db").toAbsolutePath();
            Path oldBackups = Paths.get("backups").toAbsolutePath();

            Path newDb = AppPaths.getDatabasePath();
            Path newBackupDir = AppPaths.getBackupDir();

            // ===== MIGRATE DATABASE =====
            if (Files.exists(oldDb) && !Files.exists(newDb)) {

                Files.createDirectories(newDb.getParent());

                Files.move(oldDb, newDb,
                        StandardCopyOption.REPLACE_EXISTING);

                System.out.println("Database migrated.");
            }

            // ===== MIGRATE BACKUPS FILE-BY-FILE =====
            if (Files.exists(oldBackups)) {

                Files.createDirectories(newBackupDir);

                Files.walk(oldBackups)
                        .forEach(source -> {

                            try {

                                Path relative =
                                        oldBackups.relativize(source);

                                Path target =
                                        newBackupDir.resolve(relative);

                                if (Files.isDirectory(source)) {

                                    Files.createDirectories(target);

                                } else {

                                    if (!Files.exists(target)) {
                                        Files.move(source, target,
                                                StandardCopyOption.REPLACE_EXISTING);
                                    }
                                }

                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });

                System.out.println("Backups migrated (file-level).");
            }

        } catch (Exception e) {
            throw new RuntimeException("Migration failed", e);
        }
    }


}