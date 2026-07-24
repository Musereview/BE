package com.mr.domain.user.dto;

import com.mr.domain.user.entity.enums.TheoryLevel;
import io.swagger.v3.oas.annotations.media.Schema;

public class UserProfileRequestDTO {

    @Schema(description = "프로필 최초 등록(온보딩) 요청")
    public record OnboardingRequest(
            @Schema(description = "닉네임 (한글/영문/숫자 2~10자)", example = "김뮤즈")
            String nickname,

            @Schema(description = "화성학 숙련도", example = "INTERMEDIATE")
            TheoryLevel skillLevel,

            @Schema(description = "구독 등급 (버튼 선택과 무관하게 항상 PRO 전송)", example = "PRO")
            String subscriptionTier
    ) {}

    @Schema(description = "프로필 수정 요청")
    public record UpdateRequest(
            @Schema(description = "변경할 닉네임 (한글/영문/숫자 2~10자)", example = "새로운뮤즈")
            String nickname,

            @Schema(description = "변경할 화성학 숙련도", example = "ADVANCED")
            TheoryLevel skillLevel
    ) {}
}
