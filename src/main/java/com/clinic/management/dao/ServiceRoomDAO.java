package com.clinic.management.dao;

import com.clinic.management.model.Clinic;
import com.clinic.management.model.ServiceRoom;
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
public class ServiceRoomDAO extends DAO {

    private static final String TBL = "tbl_service_room";

    public ServiceRoomDAO(DataSource dataSource) {
        super(dataSource);
    }

    public List<ServiceRoom> findAll() throws DataAccessException {
        String sql = "SELECT r.id, r.name, r.type, r.des, r.clinic_id, "
                + " c.id AS cid, c.name AS cname, c.address AS caddr, c.des AS cdes "
                + " FROM " + TBL + " r "
                + " LEFT JOIN tbl_clinic c ON r.clinic_id = c.id "
                + " ORDER BY r.id ASC";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<ServiceRoom> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapJoin(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("ServiceRoomDAO.findAll failed", e) {};
        }
    }

    public Optional<ServiceRoom> findById(int id) throws DataAccessException {
        String sql = "SELECT r.id, r.name, r.type, r.des, r.clinic_id, "
                + " c.id AS cid, c.name AS cname, c.address AS caddr, c.des AS cdes "
                + " FROM " + TBL + " r "
                + " LEFT JOIN tbl_clinic c ON r.clinic_id = c.id "
                + " WHERE r.id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapJoin(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("ServiceRoomDAO.findById failed", e) {};
        }
    }

    public void insert(ServiceRoom room) throws DataAccessException {
        String sql = "INSERT INTO " + TBL + " (name, type, des, clinic_id) VALUES (?,?,?,?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, room.getName());
            ps.setString(2, room.getType());
            ps.setString(3, room.getDes());
            if (room.getClinic() != null) {
                ps.setInt(4, room.getClinic().getId());
            } else {
                ps.setObject(4, null);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    room.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("ServiceRoomDAO.insert failed", e) {};
        }
    }

    private ServiceRoom mapJoin(ResultSet rs) throws SQLException {
        ServiceRoom r = new ServiceRoom();
        r.setId(rs.getInt("id"));
        r.setName(rs.getString("name"));
        r.setType(rs.getString("type"));
        r.setDes(rs.getString("des"));
        int cid = rs.getInt("clinic_id");
        if (!rs.wasNull() && rs.getObject("cid") != null) {
            r.setClinic(new Clinic(rs.getInt("cid"), rs.getString("cname"), rs.getString("caddr"), rs.getString("cdes")));
        }
        return r;
    }
}
