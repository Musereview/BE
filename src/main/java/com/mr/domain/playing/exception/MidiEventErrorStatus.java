package com.mr.domain.playing.exception;

import com.mr.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MidiEventErrorStatus implements BaseCode {

    INVALID_PLAYING_ID(HttpStatus.BAD_REQUEST, "MIDI_400_01", "연주 ID가 올바르지 않습니다."),
    EMPTY_MIDI_EVENTS(HttpStatus.BAD_REQUEST,"MIDI_400_02", "MIDI 이벤트 목록은 필수입니다."),
    INVALID_MIDI_TYPE(HttpStatus.BAD_REQUEST, "MIDI_400_03", "MIDI 이벤트 타입이 올바르지 않습니다."),
    INVALID_PITCH_RANGE(HttpStatus.BAD_REQUEST, "MIDI_400_04", "피치 값은 0~127 사이의 값이어야 합니다."),
    INVALID_VELOCITY_RANGE(HttpStatus.BAD_REQUEST, "MIDI_400_05", "강도는 0~127 사이의 값이어야 합니다."),
    INVALID_TIMESTAMP(HttpStatus.BAD_REQUEST, "MIDI_400_06", "MIDI 이벤트 타임스탬프는 0 이상이어야 합니다."),
    INVALID_MIDI_EVENT(HttpStatus.BAD_REQUEST,"MIDI_400_07", "MIDI 이벤트 목록에 유효하지 않은 값이 포함되어 있습니다."),
    INVALID_MIDI_SEQUENCE(HttpStatus.BAD_REQUEST, "MIDI_400_08", "MIDI 이벤트 순서(sequence) 값이 유효하지 않습니다."),
    EXCEEDED_MIDI_EVENT_COUNT(HttpStatus.BAD_REQUEST, "MIDI_400_09", "MIDI 이벤트 개수가 허용 범위를 초과했습니다."),
    DUPLICATE_MIDI_SEQUENCE(HttpStatus.BAD_REQUEST, "MIDI_400_10", "동일한 시간에 중복된 MIDI sequence 값이 존재합니다."),

    PLAYING_NOT_IN_PROGRESS(HttpStatus.CONFLICT, "MIDI_409_01", "진행 중인 연주 세션에만 MIDI 이벤트를 저장할 수 있습니다."),
    MIDI_SAVE_REQUEST_TOO_FREQUENT(HttpStatus.TOO_MANY_REQUESTS, "MIDI_429_01", "연주 완료 요청은 1분에 한 번만 가능합니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
