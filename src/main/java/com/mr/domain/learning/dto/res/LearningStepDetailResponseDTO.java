package com.mr.domain.learning.dto.res;

import com.mr.domain.learning.entity.ChordExample;
import com.mr.domain.learning.entity.Learning;
import com.mr.domain.learning.entity.LearningStep;
import com.mr.domain.learning.entity.PlayingExample;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class LearningStepDetailResponseDTO {

    @Schema(description = "학습 단계별 조회 응답")
    public record StepDetailResultDTO(
            @Schema(description = "패키지 ID", example = "1")
            Long learningId,

            @Schema(description = "단계별 학습 ID", example = "12")
            Long learningStepId,

            @Schema(description = "패키지 제목", example = "Tension Notes")
            String learningTitle,

            @Schema(description = "패키지 난이도", example = "ADVANCED")
            String difficulty,

            @Schema(description = "단계 순서", example = "2")
            Integer stepNo,

            @Schema(description = "단계 제목", example = "11th 텐션 노트 활용하기")
            String stepTitle,

            @Schema(description = "이론 설명 (마크다운 원문, 가공 없이 그대로 전달)",
                    example = "11th 텐션은 기본 코드톤 위에 쌓이는 확장음으로, 옥타브 위의 4도 음을 의미합니다.\n\n> 11th = 4도 + 옥타브\n> 텐션 계열: 9th(2도), 11th(4도), 13th(6도)")
            String theoryContent,

            @Schema(description = "연습 팁 (마크다운 원문, 가공 없이 그대로 전달)",
                    example = "- Cmaj7에서 11th(F)를 눌러보고 E와의 충돌을 직접 들어보세요.\n- Cm7에서는 같은 F가 어떻게 더 자연스럽게 들리는지 비교해보세요.")
            String practiceTip,

            @Schema(description = "모범 연주 예시(재생용). 등록된 예시가 없으면 null")
            ModelPerformance modelPerformance,

            @Schema(description = "코드별 가상 건반 예시. 없으면 빈 배열")
            List<ChordExampleItem> chordExamples
    ) {
        public static StepDetailResultDTO of(Learning learning, LearningStep step,
                                             PlayingExample playingExample, String audioUrl,
                                             List<ChordExample> chordExamples) {
            return new StepDetailResultDTO(
                    learning.getId(),
                    step.getId(),
                    learning.getTitle(),
                    learning.getDifficulty().name(),
                    step.getStepNo(),
                    step.getTitle(),
                    step.getContent(),
                    step.getPracticeTip(),
                    playingExample != null ? ModelPerformance.from(playingExample, audioUrl) : null,
                    chordExamples.stream().map(ChordExampleItem::from).toList()
            );
        }
    }

    @Schema(description = "모범 연주 예시")
    public record ModelPerformance(
            @Schema(description = "예시 제목", example = "11th Tension Notes Practice - 11th 텐션 활용 예제")
            String title,

            @Schema(description = "예시 설명", example = "프로 연주자의 응용 사례")
            String description,

            @Schema(
                    description = "비공개 S3 모범 연주 재생용 Presigned GET URL. 일정 시간 후 만료",
                    example = "https://example-bucket.s3.ap-northeast-2.amazonaws.com/playing_example/triads_step1.mp3?X-Amz-Expires=600&X-Amz-Signature=example"
            )
            String audioUrl,

            @Schema(description = "재생 시간(초)", example = "154")
            Integer durationSeconds
    ) {
        public static ModelPerformance from(PlayingExample playingExample, String audioUrl) {
            return new ModelPerformance(
                    playingExample.getTitle(),
                    playingExample.getDescription(),
                    audioUrl,
                    playingExample.getPlayingSeconds() != null ? playingExample.getPlayingSeconds().intValue() : null
            );
        }
    }

    @Schema(description = "코드별 가상 건반 예시")
    public record ChordExampleItem(
            @Schema(description = "코드명", example = "Cmaj7")
            String chordName,

            @Schema(description = "코드 설명", example = "F(11th) 주의 - E(3rd)와 충돌")
            String description,

            @Schema(description = "가상 건반에 표시할 MIDI 노트 번호 목록", example = "[60, 64, 67, 70, 77]")
            List<Integer> noteNumbers
    ) {
        public static ChordExampleItem from(ChordExample chordExample) {
            return new ChordExampleItem(
                    chordExample.getChordName(),
                    chordExample.getDescription(),
                    List.copyOf(chordExample.getNoteNumbers())
            );
        }
    }
}
