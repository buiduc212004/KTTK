package com.clinic.management.dao;

import com.clinic.management.model.Specialization;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Repository
public class SpecializationDAO extends DAO {

    private static final String TBL = "tbl_specialization";

    public SpecializationDAO(DataSource dataSource) {
        super(dataSource);
    }

    public void insert(Specialization s) throws DataAccessException {
        String sql = "INSERT INTO " + TBL + " (name, des) VALUES (?,?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getDes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    s.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("SpecializationDAO.insert failed", e) {};
        }
    }
}
