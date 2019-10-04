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
    public final static String DB_BATCH_SIZE_PROP = "db_batch_size";
    public final static String DB_HEALTHCHECK_INTERVAL_PROP = "db_healthcheck_interval_ms";

    public final static String DB_PREPARED_STATEMENT = "INSERT INTO PUBLIC.HELPER_VIEW (ID, TYPE, HOST, TIMESTAMP, STATE) VALUES (?,?,?,?,?)";

    public final static String DB_CREATE_RESULTS_TABLE = "CREATE TABLE IF NOT EXISTS RESULTS (\n" +
            " id VARCHAR(50) PRIMARY KEY,\n" +
            " duration_ms INT,\n" +
            " type VARCHAR(20),\n" +
            " host VARCHAR(20),\n" +
            " alert BOOLEAN DEFAULT FALSE NOT NULL\n" +
            ");";
    public final static String DB_CHECK_RESULTS_TABLE = "SELECT * FROM PUBLIC.INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'RESULTS';";

    public final static String DB_CREATE_TIMES_TABLE = "CREATE TABLE IF NOT EXISTS TIMES (\n" +
            " id VARCHAR(50) PRIMARY KEY,\n" +
            " state VARCHAR(50),\n" +
            " timestamp BIGINT\n" +
            ");";
    public final static String DB_CHECK_TIMES_TABLE = "SELECT * FROM PUBLIC.INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'TIMES';";

    public final static String DB_CREATE_COMBINED_VIEW = "CREATE VIEW IF NOT EXISTS HELPER_VIEW AS\n" +
            "SELECT RESULTS.id, RESULTS.type, RESULTS.host, TIMES.timestamp, TIMES.state\n" +
            "FROM RESULTS,TIMES\n" +
            "WHERE RESULTS.id = TIMES.id;";
    public final static String DB_CHECK_COMBINED_VIEW = "SELECT * FROM PUBLIC.INFORMATION_SCHEMA.VIEWS WHERE TABLE_NAME = 'HELPER_VIEW';";

    public final static String DB_DROP_PROCEDURE = "DROP PROCEDURE IF EXISTS PROCESS_UPDATE;";

    public final static String DB_CREATE_PROCEDURE = "CREATE PROCEDURE PROCESS_UPDATE(IN N_id VARCHAR(50), IN N_type VARCHAR(50), IN N_host VARCHAR(50), IN N_timestamp BIGINT, IN N_state VARCHAR(50))\n" +
            "    MODIFIES SQL DATA\n" +
            "BEGIN ATOMIC\n" +
            "DECLARE EXIST_ID VARCHAR(50);\n" +
            "DECLARE EXIST_TIME BIGINT;\n" +
            "DECLARE EXIST_STATE VARCHAR(50);\n" +
            "DECLARE DURATION INT;\n" +
            "DECLARE EXISTING VARCHAR(50);\n" +
            "DECLARE ALERT BOOLEAN;\n" +
            "SELECT id INTO EXISTING FROM RESULTS WHERE id = N_id;\n" +
            "IF EXISTING IS NULL THEN\n" +
            "SELECT id, state, timestamp INTO EXIST_ID, EXIST_STATE, EXIST_TIME FROM TIMES WHERE id = N_id;\n" +
            "IF EXIST_ID IS NULL THEN\n" +
            "    INSERT INTO TIMES (id,state,timestamp) VALUES (N_id, N_state, N_timestamp);\n" +
            "ELSE\n" +
            "    IF EXIST_STATE <> N_state THEN\n" +
            "        SET DURATION = ABS(EXIST_TIME - N_timestamp);\n" +
            "        SET ALERT = DURATION > 4;\n" +
            "        INSERT INTO RESULTS (id, duration_ms, type, host, alert) VALUES (N_id, DURATION, N_type, N_host, ALERT);\n" +
            "        DELETE FROM TIMES WHERE id = N_id;\n" +
            "    END IF;\n" +
            "END IF;\n" +
            "END IF;\n" +
            "END;";
    public final static String DB_CHECK_PROCEDURE = "SELECT * FROM PUBLIC.INFORMATION_SCHEMA.ROUTINES WHERE ROUTINE_NAME = 'PROCESS_UPDATE';";

    public final static String DB_DROP_TRIGGER = "DROP TRIGGER IF EXISTS HELPER_VIEW_TRIGGER;";
    public final static String DB_CREATE_TRIGGER = "CREATE TRIGGER HELPER_VIEW_TRIGGER INSTEAD OF INSERT ON HELPER_VIEW\n" +
            "    REFERENCING NEW ROW AS NEW\n" +
            "    FOR EACH ROW\n" +
            "BEGIN ATOMIC\n" +
            "CALL PROCESS_UPDATE(NEW.id, NEW.type, NEW.host, NEW.timestamp, NEW.state);\n" +
            "END;";
    public final static String DB_CHECK_TRIGGER = "SELECT * FROM PUBLIC.INFORMATION_SCHEMA.TRIGGERS WHERE TRIGGER_NAME = 'HELPER_VIEW_TRIGGER';";
    public final static String DB_ALERT_DISPLAY = "SELECT * FROM RESULTS WHERE ALERT = TRUE";
    public static final String TAIL_SHORT_FLAG = "t";
    public static final String TAIL_LONG_FLAG = "tail";

    public static final String RESULTS_SHORT_FLAG = "r";
    public static final String RESULTS_LONG_FLAG = "results";
    public static final String START_SHORT_FLAG = "s";
    public static final String START_LONG_FLAG = "start";
}