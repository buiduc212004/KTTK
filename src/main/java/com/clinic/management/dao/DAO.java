package com.clinic.management.dao;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class DAO {

    private final DataSource dataSource;

    protected DAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    protected DataSource getDataSource() {
        return dataSource;
    }
}
