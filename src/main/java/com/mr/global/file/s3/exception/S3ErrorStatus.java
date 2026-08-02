package com.mr.global.file.s3.exception;

import com.mr.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum S3ErrorStatus implements BaseCode {

    INVALID_FILE_SIZE(HttpStatus.BAD_REQUEST, "S3_400_01", "유효하지 않은 파일 크기입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "S3_400_02", "업로드 가능한 파일 크기를 초과했습니다."),
    UNSUPPORTED_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "S3_400_03", "지원하지 않는 오디오 Content-Type입니다."),
    UNSUPPORTED_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "S3_400_04", "지원하지 않는 파일 확장자입니다."),
    INVALID_OBJECT_KEY(HttpStatus.BAD_REQUEST, "S3_400_05", "접근할 수 없는 S3 Object Key입니다."),
    OBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "S3_404_01", "업로드된 파일을 찾을 수 없습니다."),
    FILE_SIZE_MISMATCH(HttpStatus.CONFLICT, "S3_409_01", "요청한 파일 크기와 업로드된 파일 크기가 일치하지 않습니다."),
    CONTENT_TYPE_MISMATCH(HttpStatus.CONFLICT, "S3_409_02", "요청한 Content-Type과 업로드된 파일의 Content-Type이 일치하지 않습니다."),
    PRESIGNED_URL_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3_500_01", "Pre-signed URL 생성에 실패했습니다."),
    OBJECT_VALIDATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3_500_02", "S3 객체 검증에 실패했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
