package com.zbychu.readers.combo;

import com.zbychu.common.ConcurrentSequence;
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

public class ComboReader {
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
            estimatelLines = Double.valueOf(log.length() / avgLineLength).longValue();
            linesPerThread = estimatelLines / 30;
            lastChunk = linesPerThread * 31;
            this.log = log;
            this.queue = ConnectionPool.getInstance().getQ();
        } catch (IOException e) {
            logger.error("Unable to read file", e);
        }
    }

    public void process(){
        AtomicBoolean eoi = new AtomicBoolean(false);
        ConcurrentHashMap<String,Long> states = new ConcurrentHashMap<>();
        List<CompletableFuture<Boolean>> readers = startReaders();
        List<CompletableFuture<Boolean>> persisters = IntStream.range(0, 31).boxed().map(p -> CompletableFuture.supplyAsync(
                () -> new ClassicPersister(states, queue, eoi).process()))
                .collect(Collectors.toList());
        ConcurrentSequence.sequence(readers).join();
        eoi.set(true);
        ConcurrentSequence.sequence(persisters).join();
    }

    public List<CompletableFuture<Boolean>> startReaders(){
                return LongStream.range(0, 32).boxed().map(p -> CompletableFuture.supplyAsync(
                        () -> readChunk(p*linesPerThread)))
                        .collect(Collectors.toList());
    }

    public boolean readChunk(long start){
        try {
            if(start == lastChunk) {
                Files.lines(log.toPath()).skip(start).forEach(s -> queue.add(s));
            }else{
                Files.lines(log.toPath()).skip(start).limit(linesPerThread).forEach(s -> queue.add(s));
            }
        } catch (IOException e) {
            logger.error("Unable to read file", e);
        }
        return true;
    }
}
