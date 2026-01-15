package io.github.anomalyforlife.hopperFilter.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class HikariConnectionProvider implements ConnectionProvider, AutoCloseable {

    private final HikariDataSource dataSource;

    public HikariConnectionProvider(HikariConfig config) {
        this.dataSource = new HikariDataSource(Objects.requireNonNull(config, "config"));
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
