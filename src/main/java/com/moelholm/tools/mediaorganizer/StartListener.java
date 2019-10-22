package com.moelholm.tools.mediaorganizer;

import com.moelholm.tools.mediaorganizer.filesystem.FileSystemType;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
public class StartListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Environment environment;

    public StartListener(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void started() {

        var startedWithMandatoryArguments =
                (environment.containsProperty(MainArgument.FROM_DIR.getArgumentName())
                        && environment.containsProperty(MainArgument.TO_DIR.getArgumentName()));

        if (!startedWithMandatoryArguments) {
            printUsageAndExit();
            return;
        }

        var fileSystemType =
                FileSystemType.fromString(
                        environment.getProperty(MainArgument.FILESYSTEM_TYPE.getArgumentName()));
        var fromDir = environment.getProperty(MainArgument.FROM_DIR.getArgumentName());
        var toDir = environment.getProperty(MainArgument.TO_DIR.getArgumentName());

        printApplicationStartedMessage(fromDir, toDir, fileSystemType);
    }

    private void printApplicationStartedMessage(
            String fromDir, String toDir, FileSystemType fileSystemType) {
        logger.info("");
        logger.info("Application started with the following arguments:");
        logger.info(
                "    --{} = {}",
                MainArgument.FILESYSTEM_TYPE.getArgumentName(),
                fileSystemType.toString().toLowerCase());
        logger.info("    --{} = {}", MainArgument.FROM_DIR.getArgumentName(), fromDir);
        logger.info("    --{}   = {}", MainArgument.TO_DIR.getArgumentName(), toDir);
        logger.info("");
        logger.info("Awaiting run signals on HTTP endpoint");
    }

    private void printUsageAndExit() {
        logger.info("");
        logger.info(
                "Usage: Main --{}=[dir to copy from] --{}=[dir to copy to] [--{}=[type]]",
                MainArgument.FROM_DIR.getArgumentName(),
                MainArgument.TO_DIR.getArgumentName(),
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
                "    --{} One of: [local, dropbox]. Default is local",
                MainArgument.FILESYSTEM_TYPE.getArgumentName());
        logger.info("");
        System.exit(-1);
    }
}
