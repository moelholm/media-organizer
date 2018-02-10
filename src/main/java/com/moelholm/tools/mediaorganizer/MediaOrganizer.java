package com.moelholm.tools.mediaorganizer;

import com.moelholm.tools.mediaorganizer.filesystem.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Component
public class MediaOrganizer {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private MediaOrganizerConfiguration configuration;

  @Autowired
  private TaskScheduler scheduler;

  @Autowired
  private FileSystem fileSystem;

  public void scheduleUndoFlatMess(Path from, Path to) {

    if (hasInvalidParameters(from, to)) {
      return;
    }

    CronTrigger trigger = new CronTrigger(configuration.getScheduleAsCronExpression());
    scheduler.schedule(() -> undoFlatMess(from, to), trigger);

    logger.info("Scheduled job that will move files from [{}] to [{}]", from, to);
    Date nextExecutionTime = trigger.nextExecutionTime(new SimpleTriggerContext());
    logger.info("    - Job will start at [{}]", formatDateAsString(nextExecutionTime));
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

    fileSystem.streamOfAllFilesFromPath(from)
        .filter(selectMediaFiles())
        .collect(groupByYearMonthDayString())
        .forEach((yearMonthDayString, mediaFilePathList) -> {

          logger.info("Processing [{}] which has [{}] media files", yearMonthDayString, mediaFilePathList.size());

          String destinationDirectoryName = generateFinalDestinationDirectoryName(yearMonthDayString, mediaFilePathList);

          Path destinationDirectoryPath = to.resolve(destinationDirectoryName);

          mediaFilePathList.stream()
              .forEach(mediaFilePath -> move(mediaFilePath, destinationDirectoryPath.resolve(mediaFilePath.getFileName())));
        });
  }

  private Collector<Path, ?, Map<String, List<Path>>> groupByYearMonthDayString() {
    return Collectors.groupingBy(this::toYearMonthDayString);
  }

  private Predicate<? super Path> selectMediaFiles() {
    return path -> configuration.getMediaFileExtensionsToMatch().stream()//
        .anyMatch(fileExtension -> path.toString().toLowerCase().endsWith(String.format(".%s", fileExtension)));
  }

  private boolean hasInvalidParameters(Path from, Path to) {

    boolean result = false;

    if (!fileSystem.existingDirectory(from)) {
      logger.info("Argument [from] is not an existing directory");
      result = true;
    }

    if (!fileSystem.existingDirectory(to)) {
      logger.info("Argument [to] is not an existing directory");
      result = true;
    }

    return result;
  }

  private String toYearMonthDayString(Path path) {
    Date date = parseDateFromPathName(path);

    if (date == null) {
      return "unknown";
    }

    Calendar dateCal = Calendar.getInstance();
    dateCal.setTime(date);

    int year = dateCal.get(Calendar.YEAR);
    String month = new DateFormatSymbols(configuration.getLocale()).getMonths()[dateCal.get(Calendar.MONTH)];
    month = Character.toUpperCase(month.charAt(0)) + month.substring(1);
    int day = dateCal.get(Calendar.DAY_OF_MONTH);

    return String.format("%s - %s - %s", year, month, day);
  }

  private String generateFinalDestinationDirectoryName(String folderName, List<Path> mediaFilePaths) {
    String lastPartOfFolderName = "( - \\d+)$";
    String replaceWithNewLastPartOfFolderName;
    if (mediaFilePaths.size() >= configuration.getAmountOfMediaFilesIndicatingAnEvent()) {
      replaceWithNewLastPartOfFolderName = String.format("$1 - %s", configuration.getSuffixForDestinationFolderOfUnknownEventMediaFiles());
    } else {
      replaceWithNewLastPartOfFolderName = String.format(" - %s", configuration.getSuffixForDestinationFolderOfMiscMediaFiles());
    }
    return folderName.replaceAll(lastPartOfFolderName, replaceWithNewLastPartOfFolderName);
  }

  private Date parseDateFromPathName(Path path) {
    SimpleDateFormat sdf = new SimpleDateFormat(configuration.getMediaFilesDatePattern());
    try {
      return sdf.parse(path.getFileName().toString());
    } catch (ParseException e) {
      logger.warn("Failed to extract date from {} (Cause says: {})", path, e.getMessage());
      return null;
    }
  }

  private String formatDateAsString(Date date) {
    return DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(date);
  }

  private void move(Path fileToMove, Path pathThatFileShouldBeMovedTo) {
    try {
      logger.info("    {}", pathThatFileShouldBeMovedTo.getFileName());
      fileSystem.move(fileToMove, pathThatFileShouldBeMovedTo);
    } catch (FileAlreadyExistsException e) {
      logger.info("File [{}] exists at destination folder - so skipping that", pathThatFileShouldBeMovedTo.getFileName());
    } catch (IOException e) {
      logger.warn(String.format("Failed to move file from [%s] to [%s]", pathThatFileShouldBeMovedTo, pathThatFileShouldBeMovedTo), e);
    }
  }

}