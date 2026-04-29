package dao;

import model.Contract;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ContractDAO {

    public long save(Contract c){

        String sql = """
            INSERT INTO contracts (
                contract_no,
                description,
                base_quantity,
                variation_percent,
                max_quantity,
                base_value,
                max_value,
                start_date,
                end_date,
                status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, c.getContractNo());
            ps.setString(2, c.getDescription());
            ps.setDouble(3, c.getBaseQuantity());
            ps.setDouble(4, c.getVariationPercent());
            ps.setDouble(5, c.getMaxQuantity());
            ps.setDouble(6, c.getBaseValue());
            ps.setDouble(7, c.getMaxValue());
            ps.setString(8, c.getStartDate() != null ? c.getStartDate().toString() : null);
            ps.setString(9, c.getEndDate() != null ? c.getEndDate().toString() : null);
            ps.setString(10, c.getStatus());

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (!rs.next()) throw new SQLException("Failed to create contract");

            return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save contract", e);
        }
    }

    public List<Contract> findAllActive() {

        String sql = "SELECT * FROM contracts WHERE status = 'ACTIVE'";

        List<Contract> list = new ArrayList<>();

        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Contract c = new Contract();

                c.setId(rs.getLong("id"));
                c.setContractNo(rs.getString("contract_no"));
                c.setMaxValue(rs.getDouble("max_value"));

                list.add(c);
            }

            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load active contracts", e);
        }
    }

    public double getTotalPOValueForContract(long contractId) {

        String sql = """
        SELECT COALESCE(SUM(grand_total),0)
        FROM purchase_orders
        WHERE contract_id = ?
        AND status != 'CLOSED'
    """;

        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, contractId);

            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to calculate contract utilization", e);
        }
    }

    public List<Contract> findAll() {

        String sql = "SELECT * FROM contracts";

        List<Contract> list = new ArrayList<>();

        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Contract c = new Contract();

                c.setId(rs.getLong("id"));
                c.setContractNo(rs.getString("contract_no"));
                c.setDescription(rs.getString("description"));
                c.setBaseQuantity(rs.getDouble("base_quantity"));
                c.setVariationPercent(rs.getDouble("variation_percent"));
                c.setMaxQuantity(rs.getDouble("max_quantity"));
                c.setBaseValue(rs.getDouble("base_value"));
                c.setMaxValue(rs.getDouble("max_value"));

                String start = rs.getString("start_date");
                String end = rs.getString("end_date");

                c.setStartDate(start != null ? LocalDate.parse(start) : null);
                c.setEndDate(end != null ? LocalDate.parse(end) : null);

                c.setStatus(rs.getString("status"));

                list.add(c);
            }

            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load contracts", e);
        }
    }
}