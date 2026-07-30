package com.mr.domain.auth.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SocialType {
    KAKAO,
    GOOGLE;

    @JsonCreator
    public static SocialType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (SocialType type : SocialType.values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return null;
    }
}
