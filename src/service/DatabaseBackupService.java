package service;

import dao.DB;
import model.BackupHealth;
import model.BackupType;

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

    private static final int MIN_AUTO_BACKUPS = 1;
    private static final int MIN_MANUAL_BACKUPS = 1; // user controlled
    private static final int MIN_BEFORE_RESTORE = 1;


    /* ================= PUBLIC API ================= */

    /* -------- MANUAL BACKUP -------- */

    public static Path manualBackup(String label) throws Exception {
        ensureDirs();

        String safe = label.replaceAll("[^a-zA-Z0-9_-]", "_");
        Path file = MANUAL_DIR.resolve(
                "invoice_" + safe + "_" + TS.format(LocalDateTime.now()) + ".db"
        );

        backupTo(file, "MANUAL", label);
        return file;
    }


    /* -------- AUTO BACKUP (ON CLOSE / DAILY) -------- */

    public static void autoBackupIfNeeded() {
        try {
            ensureDirs();

            Path todayFile =
                    AUTO_DIR.resolve("invoice_" + LocalDate.now() + ".db");

            if (Files.exists(todayFile)) return;

            backupTo(todayFile, "AUTO", "SYSTEM");
            enforceRetention(BackupType.AUTO, 7);

        } catch (Exception e) {
            e.printStackTrace(); // log only, never crash app
        }
    }

    /* -------- RESTORE -------- */

    private static void validateBackup(Path backup) throws Exception {

        Path metaFile = backup.resolveSibling(backup.getFileName() + ".meta");

        if (!Files.exists(metaFile)) {
            throw new IllegalStateException("Backup metadata missing");
        }

        BackupMetadata meta =
                BackupMetadata.parse(Files.readString(metaFile));

        String current = ChecksumUtil.sha256(backup);

        if (!current.equals(meta.checksum)) {
            throw new IllegalStateException(
                    "Backup file is corrupted (checksum mismatch)"
            );
        }
    }


    public static void restore(Path backupFile) throws Exception {

        validateBackup(backupFile);

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
    }

    /* ================= CORE ================= */

    private static void backupTo(Path target, String type, String user) throws Exception {

        try (Connection c = DB.connect();
             Statement s = c.createStatement()) {

            s.execute("PRAGMA wal_checkpoint(FULL);");

            s.execute(
                    "VACUUM INTO '" +
                            target.toAbsolutePath().toString().replace("\\", "/") +
                            "'"
            );
        }

        // ===== METADATA =====
        String checksum = ChecksumUtil.sha256(target);
        long size = Files.size(target);

        String id = target.getFileName().toString();

        BackupMetadata meta = new BackupMetadata(
                id,
                user,
                size,
                checksum,
                type
        );

        Files.writeString(
                target.resolveSibling(target.getFileName() + ".meta"),
                meta.serialize()
        );
    }


    /* ================= RETENTION ================= */

    public static int enforceRetention(BackupType type, int keepLast) {

        try {
            Path dir = switch (type) {
                case AUTO -> AUTO_DIR;
                case MANUAL -> MANUAL_DIR;
                case BEFORE_RESTORE -> BACKUP_DIR.resolve("before_restore");
            };

            if (!Files.exists(dir)) return 0;

            List<Path> backups =
                    Files.list(dir)
                            .filter(p -> p.toString().endsWith(".db"))
                            .sorted((a, b) -> {
                                try {
                                    return Files.getLastModifiedTime(b)
                                            .compareTo(Files.getLastModifiedTime(a));
                                } catch (Exception e) {
                                    return 0;
                                }
                            })
                            .toList();

            int deleted = 0;

            for (int i = keepLast; i < backups.size(); i++) {
                Path db = backups.get(i);
                Files.deleteIfExists(db);
                Files.deleteIfExists(db.resolveSibling(db.getFileName() + ".meta"));
                deleted++;
            }

            return deleted;

        } catch (Exception e) {
            throw new RuntimeException("Retention failed for " + type, e);
        }
    }

    public static List<Path> listAllBackups() {
        try {
            return Files.walk(BACKUP_DIR, 2)
                    .filter(p -> p.toString().endsWith(".db"))
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(b)
                                    .compareTo(Files.getLastModifiedTime(a));
                        } catch (Exception e) {
                            return 0;
                        }
                    })
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

    public static boolean canDelete(Path file) {

        try {
            if (!file.toAbsolutePath().startsWith(BACKUP_DIR)) return false;
            if (!Files.exists(file)) return false;
            if (file.toAbsolutePath().equals(getCurrentDbPath())) return false;

            BackupType type = getType(file);

            long count = Files.list(getDirForType(type))
                    .filter(p -> p.toString().endsWith(".db"))
                    .count();

            int minAllowed = switch (type) {
                case AUTO -> MIN_AUTO_BACKUPS;
                case MANUAL -> MIN_MANUAL_BACKUPS;
                case BEFORE_RESTORE -> MIN_BEFORE_RESTORE;
            };

            if (count <= minAllowed) return false;

            if (getHealth(file) == BackupHealth.HEALTHY) {
                long healthyCount = listAllBackups().stream()
                        .filter(p -> getHealth(p) == BackupHealth.HEALTHY)
                        .count();

                if (healthyCount <= 1) return false;
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public record DeleteResult(boolean success, String message) {}

    public static DeleteResult deleteBackup(Path file) {

        try {

            if (!file.toAbsolutePath().startsWith(BACKUP_DIR.toAbsolutePath())) {
                return new DeleteResult(false, "Invalid backup location.");
            }

            if (!file.toString().endsWith(".db")) {
                return new DeleteResult(false, "Invalid backup file.");
            }

            if (!Files.exists(file)) {
                return new DeleteResult(false, "Backup file does not exist.");
            }

            if (file.toAbsolutePath().equals(getCurrentDbPath())) {
                return new DeleteResult(false, "Cannot delete active database.");
            }

            BackupType type = getType(file);

            long count = Files.list(getDirForType(type))
                    .filter(p -> p.toString().endsWith(".db"))
                    .count();

            int minAllowed = switch (type) {
                case AUTO -> MIN_AUTO_BACKUPS;
                case MANUAL -> MIN_MANUAL_BACKUPS;
                case BEFORE_RESTORE -> MIN_BEFORE_RESTORE;
            };

            if (count <= minAllowed) {
                return new DeleteResult(false,
                        "Minimum required backups reached.");
            }

            if (getHealth(file) == BackupHealth.HEALTHY) {
                long healthyCount = listAllBackups().stream()
                        .filter(p -> getHealth(p) == BackupHealth.HEALTHY)
                        .count();

                if (healthyCount <= 1) {
                    return new DeleteResult(false,
                            "Cannot delete the last healthy backup.");
                }
            }

            // ===== PERMANENT DELETE =====
            Files.deleteIfExists(file);

            Path meta = file.resolveSibling(file.getFileName() + ".meta");
            Files.deleteIfExists(meta);

            return new DeleteResult(true, "Backup deleted.");

        } catch (Exception e) {
            return new DeleteResult(false, "Deletion failed: " + e.getMessage());
        }
    }

    /* ========UTILITY========== */

    private static Path getDirForType(BackupType type) {
        return switch (type) {
            case AUTO -> AUTO_DIR;
            case MANUAL -> MANUAL_DIR;
            case BEFORE_RESTORE -> BACKUP_DIR.resolve("before_restore");
        };
    }

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

        backupTo(target, "BEFORE_RESTORE", "SYSTEM"); // pre-restore safety
        enforceRetention(BackupType.BEFORE_RESTORE, 3);
        return target;
    }

    public static Path getCurrentDbPath() {
        return Paths.get("shree_uma_invoice.db").toAbsolutePath();
    }

    public static BackupHealth getHealth(Path backup) {
        try {
            Path metaFile = backup.resolveSibling(backup.getFileName() + ".meta");
            if (!Files.exists(metaFile)) return BackupHealth.CORRUPT;

            BackupMetadata meta =
                    BackupMetadata.parse(Files.readString(metaFile));

            String current = ChecksumUtil.sha256(backup);
            if (!current.equals(meta.checksum)) return BackupHealth.CORRUPT;

            FileTime t = Files.getLastModifiedTime(backup);
            Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);

            return t.toInstant().isBefore(cutoff)
                    ? BackupHealth.STALE
                    : BackupHealth.HEALTHY;

        } catch (Exception e) {
            return BackupHealth.CORRUPT;
        }
    }

    public static Path getRecommendedRestore() {

        return listAllBackups().stream()
                .filter(p -> getHealth(p) == BackupHealth.HEALTHY)
                .sorted((a, b) -> {
                    try {
                        return Files.getLastModifiedTime(b)
                                .compareTo(Files.getLastModifiedTime(a));
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .findFirst()
                .orElse(null);
    }

    public static int cleanupManualBackups(int keepLast) {

        try {
            List<Path> manuals =
                    Files.list(MANUAL_DIR)
                            .filter(p -> p.toString().endsWith(".db"))
                            .sorted((a, b) -> {
                                try {
                                    return Files.getLastModifiedTime(b)
                                            .compareTo(Files.getLastModifiedTime(a));
                                } catch (Exception e) {
                                    return 0;
                                }
                            })
                            .toList();

            int deleted = 0;

            for (int i = keepLast; i < manuals.size(); i++) {
                Path db = manuals.get(i);
                DeleteResult result = deleteBackup(db);
                if (result.success()) {
                    deleted++;
                }
            }

            return deleted;

        } catch (Exception e) {
            throw new RuntimeException("Manual cleanup failed", e);
        }
    }

    public static boolean isBackupGrowingAbnormally() {

        try {
            List<Path> autos =
                    Files.list(AUTO_DIR)
                            .filter(p -> p.toString().endsWith(".db"))
                            .sorted((a, b) -> {
                                try {
                                    return Files.getLastModifiedTime(b)
                                            .compareTo(Files.getLastModifiedTime(a));
                                } catch (Exception e) {
                                    return 0;
                                }
                            })
                            .limit(5)
                            .toList();

            if (autos.size() < 2) return false;

            long latest = Files.size(autos.get(0));
            long older  = Files.size(autos.get(autos.size() - 1));

            return latest > older * 1.5; // 50% growth

        } catch (Exception e) {
            return false;
        }
    }

    public static String prettyBackupName(Path file) {

        String raw = file.getFileName().toString().replace(".db", "");
        String folder = file.getParent().getFileName().toString().toUpperCase();

        // Match yyyy-MM-dd_HH-mm-ss
        var m = java.util.regex.Pattern
                .compile("(\\d{4}-\\d{2}-\\d{2})(?:_(\\d{2}-\\d{2}-\\d{2}))?")
                .matcher(raw);

        if (!m.find()) {
            return raw.replace("_", " ");
        }

        String date = m.group(1);
        String time = m.group(2);

        try {
            LocalDateTime dt = time != null
                    ? LocalDateTime.parse(
                    date + "_" + time,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            )
                    : LocalDate.parse(date).atStartOfDay();

            return switch (folder) {

                case "MANUAL" ->
                        "[Manual Backup]  • " +
                                dt.format(DateTimeFormatter.ofPattern(
                                        "dd MMM yyyy, hh:mm a"));

                case "AUTO" ->
                        "[Auto Backup]  • " +
                                dt.format(DateTimeFormatter.ofPattern(
                                        "dd MMM yyyy"));

                case "BEFORE_RESTORE" ->
                        "[Before Restore]  • " +
                                dt.format(DateTimeFormatter.ofPattern(
                                        "dd MMM yyyy, hh:mm a"));

                default ->
                        dt.format(DateTimeFormatter.ofPattern(
                                "dd MMM yyyy, hh:mm a"));
            };

        } catch (Exception e) {
            return raw.replace("_", " ");
        }
    }

    public static BackupType getType(Path file) {
        String folder = file.getParent().getFileName().toString().toUpperCase();

        return switch (folder) {
            case "AUTO" -> BackupType.AUTO;
            case "MANUAL" -> BackupType.MANUAL;
            case "BEFORE_RESTORE" -> BackupType.BEFORE_RESTORE;
            default -> throw new IllegalStateException("Unknown backup type: " + folder);
        };
    }


    /* ================= INIT ================= */

    private static void ensureDirs() throws Exception {
        Files.createDirectories(AUTO_DIR);
        Files.createDirectories(MANUAL_DIR);
    }
}
