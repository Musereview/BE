package com.mr.domain.backingtrack.exception;

import com.mr.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BackingTrackErrorStatus implements BaseCode {

    // [400] Validation Errors
    TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_01", "백킹트랙 제목은 필수입니다."),
    TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_02", "백킹트랙 제목은 50자 이내여야 합니다."),
    GENRE_REQUIRED(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_03", "장르는 필수입니다."),
    KEY_REQUIRED(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_05", "Key 정보는 필수입니다."),
    SCALE_REQUIRED(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_07", "조성은 필수입니다."),
    TIME_SIGNATURE_REQUIRED(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_09", "박자는 필수입니다."),
    BPM_REQUIRED(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_11", "BPM은 필수입니다."),
    PLAYTIME_REQUIRED(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_13", "재생 시간은 필수입니다."),
    ACCESS_LEVEL_REQUIRED(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_17", "공개 범위는 필수입니다."),
    INVALID_ACCESS_LEVEL(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_18", "공개 범위 값이 올바르지 않습니다."),
    LEVEL_REQUIRED(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_19", "난이도는 필수입니다."),
    INVALID_LEVEL(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_20", "난이도 값이 올바르지 않습니다."),
    DUPLICATE_CHORD_POSITION(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_21", "동일한 마디(measureNo)와 순서(sequenceNo)를 가진 코드 진행이 중복으로 존재합니다."),
    INVALID_CHORD_SEQUENCE(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_22", "마디의 허용된 코드 순서 범위를 초과했습니다."),
    INVALID_BACKING_TRACK_ID(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_23", "백킹트랙 ID가 1 미만이거나 형식이 올바르지 않습니다."),
    ANALYSIS_TRACK_MISMATCH(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_28", "분석 결과가 요청한 백킹트랙과 일치하지 않습니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_29", "커서 값이 올바르지 않습니다."),

    // [403] 권한 에러
    FORBIDDEN_UPDATE(HttpStatus.FORBIDDEN, "BACKING_TRACK_403_01", "백킹트랙 수정 권한이 없습니다."),
    FORBIDDEN_READ(HttpStatus.FORBIDDEN, "BACKING_TRACK_403_02", "백킹트랙 조회 권한이 없습니다."),

    // [404] 리소스 없음
    BACKING_TRACK_NOT_FOUND(HttpStatus.NOT_FOUND, "BACKING_TRACK_404_01", "존재하지 않는 백킹트랙입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
