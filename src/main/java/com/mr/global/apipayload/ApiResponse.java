package com.mr.global.apipayload;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.mr.global.apipayload.code.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonPropertyOrder({"isSuccess", "code", "message", "data"})
@Schema(description = "공통 응답 포맷")
public record ApiResponse<T>(
        @Schema(description = "요청 성공 여부", example = "true")
        boolean isSuccess,

        @Schema(description = "응답 코드 (성공: COMMON_200, 실패: 도메인별 ErrorStatus 코드)", example = "COMMON_200")
        String code,

        @Schema(description = "응답 메시지", example = "요청에 성공하였습니다.")
        String message,

        @Schema(description = "응답 데이터. 실패 시 null")
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