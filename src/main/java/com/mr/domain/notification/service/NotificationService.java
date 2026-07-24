package com.mr.domain.notification.service;

import com.mr.domain.notification.dto.res.NotificationListDTO;
import com.mr.domain.notification.entity.Notification;
import com.mr.domain.notification.repository.NotificationRepository;
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
        Page<Notification> notificationPage = notificationRepository.findAllByUserId(userId, pageable);
        return  NotificationListDTO.of(notificationPage);
    }
}
