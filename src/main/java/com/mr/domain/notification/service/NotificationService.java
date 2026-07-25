package com.mr.domain.notification.service;

import com.mr.domain.notification.dto.res.NotificationListDTO;
import com.mr.domain.notification.entity.Notification;
import com.mr.domain.notification.exception.NotificationErrorStatus;
import com.mr.domain.notification.repository.NotificationRepository;
import com.mr.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationListDTO getNotificationList(Long userId, Pageable pageable){
        Page<Notification> notificationPage = notificationRepository.findAllByUserIdAndDeletedAtIsNull(userId, pageable);
        return  NotificationListDTO.of(notificationPage);
    }

    // 알림 읽음 처리
    @Transactional
    public void readNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new GeneralException(NotificationErrorStatus.NOTIFICATION_NOT_FOUND));

        // 본인 알림이 맞는지 검증 (선택 사항이나 보안상 권장)
        if (!notification.getUser().getUserId().equals(userId)) {
            throw new GeneralException(NotificationErrorStatus.FORBIDDEN_NOTIFICATION);
        }

        notification.markAsRead();
    }

    // 알림 전체 읽음 처리
    @Transactional
    public void readAllNotifications(Long userId) {
        notificationRepository.bulkMarkAllAsReadByUserId(userId);
    }

    // 안 읽은 알림 존재 여부 확인
    public boolean checkUnreadNotification(Long userId) {
        return notificationRepository.existsByUserUserIdAndIsReadFalseAndDeletedAtIsNull(userId);
    }
}
