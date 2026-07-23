package com.mr.global.security.principal;

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
        Long userId = Long.parseLong(username);
        // TODO: User user = userRepository.findById(userId).orElseThrow(...);

        return new CustomUserDetails(userId, "user@example.com", "ROLE_USER");
    }
}