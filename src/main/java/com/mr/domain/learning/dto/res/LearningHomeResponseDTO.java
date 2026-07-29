package com.mr.domain.learning.dto.res;

import com.mr.domain.learning.entity.Learning;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class LearningHomeResponseDTO {

    @Schema(description = "학습 홈 조회 응답")
    public record HomeResultDTO(
            @Schema(description = "최근 학습 이어서 하기. 학습 기록이 하나도 없으면 null")
            CurrentLearning currentLearning,

            @Schema(description = "난이도별 대표 학습 주제(THEORY), 최대 3개. 데이터가 없는 난이도는 제외되어 3개 미만일 수 있음")
            List<TheoryPackageItem> theoryPackages,

            @Schema(description = "대표 실전 반주법 패키지(ACCOMPANIMENT), 이름순 3개")
            List<LearningAccompanimentListResponseDTO.AccompanimentItem> accompanimentPackages
    ) {
        public static HomeResultDTO of(CurrentLearning currentLearning,
                                       List<TheoryPackageItem> theoryPackages,
                                       List<LearningAccompanimentListResponseDTO.AccompanimentItem> accompanimentPackages) {
            return new HomeResultDTO(currentLearning, theoryPackages, accompanimentPackages);
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
