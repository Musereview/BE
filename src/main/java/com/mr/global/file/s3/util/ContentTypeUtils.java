package com.mr.global.file.s3.util;

import java.util.Locale;

public final class ContentTypeUtils {

    private ContentTypeUtils() {
    }

    public static String normalize(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }

        return contentType.split(";", 2)[0]
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
