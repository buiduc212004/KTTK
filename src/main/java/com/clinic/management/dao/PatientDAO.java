package com.clinic.management.dao;

import com.clinic.management.model.Patient;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class PatientDAO extends DAO {

    private static final String TBL = "tbl_patient";

    public PatientDAO(DataSource dataSource) {
        super(dataSource);
    }

    public Optional<Patient> findById(int id) throws DataAccessException {
        String sql = "SELECT id, name, gender, date_of_birth, tel_number, email, allergies FROM " + TBL + " WHERE id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("PatientDAO.findById failed", e) {};
        }
    }

    public void insert(Patient p) throws DataAccessException {
        String sql = "INSERT INTO " + TBL + " (name, gender, date_of_birth, tel_number, email, allergies) VALUES (?,?,?,?,?,?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getGender());
            if (p.getDateOfBirth() == null) {
                ps.setObject(3, null);
            } else {
                ps.setObject(3, p.getDateOfBirth());
            }
            ps.setString(4, p.getTelNumber());
            ps.setString(5, p.getEmail());
            ps.setString(6, p.getAllergies());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("PatientDAO.insert failed", e) {};
        }
    }

    private static Patient map(ResultSet rs) throws SQLException {
        return new Patient(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("gender"),
                rs.getObject("date_of_birth", java.time.LocalDate.class),
                rs.getString("tel_number"),
                rs.getString("email"),
                rs.getString("allergies")
        );
    }
}
