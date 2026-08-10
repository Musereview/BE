package com.mr.domain.backingtrack.repository;

import com.mr.domain.backingtrack.entity.BackingTrack;
import com.mr.domain.backingtrack.entity.enums.AccessLevel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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
            @Param("oneWeekAgo") Instant oneWeekAgo,
            Pageable pageable
    );

    // 서비스 단에서 깔끔하게 호출하기 위한 default 메서드 래핑
    default List<BackingTrack> findTop3RecommendedTracks(Instant oneWeekAgo) {
        return findTopByAccessLevelAndAnalysisCompleted(
                AccessLevel.PUBLIC,
                oneWeekAgo,
                org.springframework.data.domain.PageRequest.of(0, 3)
        );
    }

    // 연동 기준으로 쿼리 수정
    @Query("SELECT b FROM BackingTrack b " +
            "WHERE b.deletedAt IS NULL " +
            "AND (b.accessLevel = :accessLevel OR b.user.userId = :userId) "+
            "AND (:cursor IS NULL OR b.id < :cursor)" +
            "ORDER BY b.id DESC")
    List<BackingTrack> findVisibleTracksAfterCursor(
            @Param("accessLevel") AccessLevel accessLevel,
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE BackingTrack b SET b.playCount = b.playCount + 1 " +
            "WHERE b.id = :id AND b.deletedAt IS NULL")
    int increasePlayCount(@Param("id") Long id);

    // 코드 진행 정보까지 함께 조회
    @Query("""
            SELECT DISTINCT bt
            FROM BackingTrack bt
            LEFT JOIN FETCH bt.chordProgressions cp
            WHERE bt.id = :backingTrackId
              AND bt.deletedAt IS NULL
            """)
    Optional<BackingTrack> findByIdWithChordProgressions(
            @Param("backingTrackId") Long backingTrackId
    );

    @Modifying(clearAutomatically = true)
    @Query("delete from BackingTrack b where b.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
