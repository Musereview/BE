package com.mr.domain.learning.dto.res;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.mr.domain.learning.entity.PlayingExample;
import io.swagger.v3.oas.annotations.media.Schema;

public class LearningPracticeDataResponseDTO {

    @Schema(description = "단계별 연습 실행 정보 조회 응답")
    public record PracticeDataResultDTO(
            @Schema(description = "연주 BPM", example = "90")
            Integer bpm,

            @Schema(description = "연주 조성", example = "C")
            String keySignature,

            @Schema(description = "AI 채점용 MIDI 데이터 (실제 JSON 객체로 응답됨)", example = "{\"notes\": [60, 64, 67]}")
            @JsonRawValue String midiData
    ) {
        public static PracticeDataResultDTO from(PlayingExample playingExample) {
            return new PracticeDataResultDTO(
                    playingExample.getBpm(),
                    playingExample.getKeySignature(),
                    playingExample.getMidiData()
            );
        }
    }
}
