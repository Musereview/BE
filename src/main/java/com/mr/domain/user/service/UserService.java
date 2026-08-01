package com.mr.domain.user.service;

import com.mr.domain.user.dto.res.UserResponseDTO;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final String NICKNAME_AVAILABLE_MESSAGE = "사용 가능한 닉네임입니다.";
    private static final String NICKNAME_TAKEN_MESSAGE = "이미 사용 중인 닉네임입니다.";

    private final UserRepository userRepository;

    public UserResponseDTO.NicknameCheckResponse checkNicknameAvailable(String nickname) {
        String trimmedNickname = nickname == null ? null : nickname.trim();
        User.validateNicknameFormat(trimmedNickname);

        Long userId = SecurityUtil.getCurrentUserId();
        boolean available = !userRepository.existsByNicknameAndUserIdNot(trimmedNickname, userId);

        return UserResponseDTO.NicknameCheckResponse.builder()
                .isAvailable(available)
                .nickname(trimmedNickname)
                .message(available ? NICKNAME_AVAILABLE_MESSAGE : NICKNAME_TAKEN_MESSAGE)
                .build();
    }
}
