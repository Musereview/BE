package com.mr.global.security.principal;

import com.mr.domain.user.entity.User;
import com.mr.domain.user.entity.enums.UserRole;
import com.mr.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            Long userId = Long.parseLong(username);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자입니다. ID: " + userId));

            UserRole role = user.isOnboardingCompleted() ? UserRole.ROLE_STUDENT : UserRole.ROLE_GUEST;

            return new CustomUserDetails(userId, role);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("올바르지 않은 사용자 ID 포맷입니다: " + username, e);
        }
    }
}