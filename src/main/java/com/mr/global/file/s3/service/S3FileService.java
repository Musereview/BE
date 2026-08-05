package com.mr.global.file.s3.service;

import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.file.s3.config.S3Properties;
import com.mr.global.file.s3.dto.FileUploadCommand;
import com.mr.global.file.s3.dto.ValidatedFile;
import com.mr.global.file.s3.dto.PresignedUrlUpload;
import com.mr.global.file.s3.exception.S3ErrorStatus;
import com.mr.global.file.s3.util.ContentTypeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileService {

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final S3ObjectKeyGenerator objectKeyGenerator;
    private final S3Presigner s3Presigner;

    /**
     * 클라이언트가 S3에 직접 파일을 업로드할 수 있도록
     * Presigned PUT URL을 발급합니다.
     */
    public PresignedUrlUpload createPresignedUpload(
            Long ownerId, FileUploadCommand command
    ){

        validateOwnerId(ownerId);
        validateUploadCommand(command);

        String normalizedContentType = ContentTypeUtils.normalize(command.contentType());

        String objectKey =
                objectKeyGenerator.generate(
                        ownerId,
                        command.originalFileName(),
                        normalizedContentType
                );

        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .bucket(s3Properties.bucket())
                        .key(objectKey)
                        .contentType(normalizedContentType)
                        .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(
                                s3Properties.presignedUrlExpiration()
                        )
                        .putObjectRequest(putObjectRequest)
                        .build();

        try {
            PresignedPutObjectRequest presignedRequest =
                    s3Presigner.presignPutObject(
                            presignRequest
                    );

            return new PresignedUrlUpload(
                    objectKey,
                    presignedRequest.url().toString(),
                    Instant.now().plus(
                            s3Properties
                                    .presignedUrlExpiration()
                    ),
                    Map.of(
                            CONTENT_TYPE_HEADER,
                            normalizedContentType
                    )
            );
        } catch (SdkException exception) {
            log.error(
                    "S3 Presigned URL 발급에 실패했습니다. ownerId={}, objectKey={}",
                    ownerId,
                    objectKey,
                    exception
            );

            throw new GeneralException(
                    S3ErrorStatus.PRESIGNED_URL_CREATE_FAILED
            );
        }
    }

    /**
     * 클라이언트가 업로드한 S3 객체를 검증합니다.
     *
     * 검증 항목:
     * - Object Key가 현재 사용자 소유 경로인지
     * - S3 객체가 실제로 존재하는지
     * - 실제 파일 크기가 허용 범위인지
     * - 실제 Content-Type이 허용된 형식인지
     */
    public ValidatedFile validateUploadedFile(
            Long ownerId,
            String objectKey
    ) {
        validateOwnerId(ownerId);
        validateObjectKey(ownerId, objectKey);

        HeadObjectResponse headObject = getHeadObject(objectKey);

        try {
            validateUploadedMetadata(
                    headObject.contentLength(),
                    headObject.contentType()
            );
        } catch (GeneralException exception) {
            deleteObject(objectKey);
            throw exception;
        }

        String normalizedContentType =
                ContentTypeUtils.normalize(
                        headObject.contentType()
                );

        return new ValidatedFile(
                objectKey,
                buildFileUrl(objectKey),
                headObject.contentLength(),
                normalizedContentType
        );
    }

    /**
     * S3 객체를 삭제합니다.
     *
     * 검증에 실패한 객체 정리에 사용하며,
     * 삭제 실패가 기존 비즈니스 예외를 덮지 않도록
     * 삭제 오류는 로그만 남깁니다.
     */
    public void deleteObject(String objectKey) {

        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(objectKey)
                .build();

        try {
            s3Client.deleteObject(deleteRequest);
        } catch (SdkException e) {
            log.error("검증에 실패한 S3 객체 삭제에 실패했습니다. objectKey={}", objectKey, e);
        }
    }

    private HeadObjectResponse getHeadObject(String objectKey) {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(objectKey)
                .build();

        try {
            return s3Client.headObject(request);
        } catch (S3Exception e) {
            // 요청한 Object Key에 해당하는 객체가 존재하지 않음
            if (e.statusCode() == 404) {
                throw new GeneralException(S3ErrorStatus.OBJECT_NOT_FOUND);
            }
            // 권한, 버킷 설정, 네트워크 외 S3 응답 오류 등
            throw new GeneralException(S3ErrorStatus.OBJECT_VALIDATION_FAILED);
        } catch (SdkException e) {
            throw new GeneralException(S3ErrorStatus.OBJECT_VALIDATION_FAILED);
        }
    }

    private void validateOwnerId(Long ownerId) {
        if (ownerId == null || ownerId <= 0) {
            throw new GeneralException(
                    S3ErrorStatus.INVALID_OBJECT_KEY
            );
        }
    }

    private void validateUploadCommand(
            FileUploadCommand command
    ) {
        if (command == null) {
            throw new GeneralException(
                    S3ErrorStatus.INVALID_FILE_SIZE
            );
        }

        validateFileSize(command.fileSize());

        validateAllowedContentType(
                command.contentType()
        );
    }

    private void validateUploadedMetadata(
            long fileSize,
            String contentType
    ) {
        validateFileSize(fileSize);
        validateAllowedContentType(contentType);
    }

    private void validateFileSize(long fileSize) {
        if (fileSize <= 0) {
            throw new GeneralException(
                    S3ErrorStatus.INVALID_FILE_SIZE
            );
        }

        if (fileSize > s3Properties.maxFileSize()) {
            throw new GeneralException(
                    S3ErrorStatus.FILE_SIZE_EXCEEDED
            );
        }
    }

    private void validateAllowedContentType(String contentType) {
        String normalizedContentType =
                ContentTypeUtils.normalize(contentType);

        if (normalizedContentType == null
                || !s3Properties.allowedContentTypes().contains(normalizedContentType)) {
            throw new GeneralException(S3ErrorStatus.UNSUPPORTED_CONTENT_TYPE);
        }
    }

    private void validateObjectKey(
            Long ownerId,
            String objectKey
    ) {

        if (objectKey == null || objectKey.isBlank()) {
            throw new GeneralException(
                    S3ErrorStatus.INVALID_OBJECT_KEY
            );
        }

        if (!objectKeyGenerator.belongsToOwner(ownerId, objectKey)) {
            throw new GeneralException(S3ErrorStatus.INVALID_OBJECT_KEY);
        }
    }

    private String buildFileUrl(String objectKey) {
        String fileUrl = s3Client.utilities()
                .getUrl(builder -> builder
                        .bucket(s3Properties.bucket())
                        .key(objectKey))
                .toString();

        return fileUrl;
    }
}
