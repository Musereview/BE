package com.mr.domain.playing.repository;

import com.mr.domain.playing.entity.Playing;
import com.mr.domain.playing.entity.enums.PlayingStatus;
import java.time.LocalDateTime;
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
    Slice<Playing> findCompletedPlayingsByUser(
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
}
