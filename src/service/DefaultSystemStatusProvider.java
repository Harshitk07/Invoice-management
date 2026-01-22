package service;

import dao.DB;
import service.DatabaseBackupService;
import ui.interfaces.SystemStatusProvider;
import ui.interfaces.SystemStatusSnapshot;

public class DefaultSystemStatusProvider implements SystemStatusProvider {

    @Override
    public SystemStatusSnapshot fetchStatus() {

        boolean autoBackupEnabled = true; // or read from config
        String lastBackupText = DatabaseBackupService.getLastAutoBackupStatus();

        long dbSizeBytes = DB.getDatabaseSizeBytes();
        int invoiceCount = DB.getInvoiceCount();

        return new SystemStatusSnapshot(
                autoBackupEnabled,
                lastBackupText,
                dbSizeBytes,
                invoiceCount
        );
    }
}
