package com.mr.domain.auth.dto.res;

public record GoogleUserResponse(
        String id,
        String email,
        String name,
        String picture
) {}
