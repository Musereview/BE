package com.mr.domain.user.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public class UserResponseDTO {

    @Builder
    @Schema(description = "닉네임 중복 확인 응답")
    public record NicknameCheckResponse(
            @Schema(description = "사용 가능 여부 (true: 사용 가능, false: 이미 사용 중)", example = "true")
            boolean isAvailable,

            @Schema(description = "확인한 닉네임", example = "김뮤즈")
            String nickname,

            @Schema(description = "화면에 바로 표시할 안내 문구", example = "사용 가능한 닉네임입니다.")
            String message
    ) {}
}
