package com.mr.global.file.s3.service;

import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.file.s3.config.S3Properties;
import com.mr.global.file.s3.dto.req.RecordingPresignedUrlRequest;
import com.mr.global.file.s3.dto.req.RecordingUploadCompleteRequest;
import com.mr.global.file.s3.dto.res.RecordingPresignedUrlResponse;
import com.mr.global.file.s3.dto.res.RecordingUploadCompleteResponse;
import com.mr.global.file.s3.exception.S3ErrorStatus;
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
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingUploadService {

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final RecordingObjectKeyGenerator objectKeyGenerator;
    private final S3Presigner s3Presigner;

    public RecordingPresignedUrlResponse createPresignedUrl(
            Long userId, RecordingPresignedUrlRequest request
    ){
        // URL 발급 전에 파일 크기와 Content-Type 검증
        validateFile(request.fileSize(), request.contentType());

        // 사용자별 경로를 포함한 고유한 S3 Object Key 생성
        String objectKey = objectKeyGenerator.generate(
                userId,
                request.fileName(),
                request.contentType()
        );

        // 실제 S3 PUT 요청에 사용할 버킷, Object Key, Content-Type 설정
        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .bucket(s3Properties.bucket())
                        .key(objectKey)
                        .contentType(request.contentType())
                        .build();

        // Pre-signed URL의 만료 시간, 실제 PUT 요청 정보 설정
        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(s3Properties.presignedUrlExpiration())
                        .putObjectRequest(putObjectRequest)
                        .build();

        try {
            // 클라이언트가 S3에 직접 PUT 요청할 수 있는 URL 생성
            PresignedPutObjectRequest presignedRequest =
                    s3Presigner.presignPutObject(presignRequest);

            System.out.println("PutObject bucket = " + putObjectRequest.bucket());
            System.out.println("Presigned URL = " + presignedRequest.url());

            // 클라이언트에게 전달할 URL 만료 시각 계산 : 10분
            Instant expiresAt = Instant.now()
                    .plus(s3Properties.presignedUrlExpiration());

            return new RecordingPresignedUrlResponse(
                    objectKey,
                    presignedRequest.url().toString(),
                    expiresAt,
                    Map.of(
                            CONTENT_TYPE_HEADER,
                            request.contentType()
                    )
            );
        } catch (SdkException e) {
            throw new GeneralException(S3ErrorStatus.PRESIGNED_URL_CREATE_FAILED);
        }
    }

    // 클라이언트의 S3 직접 업로드가 완료된 후 실제 객체 검증
    public RecordingUploadCompleteResponse completeUpload(
            Long userId, RecordingUploadCompleteRequest request
    ){

        // 요청한 Object Key가 현재 사용자에게 속한 경로인지 검증
        validateObjectKey(userId, request.objectKey());

        // S3 HeadObject 요청으로 업로드된 객체의 메타데이터를 조회
        HeadObjectResponse headObject = getHeadObject(request.objectKey());

        // 요청 정보와 실제 S3 객체의 크기 및 Content-Type 비교
        validateUploadedObject(request, headObject);

        return new RecordingUploadCompleteResponse(
                request.objectKey(),
                headObject.contentLength(),
                headObject.contentType()
        );
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

    private void validateFile(
            Long fileSize,
            String contentType
    ) {
        if (fileSize == null || fileSize <= 0) {
            throw new GeneralException(S3ErrorStatus.INVALID_FILE_SIZE);
        }

        if (fileSize > s3Properties.maxFileSize()) {
            throw new GeneralException(S3ErrorStatus.FILE_SIZE_EXCEEDED);
        }

        if (!s3Properties.allowedContentTypes().contains(contentType)) {
            throw new GeneralException(S3ErrorStatus.UNSUPPORTED_CONTENT_TYPE);
        }
    }

    private void validateObjectKey(
            Long userId,
            String objectKey
    ) {
        if (!objectKeyGenerator.belongsToUser(userId, objectKey)) {
            throw new GeneralException(S3ErrorStatus.INVALID_OBJECT_KEY);
        }
    }

    private void validateUploadedObject(
            RecordingUploadCompleteRequest request,
            HeadObjectResponse headObject
    ){
        long uploadedSize = headObject.contentLength();
        String uploadedContentType = headObject.contentType();
        String objectKey = request.objectKey();

        if (uploadedSize <= 0) {
            deleteObject(objectKey);
            throw new GeneralException(S3ErrorStatus.INVALID_FILE_SIZE);
        }

        if (uploadedSize > s3Properties.maxFileSize()) {
            deleteObject(objectKey);
            throw new GeneralException(S3ErrorStatus.FILE_SIZE_EXCEEDED);
        }

        if (uploadedSize != request.fileSize()) {
            deleteObject(objectKey);
            throw new GeneralException(S3ErrorStatus.FILE_SIZE_MISMATCH);
        }

        if (!s3Properties.allowedContentTypes().contains(headObject.contentType())) {
            deleteObject(objectKey);
            throw new GeneralException(S3ErrorStatus.UNSUPPORTED_CONTENT_TYPE);
        }

        if (!Objects.equals(request.fileSize(), headObject.contentLength())) {
            deleteObject(objectKey);
            throw new GeneralException(S3ErrorStatus.FILE_SIZE_MISMATCH);
        }
    }

    // 업로드 검증에 실패한 객체를 S3에서 삭제
    private void deleteObject(String objectKey) {
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
}
