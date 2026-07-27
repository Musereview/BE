package com.mr.domain.auth.dto;

import lombok.Builder;

@Builder
public record OAuthUserInfo(
        Long socialId,
        String profileImgUrl
) {}
