package com.mr.domain.learning.dto.res;

import com.mr.domain.learning.entity.Learning;
import com.mr.domain.learning.entity.LearningStep;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class LearningCurriculumResponseDTO {

    @Schema(description = "학습 커리큘럼 조회 응답")
    public record CurriculumResultDTO(
            @Schema(description = "패키지 ID", example = "1")
            Long learningId,

            @Schema(description = "패키지 제목", example = "Tension Notes")
            String title,

            @Schema(description = "패키지 부제목/요약", example = "코드에 9th, 11th, 13th를 더해 더 풍부한 화성을 만드는 방법을 배웁니다.")
            String subtitle,

            @Schema(description = "패키지 난이도", example = "ADVANCED")
            String difficulty,

            @Schema(description = "이론 설명 (마크다운 원문, 가공 없이 그대로 전달)",
                    example = "텐션 노트는 기본 3화음이나 7화음 위에 추가되는 9th, 11th, 13th 음정입니다. 이 음들은 코드의 색채를 풍부하게 만들며, 재즈와 현대 음악에서 필수적인 요소입니다.")
            String theoryContent,

            @Schema(description = "연습 팁 (마크다운 원문, 가공 없이 그대로 전달)",
                    example = "- Cmaj7에서 9th와 13th는 안전한 텐션입니다.\n- 11th는 메이저 코드에서 주의해야 합니다.\n- 도미넌트 7th 코드는 가장 다양한 텐션을 사용할 수 있습니다.")
            String practiceTip,

            @Schema(description = "패키지 전체 진행률")
            ProgressInfo progress,

            @Schema(description = "단계별 학습 목록(step_no 오름차순)")
            List<StepItem> steps
    ) {
        public static CurriculumResultDTO of(Learning learning, ProgressInfo progress, List<StepItem> steps) {
            return new CurriculumResultDTO(
                    learning.getId(),
                    learning.getTitle(),
                    learning.getSummary(),
                    learning.getDifficulty().name(),
                    learning.getContent(),
                    learning.getPracticeTip(),
                    progress,
                    steps
            );
        }
    }

    @Schema(description = "패키지 전체 진행률")
    public record ProgressInfo(
            @Schema(description = "완료된(score>=90) 단계 수", example = "1")
            Integer completedStepCount,

            @Schema(description = "전체 단계 수", example = "4")
            Integer totalStepCount,

            @Schema(description = "진행률(%), 0~100", example = "25")
            Integer progressRate
    ) {
        public static ProgressInfo of(long completedStepCount, long totalStepCount) {
            int progressRate;
            if (totalStepCount == 0) {
                progressRate = 0;
            } else {
                int rawProgressRate = (int) Math.round((double) completedStepCount / totalStepCount * 100);
                progressRate = Math.min(100, Math.max(0, rawProgressRate));
            }
            return new ProgressInfo((int) completedStepCount, (int) totalStepCount, progressRate);
        }
    }

    @Schema(description = "단계별 학습 항목")
    public record StepItem(
            @Schema(description = "단계별 학습 ID", example = "11")
            Long learningStepId,

            @Schema(description = "단계 순서", example = "1")
            Integer stepNo,

            @Schema(description = "단계 제목", example = "9th 텐션 노트 활용하기")
            String title,

            @Schema(description = "단계 설명", example = "코드에 자연스럽게 색채를 더하는 9th를 학습합니다.")
            String description,

            @Schema(description = "예상 소요 시간(분)", example = "10")
            Integer estimatedMinutes,

            @Schema(description = "단계별 학습 상태 (NOT_STARTED / RETRY / COMPLETED)", example = "COMPLETED")
            String status,

            @Schema(description = "해당 단계 최근 점수. 없으면 null", example = "93")
            Integer score
    ) {
        public static StepItem of(LearningStep step, String status, Integer score) {
            return new StepItem(
                    step.getId(),
                    step.getStepNo(),
                    step.getTitle(),
                    step.getSummary(),
                    step.getEstimatedMinutes(),
                    status,
                    score
            );
        }
    }
}
