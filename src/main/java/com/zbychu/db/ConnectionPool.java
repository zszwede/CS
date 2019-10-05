package com.zbychu.db;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zbychu.common.Config;
import com.zbychu.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionPool {
    private final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);
    private AtomicBoolean dbReady = new AtomicBoolean(false);
    private AtomicBoolean poisonPill = new AtomicBoolean(false);
    private static ConnectionPool INSTANCE;
    private HikariDataSource ds;
    private LinkedBlockingQueue<String> q = new LinkedBlockingQueue<>();

    private ConnectionPool() {
        this.ds = getDataSource();
    }

    public boolean isDbReady() {
        return dbReady.get();
    }

    public void setDbReady(boolean ready) {
        dbReady.set(ready);
    }

    public void setPoison(){
        poisonPill.set(true);

    }

    public static synchronized ConnectionPool getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new ConnectionPool();
        }
        return INSTANCE;
    }

    public Connection getConnection(){
        try {
            return ds.getConnection();
        } catch (SQLException e) {
            logger.error("Unable to get connection from pool ", e);
        }
        return null;
    }

    private HikariConfig hikariConfig() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(Config.getString(Constants.DB_URL_PROP));
        config.setUsername(Config.getString(Constants.DB_USER_PROP));
        config.setPassword(Config.getString(Constants.DB_PASS_PROP));
        HealthCheckRegistry hcr = new HealthCheckRegistry();
        config.setHealthCheckRegistry(hcr);
        config.addHealthCheckProperty("connectivityCheckTimeoutMs", Config.getString(Constants.DB_HEALTHCHECK_INTERVAL_PROP));
        return config;
    }

    public HikariDataSource getDataSource(){
        HikariConfig config = hikariConfig();
        HikariDataSource hds = new HikariDataSource(config);
        hds.setMaximumPoolSize(Config.getInt(Constants.DB_POOL_SIZE_PROP));
        Executors.newSingleThreadExecutor().execute(
                new DatabaseHealthCheck((HealthCheckRegistry)config.getHealthCheckRegistry(), poisonPill)
        );
        return hds;
    }

    public LinkedBlockingQueue<String> getQ() {
        return q;
    }
}