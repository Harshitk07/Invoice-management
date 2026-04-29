package service;

import dao.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ContractService {

    public double getTotalBilledValue(long contractId) {

        String sql = """
            SELECT COALESCE(SUM(i.grand_total),0)
            FROM invoices i
            JOIN purchase_orders p ON p.id = i.po_id
            WHERE p.contract_id = ?
        """;

        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, contractId);

            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to calculate billed value", e);
        }
    }

    public double getTotalBilledQuantity(long contractId) {

        String sql = """
            SELECT COALESCE(SUM(ii.qty),0)
            FROM invoice_items ii
            JOIN invoices i ON i.id = ii.invoice_id
            JOIN purchase_orders p ON p.id = i.po_id
            WHERE p.contract_id = ?
        """;

        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, contractId);

            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to calculate billed quantity", e);
        }
    }
}