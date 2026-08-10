package com.mr.domain.learning.dto.res;

import com.mr.domain.learning.entity.Learning;
import com.mr.domain.learning.entity.LearningStep;
import com.mr.domain.learning.entity.UserLearningProgress;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class LearningHomeResponseDTO {

    @Schema(description = "학습 홈 조회 응답")
    public record HomeResultDTO(
            @Schema(description = "최근 학습 이어서 하기. 학습 기록이 하나도 없으면 null")
            CurrentLearning currentLearning,

            @Schema(description = "가장 최근에 시도한 단계(재도전 포함). currentLearning과 달리 진행률 0%(재도전만 있는 경우)에도 채워지지만, 이어갈 단계가 없으면(패키지 100% 완료) currentLearning과 동일하게 null")
            RecentActivity recentActivity,

            @Schema(description = "난이도별 대표 학습 주제(THEORY), 최대 3개. 데이터가 없는 난이도는 제외되어 3개 미만일 수 있음")
            List<TheoryPackageItem> theoryPackages,

            @Schema(description = "대표 실전 반주법 패키지(ACCOMPANIMENT), 이름순 3개")
            List<LearningAccompanimentListResponseDTO.AccompanimentItem> accompanimentPackages
    ) {
        public static HomeResultDTO of(CurrentLearning currentLearning,
                                       RecentActivity recentActivity,
                                       List<TheoryPackageItem> theoryPackages,
                                       List<LearningAccompanimentListResponseDTO.AccompanimentItem> accompanimentPackages) {
            return new HomeResultDTO(currentLearning, recentActivity, theoryPackages, accompanimentPackages);
        }
    }

    @Schema(description = "가장 최근에 시도한 단계(완료 여부 무관, 재도전도 포함)")
    public record RecentActivity(
            @Schema(description = "패키지 ID", example = "1")
            Long learningId,

            @Schema(description = "마지막으로 시도한 단계 ID(표시용 — 무엇을 했는지)", example = "12")
            Long learningStepId,

            @Schema(description = "마지막으로 시도한 단계의 제목", example = "11th 텐션 노트 활용하기")
            String stepTitle,

            @Schema(description = "단계 상태. RETRY(재도전) 또는 COMPLETED(완료)", example = "RETRY")
            String status,

            @Schema(description = "이동용 — [이어서 학습하기] 클릭 시 이동할 단계 ID. RETRY면 그 단계 자신, COMPLETED면 currentLearning.nextStepId와 동일한 로직으로 계산된 다음 미완료 단계", example = "12")
            Long nextStepId
    ) {
        public static RecentActivity from(UserLearningProgress progress, Long nextStepId) {
            return new RecentActivity(
                    progress.getLearning().getId(),
                    progress.getLearningStep().getId(),
                    progress.getLearningStep().getTitle(),
                    progress.getLearningStatus(),
                    nextStepId
            );
        }
    }

    @Schema(description = "최근 학습 이어서 하기")
    public record CurrentLearning(
            @Schema(description = "패키지 ID", example = "1")
            Long learningId,

            @Schema(description = "패키지 제목", example = "Tension Notes")
            String title,

            @Schema(description = "패키지 난이도", example = "ADVANCED")
            String difficulty,

            @Schema(description = "마지막으로 학습한 단계의 제목", example = "11th 텐션 노트 활용하기")
            String stepTitle,

            @Schema(description = "패키지 진행률(%), 1~99 (0 또는 100이면 currentLearning 자체가 null)", example = "10")
            Integer progressRate,

            @Schema(description = "[이어서 학습하기] 클릭 시 이동할 단계 ID", example = "13")
            Long nextStepId
    ) {
        public static CurrentLearning of(Learning learning, String stepTitle, int progressRate, Long nextStepId) {
            return new CurrentLearning(
                    learning.getId(),
                    learning.getTitle(),
                    learning.getDifficulty().name(),
                    stepTitle,
                    progressRate,
                    nextStepId
            );
        }
    }

    @Schema(description = "추천 학습")
    public record RecommendedLearning(
            @Schema(description = "패키지 ID", example = "5")
            Long learningId,

            @Schema(description = "패키지 제목", example = "Tension Notes")
            String title,

            @Schema(description = "추천 단계 제목", example = "12th 텐션 노트 활용하기")
            String subtitle,

            @Schema(description = "패키지 난이도", example = "ADVANCED")
            String level,

            @Schema(description = "이동할 단계 ID", example = "14")
            Long nextStepId
    ) {
        public static RecommendedLearning of(Learning learning, LearningStep step) {
            return new RecommendedLearning(
                    learning.getId(),
                    learning.getTitle(),
                    step.getTitle(),
                    learning.getDifficulty().name(),
                    step.getId()
            );
        }
    }

    @Schema(description = "난이도별 대표 학습 주제")
    public record TheoryPackageItem(
            @Schema(description = "패키지 ID", example = "2")
            Long learningId,

            @Schema(description = "패키지 제목", example = "Diatonic Chords")
            String title,

            @Schema(description = "패키지 난이도", example = "BEGINNER")
            String difficulty,

            @Schema(description = "패키지 요약 설명", example = "스케일 안에서 만들어지는 코드의 관계를 이해합니다.")
            String summary
    ) {
        public static TheoryPackageItem from(Learning learning) {
            return new TheoryPackageItem(
                    learning.getId(),
                    learning.getTitle(),
                    learning.getDifficulty().name(),
                    learning.getSummary()
            );
        }
    }
}
