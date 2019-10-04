package com.zbychu.readers.stream;

import com.zbychu.db.Persister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TailLogReader {
    private final File logfile;
    private final Long interval;
    private AtomicLong lastPosition;
    private static final Logger logger = LoggerFactory.getLogger(TailLogReader.class);

    public TailLogReader(File logfile, Long interval, AtomicLong lastPosition) {
        this.logfile = logfile;
        this.interval = interval;
        this.lastPosition = lastPosition;
    }

    public void readMessages(long readSize) {
        logger.info(String.format("Starting tail from position %d", readSize));
        LinkedBlockingQueue<String> workerQueue = new LinkedBlockingQueue<>();
        logger.info("Starting single-threaded persister");
        Executors.newSingleThreadExecutor().execute(new Persister(workerQueue));
        AtomicLong cnt = new AtomicLong(lastPosition.get());
        logger.info(String.format("Lines already read : %d", cnt.get()));
        while (true) {
            long currentSize = logfile.length();
            if(currentSize > readSize){
                logger.debug(String.format("Detected changes in file size (%d != %d), processing", readSize, currentSize));
                try {
                    Files.lines(logfile.toPath())
                            .skip(cnt.get())
                            .filter(String::isEmpty)
                            .forEach(s -> {
                                workerQueue.add(s);
                                cnt.incrementAndGet();
                            });
                } catch (IOException e) {
                    logger.error(String.format("Unable to read lines from file : %s", logfile.getAbsolutePath()), e);
                }
                readSize = currentSize;
            }else {
                logger.debug(String.format("No changes in file size (%d == %d), sleeping", readSize, currentSize));
                try {
                    TimeUnit.MILLISECONDS.sleep(interval);
                } catch (InterruptedException e) {
                    logger.error("Something went wrong ... ", e);
                }
            }

        }
    }
}