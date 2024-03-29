package com.moelholm.tools.mediaorganizer;

import com.moelholm.tools.mediaorganizer.filesystem.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Component
public class MediaOrganizer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AppProperties appProperties;

    private final FileSystem fileSystem;

    public MediaOrganizer(AppProperties appProperties, FileSystem fileSystem) {
        this.appProperties = appProperties;
        this.fileSystem = fileSystem;
    }

    public void undoFlatMess() {
        var from = Path.of(appProperties.source().fromDir());
        var to = Path.of(appProperties.destination().toDir());

        logger.info("Moving files from [{}] to [{}]", from, to);
        assertValidDirs(from, to);

        var groupedMediaFiles = fileSystem
                .streamOfAllFilesFromPath(from)
                .filter(mediaFiles())
                .sorted()
                .collect(groupByYearMonthDayString());

        logStatistics(groupedMediaFiles);

        groupedMediaFiles.forEach(processBatch(to));
    }

    private void assertValidDirs(Path from, Path to) {
        if (hasInvalidParameters(from, to)) {
            throw new IllegalStateException(
                    String.format(
                            "Invalid parameters: from=[%s], to=[%s]",
                            from.toAbsolutePath(), to.toAbsolutePath()));
        }
    }

    private void logStatistics(Map<String, List<Path>> groupedMediaFiles) {
        logger.info("Found [{}] media files in total", groupedMediaFiles.values().stream()
                .mapToInt(List::size)
                .sum());
        groupedMediaFiles.forEach((yearMonthDayString, mediaFilePathList) -> logger.info(
                "    [{}] has [{}] media files",
                yearMonthDayString,
                mediaFilePathList.size()));
    }

    private BiConsumer<String, List<Path>> processBatch(Path to) {
        return (yearMonthDayString, mediaFilePathList) -> {
            logger.info(
                    "Processing [{}] which has [{}] media files",
                    yearMonthDayString,
                    mediaFilePathList.size());

            var destinationDirectoryName =
                    generateFinalDestinationDirectoryName(yearMonthDayString, mediaFilePathList);

            var destinationDirectoryPath = to.resolve(destinationDirectoryName);

            mediaFilePathList.forEach(processMediaFile(destinationDirectoryPath));
        };
    }

    private Consumer<Path> processMediaFile(Path destinationDirectoryPath) {
        return mediaFilePath ->
                move(mediaFilePath, destinationDirectoryPath.resolve(mediaFilePath.getFileName()));
    }

    private Collector<Path, ?, Map<String, List<Path>>> groupByYearMonthDayString() {
        return Collectors.groupingBy(this::toYearMonthDayString);
    }

    private Predicate<? super Path> mediaFiles() {
        return path ->
                Arrays.asList(appProperties.mediafiles().extensionsToMatch()).stream()
                        .anyMatch(extensionMatches(path));
    }

    private Predicate<String> extensionMatches(Path path) {
        return fileExtension ->
                path.toString().toLowerCase().endsWith(String.format(".%s", fileExtension));
    }

    private boolean hasInvalidParameters(Path from, Path to) {

        if (!fileSystem.existingDirectory(from)) {
            logger.info("Argument [from] is not an existing directory");
            return true;
        }

        if (!fileSystem.existingDirectory(to)) {
            logger.info("Argument [to] is not an existing directory");
            return true;
        }

        return false;
    }

    private String toYearMonthDayString(Path path) {
        var date = parseDateFromPathName(path);

        if (date == null) {
            return "unknown";
        }

        var dateCal = Calendar.getInstance();
        dateCal.setTime(date);

        var year = dateCal.get(Calendar.YEAR);
        var month =
                new DateFormatSymbols(appProperties.destination().localeForGeneratingDestinationFolderNames())
                        .getMonths()[dateCal.get(Calendar.MONTH)];
        month = Character.toUpperCase(month.charAt(0)) + month.substring(1);
        var day = dateCal.get(Calendar.DAY_OF_MONTH);

        return String.format("%s - %s - %s", year, month, day);
    }

    private String generateFinalDestinationDirectoryName(
            String folderName, List<Path> mediaFilePaths) {
        var lastPartOfFolderName = "( - \\d+)$";
        String replaceWithNewLastPartOfFolderName;
        if (mediaFilePaths.size() >= appProperties.destination().amountOfMediaFilesIndicatingAnEvent()) {
            replaceWithNewLastPartOfFolderName =
                    String.format(
                            "$1 - %s",
                            appProperties.destination().suffixForDestinationFolderOfUnknownEventMediaFiles());
        } else {
            replaceWithNewLastPartOfFolderName =
                    String.format(
                            " - %s", appProperties.destination().suffixForDestinationFolderOfMiscMediaFiles());
        }
        return folderName.replaceAll(lastPartOfFolderName, replaceWithNewLastPartOfFolderName);
    }

    private Date parseDateFromPathName(Path path) {
        var sdf = new SimpleDateFormat(appProperties.mediafiles().datePattern());
        try {
            return sdf.parse(path.getFileName().toString());
        } catch (ParseException e) {
            logger.warn("Failed to extract date from {} (Cause says: {})", path, e.getMessage());
            return null;
        }
    }

    private void move(Path fileToMove, Path pathThatFileShouldBeMovedTo) {
        try {
            logger.info("    {}", pathThatFileShouldBeMovedTo.getFileName());
            fileSystem.move(fileToMove, pathThatFileShouldBeMovedTo);
        } catch (FileAlreadyExistsException e) {
            logger.info(
                    "File [{}] exists at destination folder - so skipping that",
                    pathThatFileShouldBeMovedTo.getFileName());
        } catch (IOException e) {
            logger.warn(
                    String.format(
                            "Failed to move file from [%s] to [%s]",
                            pathThatFileShouldBeMovedTo, pathThatFileShouldBeMovedTo),
                    e);
        }
    }
}
