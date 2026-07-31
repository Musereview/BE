package com.mr.domain.auth.dto.res;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleUserResponse(
        @JsonAlias({"sub", "id"}) String id,
        String email,
        String name,
        String picture
) {}
