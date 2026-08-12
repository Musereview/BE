package com.mr.global.file.s3.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum S3FileType {

    RECORDING("recordings", true),
    BACKING_TRACK("backing-tracks", true),
    PLAYING_EXAMPLE("playing_example", false);

    private final String prefix;
    private final boolean ownerScoped;
}
