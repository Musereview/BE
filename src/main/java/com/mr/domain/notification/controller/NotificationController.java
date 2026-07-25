package com.mr.domain.notification.controller;

import com.mr.domain.notification.dto.res.NotificationListDTO;
import com.mr.domain.notification.service.NotificationService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.principal.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<NotificationListDTO> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Long userId = userDetails.getUserId();
        NotificationListDTO result = notificationService.getNotificationList(userId, pageable);
        return ApiResponse.onSuccess(result);
    }

    // 알림 읽음 처리
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> readNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId
    ) {
        Long userId = userDetails.getUserId();
        notificationService.readNotification(userId, notificationId);
        return ApiResponse.onSuccess(null);
    }

    // 알림 전체 읽음 처리
    @PatchMapping("/read-all")
    public ApiResponse<Void> readAllNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.readAllNotifications(userDetails.getUserId());
        return ApiResponse.onSuccess(null);
    }

    // 안 읽은 알림 여부 확인 (사이드바 뱃지용)
    @GetMapping("/unread-status")
    public ApiResponse<Boolean> checkUnreadStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        boolean hasUnread = notificationService.checkUnreadNotification(userDetails.getUserId());
        return ApiResponse.onSuccess(hasUnread);
    }
}
