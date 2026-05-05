package com.clinic.management.dao;

import com.clinic.management.model.Clinic;
import com.clinic.management.model.Doctor;
import com.clinic.management.model.ScheduleCustomerBooking;
import com.clinic.management.model.ServiceRoom;
import com.clinic.management.model.Specialization;
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
import java.util.Optional;

@Repository
public class ScheduleCustomerBookingDAO extends DAO {

    private static final String TBL = "tbl_schedule_customer_booking";

    public ScheduleCustomerBookingDAO(DataSource dataSource) {
        super(dataSource);
    }

    public List<ScheduleCustomerBooking> findByDoctorIdAndDateSchedule(int doctorId, LocalDate date)
            throws DataAccessException {
        String sql = "SELECT id, date_schedule, time_schedule, is_state, service_room_id, doctor_id FROM " + TBL
                + " WHERE doctor_id = ? AND date_schedule = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setObject(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                List<ScheduleCustomerBooking> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapBare(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("ScheduleCustomerBookingDAO.findByDoctorIdAndDateSchedule failed", e) {};
        }
    }

    public boolean existsByDoctorIdAndDateScheduleAndTimeSchedule(int doctorId, LocalDate date, LocalTime time)
            throws DataAccessException {
        String sql = "SELECT 1 FROM " + TBL + " WHERE doctor_id = ? AND date_schedule = ? AND time_schedule = ? LIMIT 1";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setObject(2, date);
            ps.setObject(3, time);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("ScheduleCustomerBookingDAO.exists failed", e) {};
        }
    }

    public void insert(ScheduleCustomerBooking booking) throws DataAccessException {
        String sql = "INSERT INTO " + TBL + " (date_schedule, time_schedule, is_state, service_room_id, doctor_id) "
                + " VALUES (?,?,?,?,?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, booking.getDateSchedule());
            ps.setObject(2, booking.getTimeSchedule());
            ps.setString(3, booking.getIsState());
            if (booking.getServiceRoom() != null) {
                ps.setInt(4, booking.getServiceRoom().getId());
            } else {
                ps.setObject(4, null);
            }
            if (booking.getDoctor() != null) {
                ps.setInt(5, booking.getDoctor().getId());
            } else {
                ps.setObject(5, null);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    booking.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("ScheduleCustomerBookingDAO.insert failed", e) {};
        }
    }

    public Optional<ScheduleCustomerBooking> findHydrated(int id) throws DataAccessException {
        String sql = "SELECT scb.id, scb.date_schedule, scb.time_schedule, scb.is_state, "
                + " scb.service_room_id, scb.doctor_id, "
                + " d.name AS dname, d.major AS dmajor, d.phone_number AS dphone, d.email AS demail, d.specialization_id AS dspec_id, "
                + " s.id AS sid, s.name AS sname, s.des AS sdes, "
                + " r.id AS rid, r.name AS rname, r.type AS rtype, r.des AS rdes, r.clinic_id AS rclinic_id, "
                + " c.id AS cid, c.name AS cname, c.address AS caddr, c.des AS cdes "
                + " FROM " + TBL + " scb "
                + " LEFT JOIN tbl_doctor d ON scb.doctor_id = d.id "
                + " LEFT JOIN tbl_specialization s ON d.specialization_id = s.id "
                + " LEFT JOIN tbl_service_room r ON scb.service_room_id = r.id "
                + " LEFT JOIN tbl_clinic c ON r.clinic_id = c.id "
                + " WHERE scb.id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapFull(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("ScheduleCustomerBookingDAO.findHydrated failed", e) {};
        }
    }

    private ScheduleCustomerBooking mapBare(ResultSet rs) throws SQLException {
        ScheduleCustomerBooking b = new ScheduleCustomerBooking();
        b.setId(rs.getInt("id"));
        b.setDateSchedule(rs.getObject("date_schedule", LocalDate.class));
        b.setTimeSchedule(rs.getObject("time_schedule", LocalTime.class));
        b.setIsState(rs.getString("is_state"));
        return b;
    }

    private ScheduleCustomerBooking mapFull(ResultSet rs) throws SQLException {
        ScheduleCustomerBooking b = mapBare(rs);
        int did = rs.getInt("doctor_id");
        if (!rs.wasNull()) {
            Doctor d = new Doctor();
            d.setId(did);
            d.setName(rs.getString("dname"));
            d.setMajor(rs.getString("dmajor"));
            d.setPhoneNumber(rs.getString("dphone"));
            d.setEmail(rs.getString("demail"));
            if (rs.getObject("sid") != null) {
                d.setSpecialization(new Specialization(rs.getInt("sid"), rs.getString("sname"), rs.getString("sdes")));
            }
            b.setDoctor(d);
        }
        if (rs.getObject("rid") != null) {
            ServiceRoom room = new ServiceRoom();
            room.setId(rs.getInt("rid"));
            room.setName(rs.getString("rname"));
            room.setType(rs.getString("rtype"));
            room.setDes(rs.getString("rdes"));
            if (rs.getObject("cid") != null) {
                room.setClinic(new Clinic(rs.getInt("cid"), rs.getString("cname"), rs.getString("caddr"), rs.getString("cdes")));
            }
            b.setServiceRoom(room);
        }
        return b;
    }
}
