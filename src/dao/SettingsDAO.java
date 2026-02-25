package dao;

import java.sql.*;

public final class SettingsDAO {

    private SettingsDAO() {}

    public static String get(String key) {
        String sql = "SELECT value FROM app_settings WHERE key = ?";

        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, key);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("value");
                return null;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void set(String key, String value) {

        String sql = """
            INSERT INTO app_settings (key, value)
            VALUES (?, ?)
            ON CONFLICT(key) DO UPDATE SET value = excluded.value
        """;

        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}