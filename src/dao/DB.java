package dao;

import app.AppPaths;

import java.nio.file.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class DB {


    private DB() {}

    /* ================= INIT ================= */

    public static void init() {
        try {
            AppPaths.ensureDirectories();
            connect();
            initSchema();
        } catch (Exception e) {
            throw new RuntimeException("DB initialization failed", e);
        }
    }

    private static final List<Connection> openConnections = new ArrayList<>();

    public static void closeAllConnections() {
        for (Connection c : openConnections) {
            try {
                if (c != null && !c.isClosed()) {
                    c.close();
                }
            } catch (Exception ignored) {}
        }
        openConnections.clear();
    }

    public static Connection connect() throws SQLException {

        Path dbPath = AppPaths.getDatabasePath();
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();

        Connection conn = DriverManager.getConnection(url);

        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = 5000");
            st.execute("PRAGMA journal_mode = WAL");
        }

        openConnections.add(conn);   // ✅ ADD THIS

        return conn;
    }

    /* ================= REQUIRED FOR BACKUP / RESTORE ================= */

    /* ================= FOR BACKUP SERVICE ================= */

    public static Path getDatabasePath() {
        return AppPaths.getDatabasePath();
    }

    /* ================= SCHEMA ================= */

    private static void initSchema() throws SQLException {

        try (Connection conn = connect();
             Statement st = conn.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    address TEXT NOT NULL,
                    gst_no TEXT,
                    state TEXT,
                    state_code TEXT
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL,
                    hsn TEXT,
                    unit TEXT,
                    rate REAL NOT NULL,
                    gst_percent REAL NOT NULL
                )
            """);

            st.execute("""
    CREATE TABLE IF NOT EXISTS invoices (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        invoice_no INTEGER UNIQUE NOT NULL,
        invoice_date TEXT NOT NULL,
        financial_year TEXT,
        fy_invoice_no INTEGER,

        /* ===== SELLER SNAPSHOT ===== */
        seller_name TEXT,
        seller_description TEXT,
        seller_address TEXT,
        seller_gst TEXT,
        seller_phone TEXT,
        seller_email TEXT,
        seller_bank_name TEXT,
        seller_account_no TEXT,
        seller_ifsc TEXT,

        /* ===== BUYER ===== */
        buyer_name TEXT,
        buyer_address TEXT,
        buyer_gst TEXT,
        buyer_state TEXT,
        buyer_state_code TEXT,

        /* ===== CONSIGNEE ===== */
        consignee_name TEXT,
        consignee_address TEXT,
        consignee_gst TEXT,
        consignee_state TEXT,
        consignee_state_code TEXT,

        /* ===== META ===== */
        terms_of_payment TEXT,
        dispatch_through TEXT,
        po_no TEXT,
        po_date TEXT,
        dc_no TEXT,
        dc_date TEXT,
        eway_bill_no TEXT,

        /* ===== TOTALS ===== */
        taxable_subtotal REAL NOT NULL,
        cgst REAL NOT NULL,
        sgst REAL NOT NULL,
        igst REAL NOT NULL,
        round_off REAL NOT NULL,
        grand_total REAL NOT NULL,
        po_id INTEGER,
        po_reference TEXT,
        
        FOREIGN KEY(po_id)
            REFERENCES purchase_orders(id)
            ON DELETE SET NULL
            )
""");


            st.execute("""
                CREATE TABLE IF NOT EXISTS invoice_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    invoice_id INTEGER NOT NULL,

                    item_name TEXT NOT NULL,
                    hsn TEXT,
                    unit TEXT,
                    qty REAL NOT NULL,
                    rate REAL NOT NULL,
                    gst_percent REAL NOT NULL,
                    taxable_amount REAL NOT NULL,

                    FOREIGN KEY(invoice_id)
                        REFERENCES invoices(id)
                        ON DELETE CASCADE
                )
            """);


// ========== P.O. =============== //

            st.execute("""
CREATE TABLE IF NOT EXISTS contracts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    contract_no TEXT UNIQUE NOT NULL,
    description TEXT,

    base_quantity REAL NOT NULL CHECK(base_quantity >= 0),
    variation_percent REAL NOT NULL CHECK(variation_percent >= 0),

    max_quantity REAL NOT NULL CHECK(max_quantity >= 0),

    base_value REAL NOT NULL CHECK(base_value >= 0),
    max_value REAL NOT NULL CHECK(max_value >= 0),

    start_date TEXT,
    end_date TEXT,

    status TEXT NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE','COMPLETED','CLOSED'))
)
""");

            st.execute("""
CREATE TABLE IF NOT EXISTS purchase_orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    po_no TEXT UNIQUE NOT NULL,
    po_date TEXT NOT NULL,

    contract_id INTEGER,

    department_name TEXT NOT NULL,
    reference_tender TEXT,

    delivery_location TEXT,
    supply_period TEXT,

    subtotal REAL NOT NULL CHECK(subtotal >= 0),
    gst_total REAL NOT NULL CHECK(gst_total >= 0),
    grand_total REAL NOT NULL CHECK(grand_total >= 0),

    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),

    status TEXT NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN','PARTIAL','COMPLETED','CLOSED')),

    FOREIGN KEY(contract_id)
        REFERENCES contracts(id)
        ON DELETE SET NULL
)
""");

            st.execute("""
CREATE TABLE IF NOT EXISTS purchase_order_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    po_id INTEGER NOT NULL,

    item_name TEXT NOT NULL,
    hsn TEXT,
    unit TEXT,

    qty REAL NOT NULL CHECK(qty > 0),
    rate REAL NOT NULL CHECK(rate >= 0),
    gst_percent REAL NOT NULL CHECK(gst_percent >= 0),
    taxable_amount REAL NOT NULL CHECK(taxable_amount >= 0),

    FOREIGN KEY(po_id)
        REFERENCES purchase_orders(id)
        ON DELETE CASCADE
)
""");

            st.execute("""
    CREATE INDEX IF NOT EXISTS idx_po_no
    ON purchase_orders(po_no)
""");

            st.execute("""
    CREATE INDEX IF NOT EXISTS idx_po_items_po_id
    ON purchase_order_items(po_id)
""");
            st.execute("""
    CREATE INDEX IF NOT EXISTS idx_invoice_po_id
    ON invoices(po_id)
""");

            st.execute("""
    CREATE TRIGGER IF NOT EXISTS trg_po_updated
    AFTER UPDATE ON purchase_orders
    BEGIN
        UPDATE purchase_orders
        SET updated_at = datetime('now')
        WHERE id = NEW.id;
    END;
""");

            st.execute("""
    CREATE INDEX IF NOT EXISTS idx_contract_status
    ON contracts(status)
""");

            // =================== P.O. END =====================//

            st.execute("CREATE INDEX IF NOT EXISTS idx_invoice_no ON invoices(invoice_no)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice_id ON invoice_items(invoice_id)");
            st.execute("""
    CREATE TABLE IF NOT EXISTS app_settings (
        key TEXT PRIMARY KEY,
        value TEXT
    )
""");

            migrate(st, "ALTER TABLE invoices ADD COLUMN terms_of_payment TEXT");
            migrate(st, "ALTER TABLE invoices ADD COLUMN dispatch_through TEXT");
            migrate(st, "ALTER TABLE invoices ADD COLUMN eway_bill_no TEXT");
            migrate(st, "ALTER TABLE invoices ADD COLUMN seller_name TEXT");
            migrate(st, "ALTER TABLE invoices ADD COLUMN seller_description TEXT");
            migrate(st, "ALTER TABLE invoices ADD COLUMN seller_address TEXT");
            migrate(st, "ALTER TABLE invoices ADD COLUMN seller_gst TEXT");
            migrate(st, "ALTER TABLE invoices ADD COLUMN seller_phone TEXT");
            migrate(st, "ALTER TABLE invoices ADD COLUMN seller_email TEXT");
            migrate(st, "ALTER TABLE invoices ADD COLUMN seller_bank_name TEXT");
            migrate(st, "ALTER TABLE invoices ADD COLUMN seller_account_no TEXT");
            migrate(st, "ALTER TABLE invoices ADD COLUMN seller_ifsc TEXT");
            migrate(st, "ALTER TABLE invoices ADD COLUMN notes TEXT");
            migrate(st, "ALTER TABLE invoices ADD COLUMN financial_year TEXT");
            migrate(st, "ALTER TABLE invoices ADD COLUMN fy_invoice_no INTEGER");
            migrate(st, "ALTER TABLE invoices ADD COLUMN po_id INTEGER");
            migrate(st, "ALTER TABLE invoices ADD COLUMN po_reference TEXT");
            migrate(st, "ALTER TABLE purchase_orders ADD COLUMN contract_id INTEGER");

            if (columnExists(conn, "purchase_orders", "contract_id")) {
                st.execute("""
        CREATE INDEX IF NOT EXISTS idx_po_contract_id
        ON purchase_orders(contract_id)
    """);
            }

        }
    }

    private static boolean columnExists(Connection conn,
                                        String table,
                                        String column) throws SQLException {

        String sql = "PRAGMA table_info(" + table + ")";

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void migrate(Statement st, String sql) {
        try {
            st.execute(sql);
        } catch (SQLException ignored) {}
    }

    /* ================= METRICS ================= */

    public static long getDatabaseSizeBytes() {
        try {
            Path dbPath = AppPaths.getDatabasePath();

            return Files.exists(dbPath)
                    ? Files.size(dbPath)
                    : 0L;
        } catch (Exception e) {
            return 0L; // dashboard-safe fallback
        }
    }

    public static int getInvoiceCount() {
        String sql = "SELECT COUNT(*) FROM invoices";

        try (Statement st = connect().createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            return rs.next() ? rs.getInt(1) : 0;

        } catch (Exception e) {
            return 0; // dashboard-safe fallback
        }
    }


}
