package com.mr.domain.auth.service;

import com.mr.domain.auth.dto.OAuthUserInfo;
import com.mr.domain.auth.dto.res.AuthResponseDTO;
import com.mr.domain.auth.entity.SocialAuth;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.repository.SocialAuthRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final OAuthClientService oAuthClientService;
    private final UserRepository userRepository;
    private final SocialAuthRepository socialAuthRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponseDTO.LoginResponse socialLogin(SocialType socialType, String oAuthAccessToken, String deviceInfo) {
        // 1. 소셜 프로필 조회
        OAuthUserInfo userInfo = oAuthClientService.getUserInfo(socialType, oAuthAccessToken);

        // 2. SocialAuth 존재 여부 확인 및 회원가입/로그인 진행
        // [수정] OAuthUserInfo.getSocialId() -> userInfo.getSocialId()
        SocialAuth socialAuth = socialAuthRepository.findBySocialTypeAndSocialId(socialType, userInfo.getSocialId())
                .orElse(null);

        boolean isNewUser = false;
        User user;

        if (socialAuth == null) {
            // 신규 유저 생성 (OAuth 가입 단계)
            user = userRepository.save(User.createFromOAuth(userInfo.getProfileImgUrl()));
            isNewUser = true;

            // Refresh Token 생성 및 저장
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());
            String refreshTokenHash = jwtTokenProvider.hashToken(refreshToken);
            LocalDateTime expiredAt = jwtTokenProvider.getRefreshTokenExpiryTime();

            // SocialAuth 정보 신규 적재
            socialAuth = SocialAuth.create(
                    user,
                    socialType,
                    userInfo.getSocialId(),
                    refreshToken,
                    refreshTokenHash,
                    expiredAt,
                    deviceInfo
            );
            socialAuthRepository.save(socialAuth);
        } else {
            // 기존 유저 로그인 -> Refresh Token 갱신
            user = socialAuth.getUser();

            String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());
            String refreshTokenHash = jwtTokenProvider.hashToken(refreshToken);
            LocalDateTime newExpiredAt = jwtTokenProvider.getRefreshTokenExpiryTime();

            socialAuth.updateRefreshToken(refreshToken, refreshTokenHash, newExpiredAt, deviceInfo);
        }

        // 3. 서비스 전용 AccessToken 생성 및 응답 반환
        String appAccessToken = jwtTokenProvider.createAccessToken(user.getUserId());

        AuthResponseDTO.TokenResponse tokenResponse = AuthResponseDTO.TokenResponse.builder()
                .accessToken(appAccessToken)
                .refreshToken(socialAuth.getRefreshToken())
                .accessTokenExpiresInSeconds(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .build();

        return AuthResponseDTO.LoginResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .isNewUser(isNewUser)
                .isOnboardingCompleted(user.isOnboardingCompleted())
                .tokenInfo(tokenResponse)
                .build();
    }
}