package com.mr.domain.auth.dto;

import lombok.Builder;

@Builder
public record OAuthUserInfo(
        String socialId,
        String profileImgUrl
) {}
