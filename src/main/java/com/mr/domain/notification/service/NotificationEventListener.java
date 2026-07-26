package com.mr.domain.notification.service;

import com.mr.domain.notification.entity.Notification;
import com.mr.domain.notification.repository.NotificationRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    //누군가 NotificationEvent를 발행하면 이 메서드가 자동으로 실행
    // 메인 트랜잭션이 커밋된(AFTER_COMMIT) 이후에만 알림을 저장하도록 변경
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void handleNotificationEvent(NotificationEvent event) {

        // 중복 알림 검증 로직 (1분 이내 동일 제목 알림 무시)
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        if (notificationRepository.existsByUser_UserIdAndTitleAndCreatedAtAfter(
                event.getUserId(), event.getTitle(), oneMinuteAgo)) {
            return; // 걸리면 바로 종료
        }

        User user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        Notification notification = Notification.create(
                user,
                event.getTitle(),
                event.getContent()
        );

        notificationRepository.save(notification);
    }
}