package com.clinic.management.dao;

import com.clinic.management.model.Clinic;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ClinicDAO extends DAO {

    private static final String TBL = "tbl_clinic";

    public ClinicDAO(DataSource dataSource) {
        super(dataSource);
    }

    public List<Clinic> findAll() throws DataAccessException {
        String sql = "SELECT id, name, address, des FROM " + TBL + " ORDER BY id ASC";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Clinic> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("ClinicDAO.findAll failed", e) {};
        }
    }

    public Optional<Clinic> findById(int id) throws DataAccessException {
        String sql = "SELECT id, name, address, des FROM " + TBL + " WHERE id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("ClinicDAO.findById failed", e) {};
        }
    }

    public void insert(Clinic clinic) throws DataAccessException {
        String sql = "INSERT INTO " + TBL + " (name, address, des) VALUES (?,?,?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, clinic.getName());
            ps.setString(2, clinic.getAddress());
            ps.setString(3, clinic.getDes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    clinic.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("ClinicDAO.insert failed", e) {};
        }
    }

    private static Clinic map(ResultSet rs) throws SQLException {
        return new Clinic(rs.getInt("id"), rs.getString("name"), rs.getString("address"), rs.getString("des"));
    }
}
