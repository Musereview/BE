package com.mr.domain.notification.repository;

import com.mr.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.user.userId = :userId and n.isRead = false and n.deletedAt is null")
    void bulkMarkAllAsReadByUserId(@Param("userId") Long userId);

    // 유저에게 안 읽은 알림이 단 하나라도 존재하는지 확인 (사이드바 종 아이콘 뱃지용)
    boolean existsByUserUserIdAndIsReadFalseAndDeletedAtIsNull(Long userId);
}
