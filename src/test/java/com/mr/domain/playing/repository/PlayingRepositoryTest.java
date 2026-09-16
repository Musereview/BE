package com.mr.domain.playing.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.backingtrack.entity.enums.AccessLevel;
import com.mr.domain.backingtrack.entity.enums.Level;
import com.mr.domain.backingtrack.entity.enums.ScaleType;
import com.mr.domain.backingtrack.repository.BackingTrackRepository;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@DataJpaTest(properties = "spring.flyway.enabled=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PlayingRepositoryTest {

    @Autowired
    private PlayingRepository playingRepository;

    @Autowired
    private BackingTrackRepository backingTrackRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("같은 KST 날짜에 완료된 연주가 여러 개 있어도 날짜는 한 번만 조회된다")
    void findDistinctEndedDates_multiplePlaysSameKstDate_returnsSingleDate() {

        User user = userRepository.save(User.createFromOAuth("https://example.com/profile.png"));

        BackingTrack backingTrack = backingTrackRepository.save(BackingTrack.create(
                user, 1L, "test", "JAZZ", "C", ScaleType.MAJOR, "3/4",
                120, 300, "backing-tracks/practice-date-test.mp3",
                null, AccessLevel.PRIVATE, Level.BASIC));

        Playing first = playingRepository.save(Playing.createBackingTrack(user, backingTrack, 120));
        Playing second = playingRepository.save(Playing.createBackingTrack(user, backingTrack, 120));

        playingRepository.flush();

        // 테스트용 ended_at/status 설정
        updateAsCompleted(first, Instant.parse("2026-09-15T01:00:00Z"));
        updateAsCompleted(second, Instant.parse("2026-09-15T10:00:00Z"));

        entityManager.clear();

        List<Date> result =
                playingRepository.findDistinctEndedDatesByUserAndStatus(user.getUserId(), PlayingStatus.COMPLETED.name());

        assertThat(result).containsExactly(Date.valueOf(LocalDate.of(2026, 9, 15)));
    }

    @Test
    @DisplayName("UTC 날짜가 달라도 KST 기준 같은 날짜이면 한 번만 조회된다")
    void findDistinctEndedDates_differentUtcDatesSameKstDate_returnsSingleDate() {

        User user = userRepository.save(
                User.createFromOAuth("https://example.com/profile.png")
        );

        BackingTrack backingTrack = backingTrackRepository.save(
                BackingTrack.create(
                        user, 1L, "test", "JAZZ", "C", ScaleType.MAJOR, "3/4",
                        120, 300, "backing-tracks/practice-date-test.mp3",
                        null, AccessLevel.PRIVATE, Level.BASIC));

        Playing first = playingRepository.save(Playing.createBackingTrack(user, backingTrack, 120));
        Playing second = playingRepository.save(Playing.createBackingTrack(user, backingTrack, 120));

        playingRepository.flush();

        updateAsCompleted(first, Instant.parse("2026-09-14T16:30:00Z"));
        updateAsCompleted(second, Instant.parse("2026-09-15T10:00:00Z"));

        entityManager.clear();

        List<Date> result =
                playingRepository.findDistinctEndedDatesByUserAndStatus(user.getUserId(), PlayingStatus.COMPLETED.name());

        assertThat(result).containsExactly(Date.valueOf(LocalDate.of(2026, 9, 15)));
    }

    private void updateAsCompleted(Playing playing, Instant endedAt) {
        entityManager.createNativeQuery(
                        "UPDATE playing SET status = 'COMPLETED', ended_at = :endedAt, started_at = :startedAt, " +
                                "duration_sec = 300 WHERE playing_id = :playingId")
                .setParameter("endedAt", endedAt)
                .setParameter("startedAt", endedAt.minusSeconds(300))
                .setParameter("playingId", playing.getId())
                .executeUpdate();
    }
}
