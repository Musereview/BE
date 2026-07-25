package com.mr.domain.backingTrack.exception;

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
    UNSUPPORTED_GENRE(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_04", "지원하지 않는 장르입니다."),
    KEY_REQUIRED(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_05", "Key 정보는 필수입니다."),
    INVALID_KEY(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_06", "Key 정보가 올바르지 않습니다."),
    SCALE_REQUIRED(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_07", "조성은 필수입니다."),
    INVALID_SCALE(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_08", "조성 값이 올바르지 않습니다."),
    TIME_SIGNATURE_REQUIRED(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_09", "박자는 필수입니다."),
    INVALID_TIME_SIGNATURE(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_10", "박자 형식이 올바르지 않습니다."),
    BPM_REQUIRED(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_11", "BPM은 필수입니다."),
    INVALID_BPM(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_12", "BPM 값이 올바르지 않습니다."),
    PLAYTIME_REQUIRED(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_13", "재생 시간은 필수입니다."),
    INVALID_PLAYTIME(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_14", "재생 시간은 60초 이상 600초 이하여야 합니다."),
    ACCESS_LEVEL_REQUIRED(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_17", "공개 범위는 필수입니다."),
    INVALID_ACCESS_LEVEL(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_18", "공개 범위 값이 올바르지 않습니다."),
    LEVEL_REQUIRED(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_19", "난이도는 필수입니다."),
    INVALID_LEVEL(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_20", "난이도 값이 올바르지 않습니다."),
    DUPLICATE_CHORD_POSITION(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_21", "동일한 마디(measureNo)와 순서(sequenceNo)를 가진 코드 진행이 중복으로 존재합니다."),
    INVALID_CHORD_SEQUENCE(HttpStatus.BAD_REQUEST, "BACKING_TRACK_400_22", "마디의 허용된 코드 순서 범위를 초과했습니다."),

    // [403] 권한 에러
    FORBIDDEN_CREATE(HttpStatus.FORBIDDEN, "BACKING_TRACK_403_01", "백킹트랙 생성 권한이 없습니다."),
    FORBIDDEN_UPDATE(HttpStatus.FORBIDDEN, "BACKING_TRACK_403_02", "백킹트랙 수정 권한이 없습니다."),

    // [404] 리소스 없음
    BACKING_TRACK_NOT_FOUND(HttpStatus.NOT_FOUND, "BACKING_TRACK_404_01", "존재하지 않는 백킹트랙입니다."),

    // [500] 서버 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "BACKING_TRACK_500_01", "백킹트랙 생성 중 서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
