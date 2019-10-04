package com.zbychu.db;

import com.zbychu.common.Constants;
import com.zbychu.common.LogEntryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CallablePersister {
    private final Logger logger = LoggerFactory.getLogger(CallablePersister.class);

    private LinkedBlockingQueue<String> q;
    private AtomicBoolean endOfInput;

    public CallablePersister(LinkedBlockingQueue<String> q, AtomicBoolean endOfInput){
        this.q = q;
        this.endOfInput = endOfInput;
    }


    public Boolean process() {

        LinkedList<LogEntryObject> logEntryObjects = new LinkedList<>();
        while (true){
            while (!q.isEmpty()){
                String s = q.poll();
                if(s != null) {
                    logger.debug("Picked up message from queue : " + s);
                    LogEntryObject logEntryObject = LogEntryObject.fromString(s);
                    if (logEntryObject != null) {
                        logEntryObjects.add(logEntryObject);
                    } else {
                        logger.warn("Received non-deserializable string : " + s);
                    }
                    if (logEntryObjects.size() == 500) {
                        break;
                    }
                }
            }
            while(!ConnectionPool.getInstance().isDbReady()){
                try {
                    logger.error("Database connection not ready ...");
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error("Something went wrong ...");
                }
            }
            if(!logEntryObjects.isEmpty()) {
                logger.debug(String.format("About to persist %d messages", logEntryObjects.size()));
                persistMessages(logEntryObjects);
                logEntryObjects.clear();
            }else if(endOfInput.get() && q.isEmpty()){
                logger.info("Received EOI signal ... exiting");
                break;
            } else {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    logger.error("Something went wrong ...");
                }
            }
        }
        return true;
    }

    public void persistMessages(List<LogEntryObject> logEntryObjectList){
        try(Connection conn = ConnectionPool.getInstance().getConnection()){
            PreparedStatement preparedStatement = conn.prepareStatement(Constants.DB_PREPARED_STATEMENT);
            conn.setAutoCommit(true);
            for (int i = 0; i < logEntryObjectList.size(); i++) {
                LogEntryObject logEntryObject = logEntryObjectList.get(i);
                try {
                    preparedStatement.setString(1, logEntryObject.getId());
                    preparedStatement.setString(2, logEntryObject.getType());
                    preparedStatement.setString(3, logEntryObject.getHost());
                    preparedStatement.setLong(4, logEntryObject.getTimestamp());
                    preparedStatement.setString(5, logEntryObject.getState());
                    preparedStatement.addBatch();
                } catch (SQLException e) {
                    logger.error("Error during populating prepared statement:", e);
                }
            }
            logger.debug("Executing batch");
            preparedStatement.executeBatch();
        } catch (SQLException e) {
            logger.error("SQL Exception", e);
        }
    }


}