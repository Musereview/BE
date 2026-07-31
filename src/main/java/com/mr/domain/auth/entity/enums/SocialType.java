package com.mr.domain.auth.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.mr.domain.auth.exception.AuthErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;

public enum SocialType {
    KAKAO,
    GOOGLE;

    @JsonCreator
    public static SocialType from(String value) {
        if (value == null || value.isBlank()) {
            throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
        }
        for (SocialType type : SocialType.values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
    }
}
