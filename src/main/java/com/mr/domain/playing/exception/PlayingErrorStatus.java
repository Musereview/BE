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
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
