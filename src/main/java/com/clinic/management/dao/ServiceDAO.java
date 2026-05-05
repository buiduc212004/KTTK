package com.clinic.management.dao;

import com.clinic.management.model.Clinic;
import com.clinic.management.model.GeneralService;
import com.clinic.management.model.Service;
import com.clinic.management.model.TestService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
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

/**
 * DAO truy cập dữ liệu dịch vụ (JOINED inheritance) bằng JDBC — vai trò tương đương {@code ServiceDAO} trong thiết kế.
 * Đặt tên bảng/cột khớp Spring Boot Hibernate (physical naming camel_case → snake_case).
 */
@Repository
public class ServiceDAO extends DAO {

    private static final String TBL_SERVICE = "tbl_service";
    private static final String TBL_GENERAL = "tbl_general_service";
    private static final String TBL_TEST = "tbl_test_service";

    private final ClinicDAO clinicDAO;

    public ServiceDAO(DataSource dataSource, ClinicDAO clinicDAO) {
        super(dataSource);
        this.clinicDAO = clinicDAO;
    }

    /** Theo UML: lấy tất cả (không phân trang). */
    public List<Service> getAll() throws DataAccessException {
        String sql = selectBaseProjection() + " ORDER BY sid ASC ";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                List<Service> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new DataAccessException("ServiceDAO.getAll failed", e) {};
        }
    }

    /** Theo UML: tìm theo tên. */
    public List<Service> searchByName(String name) throws DataAccessException {
        return findPaged(name, "", Pageable.unpaged()).getContent();
    }

    /** Theo UML: lấy theo khóa. */
    public Optional<Service> getById(int id) throws DataAccessException {
        String sql = selectBaseProjection(" WHERE 1=1 AND s.id = ? ");
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("ServiceDAO.getById failed", e) {};
        }
    }

    /** Theo UML: thêm; chấp nhận {@link GeneralService} hoặc {@link TestService}. */
    public void add(Service s) throws DataAccessException {
        if (s instanceof GeneralService gs) {
            insertGeneral(gs);
            return;
        }
        if (s instanceof TestService ts) {
            insertTest(ts);
            return;
        }
        throw new IllegalArgumentException("Kiểu dịch vụ không được hỗ trợ cho add(): " + s.getClass());
    }

    /** Theo UML: cập nhật. */
    public void update(Service s) throws DataAccessException {
        if (s instanceof GeneralService gs) {
            updateGeneral(gs);
            return;
        }
        if (s instanceof TestService ts) {
            updateTest(ts);
            return;
        }
        throw new IllegalArgumentException("Kiểu dịch vụ không được hỗ trợ cho update(): " + s.getClass());
    }

    /** Theo UML: xóa theo id. */
    public boolean delete(int id) throws DataAccessException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                deleteChildRows(c, id);
                int n;
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + TBL_SERVICE + " WHERE id = ?")) {
                    ps.setInt(1, id);
                    n = ps.executeUpdate();
                }
                c.commit();
                return n > 0;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("ServiceDAO.delete failed", e) {};
        }
    }

    public boolean existsById(int id) throws DataAccessException {
        String sql = "SELECT 1 FROM " + TBL_SERVICE + " WHERE id = ? LIMIT 1";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("ServiceDAO.existsById failed", e) {};
        }
    }

    public long countAllServices() throws DataAccessException {
        return scalarLongSafe("SELECT COUNT(*) FROM " + TBL_SERVICE);
    }

    public long countGeneralServices() throws DataAccessException {
        return scalarLongSafe("SELECT COUNT(*) FROM " + TBL_GENERAL);
    }

    public long countTestServices() throws DataAccessException {
        return scalarLongSafe("SELECT COUNT(*) FROM " + TBL_TEST);
    }

    public Page<Service> findPaged(@Nullable String keyword, @Nullable String typeFilter, Pageable pageable)
            throws DataAccessException {

        boolean kwBlank = keyword == null || keyword.isBlank();
        String normalizedType = (typeFilter != null && !typeFilter.isBlank()) ? typeFilter : "";

        try (Connection c = getConnection()) {
            List<Object> countParams = paramsForFilters(kwBlank ? null : keyword.trim(), normalizedType);
            String where = buildWhereClause(kwBlank, normalizedType);
            String countSql = "SELECT COUNT(*) FROM " + TBL_SERVICE + " s WHERE 1=1 " + where;

            long total;
            try (PreparedStatement ps = c.prepareStatement(countSql)) {
                bindParams(ps, countParams, 1);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }

            String dataSql = selectBaseProjection(" WHERE 1=1 " + where) + " ORDER BY sid ASC ";
            if (!pageable.isPaged()) {
                try (PreparedStatement ps = c.prepareStatement(dataSql)) {
                    bindBaseFilters(ps, kwBlank ? null : keyword.trim(), normalizedType, 1);
                    try (ResultSet rs = ps.executeQuery()) {
                        List<Service> rows = loadAll(rs);
                        return new PageImpl<>(rows, pageable, total);
                    }
                }
            }

            String pagedSql = dataSql + " LIMIT ? OFFSET ? ";
            List<Object> dataParams = new ArrayList<>(paramsForFilters(kwBlank ? null : keyword.trim(), normalizedType));
            dataParams.add(pageable.getPageSize());
            dataParams.add((int) pageable.getOffset());

            try (PreparedStatement ps = c.prepareStatement(pagedSql)) {
                bindParams(ps, dataParams, 1);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Service> rows = loadAll(rs);
                    return new PageImpl<>(rows, pageable, total);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("ServiceDAO.findPaged failed", e) {};
        }
    }

    /* ======================== Private helpers ======================== */

    /**
     * Bản ghi cũ (Hibernate): {@code service_type} có thể NULL nhưng {@code dtype} vẫn là GENERAL/TEST.
     * Dùng chung cho SELECT (alias {@code stype}) và điều kiện WHERE lọc loại.
     */
    private static final String SERVICE_KIND_SQL =
            "COALESCE(NULLIF(TRIM(s.service_type), ''), NULLIF(TRIM(CAST(s.dtype AS TEXT)), ''))";

    private static String buildWhereClause(boolean skipKeyword, String normalizedType) {
        StringBuilder w = new StringBuilder();
        if (!skipKeyword) {
            w.append(" AND LOWER(s.name) LIKE LOWER(?) ");
        }
        if (!normalizedType.isEmpty()) {
            w.append(" AND (").append(SERVICE_KIND_SQL).append(") = ? ");
        }
        return w.toString();
    }

    private static List<Object> paramsForFilters(@Nullable String keywordTrimmed, String normalizedType) {
        List<Object> p = new ArrayList<>(2);
        if (keywordTrimmed != null) {
            p.add("%" + keywordTrimmed + "%");
        }
        if (!normalizedType.isEmpty()) {
            p.add(normalizedType);
        }
        return p;
    }

    private static int bindParams(PreparedStatement ps, List<Object> params, int start) throws SQLException {
        int i = start;
        for (Object o : params) {
            ps.setObject(i++, o);
        }
        return i;
    }

    private String selectBaseProjection() {
        return selectBaseProjection(" WHERE 1=1 ");
    }

    /**
     * @param trailingWhereFromOne ví dụ: {@code " WHERE 1=1 "} hoặc {@code " WHERE 1=1 AND s.id = ? "}
     *        đã chứa từ khóa {@code WHERE}.
     */
    private String selectBaseProjection(String trailingWhereFromOne) {
        return "SELECT "
                + " s.id AS sid, s.dtype AS sdtype, s.name AS sname, "
                + " (" + SERVICE_KIND_SQL + ") AS stype,"
                + " s.des AS sdes, s.price AS sprice, s.clinic_id AS sclinic_id,"
                + " g.id AS gid, g.is_active AS gactive,"
                + " t.id AS tid, t.preparation_instructions AS tprep,"
                + " t.test_method AS tmethod "
                + " FROM " + TBL_SERVICE + " s "
                + " LEFT JOIN " + TBL_GENERAL + " g ON s.id = g.id "
                + " LEFT JOIN " + TBL_TEST + " t ON s.id = t.id "
                + trailingWhereFromOne;
    }

    private int bindBaseFilters(
            PreparedStatement ps, @Nullable String keywordTrimmed, @Nullable String normalizedType, int start)
            throws SQLException {
        String nt = normalizedType == null ? "" : normalizedType;
        int i = start;
        boolean skipKeyword = keywordTrimmed == null;
        if (!skipKeyword) {
            ps.setString(i++, "%" + keywordTrimmed + "%");
        }
        if (!nt.isEmpty()) {
            ps.setString(i++, nt);
        }
        return i;
    }

    private boolean isGeneral(ResultSet rs) throws SQLException {
        String dt = rs.getString("sdtype");
        return dt != null && dt.equalsIgnoreCase("GENERAL");
    }

    private Service mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("sid");
        Object go = rs.getObject("gid");
        Object toe = rs.getObject("tid");
        if (go != null) {
            GeneralService gs = new GeneralService();
            fillBase(gs, rs, id);
            gs.setType("GENERAL");
            gs.setActive(rs.getObject("gactive") != null && rs.getBoolean("gactive"));
            return gs;
        }
        if (toe != null) {
            TestService ts = new TestService();
            fillBase(ts, rs, id);
            ts.setType("TEST");
            ts.setPreparationInstructions(rs.getString("tprep"));
            ts.setMethod(rs.getString("tmethod"));
            return ts;
        }
        /* Fallback dữ liệu lạ */
        boolean general = isGeneral(rs) || rs.getObject("gactive") != null;
        if (general) {
            GeneralService gs = new GeneralService();
            fillBase(gs, rs, id);
            gs.setType("GENERAL");
            gs.setActive(rs.getObject("gactive") != null && rs.getBoolean("gactive"));
            return gs;
        }
        TestService ts = new TestService();
        fillBase(ts, rs, id);
        ts.setType("TEST");
        ts.setPreparationInstructions(rs.getString("tprep"));
        ts.setMethod(rs.getString("tmethod"));
        return ts;
    }

    private void fillBase(Service s, ResultSet rs, int id) throws SQLException {
        s.setId(id);
        s.setName(rs.getString("sname"));
        s.setType(normalizeUiServiceKind(rs));
        s.setDes(rs.getString("sdes"));
        Object pr = rs.getObject("sprice");
        s.setPrice(pr == null ? 0d : ((Number) pr).doubleValue());
        attachClinic(s, rs);
    }

    /** Chuẩn hóa GENERAL/TEST cho Thymeleaf ({@code svc.type == 'GENERAL'}). */
    private static String normalizeUiServiceKind(ResultSet rs) throws SQLException {
        String raw = rs.getString("stype");
        if (raw != null) {
            raw = raw.trim();
        }
        if (raw != null && !raw.isBlank()) {
            if (raw.equalsIgnoreCase("GENERAL")) {
                return "GENERAL";
            }
            if (raw.equalsIgnoreCase("TEST")) {
                return "TEST";
            }
            return raw;
        }
        String dt = rs.getString("sdtype");
        if (dt != null && !dt.isBlank()) {
            if (dt.trim().equalsIgnoreCase("GENERAL")) {
                return "GENERAL";
            }
            if (dt.trim().equalsIgnoreCase("TEST")) {
                return "TEST";
            }
        }
        if (rs.getObject("gactive") != null) {
            return "GENERAL";
        }
        if (rs.getString("tprep") != null || rs.getString("tmethod") != null) {
            return "TEST";
        }
        return raw;
    }

    private void attachClinic(Service s, ResultSet rs) throws SQLException {
        int cid = rs.getInt("sclinic_id");
        if (!rs.wasNull()) {
            Optional<Clinic> c = clinicDAO.findById(cid);
            s.setClinic(c.orElse(null));
        }
    }

    private List<Service> loadAll(ResultSet rs) throws SQLException {
        List<Service> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    private long scalarLong(String sql) throws SQLException {
        try (Connection c = getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private long scalarLongSafe(String sql) throws DataAccessException {
        try {
            return scalarLong(sql);
        } catch (SQLException e) {
            throw new DataAccessException("ServiceDAO.scalarLong failed", e) {};
        }
    }

    private void insertGeneral(GeneralService gs) throws DataAccessException {
        // PostgreSQL: không dùng getGeneratedKeys() với JOINED discriminator — cột dtype (varchar) có thể là cột đầu, gây getInt("1") → "GENERAL".
        final String sqlRoot = "INSERT INTO " + TBL_SERVICE
                + " (name, service_type, des, price, dtype, clinic_id) VALUES (?,?,?,?,?,?) RETURNING id";
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sqlRoot)) {
                ps.setString(1, gs.getName());
                ps.setString(2, gs.getType());
                ps.setString(3, gs.getDes());
                ps.setDouble(4, gs.getPrice());
                ps.setString(5, "GENERAL");
                if (gs.getClinic() == null) {
                    ps.setObject(6, null);
                } else {
                    ps.setInt(6, gs.getClinic().getId());
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Không có id sau INSERT tbl_service.");
                    }
                    gs.setId(rs.getInt("id"));
                }
            }
            try (PreparedStatement chi = c.prepareStatement(
                    "INSERT INTO " + TBL_GENERAL + " (id, is_active) VALUES (?,?)")) {
                chi.setInt(1, gs.getId());
                chi.setBoolean(2, gs.isActive());
                chi.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            throw new DataAccessException("ServiceDAO.insertGeneral failed", e) {};
        }
    }

    private void insertTest(TestService ts) throws DataAccessException {
        final String sqlRoot = "INSERT INTO " + TBL_SERVICE
                + " (name, service_type, des, price, dtype, clinic_id) VALUES (?,?,?,?,?,?) RETURNING id";
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sqlRoot)) {
                ps.setString(1, ts.getName());
                ps.setString(2, ts.getType());
                ps.setString(3, ts.getDes());
                ps.setDouble(4, ts.getPrice());
                ps.setString(5, "TEST");
                if (ts.getClinic() == null) {
                    ps.setObject(6, null);
                } else {
                    ps.setInt(6, ts.getClinic().getId());
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Không có id sau INSERT tbl_service.");
                    }
                    ts.setId(rs.getInt("id"));
                }
            }
            try (PreparedStatement chi = c.prepareStatement(
                    "INSERT INTO " + TBL_TEST + " (id, preparation_instructions, test_method) VALUES (?,?,?)")) {
                chi.setInt(1, ts.getId());
                chi.setString(2, ts.getPreparationInstructions());
                chi.setString(3, ts.getMethod());
                chi.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            throw new DataAccessException("ServiceDAO.insertTest failed", e) {};
        }
    }

    private void updateGeneral(GeneralService gs) throws DataAccessException {
        final String sqlRoot = "UPDATE " + TBL_SERVICE
                + " SET name = ?, service_type = ?, des = ?, price = ?, clinic_id = ? WHERE id = ?";
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sqlRoot)) {
                ps.setString(1, gs.getName());
                ps.setString(2, gs.getType());
                ps.setString(3, gs.getDes());
                ps.setDouble(4, gs.getPrice());
                if (gs.getClinic() == null) {
                    ps.setObject(5, null);
                } else {
                    ps.setInt(5, gs.getClinic().getId());
                }
                ps.setInt(6, gs.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement chi = c.prepareStatement(
                    "UPDATE " + TBL_GENERAL + " SET is_active = ? WHERE id = ?")) {
                chi.setBoolean(1, gs.isActive());
                chi.setInt(2, gs.getId());
                chi.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            throw new DataAccessException("ServiceDAO.updateGeneral failed", e) {};
        }
    }

    private void updateTest(TestService ts) throws DataAccessException {
        final String sqlRoot = "UPDATE " + TBL_SERVICE
                + " SET name = ?, service_type = ?, des = ?, price = ?, clinic_id = ? WHERE id = ?";
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sqlRoot)) {
                ps.setString(1, ts.getName());
                ps.setString(2, ts.getType());
                ps.setString(3, ts.getDes());
                ps.setDouble(4, ts.getPrice());
                if (ts.getClinic() == null) {
                    ps.setObject(5, null);
                } else {
                    ps.setInt(5, ts.getClinic().getId());
                }
                ps.setInt(6, ts.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement chi = c.prepareStatement(
                    "UPDATE " + TBL_TEST
                            + " SET preparation_instructions = ?, test_method = ? WHERE id = ?")) {
                chi.setString(1, ts.getPreparationInstructions());
                chi.setString(2, ts.getMethod());
                chi.setInt(3, ts.getId());
                chi.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            throw new DataAccessException("ServiceDAO.updateTest failed", e) {};
        }
    }

    private void deleteChildRows(Connection c, int id) throws SQLException {
        try (PreparedStatement pg = c.prepareStatement("DELETE FROM " + TBL_GENERAL + " WHERE id = ?")) {
            pg.setInt(1, id);
            pg.executeUpdate();
        }
        try (PreparedStatement pt = c.prepareStatement("DELETE FROM " + TBL_TEST + " WHERE id = ?")) {
            pt.setInt(1, id);
            pt.executeUpdate();
        }
    }
}
