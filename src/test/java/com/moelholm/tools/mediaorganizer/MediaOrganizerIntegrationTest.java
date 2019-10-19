package com.moelholm.tools.mediaorganizer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(profiles = "test")
@SpringBootTest
public class MediaOrganizerIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(MediaOrganizerIntegrationTest.class);

    @Autowired private MediaOrganizer organizer; // S.U.T.

    private Path from;

    private Path to;

    @Test
    public void undoFlatMess_whenProcessingNonMediaFiles_thenSkipsThem() {

        // Given
        addFileToDirectoryPath(from, "README.md");
        addFileToDirectoryPath(from, "story.pdf");

        // When
        organizer.undoFlatMess(from, to);

        // Then
        assertPathExistsInDirectory(from, "README.md");
        assertPathNotExistsInDirectory(to, "README.md");
        assertPathExistsInDirectory(from, "story.pdf");
        assertPathNotExistsInDirectory(to, "story.pdf");
    }

    @Test
    public void
            undoFlatMess_whenProcessingMediaFilesWithUnparseableDates_thenMovesThemToUnknownFolder() {

        // Given
        addFileToDirectoryPath(from, "i_dont_have_a_sane_date_in_my_filename.jpg");

        // When
        organizer.undoFlatMess(from, to);

        // Then
        assertPathExistsInDirectory(to, "unknown", "i_dont_have_a_sane_date_in_my_filename.jpg");
        assertPathNotExistsInDirectory(from, "i_dont_have_a_sane_date_in_my_filename.jpg");
    }

    @Test
    public void undoFlatMess_whenInvoked_thenMovesMediaFiles() {

        // Given
        addFileToDirectoryPath(from, "2015-01-13 03.13.53.jpg");
        addFileToDirectoryPath(from, "2015-03-13 06.13.54.jpg");
        addFileToDirectoryPath(from, "2015-10-11 16.13.54.mov");
        for (int i = 0; i < 13; i++) {
            addFileToDirectoryPath(from, String.format("2015-10-11 15.13.%02d.jpg", i));
        }

        // When
        organizer.undoFlatMess(from, to);

        // Then
        assertPathExistsInDirectory(to, "2015 - January - Misc", "2015-01-13 03.13.53.jpg");
        assertPathNotExistsInDirectory(from, "2015 - January - Misc", "2015-01-13 03.13.53.jpg");

        assertPathExistsInDirectory(to, "2015 - March - Misc", "2015-03-13 06.13.54.jpg");
        assertPathNotExistsInDirectory(from, "2015 - March - Misc", "2015-03-13 06.13.54.jpg");

        assertPathExistsInDirectory(
                to, "2015 - October - 11 - This Must Be An Event", "2015-10-11 16.13.54.mov");
        assertPathNotExistsInDirectory(
                from, "2015 - October - 11 - This Must Be An Event", "2015-10-11 16.13.54.mov");

        assertPathExistsInDirectory(
                to, "2015 - October - 11 - This Must Be An Event", "2015-10-11 15.13.00.jpg");
        assertPathNotExistsInDirectory(
                from, "2015 - October - 11 - This Must Be An Event", "2015-10-11 15.13.00.jpg");

        assertPathExistsInDirectory(
                to, "2015 - October - 11 - This Must Be An Event", "2015-10-11 15.13.01.jpg");
        assertPathNotExistsInDirectory(
                from, "2015 - October - 11 - This Must Be An Event", "2015-10-11 15.13.01.jpg");

        assertPathExistsInDirectory(
                to, "2015 - October - 11 - This Must Be An Event", "2015-10-11 15.13.12.jpg");
        assertPathNotExistsInDirectory(
                from, "2015 - October - 11 - This Must Be An Event", "2015-10-11 15.13.12.jpg");
    }

    @AfterEach
    public void after() {
        LOG.info("Test stopped");
        deleteTestDataDirectory(from);
        deleteTestDataDirectory(to);
    }

    @BeforeEach
    public void before() throws IOException {
        LOG.info("Test started");
        from = createTestDataDirectoryAndReturnPath("target/testground-from");
        to = createTestDataDirectoryAndReturnPath("target/testground-to");
    }

    private void assertPathExistsInDirectory(Path directoryPath, String first, String... other) {
        Path pathInToDirectoryPath = directoryPath.resolve(Paths.get(first, other));
        assertTrue(pathInToDirectoryPath.toFile().exists());
    }

    private void assertPathNotExistsInDirectory(Path directoryPath, String first, String... other) {
        Path pathInToDirectoryPath = directoryPath.resolve(Paths.get(first, other));
        assertFalse(pathInToDirectoryPath.toFile().exists());
    }

    private void deleteTestDataDirectory(Path path) {
        LOG.info("    Deleting {}", path);
        if (path.toFile().exists()) {
            try {
                Files.walkFileTree(path, new FileDeleterVisitor());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Path createTestDataDirectoryAndReturnPath(String pathAsString) {
        Path path = Paths.get(pathAsString);
        if (path.toFile().exists()) { // A-nasty-little-side-effect-ok-for-testing-!-:)-
            deleteTestDataDirectory(path);
        }
        path.toFile().mkdirs();
        return path;
    }

    private void addFileToDirectoryPath(Path targetDirectoryPath, String fileName) {
        try {
            targetDirectoryPath.resolve(fileName).toFile().createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class FileDeleterVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            LOG.info("        Deleted {}", file.getFileName());
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            LOG.info("        Deleted {}", dir.getFileName());
            return FileVisitResult.CONTINUE;
        }
    }
}
