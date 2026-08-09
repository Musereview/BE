package com.mr.domain.user.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    ROLE_GUEST("ROLE_GUEST", "손님"),
    ROLE_STUDENT("ROLE_STUDENT", "학생"),
    ROLE_TEACHER("ROLE_TEACHER", "강사"),
    ROLE_ADMIN("ROLE_ADMIN", "관리자");

    private final String key;
    private final String title;
}