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
        OAuthUserInfo userInfo = oAuthClientService.getUserInfo(socialType, oAuthAccessToken);

        SocialAuth socialAuth = socialAuthRepository.findBySocialTypeAndSocialId(socialType, userInfo.socialId())
                .orElse(null);

        boolean isNewUser = false;
        User user;

        if (socialAuth == null) {
            user = userRepository.save(User.createFromOAuth(userInfo.profileImgUrl()));
            isNewUser = true;

            String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());
            String refreshTokenHash = jwtTokenProvider.hashToken(refreshToken);
            LocalDateTime expiredAt = jwtTokenProvider.getRefreshTokenExpiryTime();

            socialAuth = SocialAuth.create(
                    user,
                    socialType,
                    userInfo.socialId(),
                    refreshToken,
                    refreshTokenHash,
                    expiredAt,
                    deviceInfo
            );
            socialAuthRepository.save(socialAuth);
        } else {
            user = socialAuth.getUser();

            String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());
            String refreshTokenHash = jwtTokenProvider.hashToken(refreshToken);
            LocalDateTime newExpiredAt = jwtTokenProvider.getRefreshTokenExpiryTime();

            socialAuth.updateRefreshToken(refreshToken, refreshTokenHash, newExpiredAt, deviceInfo);
        }

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