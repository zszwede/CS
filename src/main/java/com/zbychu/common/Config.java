package com.zbychu.common;

import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.sync.ReadWriteSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Config {

    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    public static int getInt(String key) {
        return configuration.getInt(key);
    }

    public static long getLong(String key) {
        return configuration.getLong(key);
    }

    public static String getString(String key) {
        return configuration.getString(key);
    }

    private static FileBasedConfiguration configuration;

    public static void setConfig(String configFile){
        Parameters params = new Parameters();
        try {
            FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                    new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                            .configure(params.fileBased()
                                    .setSynchronizer(new ReadWriteSynchronizer())
                                    .setFile(new File(configFile))
                            );
            configuration = builder.getConfiguration();
        } catch (ConfigurationException e) {
            logger.error(String.format("Unable to parse configuration from (%s)", configFile), e);
            //Configuration file cannot be parsed, therefore exiting with non-zero code
            System.exit(1);
        }
    }

}