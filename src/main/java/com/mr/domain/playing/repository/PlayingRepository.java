package com.mr.domain.playing.repository;

import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import jakarta.persistence.LockModeType;
import java.sql.Date;
import java.time.LocalDateTime;
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
            select p from Playing p
            left join fetch p.backingTrack
            where p.user.userId = :userId
              and p.status = :status
              and p.deletedAt is null
            order by p.endedAt desc, p.id desc
            """)
    Slice<Playing> findPlayingsByUserAndStatus(
            @Param("userId") Long userId,
            @Param("status") PlayingStatus status,
            Pageable pageable
    );

    @Query("""
            select p from Playing p
            left join fetch p.backingTrack
            where p.user.userId = :userId
              and p.status = :status
              and p.deletedAt is null
              and p.endedAt >= :cutoff
            order by p.endedAt desc, p.id desc
            """)
    Slice<Playing> findPlayingsByUserAndStatusSince(
            @Param("userId") Long userId,
            @Param("status") PlayingStatus status,
            @Param("cutoff") LocalDateTime cutoff,
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
            select p from Playing p
            where p.user.userId = :userId
              and p.status = :status
              and p.deletedAt is null
              and p.endedAt >= :since
            order by p.endedAt desc
            """)
    List<Playing> findByUserAndStatusSince(
            @Param("userId") Long userId,
            @Param("status") PlayingStatus status,
            @Param("since") LocalDateTime since
    );

    @Query("""
            select distinct function('date', p.endedAt) from Playing p
            where p.user.userId = :userId
              and p.status = :status
              and p.deletedAt is null
              and p.endedAt is not null
            """)
    List<Date> findDistinctEndedDatesByUserAndStatus(
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
        LocalDateTime getLastEndedAt();
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
            @Param("since") LocalDateTime since,
            @Param("excludePlayingId") Long excludePlayingId
    );
}
