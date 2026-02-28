package dao;

import app.AppPaths;

import java.nio.file.*;
import java.sql.*;

public final class DB {

    private static Connection connection;

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

    public static Connection connect() throws SQLException {

        if (connection == null || connection.isClosed()) {

            Path dbPath = AppPaths.getDatabasePath();

            String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();

            connection = DriverManager.getConnection(url);

            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
                st.execute("PRAGMA busy_timeout = 5000");
                st.execute("PRAGMA journal_mode = WAL");
            }
        }

        return connection;
    }

    /* ================= REQUIRED FOR BACKUP / RESTORE ================= */

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
        connection = null;
    }

    /* ================= FOR BACKUP SERVICE ================= */

    public static Path getDatabasePath() {
        return AppPaths.getDatabasePath();
    }

    /* ================= SCHEMA ================= */

    private static void initSchema() throws SQLException {
        try (Statement st = connect().createStatement()) {

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
        grand_total REAL NOT NULL
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

        }
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
