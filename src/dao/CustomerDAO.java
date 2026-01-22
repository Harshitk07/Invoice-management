package dao;

import model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class CustomerDAO {

    private CustomerDAO() {}

    // =====================
    // LOAD ALL (MASTER USE)
    // =====================
    public static List<Customer> findAll() {

        String sql = """
            SELECT id, name, address, gst_no, state, state_code, active
            FROM customers
            ORDER BY name
        """;

        return queryList(sql);
    }

    // =====================
    // LOAD ACTIVE (INVOICE USE)
    // =====================
    public static List<Customer> findActive() {

        String sql = """
            SELECT id, name, address, gst_no, state, state_code, active
            FROM customers
            WHERE active = 1
            ORDER BY name
        """;

        return queryList(sql);
    }

    // =====================
    // SAVE (ALWAYS ACTIVE)
    // =====================
    public static void save(Customer c) {

        String sql = """
            INSERT INTO customers
            (name, address, gst_no, state, state_code, active)
            VALUES (?, ?, ?, ?, ?, 1)
        """;

        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, c.getName());
            ps.setString(2, c.getAddress());
            ps.setString(3, c.getGstNo());
            ps.setString(4, c.getState());
            ps.setString(5, c.getStateCode());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save customer", e);
        }
    }

    // =====================
    // UPDATE (NO DELETE)
    // =====================
    public static void update(Customer c) {
        String sql = """
        UPDATE customers
        SET name=?, address=?, gst_no=?, state=?, state_code=?, active=?
        WHERE id=?
    """;

        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, c.getName());
            ps.setString(2, c.getAddress());
            ps.setString(3, c.getGstNo());
            ps.setString(4, c.getState());
            ps.setString(5, c.getStateCode());
            ps.setInt(6, c.isActive() ? 1 : 0);
            ps.setInt(7, c.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // =====================
    // SOFT ENABLE / DISABLE
    // =====================
    public static void updateActive(int id, boolean active) {

        String sql = "UPDATE customers SET active=? WHERE id=?";

        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update customer status", e);
        }
    }

    // =====================
    // FIND BY ID (REPRINT)
    // =====================
    public static Customer findById(int id) {

        String sql = """
            SELECT id, name, address, gst_no, state, state_code, active
            FROM customers
            WHERE id = ?
        """;

        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load customer", e);
        }

        return null;
    }

    // =====================
    // INTERNAL HELPERS
    // =====================
    private static List<Customer> queryList(String sql) {

        List<Customer> list = new ArrayList<>();

        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load customers", e);
        }

        return list;
    }

    private static Customer map(ResultSet rs) throws SQLException {

        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setAddress(rs.getString("address"));
        c.setGstNo(rs.getString("gst_no"));
        c.setState(rs.getString("state"));
        c.setStateCode(rs.getString("state_code"));
        c.setActive(rs.getInt("active") == 1);
        return c;
    }
}
