package com.mr.domain.learning.dto.res;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.mr.domain.learning.entity.PlayingExample;
import io.swagger.v3.oas.annotations.media.Schema;

public class LearningPracticeDataResponseDTO {

    @Schema(description = "단계별 연습 실행 정보 조회 응답")
    public record PracticeDataResultDTO(
            @Schema(description = "연주 BPM", example = "90")
            Integer bpm,

            @Schema(description = "연주 조성(톤+장단조)", example = "C major")
            String keySignature,

            @Schema(description = "AI 채점용 + 악보 렌더링용 데이터 (실제 JSON 객체로 응답됨)",
                    example = "{\"recording\":[{\"midi\":60,\"velocity\":100,\"start\":0,\"duration\":1}],\"options\":{\"bpm\":120,\"beatsPerBar\":4,\"beatType\":4,\"key\":\"C\",\"mode\":\"major\",\"title\":\"테스트 연주\"},\"chords\":[{\"bar\":1,\"beat\":1,\"symbol\":\"Dm7\"},{\"bar\":2,\"beat\":1,\"symbol\":\"G7\"},{\"bar\":3,\"beat\":1,\"symbol\":\"Cmaj7\"}]}")
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
