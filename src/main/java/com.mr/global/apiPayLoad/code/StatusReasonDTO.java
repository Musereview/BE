package com.mr.global.apiPayLoad.code;

import org.springframework.http.HttpStatus;

public record StatusReasonDTO(
        HttpStatus status,
        boolean isSuccess,
        String code,
        String message
) {}