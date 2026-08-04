package com.mr.domain.notification.dto.res;

import com.mr.domain.notification.entity.Notification;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.event.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;

import java.util.List;
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
                        String fallbackContent = notification.getContent() != null ? notification.getContent().split(NotificationEvent.SEPARATOR)[0] : "내용을 불러올 수 없습니다."; // 갯수 유지용 fallback 객체
                        return new NotificationResponseDTO(
                                notification.getId(),
                                "DEFAULT", // 클릭해도 이동하지 않는 기본 타입으로 강제 전환
                                notification.getTitle(),
                                fallbackContent, // "::" 뒤의 깨진 데이터는 안 보이게 처리
                                notification.isRead(),
                                null,      // targetId 없음
                                notification.getCreatedAt()
                        );
                    }
                })
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
