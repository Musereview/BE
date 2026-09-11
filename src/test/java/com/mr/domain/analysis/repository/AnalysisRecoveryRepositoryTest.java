package com.mr.domain.analysis.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.backingtrack.entity.enums.AccessLevel;
import com.mr.domain.backingtrack.entity.enums.Level;
import com.mr.domain.backingtrack.entity.enums.ScaleType;
import com.mr.domain.backingtrack.repository.BackingTrackRepository;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.repository.PlayingRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnalysisRecoveryRepositoryTest {

    @Autowired
    private AnalysisRepository analysisRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BackingTrackRepository backingTrackRepository;

    @Autowired
    private PlayingRepository playingRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void recoveryQueriesFilterTheLiteralStatusAndPreserveOldestFirstOrder() {
        User user = userRepository.save(User.createFromOAuth("https://example.com/profile.png"));
        BackingTrack backingTrack = backingTrackRepository.save(BackingTrack.create(
                user,
                1L,
                "복구 쿼리 테스트",
                "JAZZ",
                "C",
                ScaleType.MAJOR,
                "4/4",
                120,
                300,
                "backing-tracks/recovery-query-test.mp3",
                null,
                AccessLevel.PRIVATE,
                Level.BASIC
        ));
        Playing playing = playingRepository.save(Playing.createBackingTrack(user, backingTrack, 120));

        Analysis firstStalePending = analysisRepository.save(
                Analysis.createPending(user, playing, 1, 2, "{}"));
        Analysis secondStalePending = analysisRepository.save(
                Analysis.createPending(user, playing, 3, 4, "{}"));
        Analysis recentPending = analysisRepository.save(
                Analysis.createPending(user, playing, 5, 6, "{}"));
        Analysis staleProcessing = Analysis.createPending(user, playing, 7, 8, "{}");
        staleProcessing.startProcessing(Instant.parse("2026-09-10T23:59:00Z"));
        analysisRepository.save(staleProcessing);
        Analysis recentProcessing = Analysis.createPending(user, playing, 9, 10, "{}");
        recentProcessing.startProcessing(Instant.parse("2026-09-11T00:01:00Z"));
        analysisRepository.saveAndFlush(recentProcessing);

        PageRequest limit = PageRequest.of(0, 20);
        Instant cutoff = Instant.parse("2026-09-11T00:00:00Z");
        updateCreatedAt(firstStalePending, Instant.parse("2026-09-10T23:58:00Z"));
        updateCreatedAt(secondStalePending, Instant.parse("2026-09-10T23:59:00Z"));
        updateCreatedAt(recentPending, Instant.parse("2026-09-11T00:01:00Z"));

        assertThat(analysisRepository.findPendingIdsByCreatedAtBefore(cutoff, limit))
                .containsExactly(firstStalePending.getId(), secondStalePending.getId());
        assertThat(analysisRepository.findProcessingIdsByProcessingStartedAtBefore(cutoff, limit))
                .containsExactly(staleProcessing.getId());
    }

    private void updateCreatedAt(Analysis analysis, Instant createdAt) {
        entityManager.createNativeQuery("UPDATE analysis SET created_at = :createdAt WHERE analysis_id = :analysisId")
                .setParameter("createdAt", createdAt)
                .setParameter("analysisId", analysis.getId())
                .executeUpdate();
    }
}
