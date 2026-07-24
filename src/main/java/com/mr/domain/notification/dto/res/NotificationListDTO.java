package com.mr.domain.notification.dto.res;

import com.mr.domain.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListDTO {
    private List<NotificationDTO> notificationList;
    private Integer listSize;
    private Integer totalPage;
    private Long totalElements;
    private Boolean isFirst;
    private Boolean isLast;

    public static NotificationListDTO of(Page<Notification> notificationPage) {
        List<NotificationDTO> notificationDTOList = notificationPage.getContent().stream()
                .map(NotificationDTO::from)
                .collect(Collectors.toList());

        return NotificationListDTO.builder()
                .notificationList(notificationDTOList)
                .listSize(notificationDTOList.size())
                .totalPage(notificationPage.getTotalPages())
                .totalElements(notificationPage.getTotalElements())
                .isFirst(notificationPage.isFirst())
                .isLast(notificationPage.isLast())
                .build();
    }
}
