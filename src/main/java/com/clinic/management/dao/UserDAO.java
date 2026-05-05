package com.clinic.management.dao;

import com.clinic.management.model.User;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Bảng {@code tbl_user} (snake_case cột: username, password, full_name, role) — khớp schema Hibernate cũ.
 */
@Repository
public class UserDAO extends DAO {

    private static final String TBL = "tbl_user";

    public UserDAO(DataSource dataSource) {
        super(dataSource);
    }

    public long count() throws DataAccessException {
        String sql = "SELECT COUNT(*) FROM " + TBL;
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new DataAccessException("UserDAO.count failed", e) {};
        }
    }

    public Optional<User> findByUsernameAndPassword(String username, String password) throws DataAccessException {
        String sql = "SELECT id, username, password, full_name, role FROM " + TBL
                + " WHERE LOWER(TRIM(username)) = LOWER(TRIM(?)) AND password = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("UserDAO.findByUsernameAndPassword failed", e) {};
        }
    }

    public boolean existsByUsername(String username) throws DataAccessException {
        String sql = "SELECT 1 FROM " + TBL + " WHERE LOWER(TRIM(username)) = LOWER(TRIM(?)) LIMIT 1";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("UserDAO.existsByUsername failed", e) {};
        }
    }

    public void insert(User user) throws DataAccessException {
        String sql = "INSERT INTO " + TBL + " (username, password, full_name, role) VALUES (?,?,?,?) RETURNING id";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user.getUsername().trim());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName() != null ? user.getFullName().trim() : "");
            ps.setString(4, user.getRole());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.setId(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("UserDAO.insert failed", e) {};
        }
    }

    private static User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("full_name"),
                rs.getString("role"));
    }
}
