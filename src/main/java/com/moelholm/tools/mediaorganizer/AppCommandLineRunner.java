package com.moelholm.tools.mediaorganizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class AppCommandLineRunner implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private final AppProperties appProperties;
    private final MediaOrganizer organizer;

    AppCommandLineRunner(MediaOrganizer organizer, AppProperties appProperties) {
        this.appProperties = appProperties;
        this.organizer = organizer;
    }

    @Override
    public void run(String... args) {
        try {
            LOGGER.info("Application started");
            LOGGER.info("Configuration: [{}]", appProperties);
            organizer.undoFlatMess();
        } finally {
            LOGGER.info("Application finished");
        }
    }
}
