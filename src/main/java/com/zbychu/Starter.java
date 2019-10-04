package com.zbychu;

import com.zbychu.common.Config;
import com.zbychu.common.Constants;
import com.zbychu.db.ConnectionPool;
import com.zbychu.db.DatabaseOperations;
import com.zbychu.readers.combo.ComboReader;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Starter {
    private static Logger logger;
    public Starter(CommandLine cmdLine){


        Config.setConfig(
                cmdLine.hasOption(Constants.CONFIG_SHORT_FLAG) ?
                        cmdLine.getOptionValue(Constants.CONFIG_SHORT_FLAG) :
                        Constants.CONFIG_FILE_DEFAULT
        );
        long start = 0L;
        if(cmdLine.hasOption(Constants.TAIL_SHORT_FLAG) && cmdLine.hasOption(Constants.RESULTS_SHORT_FLAG)){
            System.err.println("Results wont be displayed when using TAIL flag");
            logger.warn("Results wont be displayed when using TAIL flag");
        }
        if(cmdLine.hasOption(Constants.START_SHORT_FLAG)){
            try {
                start = Long.parseLong(cmdLine.getOptionValue(Constants.START_SHORT_FLAG));
            }catch (NumberFormatException e){
                System.err.println("Incorrect line number provided");
                System.exit(1);
            }
        }

        DatabaseOperations dbo = new DatabaseOperations();
        boolean dbResult = dbo.setupObjects();
        ConnectionPool.getInstance().setDbReady(dbResult);
        addShutDownHook(dbo, cmdLine);
        while(!ConnectionPool.getInstance().isDbReady()){
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                logger.error("Something went wrong ...");
            }
        }
        if(ConnectionPool.getInstance().isDbReady()) {
            long s = new Date().getTime();
            File log = new File(cmdLine.getOptionValue(Constants.FILE_SHORT_FLAG));
            new ComboReader(log).process();
/*
            try {
                IntSummaryStatistics stats = Files.lines(log.toPath()).limit(1000).map(String::length).collect(Collectors.summarizingInt(i -> i));
                int avgLineLength = Double.valueOf(stats.getAverage()).intValue();
                long estimatelLines = log.length() / avgLineLength;
                long linesPerThread = estimatelLines / 30;
                ExecutorService exec = Executors.newFixedThreadPool(30);
                List<Boolean> t = ConcurrentSequence.sequence(
                        LongStream.range(0, 32).boxed().map(p -> CompletableFuture.supplyAsync(
                                () -> new StreamLogReader(
                                        new File(cmdLine.getOptionValue(Constants.FILE_SHORT_FLAG)),
                                        cmdLine.hasOption(Constants.TAIL_SHORT_FLAG)
                                ).processLogFile(p * linesPerThread, Long.MAX_VALUE), exec))
                                .collect(Collectors.toList())
                ).join();
                System.out.println("");
            } catch (IOException e) {
                e.printStackTrace();
            }*/
 /*           new StreamLogReader(
                    new File(cmdLine.getOptionValue(Constants.FILE_SHORT_FLAG)),
                    cmdLine.hasOption(Constants.TAIL_SHORT_FLAG)
            ).processLogFile(start, Long.MAX_VALUE);*/
            long e = new Date().getTime();
            System.out.println("Time Taken ms : " + (e-s));
        }else{
            logger.error("Unable to proceed, Database not set correctly");
            System.exit(1);
        }
        System.exit(0);
    }

    public void closeDatabaseThreads(){
        ConnectionPool.getInstance().setPoison();
        ConnectionPool.getInstance().getDataSource().close();
    }

    public void addShutDownHook(DatabaseOperations dbo, CommandLine cmdLine){
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown Hook is running !");
            logger.info("Shutdown Hook is running !");
            if (cmdLine.hasOption(Constants.RESULTS_SHORT_FLAG)){
                dbo.showAlerts();
            }
            closeDatabaseThreads();
        }));
    }

    public static void main(String[] args) {
        //Command Line check
        CommandLine cmdLine = getArguments(args);
        if(cmdLine == null){
            System.err.println("Unable to parse command line");
            System.exit(1);
        }
        File f = new File(cmdLine.getOptionValue(Constants.FILE_SHORT_FLAG));
        if(!f.exists()){
            System.err.println(String.format("Unable to find source file %s", f.getAbsolutePath()));
            System.exit(1);
        }
        if(cmdLine.hasOption(Constants.CONFIG_SHORT_FLAG)){
            f = new File(cmdLine.getOptionValue(Constants.CONFIG_SHORT_FLAG));
            if(!f.exists()){
                System.err.println(String.format("Unable to find config file %s", f.getAbsolutePath()));
                System.exit(1);
            }
        }

        logger = LoggerFactory.getLogger(Starter.class);
        new Starter(cmdLine);
    }

    private static CommandLine getArguments(String[] args) {
        HelpFormatter helpFormatter = new HelpFormatter();
        Options options = new Options();

        Option fileOption = Option.builder(Constants.FILE_SHORT_FLAG)
                .required(true)
                .argName("application log")
                .numberOfArgs(1)
                .desc("Path of the file to read/tail")
                .longOpt(Constants.FILE_LONG_FLAG)
                .build();

        Option configOption = Option.builder(Constants.CONFIG_SHORT_FLAG)
                .required(false)
                .argName("config file")
                .numberOfArgs(1)
                .desc("Override configuration file [default:config.properties]")
                .longOpt(Constants.CONFIG_LONG_FLAG)
                .build();

        Option tailOption = Option.builder(Constants.TAIL_SHORT_FLAG)
                .desc("Continue updating database with newly added lines")
                .numberOfArgs(0)
                .longOpt(Constants.TAIL_LONG_FLAG)
                .build();

        Option resultOption = Option.builder(Constants.RESULTS_SHORT_FLAG)
                .desc("Show results (>4ms) after completion")
                .numberOfArgs(0)
                .longOpt(Constants.RESULTS_LONG_FLAG)
                .build();
        Option startOption = Option.builder(Constants.START_SHORT_FLAG)
                .desc("Start processing from N-th line of file")
                .numberOfArgs(1)
                .argName("line number")
                .longOpt(Constants.START_LONG_FLAG)
                .build();

        options.addOption(fileOption);
        options.addOption(configOption);
        options.addOption(tailOption);
        options.addOption(resultOption);
        options.addOption(startOption);

        CommandLineParser parser = new DefaultParser();
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            helpFormatter.setWidth(500);

            helpFormatter.printHelp("java -jar CS-1.0.jar", options, true);
            //logger.error(String.format("Command Line parse error: %s", e.getMessage()));
            System.err.println(e.getMessage());
            //Vital arguments were not provided, therefore exiting with non-zero code
            System.exit(1);
        }
        return null;
    }

}