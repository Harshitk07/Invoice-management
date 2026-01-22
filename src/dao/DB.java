package dao;

import java.nio.file.*;
import java.sql.*;

public final class DB {

    /* ================= PATH ================= */

    private static final Path BASE_DIR =
            Paths.get(System.getProperty("user.dir"));

    private static final Path DB_DIR = BASE_DIR.resolve("data");
    private static final Path DB_PATH = DB_DIR.resolve("shree_uma_invoice.db");

    private static final String URL =
            "jdbc:sqlite:" + DB_PATH.toAbsolutePath();

    private static Connection connection;

    private DB() {}

    /* ================= INIT ================= */

    // CALL ONCE from MainApp.start()
    public static void init() {
        try {
            Files.createDirectories(DB_DIR);
            connect();          // ensures DB file exists
            initSchema();
        } catch (Exception e) {
            throw new RuntimeException("DB initialization failed", e);
        }
    }

    public static Connection connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL);

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
        return DB_PATH;
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

                    buyer_name TEXT,
                    buyer_address TEXT,
                    buyer_gst TEXT,
                    buyer_state TEXT,
                    buyer_state_code TEXT,

                    consignee_name TEXT,
                    consignee_address TEXT,
                    consignee_gst TEXT,
                    consignee_state TEXT,
                    consignee_state_code TEXT,

                    terms_of_payment TEXT,
                    dispatch_through TEXT,
                    po_no TEXT,
                    po_date TEXT,
                    dc_no TEXT,
                    dc_date TEXT,
                    eway_bill_no TEXT,

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

            migrate(st, "ALTER TABLE invoices ADD COLUMN terms_of_payment TEXT");
            migrate(st, "ALTER TABLE invoices ADD COLUMN dispatch_through TEXT");
            migrate(st, "ALTER TABLE invoices ADD COLUMN eway_bill_no TEXT");
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
            return Files.exists(DB_PATH)
                    ? Files.size(DB_PATH)
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
