package service;

import dao.DB;

import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class DatabaseBackupService {

    private DatabaseBackupService() {}

    /* ================= CONFIG ================= */

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    // App root directory (NOT Documents)
    private static final Path BASE_DIR =
            Paths.get(System.getProperty("user.dir"));

    private static final Path BACKUP_DIR = BASE_DIR.resolve("backups");
    private static final Path AUTO_DIR   = BACKUP_DIR.resolve("auto");
    private static final Path MANUAL_DIR = BACKUP_DIR.resolve("manual");

    private static final int AUTO_RETENTION_DAYS = 30;

    /* ================= PUBLIC API ================= */

    /* -------- MANUAL BACKUP -------- */

    public static Path manualBackup(String label) throws Exception {
        ensureDirs();

        String safe = label.replaceAll("[^a-zA-Z0-9_-]", "_");
        Path file = MANUAL_DIR.resolve(
                "invoice_" + safe + "_" + TS.format(LocalDateTime.now()) + ".db"
        );

        backupTo(file);
        return file;
    }

    /* -------- AUTO BACKUP (ON CLOSE / DAILY) -------- */

    public static void autoBackupIfNeeded() {
        try {
            ensureDirs();

            Path todayFile =
                    AUTO_DIR.resolve("invoice_" + LocalDate.now() + ".db");

            if (Files.exists(todayFile)) return;

            backupTo(todayFile);
            enforceRetention(AUTO_RETENTION_DAYS);

        } catch (Exception e) {
            e.printStackTrace(); // log only, never crash app
        }
    }

    /* -------- RESTORE -------- */

    public static void restore(Path backupFile) throws Exception {

        backupBeforeRestore();

        Path dbPath = DB.getDatabasePath();

        Path corrupt = dbPath.resolveSibling(
                "corrupt_" + TS.format(LocalDateTime.now()) + ".db"
        );

        Path temp = dbPath.resolveSibling("restore_tmp.db");

        // Step 1: Copy backup to temp FIRST
        Files.copy(backupFile, temp, StandardCopyOption.REPLACE_EXISTING);

        // Step 2: Close DB cleanly
        DB.close();

        // Step 3: Move old DB aside
        Files.move(dbPath, corrupt, StandardCopyOption.REPLACE_EXISTING);

        // Step 4: Atomic swap
        Files.move(temp, dbPath, StandardCopyOption.ATOMIC_MOVE);

        // Step 5: Hard restart (required)
        System.exit(0);
    }

    /* ================= CORE ================= */

    private static void backupTo(Path target) throws Exception {

        try (Connection c = DB.connect();
             Statement s = c.createStatement()) {

            // Ensure WAL flushed
            s.execute("PRAGMA wal_checkpoint(FULL);");

            // SQLite-native safe copy
            s.execute(
                    "VACUUM INTO '" +
                            target.toAbsolutePath().toString().replace("\\", "/") +
                            "'"
            );
        }
    }

    /* ================= RETENTION ================= */

    private static void enforceRetention(int days) throws Exception {

        if (!Files.exists(AUTO_DIR)) return;

        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);

        Files.list(AUTO_DIR)
                .filter(p -> p.getFileName().toString().endsWith(".db"))
                .forEach(p -> {
                    try {
                        FileTime t = Files.getLastModifiedTime(p);
                        if (t.toInstant().isBefore(cutoff)) {
                            Files.deleteIfExists(p);
                        }
                    } catch (Exception ignored) {}
                });
    }

    public static List<Path> listAllBackups() {
        try {
            return Files.walk(BACKUP_DIR, 1)
                    .filter(p -> p.toString().endsWith(".db"))
                    .sorted((a, b) -> b.getFileName().toString()
                            .compareTo(a.getFileName().toString()))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to list backups", e);
        }
    }

    public static String getLastAutoBackupStatus() {
        try {
            if (!Files.exists(AUTO_DIR)) {
                return "No auto backup yet";
            }

            return Files.list(AUTO_DIR)
                    .filter(p -> p.toString().endsWith(".db"))
                    .max((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(a)
                                    .compareTo(Files.getLastModifiedTime(b));
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .map(p -> {
                        try {
                            FileTime t = Files.getLastModifiedTime(p);
                            LocalDate date =
                                    t.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                            return date.equals(LocalDate.now())
                                    ? "Auto backup done today"
                                    : "Last auto backup: " + date;
                        } catch (Exception e) {
                            return "Auto backup status unknown";
                        }
                    })
                    .orElse("No auto backup yet");

        } catch (Exception e) {
            return "Auto backup status unknown";
        }
    }

    /* ========UTILITY========== */

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public static String formatTimestamp(Path file) throws Exception {
        return Files.getLastModifiedTime(file)
                .toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
    }

    public static Path backupBeforeRestore() throws Exception {
        Path safetyDir = BACKUP_DIR.resolve("before_restore");
        Files.createDirectories(safetyDir);

        Path target = safetyDir.resolve(
                "pre_restore_" + TS.format(LocalDateTime.now()) + ".db"
        );

        backupTo(target);
        return target;
    }

    public static Path getCurrentDbPath() {
        return Paths.get("shree_uma_invoice.db").toAbsolutePath();
    }


    /* ================= INIT ================= */

    private static void ensureDirs() throws Exception {
        Files.createDirectories(AUTO_DIR);
        Files.createDirectories(MANUAL_DIR);
    }
}
