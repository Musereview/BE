package com.mr.domain.playing.repository;

import com.mr.domain.playing.projection.HistoryPlayingSummary;
import com.mr.domain.playing.projection.HomeRecentPlayingSummary;
import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayingRepository extends JpaRepository<Playing, Long> {

    @Query("""
            select p.id from Playing p
            where p.user.userId = :userId
              and p.status = :status
              and p.deletedAt is null
              and (p.endedAt < :endedAt or (p.endedAt = :endedAt and p.id < :playingId))
            order by p.endedAt desc, p.id desc
            """)
    List<Long> findNextPlayingId(
            @Param("userId") Long userId,
            @Param("status") PlayingStatus status,
            @Param("endedAt") Instant endedAt,
            @Param("playingId") Long playingId,
            Pageable pageable
    );

    @Query("""
            select p.id from Playing p
            where p.user.userId = :userId
              and p.status = :status
              and p.deletedAt is null
              and p.endedAt >= :cutoff
              and (p.endedAt < :endedAt or (p.endedAt = :endedAt and p.id < :playingId))
            order by p.endedAt desc, p.id desc
            """)
    List<Long> findNextPlayingIdSince(
            @Param("userId") Long userId,
            @Param("status") PlayingStatus status,
            @Param("cutoff") Instant cutoff,
            @Param("endedAt") Instant endedAt,
            @Param("playingId") Long playingId,
            Pageable pageable
    );

    @Query("""
            select p from Playing p
            left join fetch p.backingTrack
            where p.id = :id
              and p.deletedAt is null
            """)
    Optional<Playing> findByIdWithBackingTrack(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Playing p
            left join fetch p.backingTrack
            where p.id = :id
              and p.deletedAt is null
            """)
    Optional<Playing> findByIdWithBackingTrackForUpdate(@Param("id") Long id);

    @Query("""
            select count(p) as sessionCount,
                   coalesce(sum(p.durationSec), 0) as totalDurationSec
            from Playing p
            where p.user.userId = :userId
              and p.status = :status
              and p.deletedAt is null
              and p.endedAt >= :since
            """)
    WeeklyPracticeTotals aggregateTotalsByUserAndStatusSince(
            @Param("userId") Long userId,
            @Param("status") PlayingStatus status,
            @Param("since") Instant since
    );

    @Query("""
            select p.endedAt from Playing p
            where p.user.userId = :userId
              and p.status = :status
              and p.deletedAt is null
              and p.endedAt is not null
            """)
    List<Instant> findDistinctEndedDatesByUserAndStatus(
            @Param("userId") Long userId,
            @Param("status") PlayingStatus status
    );

    @Query("""
            select count(p) as sessionCount,
                   coalesce(sum(p.durationSec), 0) as totalDurationSec,
                   max(p.endedAt) as lastEndedAt
            from Playing p
            where p.user.userId = :userId
              and p.status = :status
              and p.deletedAt is null
            """)
    PracticeTotals aggregateTotalsByUserAndStatus(
            @Param("userId") Long userId,
            @Param("status") PlayingStatus status
    );

    interface PracticeTotals {
        Long getSessionCount();
        Long getTotalDurationSec();
        Instant getLastEndedAt();
    }

    interface WeeklyPracticeTotals {
        Long getSessionCount();
        Long getTotalDurationSec();
    }

    Optional<Playing> findByIdAndDeletedAtIsNull(Long playingId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Playing p where p.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Query("""
            select coalesce(sum(p.durationSec), 0) from Playing p
            where p.user.userId = :userId
              and p.status = :status
              and p.deletedAt is null
              and p.endedAt >= :since
              and p.id != :excludePlayingId
            """)
    Long sumDurationSecExcludeCurrent(
            @Param("userId") Long userId,
            @Param("status") PlayingStatus status,
            @Param("since") Instant since,
            @Param("excludePlayingId") Long excludePlayingId
    );

    @Query("""
        select p.id as playingId,
               bt.id as backingTrackId,
               bt.title as backingTrackTitle,
               p.durationSec as durationSec,
               p.endedAt as endedAt
        from Playing p
        left join p.backingTrack bt
        where p.user.userId = :userId
          and p.status = :status
          and p.deletedAt is null
        order by p.endedAt desc, p.id desc
        """)
    Slice<HistoryPlayingSummary> findHistorySummariesByUserAndStatus(
            @Param("userId") Long userId,
            @Param("status") PlayingStatus status,
            Pageable pageable
    );

    @Query("""
        select p.id as playingId,
               bt.id as backingTrackId,
               bt.title as backingTrackTitle,
               p.durationSec as durationSec,
               p.endedAt as endedAt
        from Playing p
        left join p.backingTrack bt
        where p.user.userId = :userId
          and p.status = :status
          and p.deletedAt is null
          and p.endedAt >= :cutoff
        order by p.endedAt desc, p.id desc
        """)
    Slice<HistoryPlayingSummary> findHistorySummariesByUserAndStatusSince(
            @Param("userId") Long userId,
            @Param("status") PlayingStatus status,
            @Param("cutoff") Instant cutoff,
            Pageable pageable
    );

    @Query("""
        select p.id as playingId,
            bt.title as backingTrackTitle,
            bt.genre as backingTrackGenre,
            bt.keySignature as backingTrackKeySignature,
            p.bpm as bpm,
            p.endedAt as endedAt,
            p.durationSec as durationSec
        from Playing p
        left join p.backingTrack bt
        where p.user.userId = :userId
            and p.status = :status
            and p.deletedAt is null 
        order by p.endedAt desc, p.id desc
        """)
    Slice<HomeRecentPlayingSummary> findRecentPlayingSummariesByUserAndStatus(
            @Param("userId") Long userId,
            @Param("status") PlayingStatus status,
            Pageable pageable
    );
}
