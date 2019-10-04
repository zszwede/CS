package com.zbychu.db;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class DatabaseHealthCheck implements Runnable{
    private final Logger logger = LoggerFactory.getLogger(DatabaseHealthCheck.class);
    private HealthCheckRegistry hcr;
    private AtomicBoolean poisonPill;

    public DatabaseHealthCheck(HealthCheckRegistry hcr, AtomicBoolean poisonPill){
        this.hcr = hcr;
        this.poisonPill = poisonPill;
    }

    @Override
    public void run() {
        while (true){
            SortedMap<String, HealthCheck.Result> r = hcr.runHealthChecks();
            r.forEach((key, value) -> {
                logger.debug("Database connection HealthCheck result : " + value.isHealthy());
                ConnectionPool.getInstance().setDbReady(value.isHealthy());
            });
            if(poisonPill.get()){
                logger.info("Poison pill received, exiting ...");
                break;
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                logger.error("Something went wrong ... ", e);
                break;
            }
        }
    }
}