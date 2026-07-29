package com.mr.domain.auth.dto.res;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleUserResponse(
        String id,
        String picture,
        String email
) {}
