package com.mr.global.file.s3.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import com.mr.global.file.s3.enums.S3FileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.file.s3.config.S3Properties;
import com.mr.global.file.s3.exception.S3ErrorStatus;

class RecordingObjectKeyGeneratorTest {

    private static final String KEY_PREFIX = "recordings";
    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final S3FileType FILE_TYPE = S3FileType.RECORDING;

    private S3ObjectKeyGenerator objectKeyGenerator;

    @BeforeEach
    void setUp() {
        S3Properties s3Properties = new S3Properties(
                new S3Properties.Credentials(
                        "test-access-key",
                        "test-secret-key"
                ),
                "test-bucket",
                "ap-northeast-2",
                Duration.ofMinutes(10),
                30L * 1024 * 1024,
                KEY_PREFIX,
                Set.of(
                        "audio/mpeg",
                        "audio/wav",
                        "audio/x-wav"
                )
        );

        objectKeyGenerator =
                new S3ObjectKeyGenerator(s3Properties);
    }

    @Nested
    @DisplayName("S3 Object Key 생성")
    class GenerateObjectKey {

        @Test
        @DisplayName("MP3 파일의 Object Key를 생성한다")
        void generateMp3ObjectKey() {
            // given
            Long userId = 1L;

            LocalDate beforeGenerate =
                    LocalDate.now(KOREA_ZONE_ID);

            // when
            String objectKey = objectKeyGenerator.generate(
                    userId,
                    FILE_TYPE,
                    "recording.mp3",
                    "audio/mpeg"
            );

            LocalDate afterGenerate =
                    LocalDate.now(KOREA_ZONE_ID);

            // then
            assertThat(objectKey)
                    .startsWith(KEY_PREFIX + "/" + userId + "/")
                    .endsWith(".mp3");

            String beforeDate =
                    beforeGenerate.format(DATE_FORMATTER);

            String afterDate =
                    afterGenerate.format(DATE_FORMATTER);

            assertThat(objectKey)
                    .satisfiesAnyOf(
                            key -> assertThat(key)
                                    .startsWith(
                                            "%s/%d/%s/"
                                                    .formatted(
                                                            KEY_PREFIX,
                                                            userId,
                                                            beforeDate
                                                    )
                                    ),
                            key -> assertThat(key)
                                    .startsWith(
                                            "%s/%d/%s/"
                                                    .formatted(
                                                            KEY_PREFIX,
                                                            userId,
                                                            afterDate
                                                    )
                                    )
                    );

            assertThat(objectKey)
                    .matches(
                            "recordings/1/"
                                    + "\\d{4}-\\d{2}-\\d{2}/"
                                    + "\\d{6}_[0-9a-f]{6}\\.mp3"
                    );
        }

        @Test
        @DisplayName("WAV 파일의 Object Key를 생성한다")
        void generateWavObjectKey() {
            // given
            Long userId = 2L;

            // when
            String objectKey = objectKeyGenerator.generate(
                    userId,
                    FILE_TYPE,
                    "recording.wav",
                    "audio/wav"
            );

            // then
            assertThat(objectKey)
                    .startsWith(KEY_PREFIX + "/" + userId + "/")
                    .endsWith(".wav")
                    .matches(
                            "recordings/2/"
                                    + "\\d{4}-\\d{2}-\\d{2}/"
                                    + "\\d{6}_[0-9a-f]{6}\\.wav"
                    );
        }

        @Test
        @DisplayName("파일명에 확장자가 없으면 Content-Type으로 MP3 확장자를 결정한다")
        void generateMp3ExtensionFromContentType() {
            // given
            Long userId = 1L;

            // when
            String objectKey = objectKeyGenerator.generate(
                    userId,
                    FILE_TYPE,
                    "recording",
                    "audio/mpeg"
            );

            // then
            assertThat(objectKey)
                    .endsWith(".mp3")
                    .matches(
                            "recordings/1/"
                                    + "\\d{4}-\\d{2}-\\d{2}/"
                                    + "\\d{6}_[0-9a-f]{6}\\.mp3"
                    );
        }

        @Test
        @DisplayName("파일명에 확장자가 없으면 Content-Type으로 WAV 확장자를 결정한다")
        void generateWavExtensionFromContentType() {
            // given
            Long userId = 1L;

            // when
            String objectKey = objectKeyGenerator.generate(
                    userId,
                    FILE_TYPE,
                    "recording",
                    "audio/x-wav"
            );

            // then
            assertThat(objectKey)
                    .endsWith(".wav")
                    .matches(
                            "recordings/1/"
                                    + "\\d{4}-\\d{2}-\\d{2}/"
                                    + "\\d{6}_[0-9a-f]{6}\\.wav"
                    );
        }

        @Test
        @DisplayName("지원하지 않는 파일 확장자이면 예외가 발생한다")
        void generateUnsupportedFileExtension() {
            // when & then
            assertThatThrownBy(() ->
                    objectKeyGenerator.generate(
                            1L,
                            FILE_TYPE,
                            "recording.m4a",
                            "audio/mpeg"
                    )
            )
                    .isInstanceOf(GeneralException.class)
                    .satisfies(exception -> {
                        GeneralException generalException =
                                (GeneralException) exception;

                        assertThat(generalException.getCode())
                                .isEqualTo(
                                        S3ErrorStatus
                                                .UNSUPPORTED_FILE_EXTENSION
                                );
                    });
        }

        @Test
        @DisplayName("확장자가 없고 지원하지 않는 Content-Type이면 예외가 발생한다")
        void generateUnsupportedContentType() {
            // when & then
            assertThatThrownBy(() ->
                    objectKeyGenerator.generate(
                            1L,
                            FILE_TYPE,
                            "recording",
                            "audio/mp4"
                    )
            )
                    .isInstanceOf(GeneralException.class)
                    .satisfies(exception -> {
                        GeneralException generalException =
                                (GeneralException) exception;

                        assertThat(generalException.getCode())
                                .isEqualTo(
                                        S3ErrorStatus
                                                .UNSUPPORTED_CONTENT_TYPE
                                );
                    });
        }
    }

    @Nested
    @DisplayName("Object Key 소유자 검증")
    class ValidateObjectKeyOwner {

        @Test
        @DisplayName("현재 사용자의 Object Key이면 true를 반환한다")
        void belongsToUser() {
            // given
            Long userId = 1L;

            String objectKey =
                    "recordings/1/2026-08-02/031726_a12f9c.mp3";

            // when
            boolean result =
                    objectKeyGenerator.belongsToOwner(
                            userId,
                            FILE_TYPE,
                            objectKey
                    );

            // then
            assertThat(result)
                    .isTrue();
        }

        @Test
        @DisplayName("다른 사용자의 Object Key이면 false를 반환한다")
        void doesNotBelongToUser() {
            // given
            Long userId = 1L;

            String objectKey =
                    "recordings/2/2026-08-02/031726_a12f9c.mp3";

            // when
            boolean result =
                    objectKeyGenerator.belongsToOwner(
                            userId,
                            FILE_TYPE,
                            objectKey
                    );

            // then
            assertThat(result)
                    .isFalse();
        }

        @Test
        @DisplayName("사용자 ID가 null이면 false를 반환한다")
        void nullUserId() {
            assertThat(
                    objectKeyGenerator.belongsToOwner(
                            null,
                            FILE_TYPE,
                            "recordings/1/2026-08-02/test.mp3"
                    )
            )
                    .isFalse();
        }

        @Test
        @DisplayName("Object Key가 null이면 false를 반환한다")
        void nullObjectKey() {
            assertThat(
                    objectKeyGenerator.belongsToOwner(
                            1L,
                            FILE_TYPE,
                            null
                    )
            )
                    .isFalse();
        }

        @Test
        @DisplayName("Object Key가 빈 문자열이면 false를 반환한다")
        void blankObjectKey() {
            assertThat(
                    objectKeyGenerator.belongsToOwner(
                            1L,
                            FILE_TYPE,
                            " "
                    )
            )
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("Object Key 파일 타입 검증")
    class ValidateObjectKeyFileType {

        @Test
        @DisplayName("belongsToFileType - 파일 타입 prefix와 일치하면 true를 반환한다")
        void belongsToFileType_matchingPrefix_returnsTrue() {
            boolean result = objectKeyGenerator.belongsToFileType(
                    S3FileType.PLAYING_EXAMPLE,
                    "playing_example/triads_step1.mp3"
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("belongsToFileType - 파일 타입 prefix와 일치하지 않으면 false를 반환한다")
        void belongsToFileType_mismatchedPrefix_returnsFalse() {
            boolean result = objectKeyGenerator.belongsToFileType(
                    S3FileType.PLAYING_EXAMPLE,
                    "backing-tracks/1/triads_step1.mp3"
            );

            assertThat(result).isFalse();
        }
    }
}
