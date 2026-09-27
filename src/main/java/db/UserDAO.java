package db;

import model.User;
import util.PasswordUtil;

import java.sql.*;
import java.util.Optional;

public class UserDAO {

    public void ensureSchema() throws SQLException {
        String usersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) NOT NULL UNIQUE,
                password_hash VARCHAR(128) NOT NULL,
                salt VARCHAR(64) NOT NULL,
                full_name VARCHAR(100) NOT NULL,
                role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
                designation VARCHAR(100) NULL,
                authority_category VARCHAR(50) NULL,
                authority_level INT NULL,
                email VARCHAR(120) NULL,
                phone VARCHAR(30) NULL
            )
            """;

        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate(usersTable);
            addColumnIfMissing(conn, "users", "designation", "VARCHAR(100) NULL");
            addColumnIfMissing(conn, "users", "authority_category", "VARCHAR(50) NULL");
            addColumnIfMissing(conn, "users", "authority_level", "INT NULL");
            addColumnIfMissing(conn, "users", "email", "VARCHAR(120) NULL");
            addColumnIfMissing(conn, "users", "phone", "VARCHAR(30) NULL");

            addColumnIfMissing(conn, "complaints", "user_id", "INT NULL");
            addColumnIfMissing(conn, "complaints", "assigned_designation", "VARCHAR(100) NULL");
            addColumnIfMissing(conn, "complaints", "level0_designation", "VARCHAR(100) NULL");
            addForeignKeyIfMissing(conn);
        }
    }

    private void addColumnIfMissing(Connection conn, String table, String column, String definition) throws SQLException {
        String check = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                    }
                }
            }
        }
    }

    private void addForeignKeyIfMissing(Connection conn) throws SQLException {
        String check = "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'complaints' AND CONSTRAINT_NAME = 'fk_complaints_user'";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(check)) {
            if (rs.next() && rs.getInt(1) == 0) {
                st.executeUpdate("ALTER TABLE complaints ADD CONSTRAINT fk_complaints_user " +
                        "FOREIGN KEY (user_id) REFERENCES users(id)");
            }
        } catch (SQLException e) {
            // FK may fail if orphan rows exist
        }
    }

    public boolean adminExists() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public boolean authorityAccountExists(String username) throws SQLException {
        return findByUsername(username).isPresent();
    }

    public int countAuthorityAccounts() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'AUTHORITY'";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public User createUser(String username, String plainPassword, String fullName, String role) throws SQLException {
        return createUser(username, plainPassword, fullName, role, null, null, null, null, null);
    }

    public User createStudent(String username, String plainPassword, String fullName, String email, String phone) throws SQLException {
        return createUser(username, plainPassword, fullName, "STUDENT", null, null, null, email, phone);
    }

    public User createAuthorityUser(String username, String plainPassword, String fullName,
                                    String designation, String authorityCategory, int authorityLevel) throws SQLException {
        return createUser(username, plainPassword, fullName, "AUTHORITY", designation, authorityCategory, authorityLevel, null, null);
    }

    private User createUser(String username, String plainPassword, String fullName, String role,
                            String designation, String authorityCategory, Integer authorityLevel,
                            String email, String phone) throws SQLException {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(plainPassword, salt);

        String sql = "INSERT INTO users (username, password_hash, salt, full_name, role, designation, authority_category, authority_level, email, phone) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username.toLowerCase());
            ps.setString(2, hash);
            ps.setString(3, salt);
            ps.setString(4, fullName);
            ps.setString(5, role);
            ps.setString(6, designation);
            ps.setString(7, authorityCategory);
            if (authorityLevel != null) ps.setInt(8, authorityLevel);
            else ps.setNull(8, Types.INTEGER);
            ps.setString(9, email);
            ps.setString(10, phone);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return getById(keys.getInt(1)).orElseThrow();
                }
            }
        }
        return null;
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<User> getById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSalt(rs.getString("salt"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(rs.getString("role"));
        u.setDesignation(rs.getString("designation"));
        u.setAuthorityCategory(rs.getString("authority_category"));
        int al = rs.getInt("authority_level");
        if (!rs.wasNull()) u.setAuthorityLevel(al);
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        return u;
    }
}
