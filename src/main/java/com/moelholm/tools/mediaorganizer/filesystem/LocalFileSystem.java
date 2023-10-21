package com.moelholm.tools.mediaorganizer.filesystem;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(name = "mediaorganizer.fileSystemType", havingValue = "local")
public class LocalFileSystem implements FileSystem {

    @Override
    public boolean existingDirectory(Path pathToTest) {
        return (pathToTest != null) && (pathToTest.toFile().isDirectory());
    }

    @Override
    public Stream<Path> streamOfAllFilesFromPath(Path from) {
        try {
            return Files.list(from);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void move(Path from, Path to) throws IOException {
        ensureDirectoryStructureExists(to.getParent());
        Files.move(from, to);
    }

    private void ensureDirectoryStructureExists(Path directoryPath) {
        if (directoryPath != null && !directoryPath.toFile().exists()) {
            directoryPath.toFile().mkdirs();
        }
    }
}
