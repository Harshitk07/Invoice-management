package service;

import dao.DB;
import dao.PurchaseOrderDAO;
import model.*;

import java.sql.*;
import java.util.*;

public class PurchaseOrderService {

    private final PurchaseOrderDAO poDAO = new PurchaseOrderDAO();

    // =========================
    // GET ITEM USAGE (FOR WARNINGS)
    // =========================
    public List<POItemUsage> getItemUsage(long poId) throws Exception {

        String billedSql = """
            SELECT ii.item_id, COALESCE(SUM(ii.qty),0)
            FROM invoice_items ii
            JOIN invoices i ON i.id = ii.invoice_id
            WHERE i.po_id = ?
            GROUP BY ii.item_id
        """;

        Map<Long, Double> billedMap = new HashMap<>();

        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(billedSql)) {

            ps.setLong(1, poId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                billedMap.put(
                        rs.getLong(1),
                        rs.getDouble(2)
                );
            }
        }

        PurchaseOrder po = poDAO.findById(poId);

        List<POItemUsage> result = new ArrayList<>();

        for (PurchaseOrderItem item : po.getItems()) {

            long itemId = item.getItem().getId();

            double billed = billedMap.getOrDefault(itemId, 0.0);

            POItemUsage usage = new POItemUsage();
            usage.setItemName(item.getItemName());
            usage.setOrderedQty(item.getQty());
            usage.setBilledQty(billed);
            usage.setRemainingQty(item.getQty() - billed);

            result.add(usage);
        }

        return result;
    }

    public PurchaseOrder findById(long poId) {
        return poDAO.findById(poId);
    }
}