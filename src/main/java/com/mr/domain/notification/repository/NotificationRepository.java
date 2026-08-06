package com.mr.domain.notification.repository;

import com.mr.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Slice<Notification> findAllByUser_UserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    Optional<Notification> findByIdAndDeletedAtIsNull(Long id);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.user.userId = :userId and n.isRead = false and n.deletedAt is null")
    void bulkMarkAllAsReadByUserId(@Param("userId") Long userId);

    // 유저에게 안 읽은 알림이 단 하나라도 존재하는지 확인 (사이드바 종 아이콘 뱃지용)
    boolean existsByUser_UserIdAndIsReadFalseAndDeletedAtIsNull(Long userId);

    // 중복 알림 방지용 쿼리 메서드
    boolean existsByUser_UserIdAndTitleAndCreatedAtAfter(Long userId, String title, LocalDateTime time);

    @Modifying(clearAutomatically = true)
    @Query("delete from Notification n where n.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
