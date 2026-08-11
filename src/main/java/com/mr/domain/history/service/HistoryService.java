package com.mr.domain.history.service;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.analysis.service.AnalysisBarCalculator;
import com.mr.domain.analysis.service.AnalysisBarCalculator.BarMetrics;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.history.dto.req.HistoryPeriod;
import com.mr.domain.history.dto.res.HistoryDetailResponseDTO;
import com.mr.domain.history.dto.res.HistoryListResponseDTO;
import com.mr.domain.history.dto.res.HistoryListResponseDTO.Item;
import com.mr.domain.history.exception.HistoryErrorStatus;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.file.s3.enums.S3FileType;
import com.mr.global.file.s3.service.S3FileService;
import com.mr.global.util.RelativeDateFormatter;
import java.time.LocalDateTime;
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
    private final S3FileService s3FileService;
    private final AnalysisBarCalculator analysisBarCalculator;

    public HistoryListResponseDTO getHistories(Long userId, int page, int size, HistoryPeriod period) {
        validatePaging(page, size);

        LocalDateTime cutoff = resolveCutoff(period);
        PageRequest pageRequest = PageRequest.of(page, size);
        Slice<Playing> slice = cutoff == null
                ? playingRepository.findPlayingsByUserAndStatus(
                        userId, PlayingStatus.COMPLETED, pageRequest)
                : playingRepository.findPlayingsByUserAndStatusSince(
                        userId, PlayingStatus.COMPLETED, cutoff, pageRequest);

        List<Playing> playings = slice.getContent();
        Long nextPlayingId = findNextPlayingId(userId, cutoff, slice);
        Map<Long, Analysis> latestByPlayingId = fetchLatestCompletedAnalyses(playings, nextPlayingId);

        return HistoryListResponseDTO.of(
                page, size, slice.hasNext(), buildItems(playings, nextPlayingId, latestByPlayingId));
    }

    public HistoryDetailResponseDTO getHistoryDetail(Long userId, Long playingId) {
        validatePlayingId(playingId);

        Playing playing = playingRepository.findByIdWithBackingTrack(playingId)
                .orElseThrow(() -> new GeneralException(HistoryErrorStatus.HISTORY_NOT_FOUND));

        validateOwner(playing, userId);
        validateCompleted(playing);

        List<Analysis> analyses =
                analysisRepository.findByPlayingIdAndUserIdOrderByStartBarAscIdAsc(playingId, userId);

        String recordingFileUrl =
                s3FileService.createPresignedDownload(
                        userId,
                        S3FileType.RECORDING,
                        playing.getRecordingObjectKey()
                );

        BackingTrack backingTrack = playing.getBackingTrack();

        String backingTrackAudioFileUrl = null;

        if (backingTrack != null
                && backingTrack.getAudioObjectKey() != null
                && !backingTrack.getAudioObjectKey().isBlank()) {

            backingTrackAudioFileUrl =
                    s3FileService.createPresignedDownload(
                            backingTrack.getUser().getUserId(),
                            S3FileType.BACKING_TRACK,
                            backingTrack.getAudioObjectKey()
                    );
        }

        return HistoryDetailResponseDTO.from(
                playing, analyses, recordingFileUrl, backingTrackAudioFileUrl, resolveBarMetrics(playing));
    }

    // 백킹트랙 정보 불완전 시 조회 실패 대신 마디 관련 필드만 null 처리
    private BarMetrics resolveBarMetrics(Playing playing) {
        try {
            return analysisBarCalculator.calculate(playing);
        } catch (GeneralException exception) {
            return null;
        }
    }

    private List<Item> buildItems(
            List<Playing> playings, Long nextPlayingId, Map<Long, Analysis> latestByPlayingId) {
        List<Item> items = new ArrayList<>();

        for (int i = 0; i < playings.size(); i++) {
            Playing current = playings.get(i);
            Analysis currentAnalysis = latestByPlayingId.get(current.getId());

            Long previousPlayingId = i + 1 < playings.size()
                    ? playings.get(i + 1).getId()
                    : nextPlayingId;
            Analysis previousAnalysis = latestByPlayingId.get(previousPlayingId);
            Integer scoreChange = computeScoreChange(currentAnalysis, previousAnalysis);

            items.add(Item.of(current, currentAnalysis, scoreChange, RelativeDateFormatter.format(current.getEndedAt())));
        }

        return items;
    }

    private Long findNextPlayingId(Long userId, LocalDateTime cutoff, Slice<Playing> slice) {
        if (!slice.hasNext() || slice.getContent().isEmpty()) {
            return null;
        }

        Playing lastPlaying = slice.getContent().get(slice.getContent().size() - 1);
        PageRequest pageRequest = PageRequest.of(0, 1);
        List<Long> nextPlayingIds = cutoff == null
                ? playingRepository.findNextPlayingId(
                        userId, PlayingStatus.COMPLETED,
                        lastPlaying.getEndedAt(), lastPlaying.getId(), pageRequest)
                : playingRepository.findNextPlayingIdSince(
                        userId, PlayingStatus.COMPLETED, cutoff,
                        lastPlaying.getEndedAt(), lastPlaying.getId(), pageRequest);

        return nextPlayingIds
                .stream()
                .findFirst()
                .orElse(null);
    }

    private Map<Long, Analysis> fetchLatestCompletedAnalyses(List<Playing> playings, Long nextPlayingId) {
        List<Long> playingIds = new ArrayList<>(playings.stream().map(Playing::getId).toList());
        if (nextPlayingId != null) {
            playingIds.add(nextPlayingId);
        }

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

}
