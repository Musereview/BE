package com.mr.global.security;

import com.mr.global.apipayload.code.CommonStatus;
import com.mr.global.apipayload.exception.GeneralException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    private SecurityUtil() {
    }

    public static Long getCurrentUserId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null || authentication.getName().equals("anonymousUser")) {
            throw new GeneralException(CommonStatus.INVALID_INPUT_VALUE);
        }

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new GeneralException(CommonStatus.INVALID_INPUT_VALUE);
        }
    }
}