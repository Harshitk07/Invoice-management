package service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class BackupMetadata {

    public final String backupId;
    public final String createdAt;
    public final String createdBy;
    public final long fileSize;
    public final String checksum;
    public final String type; // AUTO / MANUAL

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /* ================= CREATE (NEW BACKUP) ================= */

    public BackupMetadata(
            String backupId,
            String createdBy,
            long fileSize,
            String checksum,
            String type
    ) {
        this(
                backupId,
                LocalDateTime.now().format(TS),
                createdBy,
                fileSize,
                checksum,
                type
        );
    }

    /* ================= PARSE (FROM META FILE) ================= */

    private BackupMetadata(
            String backupId,
            String createdAt,
            String createdBy,
            long fileSize,
            String checksum,
            String type
    ) {
        this.backupId = require(backupId, "backupId");
        this.createdAt = require(createdAt, "createdAt");
        this.createdBy = require(createdBy, "createdBy");
        this.fileSize = fileSize;
        this.checksum = require(checksum, "checksum");
        this.type = require(type, "type");
    }

    /* ================= SERIALIZATION ================= */

    public String serialize() {
        return
                "backupId=" + backupId + "\n" +
                        "createdAt=" + createdAt + "\n" +
                        "createdBy=" + createdBy + "\n" +
                        "fileSize=" + fileSize + "\n" +
                        "checksum=" + checksum + "\n" +
                        "type=" + type + "\n";
    }

    public static BackupMetadata parse(String text) {

        String id = null, at = null, by = null, sum = null, type = null;
        long size = 0;

        for (String line : text.split("\n")) {
            String[] p = line.split("=", 2);
            if (p.length != 2) continue;

            switch (p[0]) {
                case "backupId" -> id = p[1];
                case "createdAt" -> at = p[1];
                case "createdBy" -> by = p[1];
                case "fileSize" -> size = Long.parseLong(p[1]);
                case "checksum" -> sum = p[1];
                case "type" -> type = p[1];
            }
        }

        return new BackupMetadata(id, at, by, size, sum, type);
    }

    /* ================= UTIL ================= */

    private static String require(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new IllegalStateException(
                    "Invalid backup metadata: missing " + name
            );
        }
        return v;
    }
}
