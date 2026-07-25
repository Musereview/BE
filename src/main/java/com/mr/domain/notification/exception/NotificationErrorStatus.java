package com.mr.domain.notification.exception;

import com.mr.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorStatus implements BaseCode {

    // [403] 권한 에러
    FORBIDDEN_NOTIFICATION(HttpStatus.FORBIDDEN, "NOTIFICATION_403_01", "해당 알림에 대한 권한이 없습니다."),

    // [404] 리소스 없음
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_404_01", "존재하지 않는 알림입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
