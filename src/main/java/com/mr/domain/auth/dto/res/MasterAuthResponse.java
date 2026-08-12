package com.mr.domain.auth.dto.res;

public record MasterAuthResponse(
        Long userId,
        String accessToken,
        Long accessTokenExpiresInSeconds
) {}
