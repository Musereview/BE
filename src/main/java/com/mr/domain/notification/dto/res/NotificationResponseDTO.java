package com.mr.domain.notification.dto.res;

import com.mr.domain.notification.entity.Notification;
import com.mr.domain.notification.exception.NotificationErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;

import java.time.LocalDateTime;


public record NotificationResponseDTO (
        Long notificationId,
        String type,
        String title,
        String content,
        Boolean isRead,
        Long targetId,
        LocalDateTime createdAt
) {
    public static NotificationResponseDTO from(Notification notification) {
        String rawContent = notification.getContent();

        String displayContent = rawContent;
        String type = "DEFAULT";
        Long targetId = null;

        // "::" 구분자가 있다면 파싱 로직 수행
        if (rawContent != null && rawContent.contains("::")) {
            String[] parts = rawContent.split("::");
            displayContent = parts[0]; // "지금 바로 분석 결과를 확인해보세요!"

            if (parts.length > 1) {
                type = parts[1];       // "ANALYSIS"
            }
            if (parts.length > 2 && !parts[2].equals("null")) {
                try {
                    targetId = Long.valueOf(parts[2]); // 456
                } catch (NumberFormatException e) {
                    targetId = null;
                }
            }
        }
        // ANALYSIS 타입인데 targetId가 유효하지 않으면 예외 발생
        if ("ANALYSIS".equals(type) && targetId == null) {
            throw new GeneralException(NotificationErrorStatus.INVALID_NOTIFICATION_FORMAT);
        }

        return new NotificationResponseDTO(
                notification.getId(),
                type,
                notification.getTitle(),
                displayContent,
                notification.isRead(),
                targetId,
                notification.getCreatedAt()
        );
    }
}
