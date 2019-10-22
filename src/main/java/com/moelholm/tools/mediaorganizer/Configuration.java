package com.moelholm.tools.mediaorganizer;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Configuration {

    @Value("${mediafiles.datepattern}")
    private String mediaFilesDatePattern;

    @Value("${mediafiles.mediaFileExtensionsToMatch}")
    private String[] mediaFileExtensionsToMatch;

    @Value("${destination.amountOfMediaFilesIndicatingAnEvent}")
    private int amountOfMediaFilesIndicatingAnEvent;

    @Value("${destination.localeForGeneratingDestinationFolderNames}")
    private Locale locale;

    @Value("${destination.suffixForDestinationFolderOfUnknownEventMediaFiles}")
    private String suffixForDestinationFolderOfUnknownEventMediaFiles;

    @Value("${destination.suffixForDestinationFolderOfMiscMediaFiles}")
    private String suffixForDestinationFolderOfMiscMediaFiles;

    String getMediaFilesDatePattern() {
        return mediaFilesDatePattern;
    }

    List<String> getMediaFileExtensionsToMatch() {
        return Arrays.asList(mediaFileExtensionsToMatch);
    }

    int getAmountOfMediaFilesIndicatingAnEvent() {
        return amountOfMediaFilesIndicatingAnEvent;
    }

    Locale getLocale() {
        return locale;
    }

    String getSuffixForDestinationFolderOfUnknownEventMediaFiles() {
        return suffixForDestinationFolderOfUnknownEventMediaFiles;
    }

    String getSuffixForDestinationFolderOfMiscMediaFiles() {
        return suffixForDestinationFolderOfMiscMediaFiles;
    }
}
