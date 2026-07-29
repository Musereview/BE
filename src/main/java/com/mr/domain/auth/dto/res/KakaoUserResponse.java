package com.mr.domain.auth.dto.res;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponse(
        Long id,
        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount
) {
    public record KakaoAccount(
            Profile profile
    ) {
        public record Profile(
                @JsonProperty("profile_image_url")
                String profileImageUrl
        ) {}
    }
}
