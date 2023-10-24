package com.moelholm.tools.mediaorganizer;

import com.moelholm.tools.mediaorganizer.filesystem.FileSystemType;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;

@ConfigurationProperties(prefix = "mediaorganizer")
public record AppProperties(FileSystemType fileSystemType, Source source, Destination destination, Mediafiles mediafiles) {

    public record Destination(
        String toDir,
        int amountOfMediaFilesIndicatingAnEvent,
        Locale localeForGeneratingDestinationFolderNames,
        String suffixForDestinationFolderOfMiscMediaFiles,
        String suffixForDestinationFolderOfUnknownEventMediaFiles) {
    }

    public record Source (String fromDir) {}
    public record Mediafiles(String datePattern, String[] extensionsToMatch) {}
}
