package com.mr.global.file.s3.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.file.s3.config.S3Properties;
import com.mr.global.file.s3.dto.FileUploadCommand;
import com.mr.global.file.s3.dto.PresignedUrlUpload;
import com.mr.global.file.s3.dto.ValidatedFile;
import com.mr.global.file.s3.exception.S3ErrorStatus;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class S3FileServiceTest {

    private static final Long OWNER_ID = 1L;

    private static final String BUCKET = "test-bucket";
    private static final String REGION = "ap-northeast-2";
    private static final String KEY_PREFIX = "recordings";

    private static final String CONTENT_TYPE = "audio/mpeg";
    private static final String NORMALIZED_WEBM_CONTENT_TYPE =
            "audio/webm";

    private static final String OBJECT_KEY =
            "recordings/1/2026-08-05/174500_test.mp3";

    private static final String OTHER_OWNER_OBJECT_KEY =
            "recordings/2/2026-08-05/174500_test.mp3";

    private static final String FILE_URL =
            "https://"
                    + BUCKET
                    + ".s3."
                    + REGION
                    + ".amazonaws.com/"
                    + OBJECT_KEY;

    private static final String UPLOAD_URL =
            FILE_URL
                    + "?X-Amz-Algorithm=AWS4-HMAC-SHA256"
                    + "&X-Amz-Signature=test-signature";

    private static final long FILE_SIZE = 1_024L;
    private static final long MAX_FILE_SIZE =
            30L * 1_024 * 1_024;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3ObjectKeyGenerator objectKeyGenerator;

    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;

    @Mock
    private PresignedGetObjectRequest presignedGetObjectRequest;

    private S3FileService s3FileService;

    @BeforeEach
    void setUp() {
        S3Properties s3Properties =
                new S3Properties(
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
                                "audio/x-wav",
                                "audio/webm",
                                "audio/ogg"
                        )
                );

        s3FileService =
                new S3FileService(
                        s3Client,
                        s3Properties,
                        objectKeyGenerator,
                        s3Presigner
                );
    }

    @Nested
    @DisplayName("Presigned PUT URL 발급")
    class CreatePresignedUpload {

        @Test
        @DisplayName("유효한 파일 정보이면 Presigned PUT URL을 생성한다")
        void createPresignedUpload_success() throws Exception {
            // given
            FileUploadCommand command =
                    new FileUploadCommand(
                            "recording.mp3",
                            CONTENT_TYPE,
                            FILE_SIZE
                    );

            when(objectKeyGenerator.generate(
                    OWNER_ID,
                    command.originalFileName(),
                    CONTENT_TYPE
            )).thenReturn(OBJECT_KEY);

            when(s3Presigner.presignPutObject(
                    any(PutObjectPresignRequest.class)
            )).thenReturn(presignedPutObjectRequest);

            when(presignedPutObjectRequest.url())
                    .thenReturn(
                            URI.create(UPLOAD_URL).toURL()
                    );

            // when
            PresignedUrlUpload response =
                    s3FileService.createPresignedUpload(
                            OWNER_ID,
                            command
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

            assertThat(
                    capturedRequest
                            .putObjectRequest()
                            .bucket()
            ).isEqualTo(BUCKET);

            assertThat(
                    capturedRequest
                            .putObjectRequest()
                            .key()
            ).isEqualTo(OBJECT_KEY);

            assertThat(
                    capturedRequest
                            .putObjectRequest()
                            .contentType()
            ).isEqualTo(CONTENT_TYPE);
        }

        @Test
        @DisplayName("Content-Type 파라미터를 제거한 뒤 Presigned URL을 생성한다")
        void createPresignedUpload_normalizesContentType()
                throws Exception {
            // given
            FileUploadCommand command =
                    new FileUploadCommand(
                            "recording.webm",
                            "audio/webm;codecs=opus",
                            FILE_SIZE
                    );

            when(objectKeyGenerator.generate(
                    OWNER_ID,
                    command.originalFileName(),
                    NORMALIZED_WEBM_CONTENT_TYPE
            )).thenReturn(OBJECT_KEY);

            when(s3Presigner.presignPutObject(
                    any(PutObjectPresignRequest.class)
            )).thenReturn(presignedPutObjectRequest);

            when(presignedPutObjectRequest.url())
                    .thenReturn(
                            URI.create(UPLOAD_URL).toURL()
                    );

            // when
            PresignedUrlUpload response =
                    s3FileService.createPresignedUpload(
                            OWNER_ID,
                            command
                    );

            // then
            assertThat(response.requiredHeaders())
                    .containsExactly(
                            Map.entry(
                                    "Content-Type",
                                    NORMALIZED_WEBM_CONTENT_TYPE
                            )
                    );

            verify(objectKeyGenerator)
                    .generate(
                            OWNER_ID,
                            command.originalFileName(),
                            NORMALIZED_WEBM_CONTENT_TYPE
                    );

            ArgumentCaptor<PutObjectPresignRequest> captor =
                    ArgumentCaptor.forClass(
                            PutObjectPresignRequest.class
                    );

            verify(s3Presigner)
                    .presignPutObject(captor.capture());

            assertThat(
                    captor.getValue()
                            .putObjectRequest()
                            .contentType()
            ).isEqualTo(NORMALIZED_WEBM_CONTENT_TYPE);
        }

        @Test
        @DisplayName("사용자 ID가 null이면 예외가 발생한다")
        void createPresignedUpload_nullOwnerId() {
            // given
            FileUploadCommand command =
                    new FileUploadCommand(
                            "recording.mp3",
                            CONTENT_TYPE,
                            FILE_SIZE
                    );

            // when & then
            assertGeneralException(
                    () -> s3FileService.createPresignedUpload(
                            null,
                            command
                    ),
                    S3ErrorStatus.INVALID_OBJECT_KEY
            );

            verify(objectKeyGenerator, never())
                    .generate(
                            anyLong(),
                            anyString(),
                            anyString()
                    );

            verify(s3Presigner, never())
                    .presignPutObject(
                            any(PutObjectPresignRequest.class)
                    );
        }

        @Test
        @DisplayName("사용자 ID가 0 이하이면 예외가 발생한다")
        void createPresignedUpload_invalidOwnerId() {
            // given
            FileUploadCommand command =
                    new FileUploadCommand(
                            "recording.mp3",
                            CONTENT_TYPE,
                            FILE_SIZE
                    );

            // when & then
            assertGeneralException(
                    () -> s3FileService.createPresignedUpload(
                            0L,
                            command
                    ),
                    S3ErrorStatus.INVALID_OBJECT_KEY
            );

            verify(objectKeyGenerator, never())
                    .generate(
                            anyLong(),
                            anyString(),
                            anyString()
                    );
        }

        @Test
        @DisplayName("업로드 명령이 null이면 예외가 발생한다")
        void createPresignedUpload_nullCommand() {
            // when & then
            assertGeneralException(
                    () -> s3FileService.createPresignedUpload(
                            OWNER_ID,
                            null
                    ),
                    S3ErrorStatus.INVALID_FILE_SIZE
            );

            verify(objectKeyGenerator, never())
                    .generate(
                            anyLong(),
                            anyString(),
                            anyString()
                    );
        }

        @Test
        @DisplayName("파일 크기가 0 이하이면 예외가 발생한다")
        void createPresignedUpload_invalidFileSize() {
            // given
            FileUploadCommand command =
                    new FileUploadCommand(
                            "recording.mp3",
                            CONTENT_TYPE,
                            0L
                    );

            // when & then
            assertGeneralException(
                    () -> s3FileService.createPresignedUpload(
                            OWNER_ID,
                            command
                    ),
                    S3ErrorStatus.INVALID_FILE_SIZE
            );

            verify(objectKeyGenerator, never())
                    .generate(
                            anyLong(),
                            anyString(),
                            anyString()
                    );

            verify(s3Presigner, never())
                    .presignPutObject(
                            any(PutObjectPresignRequest.class)
                    );
        }

        @Test
        @DisplayName("파일 크기가 최대 허용 크기를 초과하면 예외가 발생한다")
        void createPresignedUpload_fileSizeExceeded() {
            // given
            FileUploadCommand command =
                    new FileUploadCommand(
                            "recording.mp3",
                            CONTENT_TYPE,
                            MAX_FILE_SIZE + 1
                    );

            // when & then
            assertGeneralException(
                    () -> s3FileService.createPresignedUpload(
                            OWNER_ID,
                            command
                    ),
                    S3ErrorStatus.FILE_SIZE_EXCEEDED
            );

            verify(objectKeyGenerator, never())
                    .generate(
                            anyLong(),
                            anyString(),
                            anyString()
                    );

            verify(s3Presigner, never())
                    .presignPutObject(
                            any(PutObjectPresignRequest.class)
                    );
        }

        @Test
        @DisplayName("지원하지 않는 Content-Type이면 예외가 발생한다")
        void createPresignedUpload_unsupportedContentType() {
            // given
            FileUploadCommand command =
                    new FileUploadCommand(
                            "image.png",
                            "image/png",
                            FILE_SIZE
                    );

            // when & then
            assertGeneralException(
                    () -> s3FileService.createPresignedUpload(
                            OWNER_ID,
                            command
                    ),
                    S3ErrorStatus.UNSUPPORTED_CONTENT_TYPE
            );

            verify(objectKeyGenerator, never())
                    .generate(
                            anyLong(),
                            anyString(),
                            anyString()
                    );

            verify(s3Presigner, never())
                    .presignPutObject(
                            any(PutObjectPresignRequest.class)
                    );
        }

        @Test
        @DisplayName("Presigned URL 발급 중 SDK 오류가 발생하면 예외가 발생한다")
        void createPresignedUpload_sdkException() {
            // given
            FileUploadCommand command =
                    new FileUploadCommand(
                            "recording.mp3",
                            CONTENT_TYPE,
                            FILE_SIZE
                    );

            when(objectKeyGenerator.generate(
                    OWNER_ID,
                    command.originalFileName(),
                    CONTENT_TYPE
            )).thenReturn(OBJECT_KEY);

            when(s3Presigner.presignPutObject(
                    any(PutObjectPresignRequest.class)
            )).thenThrow(
                    SdkClientException.create(
                            "Presigned URL creation failed"
                    )
            );

            // when & then
            assertGeneralException(
                    () -> s3FileService.createPresignedUpload(
                            OWNER_ID,
                            command
                    ),
                    S3ErrorStatus.PRESIGNED_URL_CREATE_FAILED
            );
        }
    }

    @Nested
    @DisplayName("업로드된 S3 파일 검증")
    class ValidateUploadedFile {

        @Test
        @DisplayName("업로드된 객체가 유효하면 검증된 파일 정보를 반환한다")
        void validateUploadedFile_success() throws Exception {
            // given
            HeadObjectResponse headObjectResponse =
                    HeadObjectResponse.builder()
                            .contentLength(FILE_SIZE)
                            .contentType(CONTENT_TYPE)
                            .build();

            when(objectKeyGenerator.belongsToOwner(
                    OWNER_ID,
                    OBJECT_KEY
            )).thenReturn(true);

            when(s3Client.headObject(
                    any(HeadObjectRequest.class)
            )).thenReturn(headObjectResponse);

            mockFileUrl();

            // when
            ValidatedFile response =
                    s3FileService.validateUploadedFile(
                            OWNER_ID,
                            OBJECT_KEY
                    );

            // then
            assertThat(response.objectKey())
                    .isEqualTo(OBJECT_KEY);

            assertThat(response.fileUrl())
                    .isEqualTo(FILE_URL);

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

            verify(objectKeyGenerator)
                    .belongsToOwner(
                            OWNER_ID,
                            OBJECT_KEY
                    );
        }

        @Test
        @DisplayName("업로드된 객체의 Content-Type을 정규화해서 반환한다")
        void validateUploadedFile_normalizesContentType()
                throws Exception {
            // given
            HeadObjectResponse headObjectResponse =
                    HeadObjectResponse.builder()
                            .contentLength(FILE_SIZE)
                            .contentType(
                                    "audio/webm;codecs=opus"
                            )
                            .build();

            when(objectKeyGenerator.belongsToOwner(
                    OWNER_ID,
                    OBJECT_KEY
            )).thenReturn(true);

            when(s3Client.headObject(
                    any(HeadObjectRequest.class)
            )).thenReturn(headObjectResponse);

            mockFileUrl();

            // when
            ValidatedFile response =
                    s3FileService.validateUploadedFile(
                            OWNER_ID,
                            OBJECT_KEY
                    );

            // then
            assertThat(response.contentType())
                    .isEqualTo(
                            NORMALIZED_WEBM_CONTENT_TYPE
                    );
        }

        @Test
        @DisplayName("사용자 ID가 유효하지 않으면 예외가 발생한다")
        void validateUploadedFile_invalidOwnerId() {
            // when & then
            assertGeneralException(
                    () -> s3FileService.validateUploadedFile(
                            null,
                            OBJECT_KEY
                    ),
                    S3ErrorStatus.INVALID_OBJECT_KEY
            );

            verify(objectKeyGenerator, never())
                    .belongsToOwner(
                            anyLong(),
                            anyString()
                    );

            verify(s3Client, never())
                    .headObject(
                            any(HeadObjectRequest.class)
                    );
        }

        @Test
        @DisplayName("Object Key가 null이면 예외가 발생한다")
        void validateUploadedFile_nullObjectKey() {
            // when & then
            assertGeneralException(
                    () -> s3FileService.validateUploadedFile(
                            OWNER_ID,
                            null
                    ),
                    S3ErrorStatus.INVALID_OBJECT_KEY
            );

            verify(objectKeyGenerator, never())
                    .belongsToOwner(
                            anyLong(),
                            anyString()
                    );

            verify(s3Client, never())
                    .headObject(
                            any(HeadObjectRequest.class)
                    );
        }

        @Test
        @DisplayName("Object Key가 빈 문자열이면 예외가 발생한다")
        void validateUploadedFile_blankObjectKey() {
            // when & then
            assertGeneralException(
                    () -> s3FileService.validateUploadedFile(
                            OWNER_ID,
                            " "
                    ),
                    S3ErrorStatus.INVALID_OBJECT_KEY
            );

            verify(objectKeyGenerator, never())
                    .belongsToOwner(
                            anyLong(),
                            anyString()
                    );
        }

        @Test
        @DisplayName("다른 사용자의 Object Key이면 예외가 발생한다")
        void validateUploadedFile_invalidObjectKey() {
            // given
            when(objectKeyGenerator.belongsToOwner(
                    OWNER_ID,
                    OTHER_OWNER_OBJECT_KEY
            )).thenReturn(false);

            // when & then
            assertGeneralException(
                    () -> s3FileService.validateUploadedFile(
                            OWNER_ID,
                            OTHER_OWNER_OBJECT_KEY
                    ),
                    S3ErrorStatus.INVALID_OBJECT_KEY
            );

            verify(s3Client, never())
                    .headObject(
                            any(HeadObjectRequest.class)
                    );
        }

        @Test
        @DisplayName("S3 객체를 찾을 수 없으면 예외가 발생한다")
        void validateUploadedFile_objectNotFound() {
            // given
            S3Exception exception =
                    (S3Exception) S3Exception.builder()
                            .statusCode(404)
                            .message("Object not found")
                            .build();

            when(objectKeyGenerator.belongsToOwner(
                    OWNER_ID,
                    OBJECT_KEY
            )).thenReturn(true);

            when(s3Client.headObject(
                    any(HeadObjectRequest.class)
            )).thenThrow(exception);

            // when & then
            assertGeneralException(
                    () -> s3FileService.validateUploadedFile(
                            OWNER_ID,
                            OBJECT_KEY
                    ),
                    S3ErrorStatus.OBJECT_NOT_FOUND
            );
        }

        @Test
        @DisplayName("S3가 404 이외 오류를 반환하면 객체 검증 실패 예외가 발생한다")
        void validateUploadedFile_s3Exception() {
            // given
            S3Exception exception =
                    (S3Exception) S3Exception.builder()
                            .statusCode(403)
                            .message("Access denied")
                            .build();

            when(objectKeyGenerator.belongsToOwner(
                    OWNER_ID,
                    OBJECT_KEY
            )).thenReturn(true);

            when(s3Client.headObject(
                    any(HeadObjectRequest.class)
            )).thenThrow(exception);

            // when & then
            assertGeneralException(
                    () -> s3FileService.validateUploadedFile(
                            OWNER_ID,
                            OBJECT_KEY
                    ),
                    S3ErrorStatus.OBJECT_VALIDATION_FAILED
            );
        }

        @Test
        @DisplayName("S3 SDK 오류가 발생하면 객체 검증 실패 예외가 발생한다")
        void validateUploadedFile_sdkException() {
            // given
            when(objectKeyGenerator.belongsToOwner(
                    OWNER_ID,
                    OBJECT_KEY
            )).thenReturn(true);

            when(s3Client.headObject(
                    any(HeadObjectRequest.class)
            )).thenThrow(
                    SdkClientException.create(
                            "S3 connection failed"
                    )
            );

            // when & then
            assertGeneralException(
                    () -> s3FileService.validateUploadedFile(
                            OWNER_ID,
                            OBJECT_KEY
                    ),
                    S3ErrorStatus.OBJECT_VALIDATION_FAILED
            );
        }

        @Test
        @DisplayName("업로드된 객체 크기가 0 이하이면 삭제하고 예외가 발생한다")
        void validateUploadedFile_invalidFileSize() {
            // given
            HeadObjectResponse headObjectResponse =
                    HeadObjectResponse.builder()
                            .contentLength(0L)
                            .contentType(CONTENT_TYPE)
                            .build();

            when(objectKeyGenerator.belongsToOwner(
                    OWNER_ID,
                    OBJECT_KEY
            )).thenReturn(true);

            when(s3Client.headObject(
                    any(HeadObjectRequest.class)
            )).thenReturn(headObjectResponse);

            // when & then
            assertGeneralException(
                    () -> s3FileService.validateUploadedFile(
                            OWNER_ID,
                            OBJECT_KEY
                    ),
                    S3ErrorStatus.INVALID_FILE_SIZE
            );

            verifyDeleteObject();
        }

        @Test
        @DisplayName("업로드된 객체가 최대 크기를 초과하면 삭제하고 예외가 발생한다")
        void validateUploadedFile_fileSizeExceeded() {
            // given
            HeadObjectResponse headObjectResponse =
                    HeadObjectResponse.builder()
                            .contentLength(
                                    MAX_FILE_SIZE + 1
                            )
                            .contentType(CONTENT_TYPE)
                            .build();

            when(objectKeyGenerator.belongsToOwner(
                    OWNER_ID,
                    OBJECT_KEY
            )).thenReturn(true);

            when(s3Client.headObject(
                    any(HeadObjectRequest.class)
            )).thenReturn(headObjectResponse);

            // when & then
            assertGeneralException(
                    () -> s3FileService.validateUploadedFile(
                            OWNER_ID,
                            OBJECT_KEY
                    ),
                    S3ErrorStatus.FILE_SIZE_EXCEEDED
            );

            verifyDeleteObject();
        }

        @Test
        @DisplayName("업로드된 객체의 Content-Type이 허용되지 않으면 삭제하고 예외가 발생한다")
        void validateUploadedFile_unsupportedContentType() {
            // given
            HeadObjectResponse headObjectResponse =
                    HeadObjectResponse.builder()
                            .contentLength(FILE_SIZE)
                            .contentType("image/png")
                            .build();

            when(objectKeyGenerator.belongsToOwner(
                    OWNER_ID,
                    OBJECT_KEY
            )).thenReturn(true);

            when(s3Client.headObject(
                    any(HeadObjectRequest.class)
            )).thenReturn(headObjectResponse);

            // when & then
            assertGeneralException(
                    () -> s3FileService.validateUploadedFile(
                            OWNER_ID,
                            OBJECT_KEY
                    ),
                    S3ErrorStatus.UNSUPPORTED_CONTENT_TYPE
            );

            verifyDeleteObject();
        }
    }

    @Nested
    @DisplayName("S3 객체 삭제")
    class DeleteObject {

        @Test
        @DisplayName("Object Key가 유효하면 S3 객체를 삭제한다")
        void deleteObject_success() {
            // when
            s3FileService.deleteObject(OBJECT_KEY);

            // then
            ArgumentCaptor<DeleteObjectRequest> captor =
                    ArgumentCaptor.forClass(
                            DeleteObjectRequest.class
                    );

            verify(s3Client)
                    .deleteObject(captor.capture());

            assertThat(captor.getValue().bucket())
                    .isEqualTo(BUCKET);

            assertThat(captor.getValue().key())
                    .isEqualTo(OBJECT_KEY);
        }

        @Test
        @DisplayName("Object Key가 null이면 삭제 요청을 하지 않는다")
        void deleteObject_nullObjectKey() {
            // when
            s3FileService.deleteObject(null);

            // then
            verify(s3Client, never())
                    .deleteObject(
                            any(DeleteObjectRequest.class)
                    );
        }

        @Test
        @DisplayName("Object Key가 빈 문자열이면 삭제 요청을 하지 않는다")
        void deleteObject_blankObjectKey() {
            // when
            s3FileService.deleteObject(" ");

            // then
            verify(s3Client, never())
                    .deleteObject(
                            any(DeleteObjectRequest.class)
                    );
        }

        @Test
        @DisplayName("S3 삭제 중 SDK 오류가 발생해도 예외를 전파하지 않는다")
        void deleteObject_sdkException() {
            // given
            when(s3Client.deleteObject(
                    any(DeleteObjectRequest.class)
            )).thenThrow(
                    SdkClientException.create(
                            "Delete failed"
                    )
            );

            // when
            s3FileService.deleteObject(OBJECT_KEY);

            // then
            verify(s3Client)
                    .deleteObject(
                            any(DeleteObjectRequest.class)
                    );
        }
    }

    @Nested
    @DisplayName("Presigned GET URL 발급")
    class CreatePresignedDownload {

        @Test
        @DisplayName("소유자의 Object Key이면 Presigned GET URL을 발급한다")
        void createPresignedDownloadUrl_success()
                throws Exception {
            // given
            String downloadUrl =
                    "https://example.com/presigned-download-url";

            when(objectKeyGenerator.belongsToOwner(
                    OWNER_ID,
                    OBJECT_KEY
            )).thenReturn(true);

            when(s3Presigner.presignGetObject(
                    any(GetObjectPresignRequest.class)
            )).thenReturn(presignedGetObjectRequest);

            when(presignedGetObjectRequest.url())
                    .thenReturn(
                            URI.create(downloadUrl).toURL()
                    );

            // when
            String response =
                    s3FileService.createPresignedDownload(
                            OWNER_ID,
                            OBJECT_KEY
                    );

            // then
            assertThat(response)
                    .isEqualTo(downloadUrl);

            ArgumentCaptor<GetObjectPresignRequest> captor =
                    ArgumentCaptor.forClass(
                            GetObjectPresignRequest.class
                    );

            verify(s3Presigner)
                    .presignGetObject(captor.capture());

            GetObjectPresignRequest capturedRequest =
                    captor.getValue();

            assertThat(capturedRequest.signatureDuration())
                    .isEqualTo(Duration.ofMinutes(10));

            assertThat(
                    capturedRequest
                            .getObjectRequest()
                            .bucket()
            ).isEqualTo(BUCKET);

            assertThat(
                    capturedRequest
                            .getObjectRequest()
                            .key()
            ).isEqualTo(OBJECT_KEY);

            verify(objectKeyGenerator)
                    .belongsToOwner(
                            OWNER_ID,
                            OBJECT_KEY
                    );
        }

        @Test
        @DisplayName("다른 소유자의 Object Key이면 Presigned GET URL을 발급하지 않는다")
        void createPresignedDownload_invalidObjectKey() {
            // given
            when(objectKeyGenerator.belongsToOwner(
                    OWNER_ID,
                    OTHER_OWNER_OBJECT_KEY
            )).thenReturn(false);

            // when & then
            assertGeneralException(
                    () -> s3FileService.createPresignedDownload(
                            OWNER_ID,
                            OTHER_OWNER_OBJECT_KEY
                    ),
                    S3ErrorStatus.INVALID_OBJECT_KEY
            );

            verify(s3Presigner, never())
                    .presignGetObject(
                            any(GetObjectPresignRequest.class)
                    );
        }

        @Test
        @DisplayName("Presigned GET URL 발급 중 SDK 오류가 발생하면 예외가 발생한다")
        void createPresignedDownload_sdkException() {
            // given
            when(objectKeyGenerator.belongsToOwner(
                    OWNER_ID,
                    OBJECT_KEY
            )).thenReturn(true);

            when(s3Presigner.presignGetObject(
                    any(GetObjectPresignRequest.class)
            )).thenThrow(
                    SdkClientException.create(
                            "Presigned GET URL creation failed"
                    )
            );

            // when & then
            assertGeneralException(
                    () -> s3FileService.createPresignedDownload(
                            OWNER_ID,
                            OBJECT_KEY
                    ),
                    S3ErrorStatus.PRESIGNED_URL_CREATE_FAILED
            );
        }
    }

    private void mockFileUrl() throws Exception {
        S3Utilities s3Utilities =
                mock(S3Utilities.class);

        when(s3Client.utilities())
                .thenReturn(s3Utilities);

        when(s3Utilities.getUrl(
                org.mockito.ArgumentMatchers
                        .<Consumer<GetUrlRequest.Builder>>any()
        )).thenReturn(
                URI.create(FILE_URL).toURL()
        );
    }

    private void verifyDeleteObject() {
        ArgumentCaptor<DeleteObjectRequest> captor =
                ArgumentCaptor.forClass(
                        DeleteObjectRequest.class
                );

        verify(s3Client)
                .deleteObject(captor.capture());

        assertThat(captor.getValue().bucket())
                .isEqualTo(BUCKET);

        assertThat(captor.getValue().key())
                .isEqualTo(OBJECT_KEY);
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