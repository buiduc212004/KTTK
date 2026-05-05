package com.clinic.management.dao;

import com.clinic.management.model.Doctor;
import com.clinic.management.model.Specialization;
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
public class DoctorDAO extends DAO {

    private static final String TBL = "tbl_doctor";

    public DoctorDAO(DataSource dataSource) {
        super(dataSource);
    }

    public List<Doctor> findAll() throws DataAccessException {
        String sql = "SELECT d.id, d.name, d.major, d.phone_number, d.email, d.specialization_id, "
                + " s.id AS sid, s.name AS sname, s.des AS sdes "
                + " FROM " + TBL + " d "
                + " LEFT JOIN tbl_specialization s ON d.specialization_id = s.id "
                + " ORDER BY d.id ASC";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Doctor> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapJoin(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("DoctorDAO.findAll failed", e) {};
        }
    }

    public Optional<Doctor> findById(int id) throws DataAccessException {
        String sql = "SELECT d.id, d.name, d.major, d.phone_number, d.email, d.specialization_id, "
                + " s.id AS sid, s.name AS sname, s.des AS sdes "
                + " FROM " + TBL + " d "
                + " LEFT JOIN tbl_specialization s ON d.specialization_id = s.id "
                + " WHERE d.id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapJoin(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("DoctorDAO.findById failed", e) {};
        }
    }

    public void insert(Doctor d) throws DataAccessException {
        String sql = "INSERT INTO " + TBL + " (name, major, phone_number, email, specialization_id) VALUES (?,?,?,?,?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getName());
            ps.setString(2, d.getMajor());
            ps.setString(3, d.getPhoneNumber());
            ps.setString(4, d.getEmail());
            if (d.getSpecialization() != null) {
                ps.setInt(5, d.getSpecialization().getId());
            } else {
                ps.setObject(5, null);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    d.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("DoctorDAO.insert failed", e) {};
        }
    }

    private Doctor mapJoin(ResultSet rs) throws SQLException {
        Doctor d = new Doctor();
        d.setId(rs.getInt("id"));
        d.setName(rs.getString("name"));
        d.setMajor(rs.getString("major"));
        d.setPhoneNumber(rs.getString("phone_number"));
        d.setEmail(rs.getString("email"));
        int specFk = rs.getInt("specialization_id");
        if (!rs.wasNull() && rs.getObject("sid") != null) {
            d.setSpecialization(new Specialization(rs.getInt("sid"), rs.getString("sname"), rs.getString("sdes")));
        }
        return d;
    }
}
