package com.mr.domain.notification.service;

import com.mr.domain.notification.entity.Notification;
import com.mr.domain.notification.repository.NotificationRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    //누군가 NotificationEvent를 발행하면 이 메서드가 자동으로 실행
    // DB 저장 실패 시 최대 3번 재시도 (1초 간격)
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)  // 메인 트랜잭션이 커밋된(AFTER_COMMIT) 이후에만 알림을 저장하도록 변경
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

    //  3번의 재시도 전부 실패했을 때 실행되는 '영구 보정 경로(Fallback)'
    @Recover
    public void recoverNotificationEvent(Exception e, NotificationEvent event) {
        // 나중에 이 로그를 수집해서 슬랙 알림을 보내거나, 수동으로 복구할 수 있도록 단서를 남김
        log.error("[알림 저장 최종 실패] userId: {}, title: {}, 원인: {}",
                event.getUserId(), event.getTitle(), e.getMessage());
    }
}