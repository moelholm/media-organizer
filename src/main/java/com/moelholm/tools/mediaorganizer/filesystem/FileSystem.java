package com.moelholm.tools.mediaorganizer.filesystem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface FileSystem {

    void move(Path from, Path to) throws IOException;

    Stream<Path> streamOfAllFilesFromPath(Path from);

    boolean existingDirectory(Path from);
}
