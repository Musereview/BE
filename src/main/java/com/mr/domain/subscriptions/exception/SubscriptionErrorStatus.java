package com.mr.domain.subscriptions.exception;

import com.mr.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SubscriptionErrorStatus implements BaseCode {

    INVALID_SUBSCRIPTION_DATE(HttpStatus.BAD_REQUEST, "SUBSCRIPTION_400_01", "구독 종료일은 시작일보다 이전일 수 없습니다."),
    INVALID_SUBSCRIPTION_EXTENSION_DATE(HttpStatus.BAD_REQUEST, "SUBSCRIPTION_400_02", "연장할 종료일은 기존 종료일보다 이후여야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
