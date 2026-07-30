package com.mr.domain.statistics.dto.res;

import com.mr.domain.statistics.entity.enums.SkillType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "통계 화면 조회 응답")
public record StatisticsResponseDTO(
        WeeklySummary weeklySummary,
        List<DomainGrowth> domainGrowth,
        WeeklyTrend weeklyTrend
) {

    @Schema(description = "이번 주 연주 요약")
    public record WeeklySummary(
            @Schema(description = "이번 주 평균 정확도(COMPLETED Analysis의 totalScore 평균), 데이터 없으면 0", example = "91.0")
            BigDecimal accuracy,

            @Schema(description = "이번 주 총 연습 시간(분), COMPLETED Playing 기준", example = "65")
            int practiceMinutes,

            @Schema(description = "이번 주 완료 세션 수, COMPLETED Playing 기준", example = "6")
            int completedSessionCount,

            @Schema(description = "전 주 대비 정확도 변화량, 이번/지난 주 중 데이터 없으면 0", example = "4.0")
            BigDecimal accuracyDiff,

            @Schema(description = "전 주 대비 연습 시간 변화량(분)", example = "18")
            int practiceMinutesDiff,

            @Schema(description = "전 주 대비 완료 세션 수 변화량", example = "2")
            int completedSessionCountDiff
    ) {
    }

    @Schema(description = "영역별 성장 변화")
    public record DomainGrowth(
            @Schema(description = "영역 코드")
            SkillType domain,

            @Schema(description = "화면 표시용 영역 이름", example = "스케일")
            String label,

            @Schema(description = "이번 주 평균 점수, 데이터 없으면 0", example = "85.0")
            BigDecimal currentScore,

            @Schema(description = "지난 주 평균 점수, 데이터 없으면 0", example = "77.0")
            BigDecimal previousScore,

            @Schema(description = "currentScore - previousScore, 둘 중 하나라도 데이터 없으면 0", example = "8.0")
            BigDecimal diff
    ) {
    }

    @Schema(description = "최근 4주 평균 점수 추이")
    public record WeeklyTrend(
            @Schema(description = "전 주 대비 이번 주 평균 점수 변화량(반올림), 데이터 없으면 0", example = "9")
            int diffFromPreviousWeek,

            @Schema(description = "3주 전, 2주 전, 지난주, 이번주 순서 고정 4개")
            List<TrendItem> items
    ) {
    }

    @Schema(description = "주차별 평균 점수")
    public record TrendItem(
            @Schema(description = "화면 표시용 주차 라벨", example = "이번주")
            String label,

            @Schema(description = "해당 주차 COMPLETED Analysis totalScore 평균, 데이터 없으면 0", example = "93.0")
            BigDecimal averageScore
    ) {
    }
}
