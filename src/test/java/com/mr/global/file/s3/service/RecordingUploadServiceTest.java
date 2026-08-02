package com.mr.global.file.s3.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.file.s3.config.S3Properties;
import com.mr.global.file.s3.dto.req.RecordingPresignedUrlRequest;
import com.mr.global.file.s3.dto.req.RecordingUploadCompleteRequest;
import com.mr.global.file.s3.dto.res.RecordingPresignedUrlResponse;
import com.mr.global.file.s3.dto.res.RecordingUploadCompleteResponse;
import com.mr.global.file.s3.exception.S3ErrorStatus;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class RecordingUploadServiceTest {

    private static final Long USER_ID = 1L;

    private static final String BUCKET = "test-bucket";
    private static final String REGION = "ap-northeast-2";
    private static final String KEY_PREFIX = "recordings";

    private static final String CONTENT_TYPE = "audio/mpeg";
    private static final String OTHER_CONTENT_TYPE = "audio/wav";

    private static final String OBJECT_KEY =
            "recordings/1/2026-08-02/033746_e90683.mp3";

    private static final String RECORDING_FILE_URL =
            "https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/" + OBJECT_KEY;
    private static final String UPLOAD_URL =
            RECORDING_FILE_URL
                    + "?X-Amz-Algorithm=AWS4-HMAC-SHA256"
                    + "&X-Amz-Signature=test-signature";

    private static final Long FILE_SIZE = 1024L;
    private static final Long MAX_FILE_SIZE = 30L * 1024 * 1024;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private RecordingObjectKeyGenerator objectKeyGenerator;

    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;

    private RecordingUploadService recordingUploadService;

    @BeforeEach
    void setUp() {
        S3Properties s3Properties = new S3Properties(
                new S3Properties.Credentials(
                        "test-access-key",
                        "test-secret-key"
                ),
                BUCKET,
                REGION,
                Duration.ofMinutes(10),
                MAX_FILE_SIZE,
                KEY_PREFIX,
                Set.of(
                        "audio/mpeg",
                        "audio/wav",
                        "audio/x-wav"
                )
        );

        recordingUploadService = new RecordingUploadService(
                s3Client,
                s3Properties,
                objectKeyGenerator,
                s3Presigner
        );
    }

    @Nested
    @DisplayName("Pre-signed PUT URL 생성")
    class CreatePresignedUrl {

        @Test
        @DisplayName("유효한 파일 정보로 Pre-signed PUT URL을 생성한다")
        void createPresignedUrlSuccess() throws Exception {
            // given
            RecordingPresignedUrlRequest request =
                    new RecordingPresignedUrlRequest(
                            "recording.mp3",
                            CONTENT_TYPE,
                            FILE_SIZE
                    );

            when(objectKeyGenerator.generate(
                    USER_ID,
                    request.fileName(),
                    request.contentType()
            )).thenReturn(OBJECT_KEY);

            when(s3Presigner.presignPutObject(
                    any(PutObjectPresignRequest.class)
            )).thenReturn(presignedPutObjectRequest);

            when(presignedPutObjectRequest.url())
                    .thenReturn(URI.create(UPLOAD_URL).toURL());

            // when
            RecordingPresignedUrlResponse response =
                    recordingUploadService.createPresignedUrl(
                            USER_ID,
                            request
                    );

            // then
            assertThat(response.objectKey())
                    .isEqualTo(OBJECT_KEY);

            assertThat(response.uploadUrl())
                    .isEqualTo(UPLOAD_URL);

            assertThat(response.expiresAt())
                    .isNotNull();

            assertThat(response.requiredHeaders())
                    .containsExactly(
                            Map.entry(
                                    "Content-Type",
                                    CONTENT_TYPE
                            )
                    );

            ArgumentCaptor<PutObjectPresignRequest> captor =
                    ArgumentCaptor.forClass(
                            PutObjectPresignRequest.class
                    );

            verify(s3Presigner)
                    .presignPutObject(captor.capture());

            PutObjectPresignRequest capturedRequest =
                    captor.getValue();

            assertThat(capturedRequest.signatureDuration())
                    .isEqualTo(Duration.ofMinutes(10));

            assertThat(capturedRequest.putObjectRequest().bucket())
                    .isEqualTo(BUCKET);

            assertThat(capturedRequest.putObjectRequest().key())
                    .isEqualTo(OBJECT_KEY);

            assertThat(capturedRequest.putObjectRequest().contentType())
                    .isEqualTo(CONTENT_TYPE);
        }

        @Test
        @DisplayName("파일 크기가 최대 허용 크기를 초과하면 예외가 발생한다")
        void createPresignedUrlFileSizeExceeded() {
            // given
            RecordingPresignedUrlRequest request =
                    new RecordingPresignedUrlRequest(
                            "recording.mp3",
                            CONTENT_TYPE,
                            MAX_FILE_SIZE + 1
                    );

            // when & then
            assertGeneralException(
                    () -> recordingUploadService.createPresignedUrl(
                            USER_ID,
                            request
                    ),
                    S3ErrorStatus.FILE_SIZE_EXCEEDED
            );

            verify(objectKeyGenerator, never())
                    .generate(
                            any(),
                            any(),
                            any()
                    );

            verify(s3Presigner, never())
                    .presignPutObject(
                            any(PutObjectPresignRequest.class)
                    );
        }

        @Test
        @DisplayName("지원하지 않는 Content-Type이면 예외가 발생한다")
        void createPresignedUrlUnsupportedContentType() {
            // given
            RecordingPresignedUrlRequest request =
                    new RecordingPresignedUrlRequest(
                            "recording.png",
                            "image/png",
                            FILE_SIZE
                    );

            // when & then
            assertGeneralException(
                    () -> recordingUploadService.createPresignedUrl(
                            USER_ID,
                            request
                    ),
                    S3ErrorStatus.UNSUPPORTED_CONTENT_TYPE
            );

            verify(objectKeyGenerator, never())
                    .generate(
                            any(),
                            any(),
                            any()
                    );

            verify(s3Presigner, never())
                    .presignPutObject(
                            any(PutObjectPresignRequest.class)
                    );
        }
    }

    @Nested
    @DisplayName("S3 업로드 완료 검증")
    class CompleteUpload {

        @Test
        @DisplayName("업로드된 객체 정보가 요청 정보와 일치하면 완료 응답을 반환한다")
        void completeUploadSuccess() {
            // given
            RecordingUploadCompleteRequest request =
                    new RecordingUploadCompleteRequest(
                            OBJECT_KEY,
                            CONTENT_TYPE,
                            FILE_SIZE
                    );

            HeadObjectResponse headObjectResponse =
                    HeadObjectResponse.builder()
                            .contentLength(FILE_SIZE)
                            .contentType(CONTENT_TYPE)
                            .build();

            when(objectKeyGenerator.belongsToUser(
                    USER_ID,
                    OBJECT_KEY
            )).thenReturn(true);

            when(s3Client.headObject(
                    any(HeadObjectRequest.class)
            )).thenReturn(headObjectResponse);

            // when
            RecordingUploadCompleteResponse response =
                    recordingUploadService.completeUpload(
                            USER_ID,
                            request
                    );

            // then
            assertThat(response.recordingFileUrl())
                    .isEqualTo(RECORDING_FILE_URL);

            assertThat(response.fileSize())
                    .isEqualTo(FILE_SIZE);

            assertThat(response.contentType())
                    .isEqualTo(CONTENT_TYPE);

            ArgumentCaptor<HeadObjectRequest> captor =
                    ArgumentCaptor.forClass(
                            HeadObjectRequest.class
                    );

            verify(s3Client)
                    .headObject(captor.capture());

            HeadObjectRequest capturedRequest =
                    captor.getValue();

            assertThat(capturedRequest.bucket())
                    .isEqualTo(BUCKET);

            assertThat(capturedRequest.key())
                    .isEqualTo(OBJECT_KEY);
        }

        @Test
        @DisplayName("다른 사용자의 Object Key이면 예외가 발생한다")
        void completeUploadInvalidObjectKey() {
            // given
            RecordingUploadCompleteRequest request =
                    new RecordingUploadCompleteRequest(
                            "recordings/2/2026-08-02/test.mp3",
                            CONTENT_TYPE,
                            FILE_SIZE
                    );

            when(objectKeyGenerator.belongsToUser(
                    USER_ID,
                    request.objectKey()
            )).thenReturn(false);

            // when & then
            assertGeneralException(
                    () -> recordingUploadService.completeUpload(
                            USER_ID,
                            request
                    ),
                    S3ErrorStatus.INVALID_OBJECT_KEY
            );

            verify(s3Client, never())
                    .headObject(any(HeadObjectRequest.class));
        }

        @Test
        @DisplayName("요청한 파일 크기와 업로드된 파일 크기가 다르면 예외가 발생한다")
        void completeUploadFileSizeMismatch() {
            // given
            RecordingUploadCompleteRequest request =
                    new RecordingUploadCompleteRequest(
                            OBJECT_KEY,
                            CONTENT_TYPE,
                            FILE_SIZE
                    );

            HeadObjectResponse headObjectResponse =
                    HeadObjectResponse.builder()
                            .contentLength(FILE_SIZE + 1)
                            .contentType(CONTENT_TYPE)
                            .build();

            when(objectKeyGenerator.belongsToUser(
                    USER_ID,
                    OBJECT_KEY
            )).thenReturn(true);

            when(s3Client.headObject(
                    any(HeadObjectRequest.class)
            )).thenReturn(headObjectResponse);

            // when & then
            assertGeneralException(
                    () -> recordingUploadService.completeUpload(
                            USER_ID,
                            request
                    ),
                    S3ErrorStatus.FILE_SIZE_MISMATCH
            );
        }

        @Test
        @DisplayName("요청한 Content-Type과 업로드된 객체의 Content-Type이 다르면 예외가 발생한다")
        void completeUploadContentTypeMismatch() {
            // given
            RecordingUploadCompleteRequest request =
                    new RecordingUploadCompleteRequest(
                            OBJECT_KEY,
                            CONTENT_TYPE,
                            FILE_SIZE
                    );

            HeadObjectResponse headObjectResponse =
                    HeadObjectResponse.builder()
                            .contentLength(FILE_SIZE)
                            .contentType(OTHER_CONTENT_TYPE)
                            .build();

            when(objectKeyGenerator.belongsToUser(
                    USER_ID,
                    OBJECT_KEY
            )).thenReturn(true);

            when(s3Client.headObject(
                    any(HeadObjectRequest.class)
            )).thenReturn(headObjectResponse);

            // when & then
            assertGeneralException(
                    () -> recordingUploadService.completeUpload(
                            USER_ID,
                            request
                    ),
                    S3ErrorStatus.CONTENT_TYPE_MISMATCH
            );
        }
    }

    private void assertGeneralException(
            Runnable executable,
            S3ErrorStatus expectedErrorStatus
    ) {
        assertThatThrownBy(executable::run)
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> {
                    GeneralException generalException =
                            (GeneralException) exception;

                    assertThat(generalException.getCode())
                            .isEqualTo(expectedErrorStatus);
                });
    }
}