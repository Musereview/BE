package com.mr.domain.playing.exception;

import com.mr.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PlayingErrorStatus implements BaseCode {

    MISSING_USER_ID(HttpStatus.BAD_REQUEST, "PLAYING_400_01", "연주를 기록할 유저 정보(ID)가 누락되었습니다."),
    INVALID_BPM_RANGE(HttpStatus.BAD_REQUEST, "PLAYING_400_02", "BPM은 50~200 사이의 값이어야 합니다."),
    MISSING_BACKING_TRACK_ID(HttpStatus.BAD_REQUEST, "PLAYING_400_03", "백킹트랙 연주 모드에서는 백킹트랙 ID가 필수입니다."),
    MISSING_PLAYING(HttpStatus.BAD_REQUEST, "PLAYING_400_04", "MIDI 이벤트를 기록할 연주 정보가 누락되었습니다."),
    MISSING_MIDI_TYPE(HttpStatus.BAD_REQUEST, "PLAYING_400_05", "MIDI Type은 필수 입력 값입니다."),
    INVALID_PITCH_RANGE(HttpStatus.BAD_REQUEST, "PLAYING_400_06", "피치 값은 0~127 사이의 값이어야 합니다."),
    INVALID_VELOCITY_RANGE(HttpStatus.BAD_REQUEST, "PLAYING_400_07", "강도는 0~127 사이의 값이어야 합니다."),
    INVALID_TIMESTAMP(HttpStatus.BAD_REQUEST, "PLAYING_400_08", "MIDI 이벤트 타임스탬프는 0 이상이어야 합니다."),
    EMPTY_MIDI_EVENTS(HttpStatus.BAD_REQUEST, "PLAYING_400_09", "저장할 MIDI 이벤트가 없습니다."),
    INVALID_MIDI_EVENT(HttpStatus.BAD_REQUEST,"PLAYING_400_10", "MIDI 이벤트 목록에 유효하지 않은 값이 포함되어 있습니다."),
    MISSING_PLAYING_MODE(HttpStatus.BAD_REQUEST, "PLAYING_400_11", "연주 모드는 필수 입력값입니다."),
    MISSING_PLAYING_STATUS(HttpStatus.BAD_REQUEST, "PLAYING_400_12", "연주 상태는 필수 입력값입니다."),
    UNSUPPORTED_PLAYING_MODE(HttpStatus.BAD_REQUEST, "PLAYING_400_13", "현재 지원하지 않는 연주 모드입니다."),
    EXCEEDED_MIDI_EVENT_COUNT(HttpStatus.BAD_REQUEST, "PLAYING_400_14", "MIDI 이벤트 개수가 허용 범위를 초과했습니다."),
    INVALID_MIDI_SEQUENCE(HttpStatus.BAD_REQUEST, "PLAYING_400_15", "MIDI 이벤트 순서(sequence) 값이 유효하지 않습니다."),
    DUPLICATE_MIDI_SEQUENCE(HttpStatus.BAD_REQUEST, "PLAYING_400_16", "중복된 MIDI sequence 값이 존재합니다."),

    PLAYING_ACCESS_DENIED(HttpStatus.FORBIDDEN, "PLAYING_403_01", "해당 연주 기록에 접근할 수 없습니다."),

    PLAYING_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAYING_404_01", "연주 기록을 찾을 수 없습니다."),

    INVALID_PLAYING_STATUS(HttpStatus.CONFLICT, "PLAYING_409_01", "현재 연주 상태에서는 요청한 작업을 수행할 수 없습니다."),
    MISSING_PLAYING_START_TIME(HttpStatus.CONFLICT, "PLAYING_409_02", "연주 시작 시간이 기록되지 않았습니다."),
    INVALID_PLAYING_DURATION(HttpStatus.CONFLICT, "PLAYING_409_03", "연주 종료 시간이 시작 시간보다 이전일 수 없습니다."),

    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
