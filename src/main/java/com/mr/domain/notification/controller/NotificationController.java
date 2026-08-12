package com.mr.domain.notification.controller;

import com.mr.domain.notification.dto.res.NotificationListResponseDTO;
import com.mr.domain.notification.service.NotificationService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.principal.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "알림", description = "알림 API")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "알림 목록 조회 API",
            description = "사용자의 알림 화면을 켤 때, 로그인한 사용자에게 온 알림 리스트를 기본적으로 최신 순으로 반환합니다."
    )
    @GetMapping
    public ApiResponse<NotificationListResponseDTO> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Long userId = userDetails.getUserId();
        NotificationListResponseDTO result = notificationService.getNotificationList(userId, pageable);
        return ApiResponse.onSuccess(result);
    }

    @Operation(
            summary = "알림 읽음 처리 API",
            description = "해당 알림의 상태를 읽음으로 변경합니다."
    )
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> readNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId
    ) {
        Long userId = userDetails.getUserId();
        notificationService.readNotification(userId, notificationId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(
            summary = "알림 전체 읽음 처리 API",
            description = "사용자의 미확인 상태인 알림 전체 읽음 처리"
    )
    @PatchMapping("/read-all")
    public ApiResponse<Void> readAllNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.readAllNotifications(userDetails.getUserId());
        return ApiResponse.onSuccess(null);
    }

    @Operation(
            summary = "안 읽은 알림 여부 확인 (사이드바 뱃지용) API",
            description = "미확인 상태인 알림의 존재 여부 확인"
    )
    @GetMapping("/unread-status")
    public ApiResponse<Boolean> checkUnreadStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        boolean hasUnread = notificationService.checkUnreadNotification(userDetails.getUserId());
        return ApiResponse.onSuccess(hasUnread);
    }
}
