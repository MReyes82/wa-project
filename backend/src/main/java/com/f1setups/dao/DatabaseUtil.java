package com.f1setups.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
/*
    * Class to manage the database connection pools
    * Provides centralized connection management for all Dao Classes.
 */
public class DatabaseUtil
{
    private static final HikariDataSource dataSource;

    // Static initializer block to ensure one set up across the runtime
    static
    {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/f1setups");
        config.setUsername("root");
        config.setPassword("password");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        config.setAutoCommit(true);

        dataSource = new HikariDataSource(config);
    }

    /**
     * Get a connection from the pool
     * @return A Connection object from the HikariCP pool
     * @throws SQLException if connection cannot be established
     */
    public static Connection getConnection() throws SQLException
    {
        return dataSource.getConnection();
    }

    /**
     * Closes the connection pool (call on application shutdown)
     */
    public static void closePool()
    {
        if (dataSource != null && !dataSource.isClosed())
        {
            dataSource.close();
        }
    }
}
