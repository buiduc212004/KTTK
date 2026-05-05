package com.clinic.management.dao;

import com.clinic.management.model.BookingReceipt;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class BookingReceiptDAO extends DAO {

    private static final String TBL = "tbl_booking_receipt";

    private final PatientDAO patientDAO;
    private final ServiceDAO serviceDAO;
    private final ScheduleCustomerBookingDAO scheduleCustomerBookingDAO;

    public BookingReceiptDAO(
            DataSource dataSource,
            PatientDAO patientDAO,
            ServiceDAO serviceDAO,
            ScheduleCustomerBookingDAO scheduleCustomerBookingDAO) {
        super(dataSource);
        this.patientDAO = patientDAO;
        this.serviceDAO = serviceDAO;
        this.scheduleCustomerBookingDAO = scheduleCustomerBookingDAO;
    }

    public List<BookingReceipt> findByUserIdHydrated(int userId) throws DataAccessException {
        String sql = "SELECT id, booking_date, sell_off, total, note, patient_id, user_id, "
                + " schedule_customer_booking_id, service_id FROM " + TBL
                + " WHERE user_id = ? ORDER BY id DESC";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<BookingReceipt> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(hydrateRow(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("BookingReceiptDAO.findByUserIdHydrated failed", e) {};
        }
    }

    public void insert(BookingReceipt r) throws DataAccessException {
        String sql = "INSERT INTO " + TBL + " (booking_date, sell_off, total, note, patient_id, user_id, "
                + " schedule_customer_booking_id, service_id) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, r.getBookingDate());
            ps.setDouble(2, r.getSellOff());
            ps.setDouble(3, r.getTotal());
            ps.setString(4, r.getNote());
            ps.setInt(5, r.getPatient().getId());
            ps.setInt(6, r.getUser().getId());
            ps.setInt(7, r.getScheduleCustomerBooking().getId());
            ps.setInt(8, r.getService().getId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    r.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("BookingReceiptDAO.insert failed", e) {};
        }
    }

    private BookingReceipt hydrateRow(ResultSet rs) throws SQLException {
        BookingReceipt br = new BookingReceipt();
        br.setId(rs.getInt("id"));
        br.setBookingDate(rs.getObject("booking_date", LocalDate.class));
        br.setSellOff(rs.getDouble("sell_off"));
        br.setTotal(rs.getDouble("total"));
        br.setNote(rs.getString("note"));
        int pid = rs.getInt("patient_id");
        if (!rs.wasNull()) {
            br.setPatient(patientDAO.findById(pid).orElse(null));
        }
        int scbId = rs.getInt("schedule_customer_booking_id");
        if (!rs.wasNull()) {
            br.setScheduleCustomerBooking(scheduleCustomerBookingDAO.findHydrated(scbId).orElse(null));
        }
        int sid = rs.getInt("service_id");
        if (!rs.wasNull()) {
            br.setService(serviceDAO.getById(sid).orElse(null));
        }
        br.setUser(null);
        return br;
    }
}
