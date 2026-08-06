package com.mr.global.file.s3.service;

import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.file.s3.config.S3Properties;
import com.mr.global.file.s3.exception.S3ErrorStatus;
import com.mr.global.file.s3.util.ContentTypeUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Component
public class S3ObjectKeyGenerator {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HHmmss");

    private static final int UUID_LENGTH = 6;

    private final S3Properties s3Properties;

    public S3ObjectKeyGenerator(S3Properties s3Properties) {
        this.s3Properties = s3Properties;
    }

    // 사용자별 S3 Object Key를 생성
    // 생성 예시: recordings/1/2026-08-05/152310_a1b2c3.webm
    public String generate(
            Long userId,
            String originalFileName,
            String contentType
    ) {
        String extension =
                resolveExtension(originalFileName, contentType);

        LocalDateTime now =
                LocalDateTime.now(KOREA_ZONE_ID);


        String generatedFileName =
                "%s_%s.%s".formatted(
                        now.format(TIME_FORMATTER),
                        createShortUuid(),
                        extension
                );

        return "%s/%d/%s/%s".formatted(
                s3Properties.keyPrefix(),
                userId,
                now.format(DATE_FORMATTER),
                generatedFileName
        );
    }

    public boolean belongsToOwner(Long ownerId, String objectKey) {
        if (ownerId == null || objectKey == null) {
            return false;
        }

        String expectedPrefix = s3Properties.keyPrefix() + "/" + ownerId + "/";

        return objectKey.startsWith(expectedPrefix);
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

        // contentType 정규화 및 대표 확장자 추출
        String extensionByContentType = resolveExtensionByContentType(contentType);

        // originalFileName에 확장자가 있는 경우, contentType 기준 확장자와 일치하는지 검증 (선택적)
        String fileExtension = extractExtension(originalFileName);
        if (fileExtension != null && !fileExtension.equalsIgnoreCase(extensionByContentType)) {
            throw new GeneralException(S3ErrorStatus.UNSUPPORTED_FILE_EXTENSION);
        }

        // 최종적으로 contentType 기반의 올바른 대표 확장자 반환
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
