package com.moelholm.tools.mediaorganizer;

import com.moelholm.tools.mediaorganizer.filesystem.FileSystemType;
import java.nio.file.Paths;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
public class StartListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired private Environment environment;

    @Autowired private MediaOrganizer organizer;

    @PostConstruct
    public void started() {

        boolean startedWithMandatoryArguments =
                (environment.containsProperty(MainArgument.FROM_DIR.getArgumentName())
                        && environment.containsProperty(MainArgument.TO_DIR.getArgumentName()));

        if (!startedWithMandatoryArguments) {
            printUsageAndExit();
            return;
        }

        String runMode = environment.getProperty(MainArgument.RUNMODE.getArgumentName());
        FileSystemType fileSystemType =
                FileSystemType.fromString(
                        environment.getProperty(MainArgument.FILESYSTEM_TYPE.getArgumentName()));
        String fromDir = environment.getProperty(MainArgument.FROM_DIR.getArgumentName());
        String toDir = environment.getProperty(MainArgument.TO_DIR.getArgumentName());

        printApplicationStartedMessage(fromDir, toDir, runMode, fileSystemType);

        runMode = (runMode == null) ? "once" : runMode;

        if ("daemon".equalsIgnoreCase(runMode)) {
            runAsDaemon(fromDir, toDir);
        } else if ("once".equalsIgnoreCase(runMode)) {
            runOnce(fromDir, toDir);
        } else if ("web".equalsIgnoreCase(runMode)) {
            runInWebMode();
        } else {
            logValidationErrorAndExit(runMode);
        }
    }

    private void runInWebMode() {
        logger.info("Running in 'web' mode - awaiting run signals on HTTP endpoint");
    }

    private void logValidationErrorAndExit(String runMode) {
        logger.warn("Unknown run mode [{}]. Exiting application", runMode);
        System.exit(-1);
    }

    private void runOnce(String fromDir, String toDir) {
        try {
            logger.info("Running in 'once' mode");
            organizer.undoFlatMess(Paths.get(fromDir), Paths.get(toDir));
        } catch (Exception e) {
            logger.warn("Exiting application with error", e);
        } finally {
            logger.info("Exiting application");
        }
        System.exit(0);
    }

    private void runAsDaemon(String fromDir, String toDir) {
        logger.info("Running in 'daemon' mode");
        organizer.scheduleUndoFlatMess(Paths.get(fromDir), Paths.get(toDir));
    }

    private void printApplicationStartedMessage(
            String fromDir, String toDir, String runMode, FileSystemType fileSystemType) {
        logger.info("");
        logger.info("Application started with the following arguments:");
        logger.info("    --{} = {}", MainArgument.RUNMODE.getArgumentName(), runMode);
        logger.info(
                "    --{} = {}",
                MainArgument.FILESYSTEM_TYPE.getArgumentName(),
                fileSystemType.toString().toLowerCase());
        logger.info("    --{} = {}", MainArgument.FROM_DIR.getArgumentName(), fromDir);
        logger.info("    --{}   = {}", MainArgument.TO_DIR.getArgumentName(), toDir);
        logger.info("");
    }

    private void printUsageAndExit() {
        logger.info("");
        logger.info(
                "Usage: Main --{}=[dir to copy from] --{}=[dir to copy to] [--{}=[mode]] [--{}=[type]]",
                MainArgument.FROM_DIR.getArgumentName(),
                MainArgument.TO_DIR.getArgumentName(),
                MainArgument.RUNMODE.getArgumentName(),
                MainArgument.FILESYSTEM_TYPE.getArgumentName());
        logger.info("");
        logger.info("  Where:");
        logger.info("");
        logger.info(
                "    --{} folder that contains your media files",
                MainArgument.FROM_DIR.getArgumentName());
        logger.info(
                "    --{} folder that should contain the organized media files",
                MainArgument.TO_DIR.getArgumentName());
        logger.info(
                "    --{} One of: [once, daemon, web]. Default is once",
                MainArgument.RUNMODE.getArgumentName());
        logger.info(
                "    --{} One of: [local, dropbox]. Default is local",
                MainArgument.FILESYSTEM_TYPE.getArgumentName());
        logger.info("");
        System.exit(0);
    }
}
