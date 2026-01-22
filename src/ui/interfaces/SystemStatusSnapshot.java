package ui.interfaces;

public record SystemStatusSnapshot(
        boolean autoBackupEnabled,
        String lastBackupText,
        long dbSizeBytes,
        int invoiceCount
) {}

