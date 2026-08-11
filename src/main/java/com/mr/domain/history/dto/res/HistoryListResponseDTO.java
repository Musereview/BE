package com.mr.domain.history.dto.res;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.playing.entity.Playing;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "연주 히스토리 목록 조회 응답")
public record HistoryListResponseDTO(
        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        int page,

        @Schema(description = "페이지당 조회 개수", example = "10")
        int size,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,

        @Schema(description = "연주 히스토리 목록 (연주 종료 시각 최신순)")
        List<Item> items
) {

    public static HistoryListResponseDTO of(int page, int size, boolean hasNext, List<Item> items) {
        return new HistoryListResponseDTO(page, size, hasNext, items);
    }

    @Schema(description = "연주 히스토리 목록 항목")
    public record Item(
            @Schema(description = "연주 기록 ID", example = "128")
            Long playingId,

            @Schema(description = "가장 최근 완료된 분석 ID. 완료된 분석이 없으면 null", example = "342")
            Long latestAnalysisId,

            @Schema(description = "연주에 사용한 백킹트랙 제목", example = "Autumn Leaves")
            String title,

            @Schema(description = "최근 완료된 분석의 한 줄 요약. 완료된 분석이 없으면 null",
                    example = "코드 전환은 안정적이나 8마디 이후 박자가 밀립니다.")
            String summary,

            @Schema(description = "직전 연주의 분석 점수 대비 변화량. 비교 대상이 없으면 null", example = "5")
            Integer scoreChange,

            @Schema(description = "연주 길이 (분 단위, 초 단위를 60으로 나눈 몫)", example = "3")
            Integer durationMinutes,

            @Schema(description = "연주 길이 (초 단위)", example = "215")
            Integer durationSec,

            @Schema(description = "연주 종료 시각", example = "2026-08-10T21:14:32")
            LocalDateTime playedAt,

            @Schema(description = "연주 종료일 상대 표기 (오늘/어제/M월 d일)", example = "어제")
            String relativeDate
    ) {

        public static Item of(Playing playing, Analysis latestAnalysis, Integer scoreChange, String relativeDate) {
            BackingTrack backingTrack = playing.getBackingTrack();

            return new Item(
                    playing.getId(),
                    latestAnalysis != null ? latestAnalysis.getId() : null,
                    backingTrack != null ? backingTrack.getTitle() : null,
                    latestAnalysis != null ? latestAnalysis.getSummary() : null,
                    scoreChange,
                    toDurationMinutes(playing.getDurationSec()),
                    playing.getDurationSec(),
                    playing.getEndedAt(),
                    relativeDate
            );
        }

        private static Integer toDurationMinutes(Integer durationSec) {
            return durationSec != null ? durationSec / 60 : null;
        }
    }
}
