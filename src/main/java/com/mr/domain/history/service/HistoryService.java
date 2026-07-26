package com.mr.domain.history.service;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.history.dto.req.HistoryPeriod;
import com.mr.domain.history.dto.res.HistoryDetailResponseDTO;
import com.mr.domain.history.dto.res.HistoryListResponseDTO;
import com.mr.domain.history.dto.res.HistoryListResponseDTO.Item;
import com.mr.domain.history.exception.HistoryErrorStatus;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.global.apipayload.exception.GeneralException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoryService {

    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int WEEKLY_WINDOW_DAYS = 7;
    private static final int MONTHLY_WINDOW_DAYS = 30;

    private final PlayingRepository playingRepository;
    private final AnalysisRepository analysisRepository;

    public HistoryListResponseDTO getHistories(Long userId, int page, int size, HistoryPeriod period) {
        validatePaging(page, size);

        LocalDateTime cutoff = resolveCutoff(period);
        Slice<Playing> slice = playingRepository.findPlayingsByUserAndStatus(
                userId, PlayingStatus.COMPLETED, cutoff, PageRequest.of(page, size));

        List<Playing> playings = slice.getContent();
        Map<Long, Analysis> latestByPlayingId = fetchLatestCompletedAnalyses(playings);

        return HistoryListResponseDTO.of(page, size, slice.hasNext(), buildItems(playings, latestByPlayingId));
    }

    public HistoryDetailResponseDTO getHistoryDetail(Long userId, Long playingId) {
        validatePlayingId(playingId);

        Playing playing = playingRepository.findByIdWithBackingTrack(playingId)
                .orElseThrow(() -> new GeneralException(HistoryErrorStatus.HISTORY_NOT_FOUND));

        validateOwner(playing, userId);
        validateCompleted(playing);

        List<Analysis> analyses =
                analysisRepository.findByPlayingIdAndUserIdOrderByStartBarAscIdAsc(playingId, userId);

        return HistoryDetailResponseDTO.from(playing, analyses);
    }

    private List<Item> buildItems(List<Playing> playings, Map<Long, Analysis> latestByPlayingId) {
        List<Item> items = new ArrayList<>();

        for (int i = 0; i < playings.size(); i++) {
            Playing current = playings.get(i);
            Analysis currentAnalysis = latestByPlayingId.get(current.getId());

            Integer scoreChange = null;
            if (i + 1 < playings.size()) {
                Analysis previousAnalysis = latestByPlayingId.get(playings.get(i + 1).getId());
                scoreChange = computeScoreChange(currentAnalysis, previousAnalysis);
            }

            items.add(Item.of(current, currentAnalysis, scoreChange, toRelativeDate(current.getEndedAt())));
        }

        return items;
    }

    private Map<Long, Analysis> fetchLatestCompletedAnalyses(List<Playing> playings) {
        List<Long> playingIds = playings.stream().map(Playing::getId).toList();

        if (playingIds.isEmpty()) {
            return Map.of();
        }

        return analysisRepository
                .findByPlayingIdInAndStatusOrderByCreatedAtDescIdDesc(playingIds, AnalysisStatus.COMPLETED)
                .stream()
                .collect(Collectors.toMap(
                        analysis -> analysis.getPlaying().getId(),
                        analysis -> analysis,
                        (latest, older) -> latest // createdAt DESC 정렬이라 먼저 온 값이 최신
                ));
    }

    private Integer computeScoreChange(Analysis current, Analysis previous) {
        if (current == null || previous == null
                || current.getTotalScore() == null || previous.getTotalScore() == null) {
            return null;
        }

        return current.getTotalScore() - previous.getTotalScore();
    }

    private void validateOwner(Playing playing, Long userId) {
        if (!Objects.equals(playing.getUser().getUserId(), userId)) {
            throw new GeneralException(HistoryErrorStatus.HISTORY_ACCESS_DENIED);
        }
    }

    private void validateCompleted(Playing playing) {
        if (playing.getStatus() != PlayingStatus.COMPLETED) {
            throw new GeneralException(HistoryErrorStatus.HISTORY_NOT_COMPLETED);
        }
    }

    private void validatePaging(int page, int size) {
        if (page < 0 || size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new GeneralException(HistoryErrorStatus.HISTORY_INVALID_REQUEST);
        }
    }

    private void validatePlayingId(Long playingId) {
        if (playingId == null || playingId < 1) {
            throw new GeneralException(HistoryErrorStatus.HISTORY_INVALID_ID);
        }
    }

    private LocalDateTime resolveCutoff(HistoryPeriod period) {
        if (period == null) {
            return null;
        }

        return switch (period) {
            case WEEKLY -> LocalDateTime.now().minusDays(WEEKLY_WINDOW_DAYS);
            case MONTHLY -> LocalDateTime.now().minusDays(MONTHLY_WINDOW_DAYS);
            case RECENT -> null; // 필터 없음과 동일
        };
    }

    private String toRelativeDate(LocalDateTime playedAt) {
        if (playedAt == null) {
            return null;
        }

        long daysBetween = ChronoUnit.DAYS.between(playedAt.toLocalDate(), LocalDate.now());

        if (daysBetween <= 0) {
            return "오늘";
        }
        if (daysBetween == 1) {
            return "어제";
        }
        return playedAt.format(DateTimeFormatter.ofPattern("M월 d일"));
    }
}
