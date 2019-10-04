package com.zbychu.readers.stream;

import com.zbychu.common.Config;
import com.zbychu.common.Constants;
import com.zbychu.db.Persister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class StreamLogReader {
    private File logfile;
    private final boolean tail;
    private static final Logger logger = LoggerFactory.getLogger(StreamLogReader.class);

    public StreamLogReader(File logfile, boolean tail){
        this.logfile = logfile;
        this.tail = tail;
    }

    public StreamLogReader(File logfile){
        this(logfile, false);
    }

    public void processLogFile(){
        processLogFile(0L);
    }

    public void processLogFile(long start){
        logger.info("Starting Stream Log processing ...");
        AtomicLong linesRead = new AtomicLong(start);
        int threads = Config.getInt(Constants.CONFIG_THREAD_MAX_PROP);
        LinkedBlockingQueue<String> workerQueue = new LinkedBlockingQueue<>();
        AtomicBoolean endOfInput = new AtomicBoolean(false);
        long readSize = 0;
        try {
            logger.info(String.format("Starting %d workers", threads));
            for (int i = 0; i < threads; i++) {
                Executors.newSingleThreadExecutor().execute(new Persister(workerQueue, endOfInput));
            }
            Files.lines(logfile.toPath()).skip(linesRead.get()).forEach(s -> {
                logger.debug("Processing line :" + s);
                workerQueue.add(s);
                logger.debug(String.format("Worker queue size: %d", workerQueue.size()));
                linesRead.incrementAndGet();
            });
            readSize = logfile.length();
        } catch (IOException e) {
            logger.error(String.format("Unable to read lines from file : %s", logfile.getAbsolutePath()), e);
        }
        while (workerQueue.size() > 0){
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                logger.error("Something went wrong ... ", e);
            }
        }
        logger.info("Marking EndOfInput ...");
        endOfInput.set(true);
        logger.info(String.format("Read total %d lines ...", linesRead.get()));
        if(tail){
            TailLogReader tailer = new TailLogReader(logfile, Config.getLong(Constants.INTERVAL), linesRead);
            tailer.readMessages(readSize);
        }
    }
}