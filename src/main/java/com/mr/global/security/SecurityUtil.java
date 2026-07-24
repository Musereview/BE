package com.mr.global.security;

import com.mr.global.apipayload.code.CommonStatus;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.security.principal.CustomUserDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    private SecurityUtil() {
    }

    public static Long getCurrentUserId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new GeneralException(CommonStatus.UNAUTHORIZED);
        }

        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }

        throw new GeneralException(CommonStatus.UNAUTHORIZED);
    }
}