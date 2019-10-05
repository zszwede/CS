package com.zbychu.common;

public class Constants {
    public final static String INTERVAL = "tail_sleep_interval";

    public final static String FILE_SHORT_FLAG = "f";
    public final static String FILE_LONG_FLAG = "file";

    public final static String CONFIG_SHORT_FLAG = "c";
    public final static String CONFIG_LONG_FLAG = "config";
    public final static String CONFIG_FILE_DEFAULT = "config.properties";

    public final static String CONFIG_THREAD_MAX_PROP = "single_read_threads";

    public final static String DB_USER_PROP = "db_user";
    public final static String DB_PASS_PROP = "db_pass";
    public final static String DB_URL_PROP = "db_address";
    public final static String DB_HEALTHCHECK_INTERVAL_PROP = "db_healthcheck_interval_ms";

    public final static String DB_PREPARED_STATEMENT = "INSERT INTO PUBLIC.RESULTS (ID, DURATION_MS, TYPE, HOST, ALERT) VALUES (?,?,?,?,?)";

    public final static String DB_CREATE_RESULTS_TABLE = "CREATE TABLE IF NOT EXISTS RESULTS (\n" +
            " id VARCHAR(50) PRIMARY KEY,\n" +
            " duration_ms INT,\n" +
            " type VARCHAR(20),\n" +
            " host VARCHAR(20),\n" +
            " alert BOOLEAN DEFAULT FALSE NOT NULL\n" +
            ");";
    public final static String DB_CHECK_RESULTS_TABLE = "SELECT * FROM PUBLIC.INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'RESULTS';";
    public final static String DB_ALERT_DISPLAY = "SELECT * FROM RESULTS WHERE ALERT = TRUE";
    public static final String TAIL_SHORT_FLAG = "t";

    public static final String RESULTS_SHORT_FLAG = "r";
    public static final String RESULTS_LONG_FLAG = "results";
}