package com.zbychu.db;

import com.zbychu.common.LogEntryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClassicPersister {
    private final AtomicBoolean endOfInput;
    private final ConcurrentHashMap<String, Long> states;
    private final LinkedBlockingQueue<String> q;
    private final Logger logger = LoggerFactory.getLogger(ClassicPersister.class);

    public ClassicPersister(ConcurrentHashMap<String,Long> states, LinkedBlockingQueue<String> q, AtomicBoolean endOfInput){
        this.states = states;
        this.q = q;
        this.endOfInput = endOfInput;
    }

    public boolean process(){
        LinkedList<LogEntryObject> logEntryObjects = new LinkedList<>();
        while (true){
            while (!q.isEmpty()){
                String s = q.poll();
                if(s != null) {
                    logger.debug("Picked up message from queue : " + s);
                    LogEntryObject logEntryObject = LogEntryObject.fromString(s);
                    if (logEntryObject != null) {
                        if(states.containsKey(logEntryObject.getId())){
                            long duration = Math.abs(logEntryObject.getTimestamp() - states.get(logEntryObject.getId()));
                            logEntryObject.setDuration(duration);
                            logEntryObjects.add(logEntryObject);
                            states.remove(logEntryObject.getId());
                        }else{
                            states.put(logEntryObject.getId(),logEntryObject.getTimestamp());
                        }
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

    private void persistMessages(LinkedList<LogEntryObject> logEntryObjects) {
        try(Connection conn = ConnectionPool.getInstance().getConnection()){
            PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO PUBLIC.RESULTS (ID, DURATION_MS, TYPE, HOST, ALERT) VALUES (?,?,?,?,?)");
            conn.setAutoCommit(true);
            for (int i = 0; i < logEntryObjects.size(); i++) {
                LogEntryObject leo = logEntryObjects.get(i);
                try {

                    preparedStatement.setString(1, leo.getId());
                    preparedStatement.setLong(2, leo.getDuration());
                    preparedStatement.setString(3, leo.getType());
                    preparedStatement.setString(4, leo.getHost());
                    preparedStatement.setBoolean(5, leo.getDuration() > 4);
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

