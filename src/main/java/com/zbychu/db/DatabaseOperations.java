package com.zbychu.db;

import com.zbychu.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatabaseOperations {
    private final Logger logger = LoggerFactory.getLogger(DatabaseOperations.class);
    public boolean setupObjects() {
        logger.info("Starting database setup");
        String[] actionList = new String[]{
                Constants.DB_CREATE_RESULTS_TABLE,
                Constants.DB_CREATE_TIMES_TABLE,
                Constants.DB_CREATE_COMBINED_VIEW,
                Constants.DB_DROP_TRIGGER,
                Constants.DB_DROP_PROCEDURE,
                Constants.DB_CREATE_PROCEDURE,
                Constants.DB_CREATE_TRIGGER
        };
        String[] checkList = new String[]{
                Constants.DB_CHECK_RESULTS_TABLE,
                Constants.DB_CHECK_TIMES_TABLE,
                Constants.DB_CHECK_COMBINED_VIEW,
                null,
                null,
                Constants.DB_CHECK_PROCEDURE,
                Constants.DB_CHECK_TRIGGER
        };
        try (Connection c = ConnectionPool.getInstance().getConnection()) {
            c.setAutoCommit(true);
            Statement stmnt = c.createStatement();
            for (int i = 0; i < actionList.length; i++) {
                boolean actionResult = executeStatement(stmnt, actionList[i]);
                if(actionResult){
                    logger.debug("Successfully executed update :" + actionList[i]);
                }
                if(actionResult && checkList[i] != null){
                    boolean checkResult = checkResult(stmnt, checkList[i]);
                    if(!checkResult){
                        return false;
                    }else{
                        logger.debug("Check successfull : " + checkList[i]);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("SQL error :" + e.getMessage(), e);
        }
        logger.info("Ended database setup");
        return true;
    }

    private boolean executeStatement(Statement stmnt, String sql){
        try {
            stmnt.executeUpdate(sql);
        } catch (SQLException e) {
            logger.error("Unable to execute " + sql, e);
            return false;
        }
        return true;
    }

    private boolean checkResult(Statement stmnt, String sql){
        try {
            return stmnt.executeQuery(sql).next();
        } catch (SQLException e) {
            logger.error("Zero results for query " + sql, e);
            return false;
        }
    }

    public void showAlerts(){
        try {
            Statement stmnt = ConnectionPool.getInstance().getConnection().createStatement();
            ResultSet set = stmnt.executeQuery(Constants.DB_ALERT_DISPLAY);
            System.out.format("|%25s|%20s|%15s|%15s|\n", "ID" , "TYPE", "DURATION", "HOST");
            System.out.println(Stream.generate(() -> "-").limit(80).collect(Collectors.joining()));
            while (set.next()){
                String id = set.getString("ID");
                String type = set.getString("TYPE");
                type = set.wasNull() ? "" : type;
                Integer duration = set.getInt("duration_ms");
                String host = set.getString("HOST");
                host = set.wasNull() ? "" : host;
                System.out.format("|%25s|%20s|%15d|%15s|\n", id , type,  duration, host );
            }
            System.out.println(Stream.generate(() -> "-").limit(80).collect(Collectors.joining()));
        } catch (SQLException e) {
            logger.error("SQL error :" + e.getMessage(), e);
        }
    }
}