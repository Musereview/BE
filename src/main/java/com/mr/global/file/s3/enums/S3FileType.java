package com.mr.global.file.s3.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum S3FileType {

    RECORDING("recordings"),
    BACKING_TRACK("backing-tracks"),
    PLAYING_EXAMPLE("playing_example");

    private final String prefix;
}
