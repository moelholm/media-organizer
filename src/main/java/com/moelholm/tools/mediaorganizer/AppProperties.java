package com.moelholm.tools.mediaorganizer;

import com.moelholm.tools.mediaorganizer.filesystem.FileSystemType;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
@ConfigurationProperties(prefix = "mediaorganizer")
public class AppProperties {

    private FileSystemType fileSystemType;

    private Source source;

    private Destination destination;

    private Mediafiles mediafiles;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public void setFileSystemType(FileSystemType fileSystemType) {
        this.fileSystemType = fileSystemType;
    }

    public FileSystemType getFileSystemType() {
        return fileSystemType;
    }

    public Mediafiles getMediafiles() {
        return mediafiles;
    }

    public void setMediafiles(Mediafiles mediafiles) {
        this.mediafiles = mediafiles;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Path getFromDir() {
        return Path.of(getSource().getFromDir());
    }

    public Path getToDir() {
        return Path.of(getDestination().getToDir());
    }

    public String getMediafilesDatePattern() {
        return getMediafiles().getDatePattern();
    }

    public List<String> getMediaFileExtensionsToMatch() {
        return Arrays.asList(getMediafiles().getExtensionsToMatch());
    }

    public int getAmountOfMediaFilesIndicatingAnEvent() {
        return getDestination().getAmountOfMediaFilesIndicatingAnEvent();
    }

    public Locale getLocale() {
        return getDestination().getLocaleForGeneratingDestinationFolderNames();
    }

    public String getSuffixForDestinationFolderOfUnknownEventMediaFiles() {
        return getDestination().getSuffixForDestinationFolderOfUnknownEventMediaFiles();
    }

    public String getSuffixForDestinationFolderOfMiscMediaFiles() {
        return getDestination().getSuffixForDestinationFolderOfMiscMediaFiles();
    }

    public static class Destination {
        private String toDir;
        private int amountOfMediaFilesIndicatingAnEvent;
        private Locale localeForGeneratingDestinationFolderNames;
        private String suffixForDestinationFolderOfMiscMediaFiles;
        private String suffixForDestinationFolderOfUnknownEventMediaFiles;

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }

        public int getAmountOfMediaFilesIndicatingAnEvent() {
            return amountOfMediaFilesIndicatingAnEvent;
        }

        public void setAmountOfMediaFilesIndicatingAnEvent(int amountOfMediaFilesIndicatingAnEvent) {
            this.amountOfMediaFilesIndicatingAnEvent = amountOfMediaFilesIndicatingAnEvent;
        }

        public String getToDir() {
            return toDir;
        }

        public void setToDir(String toDir) {
            this.toDir = toDir;
        }

        public String getSuffixForDestinationFolderOfUnknownEventMediaFiles() {
            return suffixForDestinationFolderOfUnknownEventMediaFiles;
        }

        public void setSuffixForDestinationFolderOfUnknownEventMediaFiles(String suffixForDestinationFolderOfUnknownEventMediaFiles) {
            this.suffixForDestinationFolderOfUnknownEventMediaFiles = suffixForDestinationFolderOfUnknownEventMediaFiles;
        }

        public String getSuffixForDestinationFolderOfMiscMediaFiles() {
            return suffixForDestinationFolderOfMiscMediaFiles;
        }

        public void setSuffixForDestinationFolderOfMiscMediaFiles(String suffixForDestinationFolderOfMiscMediaFiles) {
            this.suffixForDestinationFolderOfMiscMediaFiles = suffixForDestinationFolderOfMiscMediaFiles;
        }

        public Locale getLocaleForGeneratingDestinationFolderNames() {
            return localeForGeneratingDestinationFolderNames;
        }

        public void setLocaleForGeneratingDestinationFolderNames(Locale localeForGeneratingDestinationFolderNames) {
            this.localeForGeneratingDestinationFolderNames = localeForGeneratingDestinationFolderNames;
        }
    }

    public static class Source {
        private String fromDir;

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }

        public String getFromDir() {
            return fromDir;
        }

        public void setFromDir(String fromDir) {
            this.fromDir = fromDir;
        }
    }

    public static class Mediafiles {
        private String datePattern;
        private String[] extensionsToMatch;

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }

        public String getDatePattern() {
            return datePattern;
        }

        public void setDatePattern(String datePattern) {
            this.datePattern = datePattern;
        }

        public String[] getExtensionsToMatch() {
            return extensionsToMatch;
        }

        public void setExtensionsToMatch(String[] extensionsToMatch) {
            this.extensionsToMatch = extensionsToMatch;
        }
    }
}
