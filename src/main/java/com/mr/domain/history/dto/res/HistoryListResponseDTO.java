package com.mr.domain.history.dto.res;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.playing.entity.Playing;

import java.time.LocalDateTime;
import java.util.List;

public record HistoryListResponseDTO(
        int page,
        int size,
        boolean hasNext,
        List<Item> items
) {

    public static HistoryListResponseDTO of(int page, int size, boolean hasNext, List<Item> items) {
        return new HistoryListResponseDTO(page, size, hasNext, items);
    }

    public record Item(
            Long playingId,
            Long latestAnalysisId,
            String title,
            String summary,
            Integer scoreChange,
            Integer durationMinutes,
            Integer durationSec,
            LocalDateTime playedAt,
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
