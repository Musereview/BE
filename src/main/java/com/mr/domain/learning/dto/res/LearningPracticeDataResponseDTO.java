package com.mr.domain.learning.dto.res;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.mr.domain.learning.entity.PlayingExample;

public class LearningPracticeDataResponseDTO {

    public record PracticeDataResultDTO(
            Integer bpm,
            String keySignature,
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
