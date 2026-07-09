package com.mr.global.apipayload;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.mr.global.apipayload.code.CommonStatus;

@JsonPropertyOrder({"isSuccess", "code", "message", "data"})
public record ApiResponse<T>(
        boolean isSuccess,
        String code,
        String message,
        T data
) {
    // 성공
    public static <T> ApiResponse<T> onSuccess(T data) {
        return new ApiResponse<>(true, CommonStatus.SUCCESS.getCode(), CommonStatus.SUCCESS.getMessage(), data);
    }

    // 실패
    public static <T> ApiResponse<T> onFailure(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data);
    }
}