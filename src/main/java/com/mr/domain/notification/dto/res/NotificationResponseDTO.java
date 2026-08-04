package com.mr.domain.notification.dto.res;

import com.mr.domain.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {

    private Long notificationId;
    private String type;
    private String title;
    private String content;
    private Boolean isRead;
    private Long targetId;
    private LocalDateTime createdAt;

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
        return NotificationResponseDTO.builder()
                .notificationId(notification.getId())
                .type(type)
                .title(notification.getTitle())
                .content(displayContent)
                .isRead(notification.isRead())
                .targetId(targetId)
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
