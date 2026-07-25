package com.mr.domain.learning.dto.res;

import com.mr.domain.learning.entity.ChordExample;
import com.mr.domain.learning.entity.Learning;
import com.mr.domain.learning.entity.LearningStep;
import com.mr.domain.learning.entity.PlayingExample;

import java.util.List;

public class LearningStepDetailResponseDTO {

    public record StepDetailResultDTO(
            Long learningId,
            Long learningStepId,
            String learningTitle,
            String difficulty,
            Integer stepNo,
            String stepTitle,
            String theoryContent,
            String practiceTip,
            ModelPerformance modelPerformance,
            List<ChordExampleItem> chordExamples
    ) {
        public static StepDetailResultDTO of(Learning learning, LearningStep step,
                                             PlayingExample playingExample, List<ChordExample> chordExamples) {
            return new StepDetailResultDTO(
                    learning.getId(),
                    step.getId(),
                    learning.getTitle(),
                    learning.getDifficulty().name(),
                    step.getStepNo(),
                    step.getTitle(),
                    step.getContent(),
                    step.getPracticeTip(),
                    playingExample != null ? ModelPerformance.from(playingExample) : null,
                    chordExamples.stream().map(ChordExampleItem::from).toList()
            );
        }
    }

    public record ModelPerformance(
            String title,
            String description,
            String audioUrl,
            Integer durationSeconds
    ) {
        public static ModelPerformance from(PlayingExample playingExample) {
            return new ModelPerformance(
                    playingExample.getTitle(),
                    playingExample.getDescription(),
                    playingExample.getAudioFileUrl(),
                    playingExample.getPlayingSeconds() != null ? playingExample.getPlayingSeconds().intValue() : null
            );
        }
    }

    public record ChordExampleItem(
            String chordName,
            String description,
            List<Integer> noteNumbers
    ) {
        public static ChordExampleItem from(ChordExample chordExample) {
            return new ChordExampleItem(
                    chordExample.getChordName(),
                    chordExample.getDescription(),
                    chordExample.getNoteNumbers()
            );
        }
    }
}
