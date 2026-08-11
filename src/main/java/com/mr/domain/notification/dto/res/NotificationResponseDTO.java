package com.mr.domain.notification.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mr.domain.notification.entity.Notification;
import com.mr.domain.notification.exception.NotificationErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.event.NotificationEvent;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;


public record NotificationResponseDTO (
        Long notificationId,
        String type,
        String title,
        String content,
        Boolean isRead,
        Long targetId,

        @Schema(description = "알림 생성 일시 (KST 기준 응답)", example = "2026-08-11T18:00:00", type = "string")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        Instant createdAt
) {
    public static NotificationResponseDTO from(Notification notification) {
        String rawContent = notification.getContent();

        String displayContent = rawContent;
        String type = "DEFAULT";
        Long targetId = null;

        // SEPARATOR 구분자가 있다면 파싱 로직 수행
        if (rawContent != null && rawContent.contains(NotificationEvent.SEPARATOR)) {
            String[] parts = rawContent.split(NotificationEvent.SEPARATOR);
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