package com.clinic.management.dao;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Lớp cơ sở của tầng DAO (theo thiết kế UML có quan hệ với JDBC {@link Connection}).
 * Spring Boot cấu hình {@link DataSource} từ {@code spring.datasource.*}; không giữ một
 * connection cố định suốt vòng đời Bean — lấy connection theo thao tác và đóng ngay sau khi dùng (try-with-resources).
 */
public abstract class DAO {

    private final DataSource dataSource;

    protected DAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Mở kết nối mới; caller bắt buộc đóng trong {@code try-with-resources}.
     */
    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    protected DataSource getDataSource() {
        return dataSource;
    }
}
