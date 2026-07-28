package com.mr.domain.backingtrack.repository;

import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.backingtrack.entity.enums.AccessLevel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BackingTrackRepository extends JpaRepository<BackingTrack, Long> {

    Optional<BackingTrack> findByIdAndDeletedAtIsNull(Long id);
    
    // 최근 1주일 내에 분석이 완료된 공개 트랙 필터링
    @Query("SELECT DISTINCT b FROM BackingTrack b " +
            "JOIN Playing p ON p.backingTrack = b " +
            "JOIN Analysis a ON a.playing = p " +
            "WHERE b.accessLevel = :accessLevel " +
            "AND a.status = 'COMPLETED' " +
            "AND a.completedAt >= :oneWeekAgo " +
            "ORDER BY b.playCount DESC, b.createdAt DESC")
    List<BackingTrack> findTopByAccessLevelAndAnalysisCompleted(
            @Param("accessLevel") AccessLevel accessLevel,
            @Param("oneWeekAgo") LocalDateTime oneWeekAgo,
            Pageable pageable
    );

    // 서비스 단에서 깔끔하게 호출하기 위한 default 메서드 래핑
    default List<BackingTrack> findTop3RecommendedTracks(LocalDateTime oneWeekAgo) {
        return findTopByAccessLevelAndAnalysisCompleted(
                AccessLevel.PUBLIC,
                oneWeekAgo,
                org.springframework.data.domain.PageRequest.of(0, 3)
        );
    }
}
