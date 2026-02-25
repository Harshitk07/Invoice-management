package dao;

import model.Invoice;
import model.InvoiceItem;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class InvoiceDAO {

    private InvoiceDAO() {}

    /* ================= PUBLIC API ================= */

    /**
     * Saves invoice header + items in ONE transaction.
     * The connection is GUARANTEED to be CLOSED
     * before control returns to the caller.
     */
    public static int saveInvoice(Invoice invoice, List<InvoiceItem> items) {

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Invoice must contain at least one item");
        }

        try (Connection con = DB.connect()) {

            con.setAutoCommit(false);

            try {

                int invoiceNo = getNextInvoiceNo(con);
                invoice.setInvoiceNo(invoiceNo);

                int invoiceId = insertInvoice(con, invoice);
                insertInvoiceItems(con, invoiceId, items);

                con.commit();
                return invoiceNo;

            } catch (Exception e) {
                con.rollback();
                throw e;
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to save invoice", e);
        }
    }


    /* ================= INTERNAL ================= */

    private static int getNextInvoiceNo(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COALESCE(MAX(invoice_no), 100) + 1 FROM invoices"
             )) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static int insertInvoice(Connection con, Invoice inv) throws SQLException {

        String sql = """
    INSERT INTO invoices (
        invoice_no,
        invoice_date,

        /* ===== SELLER SNAPSHOT ===== */
        seller_name,
        seller_description,
        seller_address,
        seller_gst,
        seller_phone,
        seller_email,
        seller_bank_name,
        seller_account_no,
        seller_ifsc,

        /* ===== BUYER ===== */
        buyer_name, buyer_address, buyer_gst,
        buyer_state, buyer_state_code,

        /* ===== CONSIGNEE ===== */
        consignee_name, consignee_address, consignee_gst,
        consignee_state, consignee_state_code,

        /* ===== META ===== */
        terms_of_payment,
        po_no, po_date,
        dc_no, dc_date,
        dispatch_through,
        eway_bill_no,
        notes,

        /* ===== TOTALS ===== */
        taxable_subtotal,
        cgst,
        sgst,
        igst,
        round_off,
        grand_total
    )
    VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?
                    )
""";


        try (PreparedStatement ps =
                     con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int i = 1;

            ps.setInt(i++, inv.getInvoiceNo());
            ps.setString(i++, inv.getInvoiceDate() != null ? inv.getInvoiceDate().toString() : null);

            // ===== SELLER =====
            ps.setString(i++, inv.getSellerName());
            ps.setString(i++, inv.getSellerDescription());
            ps.setString(i++, inv.getSellerAddress());
            ps.setString(i++, inv.getSellerGst());
            ps.setString(i++, inv.getSellerPhone());
            ps.setString(i++, inv.getSellerEmail());
            ps.setString(i++, inv.getSellerBankName());
            ps.setString(i++, inv.getSellerAccountNo());
            ps.setString(i++, inv.getSellerIfsc());

            ps.setString(i++, inv.getBuyerName());
            ps.setString(i++, inv.getBuyerAddress());
            ps.setString(i++, inv.getBuyerGst());
            ps.setString(i++, inv.getBuyerState());
            ps.setString(i++, inv.getBuyerStateCode());

            ps.setString(i++, inv.getConsigneeName());
            ps.setString(i++, inv.getConsigneeAddress());
            ps.setString(i++, inv.getConsigneeGst());
            ps.setString(i++, inv.getConsigneeState());
            ps.setString(i++, inv.getConsigneeStateCode());

            ps.setString(i++, inv.getTermsOfPayment());
            ps.setString(i++, inv.getPoNo());
            ps.setString(i++, inv.getPoDate() != null ? inv.getPoDate().toString() : null);
            ps.setString(i++, inv.getDcNo());
            ps.setString(i++, inv.getDcDate() != null ? inv.getDcDate().toString() : null);

            ps.setString(i++, inv.getDispatchThrough());
            ps.setString(i++, inv.getEwayBillNo());
            ps.setString(i++, inv.getNotes());
            ps.setDouble(i++, inv.getTaxableAmount());
            ps.setDouble(i++, inv.getCgstTotal());
            ps.setDouble(i++, inv.getSgstTotal());
            ps.setDouble(i++, inv.getIgstTotal());
            ps.setDouble(i++, inv.getRoundOff());
            ps.setDouble(i++, inv.getGrandTotal());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Failed to retrieve invoice ID");
                }
                return rs.getInt(1);
            }
        }
    }

    private static void insertInvoiceItems(
            Connection con,
            int invoiceId,
            List<InvoiceItem> items
    ) throws SQLException {

        String sql = """
            INSERT INTO invoice_items (
                invoice_id,
                item_name,
                hsn,
                qty,
                unit,
                rate,
                gst_percent,
                taxable_amount
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            for (InvoiceItem item : items) {
                ps.setInt(1, invoiceId);
                ps.setString(2, item.getItemName());
                ps.setString(3, item.getHsn());
                ps.setDouble(4, item.getQty());
                ps.setString(5, item.getUnit());
                ps.setDouble(6, item.getRate());
                ps.setDouble(7, item.getGstPercent());
                ps.setDouble(8, item.getAmount()); // stored value, no recalculation

                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    /* ================= READ ================= */

    public static Invoice findInvoiceByNo(int invoiceNo) {

        String sql = "SELECT * FROM invoices WHERE invoice_no = ?";

        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, invoiceNo);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                Invoice inv = new Invoice();

                inv.setInvoiceNo(rs.getInt("invoice_no"));

                inv._setInvoiceDateFromDB(
                        parseInvoiceDate(rs.getString("invoice_date"))
                );

                // ===== SELLER SNAPSHOT =====
                inv.setSellerName(rs.getString("seller_name"));
                inv.setSellerDescription(rs.getString("seller_description"));
                inv.setSellerAddress(rs.getString("seller_address"));
                inv.setSellerGst(rs.getString("seller_gst"));
                inv.setSellerPhone(rs.getString("seller_phone"));
                inv.setSellerEmail(rs.getString("seller_email"));
                inv.setSellerBankName(rs.getString("seller_bank_name"));
                inv.setSellerAccountNo(rs.getString("seller_account_no"));
                inv.setSellerIfsc(rs.getString("seller_ifsc"));


                inv.setBuyerName(rs.getString("buyer_name"));
                inv.setBuyerAddress(rs.getString("buyer_address"));
                inv.setBuyerGst(rs.getString("buyer_gst"));
                inv.setBuyerState(rs.getString("buyer_state"));
                inv.setBuyerStateCode(rs.getString("buyer_state_code"));

                inv.setConsigneeName(rs.getString("consignee_name"));
                inv.setConsigneeAddress(rs.getString("consignee_address"));
                inv.setConsigneeGst(rs.getString("consignee_gst"));
                inv.setConsigneeState(rs.getString("consignee_state"));
                inv.setConsigneeStateCode(rs.getString("consignee_state_code"));

                inv.setPoNo(rs.getString("po_no"));
                inv.setPoDate(
                        parseInvoiceDate(rs.getString("po_date"))
                );

                inv.setDcNo(rs.getString("dc_no"));
                inv.setDcDate(
                        parseInvoiceDate(rs.getString("dc_date"))
                );


                inv.setDispatchThrough(rs.getString("dispatch_through"));
                inv.setEwayBillNo(rs.getString("eway_bill_no"));
                inv.setNotes(rs.getString("notes"));
                inv.setTaxableAmount(rs.getDouble("taxable_subtotal"));
                inv.setCgstTotal(rs.getDouble("cgst"));
                inv.setSgstTotal(rs.getDouble("sgst"));
                inv.setIgstTotal(rs.getDouble("igst"));
                inv.setRoundOff(rs.getDouble("round_off"));
                inv.setGrandTotal(rs.getDouble("grand_total"));

                return inv;
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load invoice " + invoiceNo, e);
        }
    }

    public static List<InvoiceItem> findItemsByInvoiceNo(int invoiceNo) {

        String sql = """
            SELECT ii.*
            FROM invoice_items ii
            JOIN invoices i ON i.id = ii.invoice_id
            WHERE i.invoice_no = ?
            ORDER BY ii.id
        """;

        List<InvoiceItem> items = new ArrayList<>();

        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, invoiceNo);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    InvoiceItem item = new InvoiceItem();

                    item.setItemName(rs.getString("item_name"));
                    item.setHsn(rs.getString("hsn"));
                    item.setQty(rs.getDouble("qty"));
                    item.setUnit(rs.getString("unit"));
                    item.setRate(rs.getDouble("rate"));
                    item.setGstPercent(rs.getDouble("gst_percent"));

                    item.setAmount(rs.getDouble("taxable_amount")); // restore stored value

                    items.add(item);
                }
            }

            return items;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load invoice items", e);
        }
    }

    public static boolean invoiceExists(int invoiceNo) {
        String sql = "SELECT 1 FROM invoices WHERE invoice_no = ?";

        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, invoiceNo);
            return ps.executeQuery().next();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Invoice> listInvoices() {

        String sql = """
        SELECT invoice_no, invoice_date, buyer_name, grand_total
        FROM invoices
        ORDER BY invoice_no DESC
    """;

        List<Invoice> list = new ArrayList<>();

        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Invoice inv = new Invoice();
                inv.setInvoiceNo(rs.getInt("invoice_no"));
                inv._setInvoiceDateFromDB(
                        parseInvoiceDate(rs.getString("invoice_date"))
                );
                inv.setBuyerName(rs.getString("buyer_name"));
                inv.setGrandTotal(rs.getDouble("grand_total"));
                list.add(inv);
            }

            return list;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }




    /* ================= UTIL ================= */

    private static void rollbackQuietly(Connection con) {
        try {
            if (con != null) con.rollback();
        } catch (Exception ignored) {}
    }

    private static void closeQuietly(Connection con) {
        try {
            if (con != null) con.close();
        } catch (Exception ignored) {}
    }

    private static LocalDate parseInvoiceDate(String value) {

        if (value == null || value.isBlank()) return null;

        // Epoch millis (old data)
        if (value.matches("\\d+")) {
            long millis = Long.parseLong(value);
            return java.time.Instant.ofEpochMilli(millis)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }

        // ISO date (new data)
        return LocalDate.parse(value);
    }

}
