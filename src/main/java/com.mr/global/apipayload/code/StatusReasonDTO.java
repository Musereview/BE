package com.mr.global.apipayload.code;

import org.springframework.http.HttpStatus;

public record StatusReasonDTO(
        HttpStatus status,
        boolean isSuccess,
        String code,
        String message
) {}