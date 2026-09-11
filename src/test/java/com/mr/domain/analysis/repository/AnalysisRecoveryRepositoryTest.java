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

        Analysis firstPending = analysisRepository.save(
                Analysis.createPending(user, playing, 1, 2, "{}"));
        Analysis secondPending = analysisRepository.save(
                Analysis.createPending(user, playing, 3, 4, "{}"));
        Analysis processing = Analysis.createPending(user, playing, 5, 6, "{}");
        processing.startProcessing(Instant.parse("2026-09-11T00:00:00Z"));
        analysisRepository.saveAndFlush(processing);

        PageRequest limit = PageRequest.of(0, 20);
        Instant futureCutoff = Instant.parse("2099-01-01T00:00:00Z");

        assertThat(analysisRepository.findPendingIdsByCreatedAtBefore(futureCutoff, limit))
                .containsExactly(firstPending.getId(), secondPending.getId());
        assertThat(analysisRepository.findProcessingIdsByProcessingStartedAtBefore(futureCutoff, limit))
                .containsExactly(processing.getId());
    }
}
