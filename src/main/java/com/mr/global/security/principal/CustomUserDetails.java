package com.mr.global.security.principal;

import com.mr.global.apipayload.code.CommonStatus;
import com.mr.global.apipayload.exception.GeneralException;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String role;

    public CustomUserDetails(Long userId, String role) {
        if (userId == null) {
            throw new GeneralException(CommonStatus.INVALID_INPUT_VALUE);
        }
        if (!StringUtils.hasText(role)) {
            throw new GeneralException(CommonStatus.INVALID_INPUT_VALUE);
        }
        this.userId = userId;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return String.valueOf(userId);
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}