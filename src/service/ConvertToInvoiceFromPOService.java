package service;

import dao.*;
import model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class ConvertToInvoiceFromPOService {

    private final PurchaseOrderDAO poDAO = new PurchaseOrderDAO();
    private final PurchaseOrderService poService = new PurchaseOrderService();

    public Invoice prepareInvoiceFromPO(String poNo) {

        try {

            PurchaseOrder po = poDAO.findByPoNo(poNo);

            if (po == null) {
                throw new IllegalArgumentException("PO not found: " + poNo);
            }

            Invoice invoice = new Invoice();

            invoice.setPoId(po.getId());
            invoice.setPoReference(po.getPoNo());


            for (PurchaseOrderItem poItem : po.getItems()) {

                InvoiceItem item = new InvoiceItem();

                item.setItemName(poItem.getItemName());
                item.setHsn(poItem.getHsn());
                item.setUnit(poItem.getUnit());
                item.setQty(poItem.getQty());
                item.setRate(poItem.getRate());
                item.setGstPercent(poItem.getGstPercent());

                invoice.getItems().add(item);
            }

            return invoice;

        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare invoice from PO", e);
        }
    }

    private void updatePOStatus(Connection conn, long poId) throws Exception {

        String billedSql = """
        SELECT COALESCE(SUM(grand_total),0)
        FROM invoices
        WHERE po_id = ?
    """;

        String poTotalSql = """
        SELECT grand_total
        FROM purchase_orders
        WHERE id = ?
    """;

        {

            double billedTotal = 0;
            double poTotal = 0;

            try (PreparedStatement ps = conn.prepareStatement(billedSql)) {
                ps.setLong(1, poId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) billedTotal = rs.getDouble(1);
            }

            try (PreparedStatement ps = conn.prepareStatement(poTotalSql)) {
                ps.setLong(1, poId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) poTotal = rs.getDouble(1);
            }

            POStatus newStatus;

            if (Math.abs(billedTotal) < 0.01) {
                newStatus = POStatus.OPEN;
            } else if (billedTotal + 0.01 < poTotal) {
                newStatus = POStatus.PARTIAL;
            } else {
                newStatus = POStatus.COMPLETED;
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE purchase_orders SET status = ? WHERE id = ?")) {

                ps.setString(1, newStatus.name());
                ps.setLong(2, poId);
                ps.executeUpdate();
            }
        }
    }

    public boolean isInvoiceExceedingPO(long poId, double invoiceTotal) {

        String billedSql = """
        SELECT COALESCE(SUM(grand_total),0)
        FROM invoices
        WHERE po_id = ?
    """;

        String poTotalSql = "SELECT grand_total FROM purchase_orders WHERE id = ?";

        try (Connection conn = DB.connect()) {

            double billedTotal = 0;
            double poTotal = 0;

            try (PreparedStatement ps = conn.prepareStatement(billedSql)) {
                ps.setLong(1, poId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) billedTotal = rs.getDouble(1);
            }

            try (PreparedStatement ps = conn.prepareStatement(poTotalSql)) {
                ps.setLong(1, poId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) poTotal = rs.getDouble(1);
            }

            return (billedTotal + invoiceTotal) > (poTotal + 0.01);
        } catch (Exception e) {
            throw new RuntimeException("Invoice is exceeding PO", e);
        }
    }

    public boolean isAnyItemExceedingPOQty(Invoice invoice) {

        try {
            if (invoice.getPoId() == null) return false;

            List<POItemUsage> usageList =
                    poService.getItemUsage(invoice.getPoId());

            for (InvoiceItem invItem : invoice.getItems()) {

                for (POItemUsage usage : usageList) {

                    if (usage.getItemName().equals(invItem.getItemName())) {

                        double remaining = usage.getRemainingQty();

                        if (invItem.getQty() > remaining + 0.01) {
                            return true;
                        }
                    }
                }
            }

            return false;
        } catch (Exception e) {
            throw new RuntimeException("Item exceeding PO quantity", e);
        }
    }

    public int saveInvoiceFromPO(Invoice invoice) throws Exception {

        try (Connection conn = DB.connect()) {

            conn.setAutoCommit(false);

            try {

                int invoiceNo = InvoiceDAO.saveInvoice(
                        conn,
                        invoice,
                        invoice.getItems()
                );

                if (invoice.getPoId() != null) {
                    updatePOStatus(conn, invoice.getPoId());
                }

                conn.commit();
                return invoiceNo;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
