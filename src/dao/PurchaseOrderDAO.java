package dao;

import model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PurchaseOrderDAO {

    // =========================
    // SAVE
    // =========================
    public long save(PurchaseOrder po) {

        if (po.getItems() == null || po.getItems().isEmpty())
            throw new IllegalArgumentException("PO must contain at least one item");

        String insertPO = """
            INSERT INTO purchase_orders (
                po_no,
                po_date,
                delivery_by_date,
                contract_id,
                subtotal,
                gst_total,
                grand_total,
                status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        String insertItem = """
            INSERT INTO purchase_order_items (
                po_id,
                item_id,
                qty,
                rate,
                gst_percent
            ) VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = DB.connect()) {

            conn.setAutoCommit(false);

            try {

                long poId;

                try (PreparedStatement ps =
                             conn.prepareStatement(insertPO, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, po.getPoNo());

                    ps.setDate(2, po.getPoDate() != null
                            ? Date.valueOf(po.getPoDate())
                            : null);

                    ps.setDate(3, po.getDeliveryByDate() != null
                            ? Date.valueOf(po.getDeliveryByDate())
                            : null);

                    if (po.getContractId() != null)
                        ps.setLong(4, po.getContractId());
                    else
                        ps.setNull(4, Types.BIGINT);

                    ps.setDouble(5, po.getSubtotal());
                    ps.setDouble(6, po.getGstTotal());
                    ps.setDouble(7, po.getGrandTotal());
                    ps.setString(8, po.getStatus().name());

                    ps.executeUpdate();

                    ResultSet rs = ps.getGeneratedKeys();
                    if (!rs.next())
                        throw new SQLException("Failed to retrieve PO ID");

                    poId = rs.getLong(1);
                }

                try (PreparedStatement itemStmt =
                             conn.prepareStatement(insertItem)) {

                    for (PurchaseOrderItem item : po.getItems()) {

                        if (item.getItem() == null)
                            throw new IllegalStateException("Item cannot be null");

                        itemStmt.setLong(1, poId);
                        itemStmt.setLong(2, item.getItem().getId());
                        itemStmt.setDouble(3, item.getQty());
                        itemStmt.setDouble(4, item.getRate());
                        itemStmt.setDouble(5, item.getGstPercent());

                        itemStmt.addBatch();
                    }

                    itemStmt.executeBatch();
                }

                conn.commit();
                return poId;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to save purchase order", e);
        }
    }

    // =========================
    // FIND BY ID
    // =========================
    public PurchaseOrder findById(long id) {

        String sql = "SELECT * FROM purchase_orders WHERE id = ?";

        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            ResultSet rs = ps.executeQuery();

            if (!rs.next())
                throw new IllegalArgumentException("PO not found: " + id);

            PurchaseOrder po = mapPO(rs);
            po.setItems(findItemsByPoId(id));

            return po;

        } catch (Exception e) {
            throw new RuntimeException("Failed to find PO", e);
        }
    }

    // =========================
    // FIND ITEMS
    // =========================
    public List<PurchaseOrderItem> findItemsByPoId(long poId) {

        String sql = """
            SELECT 
                poi.id AS poi_id,
                poi.qty,
                poi.rate,
                poi.gst_percent,
                i.id AS item_id,
                i.name,
                i.hsn,
                i.unit
            FROM purchase_order_items poi
            JOIN items i ON poi.item_id = i.id
            WHERE poi.po_id = ?
        """;

        List<PurchaseOrderItem> list = new ArrayList<>();

        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, poId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                PurchaseOrderItem item = new PurchaseOrderItem();

                item.setId(rs.getLong("poi_id"));
                item.setPoId(poId);

                Item master = new Item();
                master.setId(rs.getInt("item_id"));
                master.setName(rs.getString("name"));
                master.setHsn(rs.getString("hsn"));
                master.setUnit(rs.getString("unit"));

                item.setItem(master);

                item.setQty(rs.getDouble("qty"));
                item.setRate(rs.getDouble("rate"));
                item.setGstPercent(rs.getDouble("gst_percent"));

                list.add(item);
            }

            return list;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load PO items", e);
        }
    }

    private PurchaseOrder mapPO(ResultSet rs) throws SQLException {

        PurchaseOrder po = new PurchaseOrder();

        po.setId(rs.getLong("id"));
        po.setPoNo(rs.getString("po_no"));

        Date poDate = rs.getDate("po_date");
        po.setPoDate(poDate != null ? poDate.toLocalDate() : null);

        Date deliveryDate = rs.getDate("delivery_by_date");
        po.setDeliveryByDate(deliveryDate != null
                ? deliveryDate.toLocalDate()
                : null);

        po.setContractId(rs.getObject("contract_id") != null
                ? rs.getLong("contract_id")
                : null);

        po.setSubtotal(rs.getDouble("subtotal"));
        po.setGstTotal(rs.getDouble("gst_total"));
        po.setGrandTotal(rs.getDouble("grand_total"));
        po.setStatus(POStatus.valueOf(rs.getString("status")));

        return po;
    }

    public PurchaseOrder findByPoNo(String poNo) {

        String sql = "SELECT * FROM purchase_orders WHERE po_no = ?";

        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, poNo);

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                return null;
            }

            PurchaseOrder po = mapPO(rs);
            po.setItems(findItemsByPoId(po.getId()));

            return po;

        } catch (Exception e) {
            throw new RuntimeException("Failed to find PO by number", e);
        }
    }

    public List<PurchaseOrder> findAll() {

        String sql = "SELECT * FROM purchase_orders ORDER BY po_date DESC";

        List<PurchaseOrder> list = new ArrayList<>();

        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                PurchaseOrder po = mapPO(rs);
                list.add(po);
            }

            return list;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load purchase orders", e);
        }
    }
}