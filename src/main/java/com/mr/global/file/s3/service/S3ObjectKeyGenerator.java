package com.mr.global.file.s3.service;

import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.file.s3.config.S3Properties;
import com.mr.global.file.s3.enums.S3FileType;
import com.mr.global.file.s3.exception.S3ErrorStatus;
import com.mr.global.file.s3.util.ContentTypeUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Component
public class S3ObjectKeyGenerator {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(KOREA_ZONE_ID);

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HHmmss").withZone(KOREA_ZONE_ID);

    private static final int UUID_LENGTH = 6;

    private final S3Properties s3Properties;

    public S3ObjectKeyGenerator(S3Properties s3Properties) {
        this.s3Properties = s3Properties;
    }

    // 사용자별 S3 Object Key를 생성
    // 생성 예시1 : recordings/1/2026-08-05/152310_a1b2c3.webm
    // 생성 예시2 : backing-tracks/1/2026-08-05/152310_a1b2c3.webm
    public String generate(
            Long userId,
            S3FileType fileType,
            String originalFileName,
            String contentType
    ) {
        String extension =
                resolveExtension(originalFileName, contentType);

        Instant now =
                Instant.now();

        String generatedFileName =
                "%s_%s.%s".formatted(
                        TIME_FORMATTER.format(now),
                        createShortUuid(),
                        extension
                );

        return "%s/%d/%s/%s".formatted(
                fileType.getPrefix(),
                userId,
                DATE_FORMATTER.format(now),
                generatedFileName
        );
    }

    public boolean belongsToOwner(Long ownerId, S3FileType fileType, String objectKey) {
        if (ownerId == null || objectKey == null) {
            return false;
        }

        String expectedPrefix = fileType.getPrefix() + "/" + ownerId + "/";

        return objectKey.startsWith(expectedPrefix);
    }

    public boolean belongsToFileType(S3FileType fileType, String objectKey) {
        if (fileType == null || objectKey == null) {
            return false;
        }

        return objectKey.startsWith(fileType.getPrefix() + "/");
    }

    private String createShortUuid() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, UUID_LENGTH);
    }

    private String resolveExtension(
            String originalFileName,
            String contentType
    ) {

        String extensionByContentType = resolveExtensionByContentType(contentType);

        // 파일명 확장자와 Content-Type이 서로 다른 요청을 거부해 메타데이터 불일치를 막는다.
        String fileExtension = extractExtension(originalFileName);
        if (fileExtension != null && !fileExtension.equalsIgnoreCase(extensionByContentType)) {
            throw new GeneralException(S3ErrorStatus.UNSUPPORTED_FILE_EXTENSION);
        }

        return extensionByContentType;
    }

    private String extractExtension(String originalFileName) {
        if (originalFileName == null
                || originalFileName.isBlank()
                || !originalFileName.contains(".")) {
            return null;
        }

        int lastDotIndex =
                originalFileName.lastIndexOf('.');

        if (lastDotIndex == originalFileName.length() - 1) {
            return null;
        }

        return originalFileName
                .substring(lastDotIndex + 1)
                .toLowerCase(Locale.ROOT);
    }

    private String resolveExtensionByContentType(
            String contentType
    ) {
        String normalizedContentType =
                ContentTypeUtils.normalize(contentType);

        if (normalizedContentType == null) {
            throw new GeneralException(
                    S3ErrorStatus.UNSUPPORTED_CONTENT_TYPE
            );
        }

        return switch (normalizedContentType) {
            case "audio/mpeg" -> "mp3";
            case "audio/wav", "audio/x-wav" -> "wav";
            case "audio/webm" -> "webm";
            case "audio/ogg" -> "ogg";

            default -> throw new GeneralException(
                    S3ErrorStatus.UNSUPPORTED_CONTENT_TYPE
            );
        };
    }
}
