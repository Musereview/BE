package com.mr.domain.notification.controller;

import com.mr.domain.notification.dto.res.NotificationListDTO;
import com.mr.domain.notification.service.NotificationService;
import com.mr.global.apipayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<NotificationListDTO> getNotifications(
            @RequestParam Long userId,  // 임시
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        NotificationListDTO result = notificationService.getNotificationList(userId, pageable);
        return ApiResponse.onSuccess(result);
    }
}
