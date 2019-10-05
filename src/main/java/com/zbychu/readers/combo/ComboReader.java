package com.zbychu.readers.combo;

import com.zbychu.common.ConcurrentSequence;
import com.zbychu.common.Config;
import com.zbychu.common.Constants;
import com.zbychu.db.ClassicPersister;
import com.zbychu.db.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class ComboReader {
    private long threads;
    private long lastChunk;
    private LinkedBlockingQueue<String> queue;
    private File log;
    private long estimatelLines = 0L;
    private long linesPerThread = 0L;
    private final Logger logger = LoggerFactory.getLogger(ComboReader.class);

    public ComboReader(File log){
        try {
            IntSummaryStatistics stats = Files.lines(log.toPath()).limit(1000).map(String::length).collect(Collectors.summarizingInt(i -> i));
            double avgLineLength = stats.getAverage();
            threads = Config.getLong(Constants.CONFIG_THREAD_MAX_PROP);
            estimatelLines = Double.valueOf(log.length() / avgLineLength).longValue();
            linesPerThread = estimatelLines / threads;
            lastChunk = linesPerThread * (threads + 1);
            this.log = log;
            this.queue = ConnectionPool.getInstance().getQ();
        } catch (IOException e) {
            logger.error("Unable to read file", e);
        }
    }

    public void process(){
        AtomicBoolean eoi = new AtomicBoolean(false);
        ConcurrentHashMap<String,Long> states = new ConcurrentHashMap<>();
        logger.info("Starting readers ...");
        List<CompletableFuture<Boolean>> readers = startReaders();
        logger.info("Starting persisters ...");
        List<CompletableFuture<Boolean>> persisters = IntStream.range(0, (int) (threads + 1)).boxed().map(p -> CompletableFuture.supplyAsync(
                () -> new ClassicPersister(states, eoi).process()))
                .collect(Collectors.toList());
        ConcurrentSequence.sequence(readers).join();
        logger.info("All readers finished ...");
        eoi.set(true);
        logger.info("End Of Input set ...");
        logger.info("Waiting for persisters to finish ...");
        ConcurrentSequence.sequence(persisters).join();
        logger.info("Persisters finished job ...");
    }

    public List<CompletableFuture<Boolean>> startReaders(){
                return LongStream.range(0, threads + 2).boxed().map(p -> CompletableFuture.supplyAsync(
                        () -> readChunk(p*linesPerThread)))
                        .collect(Collectors.toList());
    }

    public boolean readChunk(long start){
        try {
            Stream<String> stream = Files.lines(log.toPath()).skip(start);
            if(start != lastChunk) {
                stream = stream.limit(linesPerThread);
            }
            stream.forEach(s -> queue.add(s));
        } catch (IOException e) {
            logger.error("Unable to read file", e);
        }
        return true;
    }
}
