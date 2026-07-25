package com.mr.global.security.principal;

import com.mr.domain.user.entity.enums.UserRole;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    // TODO: 추후 User 엔티티 및 UserRepository 완성 시 주입
    // private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            Long userId = Long.parseLong(username);

            // TODO: User user = userRepository.findById(userId)
            //         .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자입니다. ID: " + userId));

            return new CustomUserDetails(userId, UserRole.ROLE_STUDENT);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("올바르지 않은 사용자 ID 포맷입니다: " + username, e);
        }
    }
}