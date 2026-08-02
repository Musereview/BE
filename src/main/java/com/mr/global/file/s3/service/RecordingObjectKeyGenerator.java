package com.mr.global.file.s3.service;

import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.file.s3.config.S3Properties;
import com.mr.global.file.s3.exception.S3ErrorStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Component
public class RecordingObjectKeyGenerator {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HHmmss");

    private static final int UUID_LENGTH = 6;

    private final S3Properties s3Properties;

    public RecordingObjectKeyGenerator(S3Properties s3Properties) {
        this.s3Properties = s3Properties;
    }

    public String generate(
            Long userId,
            String originalFileName,
            String contentType
    ) {
        String extension =
                resolveExtension(originalFileName, contentType);

        LocalDateTime now =
                LocalDateTime.now(KOREA_ZONE_ID);

        String dateFolder =
                now.format(DATE_FORMATTER);

        String time =
                now.format(TIME_FORMATTER);

        String shortUuid =
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, UUID_LENGTH);

        String generatedFileName =
                "%s_%s.%s".formatted(
                        time,
                        shortUuid,
                        extension
                );

        return "%s/%d/%s/%s".formatted(
                s3Properties.keyPrefix(),
                userId,
                dateFolder,
                generatedFileName
        );
    }

    public boolean belongsToUser(Long userId, String objectKey) {
        if (userId == null || objectKey == null) {
            return false;
        }

        String expectedPrefix = s3Properties.keyPrefix() + "/" + userId + "/";

        return objectKey.startsWith(expectedPrefix);
    }

    private String resolveExtension(
            String fileName,
            String contentType
    ) {
        if (fileName != null && fileName.contains(".")) {
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1)
                    .toLowerCase(Locale.ROOT);

            return validateExtension(extension);
        }

        if (contentType == null) {
            throw new GeneralException(S3ErrorStatus.UNSUPPORTED_CONTENT_TYPE);
        }

        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "audio/mpeg" -> "mp3";
            case "audio/wav", "audio/x-wav" -> "wav";
            default -> throw new GeneralException(S3ErrorStatus.UNSUPPORTED_CONTENT_TYPE);
        };
    }

    private String validateExtension(String extension) {
        return switch (extension) {
            case "mp3", "wav" -> extension;
            default -> throw new GeneralException(S3ErrorStatus.UNSUPPORTED_FILE_EXTENSION);
        };
    }
}
