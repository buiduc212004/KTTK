package com.clinic.management.dao;

import com.clinic.management.model.ScheduleRegister;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ScheduleRegisterDAO extends DAO {

    private static final String TBL = "tbl_schedule_register";

    public ScheduleRegisterDAO(DataSource dataSource) {
        super(dataSource);
    }

    public List<ScheduleRegister> findByDoctorIdAndDate(int doctorId, LocalDate date) throws DataAccessException {
        String sql = "SELECT id, date_schedule_register, time_schedule_register, clinic_id, doctor_id FROM " + TBL
                + " WHERE doctor_id = ? AND date_schedule_register = ? ORDER BY time_schedule_register ASC";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setObject(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                List<ScheduleRegister> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapBare(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("ScheduleRegisterDAO.findByDoctorIdAndDate failed", e) {};
        }
    }

    public List<LocalDate> findAvailableDatesByDoctorId(int doctorId) throws DataAccessException {
        String sql = "SELECT DISTINCT date_schedule_register FROM " + TBL
                + " WHERE doctor_id = ? AND date_schedule_register >= CURRENT_DATE "
                + " ORDER BY date_schedule_register ASC";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                List<LocalDate> dates = new ArrayList<>();
                while (rs.next()) {
                    dates.add(rs.getObject("date_schedule_register", LocalDate.class));
                }
                return dates;
            }
        } catch (SQLException e) {
            throw new DataAccessException("ScheduleRegisterDAO.findAvailableDatesByDoctorId failed", e) {};
        }
    }

    public void insert(ScheduleRegister sr) throws DataAccessException {
        String sql = "INSERT INTO " + TBL + " (date_schedule_register, time_schedule_register, clinic_id, doctor_id) VALUES (?,?,?,?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, sr.getDateScheduleRegister());
            ps.setObject(2, sr.getTimeScheduleRegister());
            if (sr.getClinic() != null) {
                ps.setInt(3, sr.getClinic().getId());
            } else {
                ps.setObject(3, null);
            }
            if (sr.getDoctor() != null) {
                ps.setInt(4, sr.getDoctor().getId());
            } else {
                ps.setObject(4, null);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    sr.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("ScheduleRegisterDAO.insert failed", e) {};
        }
    }

    private ScheduleRegister mapBare(ResultSet rs) throws SQLException {
        LocalTime t = rs.getObject("time_schedule_register", LocalTime.class);
        return new ScheduleRegister(
                rs.getInt("id"),
                rs.getObject("date_schedule_register", LocalDate.class),
                t,
                null,
                null
        );
    }
}
