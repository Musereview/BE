package com.mr.domain.notification.dto.res;

import com.mr.domain.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListResponseDTO {
    private List<NotificationResponseDTO> notificationList;
    private Integer listSize;
    private Boolean hasNext;
    private Boolean isFirst;
    private Boolean isLast;

    public static NotificationListResponseDTO of(Slice<Notification> notificationSlice) {
        List<NotificationResponseDTO> notificationDTOList = notificationSlice.getContent().stream()
                .map(NotificationResponseDTO::from)
                .collect(Collectors.toList());

        return NotificationListResponseDTO.builder()
                .notificationList(notificationDTOList)
                .listSize(notificationDTOList.size())
                .hasNext(notificationSlice.hasNext())
                .isFirst(notificationSlice.isFirst())
                .isLast(notificationSlice.isLast())
                .build();
    }
}
