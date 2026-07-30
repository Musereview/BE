package com.mr.domain.auth.dto.res;

import com.fasterxml.jackson.annotation.JsonAlias;

public record GoogleUserResponse(
        @JsonAlias({"sub", "id"}) String id,
        String email,
        String name,
        String picture
) {}
