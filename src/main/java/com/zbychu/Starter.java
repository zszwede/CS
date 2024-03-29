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
import java.util.concurrent.TimeUnit;

public class Starter {
    private static Logger logger;
    public Starter(CommandLine cmdLine){


        Config.setConfig(
                cmdLine.hasOption(Constants.CONFIG_SHORT_FLAG) ?
                        cmdLine.getOptionValue(Constants.CONFIG_SHORT_FLAG) :
                        Constants.CONFIG_FILE_DEFAULT
        );
        if(cmdLine.hasOption(Constants.TAIL_SHORT_FLAG) && cmdLine.hasOption(Constants.RESULTS_SHORT_FLAG)){
            System.err.println("Results wont be displayed when using TAIL flag");
            logger.warn("Results wont be displayed when using TAIL flag");
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
            File log = new File(cmdLine.getOptionValue(Constants.FILE_SHORT_FLAG));
            new ComboReader(log).process();
        }else{
            logger.error("Unable to proceed, Database not set correctly");
            System.exit(1);
        }
        if (cmdLine.hasOption(Constants.RESULTS_SHORT_FLAG)){
            dbo.showAlerts();
        }
        System.exit(0);
    }

    public void closeDatabaseThreads(){
        ConnectionPool.getInstance().setPoison();
        ConnectionPool.getInstance().getDataSource().close();
    }

    public void addShutDownHook(DatabaseOperations dbo, CommandLine cmdLine){
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown Hook is running !");
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

        Option resultOption = Option.builder(Constants.RESULTS_SHORT_FLAG)
                .desc("Show results (>4ms) after completion")
                .numberOfArgs(0)
                .longOpt(Constants.RESULTS_LONG_FLAG)
                .build();

        options.addOption(fileOption);
        options.addOption(configOption);
        options.addOption(resultOption);

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