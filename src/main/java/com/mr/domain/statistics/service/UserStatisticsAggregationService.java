package com.mr.domain.statistics.service;

import com.mr.domain.analysis.entity.enums.AnalysisStatus;
import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.domain.statistics.entity.UserStatistics;
import com.mr.domain.statistics.repository.UserStatisticsRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserStatisticsAggregationService {

    private static final int SCORE_SCALE = 1;
    private static final int SECONDS_PER_MINUTE = 60;

    private final UserRepository userRepository;
    private final UserStatisticsRepository userStatisticsRepository;
    private final PlayingRepository playingRepository;
    private final AnalysisRepository analysisRepository;

    public void aggregatePractice(Long userId) {
        PlayingRepository.PracticeTotals totals =
                playingRepository.aggregateTotalsByUserAndStatus(userId, PlayingStatus.COMPLETED);
        UserStatistics statistics = getOrCreate(userId);
        statistics.updatePracticeSummary(
                minutesFromSeconds(totals.getTotalDurationSec()),
                totals.getSessionCount().intValue(),
                totals.getLastEndedAt());
    }

    public void aggregateAnalysis(Long userId) {
        AnalysisRepository.AnalysisTotals totals =
                analysisRepository.aggregateTotalsByUserAndStatus(userId, AnalysisStatus.COMPLETED);
        UserStatistics statistics = getOrCreate(userId);
        statistics.updateAnalysisSummary(
                totals.getAnalysisCount().intValue(),
                toScoreScale(totals.getAverageTotalScore()));
    }

    private UserStatistics getOrCreate(Long userId) {
        return userStatisticsRepository.findByUser_UserId(userId)
                .orElseGet(() -> userStatisticsRepository.save(UserStatistics.createForUser(getUser(userId))));
    }

    private int minutesFromSeconds(Long totalSeconds) {
        return (int) ((totalSeconds != null ? totalSeconds : 0L) / SECONDS_PER_MINUTE);
    }

    private BigDecimal toScoreScale(Double average) {
        return average == null
                ? null
                : BigDecimal.valueOf(average).setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));
    }
}
