package com.mr.domain.notification.dto.res;

import com.mr.domain.notification.entity.Notification;
import com.mr.global.apipayload.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public record NotificationListResponseDTO (
        List<NotificationResponseDTO> notificationList,
        Integer listSize,
        Boolean hasNext,
        Boolean isFirst,
        Boolean isLast
){

    public static NotificationListResponseDTO of(Slice<Notification> notificationSlice) {
        List<NotificationResponseDTO> notificationDTOList = notificationSlice.getContent().stream()
                .map(notification -> {
                    try {
                        return NotificationResponseDTO.from(notification);
                    } catch (GeneralException e) {
                        log.error("[알림 파싱 실패] 알림 ID: {}, 원인: {}", notification.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new NotificationListResponseDTO(
                notificationDTOList,
                notificationDTOList.size(),
                notificationSlice.hasNext(),
                notificationSlice.isFirst(),
                notificationSlice.isLast()
        );
    }
}
