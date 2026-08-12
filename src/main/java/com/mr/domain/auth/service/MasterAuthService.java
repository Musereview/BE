package com.mr.domain.auth.service;

import com.mr.domain.auth.dto.res.MasterAuthResponse;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterAuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public MasterAuthResponse issueAccessToken(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new GeneralException(UserErrorStatus.USER_NOT_FOUND);
        }

        String accessToken = jwtTokenProvider.createAccessToken(userId);
        log.warn("[MASTER_AUTH] 마스터 토큰 발급 - userId={}", userId);

        return new MasterAuthResponse(userId, accessToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds());
    }
}
