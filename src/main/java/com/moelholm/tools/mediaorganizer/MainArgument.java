package com.moelholm.tools.mediaorganizer;

public enum MainArgument {
    FILESYSTEM_TYPE("fileSystemType"),
    FROM_DIR("fromDir"),
    TO_DIR("toDir");

    MainArgument(String argumentName) {
        this.argumentName = argumentName;
    }

    private final String argumentName;

    public String getArgumentName() {
        return argumentName;
    }
}
