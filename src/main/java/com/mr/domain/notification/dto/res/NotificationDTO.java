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
public class NotificationDTO {

    private Long notificationId;
    private String title;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationDTO from(Notification notification) {
        return NotificationDTO.builder()
                .notificationId(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
