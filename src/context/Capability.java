package context;

public enum Capability {

    // Core operations
    NEW_INVOICE,
    VIEW_HISTORY,

    // Masters
    ITEM_MASTER_EDIT,
    CUSTOMER_MASTER_EDIT,

    // Insights & personal
    VIEW_ANALYTICS,
    PERSONAL_TOOLS,

    // System (high-risk)
    SYSTEM_BACKUP,
    SYSTEM_RESTORE,
    SYSTEM_BACKUP_CLEANUP

}




