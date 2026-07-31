package com.mr.domain.playing.repository;

import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayingRepository extends JpaRepository<Playing, Long> {

    @Query("""
            select p from Playing p
            left join fetch p.backingTrack
            where p.user.userId = :userId
              and p.status = :status
              and p.deletedAt is null
              and (:cutoff is null or p.endedAt >= :cutoff)
            order by p.endedAt desc, p.id desc
            """)
    Slice<Playing> findPlayingsByUserAndStatus(
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

    // 연속 출석일수는 상한이 없어 기간 제한 없이 날짜 단위 distinct 조회
    @Query("""
            select distinct function('date', p.endedAt) from Playing p
            where p.user.userId = :userId
              and p.status = :status
              and p.deletedAt is null
              and p.endedAt is not null
            """)
    List<LocalDate> findDistinctEndedDatesByUserAndStatus(
            @Param("userId") Long userId,
            @Param("status") PlayingStatus status
    );

    // 통계 집계용 전체 기간 요약 - row를 끌어오지 않고 단일 행으로 집계
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

    boolean existsByUser_UserIdAndStatusAndEndedAtAfterAndDeletedAtIsNull(
            Long userId,
            PlayingStatus status,
            LocalDateTime endedAt
    );
}
