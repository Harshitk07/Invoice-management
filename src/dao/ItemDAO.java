package dao;

import model.Item;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    // =========================
    // SAVE ITEM
    // =========================
    public static void save(Item item) {

        String sql = """
            INSERT INTO items (name, hsn, unit, rate, gst_percent, active)
            VALUES (?, ?, ?, ?, ?, 1)
        """;

        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, item.getName());
            ps.setString(2, item.getHsn());
            ps.setString(3, item.getUnit());
            ps.setDouble(4, item.getRate());
            ps.setDouble(5, item.getGstPercent());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save item: " + item.getName(), e);
        }
    }

    // =========================
    // UPDATE ITEM (FUTURE-SAFE)
    // =========================

    public static void updateActive(int itemId, boolean active) {

        String sql = "UPDATE items SET active = ? WHERE id = ?";

        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, itemId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public static void update(Item item) {

        String sql = """
            UPDATE items
            SET hsn = ?, unit = ?, rate = ?, gst_percent = ?
            WHERE id = ?
        """;

        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, item.getHsn());
            ps.setString(2, item.getUnit());
            ps.setDouble(3, item.getRate());
            ps.setDouble(4, item.getGstPercent());
            ps.setInt(5, item.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update item: " + item.getName(), e);
        }
    }


    // =========================
    // LOAD ALL ITEMS
    // =========================
    public static List<Item> findAll() {

        List<Item> items = new ArrayList<>();

        String sql = """
            SELECT id, name, hsn, unit, rate, gst_percent, active
            FROM items
            ORDER BY name
        """;

        try (Connection con = DB.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                items.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load items", e);
        }

        return items;
    }

    // =========================
    // LOAD ALL ITEM NAMES (FAST)
    // =========================
    public static List<String> findAllNames() {

        List<String> names = new ArrayList<>();

        String sql = "SELECT name FROM items WHERE active = 1 ORDER BY name";

        try (Connection con = DB.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                names.add(rs.getString("name"));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load item names", e);
        }

        return names;
    }

    public static List<Item> findActiveItems() {

        String sql = """
        SELECT id, name, hsn, unit, rate, gst_percent, active
        FROM items
        WHERE active = 1
        ORDER BY name
    """;

        List<Item> items = new ArrayList<>();

        try (Connection con = DB.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                items.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return items;
    }



    // =========================
    // FIND BY NAME
    // =========================
    public static Item findByName(String name) {

        String sql = """
            SELECT id, name, hsn, unit, rate, gst_percent
            FROM items
            WHERE name = ? AND active = 1
        """;

        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find item: " + name, e);
        }

        return null;
    }

    public static boolean isItemUsed(int itemId) {

        String sql = """
        SELECT 1
        FROM invoice_items
        WHERE item_name = (
            SELECT name FROM items WHERE id = ?
        )
        LIMIT 1
    """;

        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, itemId);
            return ps.executeQuery().next();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    // =========================
    // ROW MAPPER (SINGLE SOURCE)
    // =========================
    private static Item mapRow(ResultSet rs) throws SQLException {

        Item item = new Item();
        item.setId(rs.getInt("id"));
        item.setName(rs.getString("name"));
        item.setHsn(rs.getString("hsn"));
        item.setUnit(rs.getString("unit"));
        item.setRate(rs.getDouble("rate"));
        item.setGstPercent(rs.getDouble("gst_percent"));
        item.setActive(rs.getInt("active") == 1);

        return item;
    }
}
