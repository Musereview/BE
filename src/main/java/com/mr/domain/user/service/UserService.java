package com.mr.domain.user.service;

import com.mr.domain.user.dto.res.UserResponseDTO;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserResponseDTO.NicknameCheckResponse checkNicknameAvailable(String nickname) {
        String trimmedNickname = nickname == null ? null : nickname.trim();
        User.validateNicknameFormat(trimmedNickname);

        boolean available = !userRepository.existsByNickname(trimmedNickname);

        return UserResponseDTO.NicknameCheckResponse.builder()
                .nickname(trimmedNickname)
                .available(available)
                .build();
    }
}
