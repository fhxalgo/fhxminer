package com.fhx.bitcoin.miner;

import com.google.inject.AbstractModule;
import org.nnsoft.guice.rocoto.configuration.ConfigurationModule;
import org.nnsoft.guice.rocoto.converters.PropertiesConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * Created by George on 1/8/14.
 */
public class ConfigModule extends AbstractModule {
    private static final Logger log = LoggerFactory.getLogger(ConfigModule.class);

    protected String instanceName;

    public String getInstanceName() {
        return this.instanceName;
    }
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    protected void configure() {
        final Properties properties = new Properties();
        String filename = System.getProperty("config.properties", "config.properties");
        final File file = new File(filename);
        if (file.exists()) {
            log.info("Loading config.properties from {} ", file.getAbsolutePath());
            FileReader reader = null;
            try {
                reader = new FileReader(file);
                properties.load(reader);
            } catch (Exception e) {
                throw new RuntimeException("Error loading properties " + file.getAbsolutePath(), e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException io) {
                        log.warn("Error closing properties file {}", io);
                    }
                }
            }
        }
        else {
            URL inputStream = getClass().getResource("/" + filename);

            if (inputStream != null) {
                try {
                    log.info("Loadning config.properties from classpath: {}", inputStream.toExternalForm());
                    properties.load(inputStream.openStream());
                } catch (IOException e) {
                    throw new RuntimeException("Error loading properties " + filename + " from classpath");
                }
            }
        }

        ConfigurationModule configurationModule = new ConfigurationModule() {
            @Override
            protected void bindConfigurations() {
                bindEnvironmentVariables();
                bindSystemProperties();
                bindProperties(properties);
            }
        };

        install(new PropertiesConverter());

        if (instanceName != null) {
            //
        }
        else {
            install(configurationModule);
        }
    }
}
