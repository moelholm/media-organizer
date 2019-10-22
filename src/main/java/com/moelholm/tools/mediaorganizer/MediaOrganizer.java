package com.moelholm.tools.mediaorganizer;

import com.moelholm.tools.mediaorganizer.filesystem.FileSystem;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class MediaOrganizer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Configuration configuration;

    private final FileSystem fileSystem;

    public MediaOrganizer(Configuration configuration, FileSystem fileSystem) {
        this.configuration = configuration;
        this.fileSystem = fileSystem;
    }

    @Async
    public void asyncUndoFlatMess(Path from, Path to) {
        undoFlatMess(from, to);
    }

    public void undoFlatMess(Path from, Path to) {

        if (hasInvalidParameters(from, to)) {
            return;
        }

        logger.info("Moving files from [{}] to [{}]", from, to);

        fileSystem
                .streamOfAllFilesFromPath(from)
                .filter(mediaFiles())
                .collect(groupByYearMonthDayString())
                .forEach(processBatch(to));
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
                configuration.getMediaFileExtensionsToMatch().stream()
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
                new DateFormatSymbols(configuration.getLocale())
                        .getMonths()[dateCal.get(Calendar.MONTH)];
        month = Character.toUpperCase(month.charAt(0)) + month.substring(1);
        var day = dateCal.get(Calendar.DAY_OF_MONTH);

        return String.format("%s - %s - %s", year, month, day);
    }

    private String generateFinalDestinationDirectoryName(
            String folderName, List<Path> mediaFilePaths) {
        var lastPartOfFolderName = "( - \\d+)$";
        String replaceWithNewLastPartOfFolderName;
        if (mediaFilePaths.size() >= configuration.getAmountOfMediaFilesIndicatingAnEvent()) {
            replaceWithNewLastPartOfFolderName =
                    String.format(
                            "$1 - %s",
                            configuration.getSuffixForDestinationFolderOfUnknownEventMediaFiles());
        } else {
            replaceWithNewLastPartOfFolderName =
                    String.format(
                            " - %s", configuration.getSuffixForDestinationFolderOfMiscMediaFiles());
        }
        return folderName.replaceAll(lastPartOfFolderName, replaceWithNewLastPartOfFolderName);
    }

    private Date parseDateFromPathName(Path path) {
        var sdf = new SimpleDateFormat(configuration.getMediaFilesDatePattern());
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
