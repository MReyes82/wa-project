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
    /**
     * Lazily initialized pool to avoid creating connections during class loading.
     */
    private static volatile HikariDataSource dataSource;
    /**
     * Indirection layer to allow tests to provide a mock connection source.
     */
    private static ConnectionProvider connectionProvider = DatabaseUtil::getPooledConnection;

    @FunctionalInterface
    interface ConnectionProvider
    {
        Connection get() throws SQLException;
    }

    /**
     * Get a connection from the pool
     * @return A Connection object from the HikariCP pool
     * @throws SQLException if connection cannot be established
     */
    public static Connection getConnection() throws SQLException
    {
        return connectionProvider.get();
    }

    /**
     * Test hook to override the connection source for DAO tests.
     * @param provider custom provider for connections
     */
    static void setConnectionProvider(ConnectionProvider provider)
    {
        connectionProvider = provider != null ? provider : DatabaseUtil::getPooledConnection;
    }

    /**
     * Restore the default pooled connection provider after tests.
     */
    static void resetConnectionProvider()
    {
        connectionProvider = DatabaseUtil::getPooledConnection;
    }

    /**
     * Get a connection from the lazily initialized pool.
     * @return pooled connection
     * @throws SQLException if connection cannot be established
     */
    private static Connection getPooledConnection() throws SQLException
    {
        return getDataSource().getConnection();
    }

    /**
     * Initialize and return the shared Hikari data source.
     * @return shared data source
     */
    private static HikariDataSource getDataSource()
    {
        if (dataSource == null)
        {
            synchronized (DatabaseUtil.class)
            {
                if (dataSource == null)
                {
                    HikariConfig config = new HikariConfig();
                    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
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
            }
        }

        return dataSource;
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
