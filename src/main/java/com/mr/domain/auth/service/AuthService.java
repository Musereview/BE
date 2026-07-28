package com.mr.domain.auth.service;

import com.mr.domain.auth.dto.OAuthUserInfo;
import com.mr.domain.auth.dto.res.AuthResponseDTO;
import com.mr.domain.auth.entity.SocialAuth;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.exception.AuthErrorStatus;
import com.mr.domain.auth.repository.SocialAuthRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.repository.UserRepository;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final SocialAuthRepository socialAuthRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final OAuthClientService oAuthClientService;

    @Value("${app.profile.default-image-url}")
    private String defaultProfileImageUrl;

    @Transactional
    public AuthResponseDTO.LoginResponse socialLogin(SocialType socialType, String accessToken, String deviceInfo) {
        OAuthUserInfo userInfo = oAuthClientService.getUserInfo(socialType, accessToken);

        Optional<SocialAuth> optionalSocialAuth = socialAuthRepository.findBySocialTypeAndSocialId(socialType, userInfo.socialId());
        boolean isNewUser = optionalSocialAuth.isEmpty();

        User user;
        SocialAuth socialAuth;

        if (optionalSocialAuth.isPresent()) {
            socialAuth = optionalSocialAuth.get();
            user = socialAuth.getUser();
        } else {
            user = registerNewUser(userInfo);
            socialAuth = null;
        }

        String newAccessToken = tokenProvider.createAccessToken(user.getUserId());
        String newRefreshToken = tokenProvider.createRefreshToken(user.getUserId());
        String refreshTokenHash = tokenProvider.hashToken(newRefreshToken);
        LocalDateTime expiryTime = tokenProvider.getRefreshTokenExpiryTime();

        if (isNewUser) {
            socialAuth = SocialAuth.create(
                    user,
                    socialType,
                    userInfo.socialId(),
                    refreshTokenHash,
                    expiryTime,
                    deviceInfo
            );
            socialAuth = socialAuthRepository.save(socialAuth);
        } else {
            socialAuth.updateRefreshToken(refreshTokenHash, expiryTime, deviceInfo);
        }

        AuthResponseDTO.TokenResponse tokenResponse = AuthResponseDTO.TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresInSeconds(tokenProvider.getAccessTokenExpirationSeconds())
                .build();

        return AuthResponseDTO.LoginResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .isNewUser(isNewUser)
                .isOnboardingCompleted(user.isOnboardingCompleted())
                .tokenInfo(tokenResponse)
                .build();
    }

    private User registerNewUser(OAuthUserInfo userInfo) {
        String profileImgUrl = userInfo.profileImgUrl();
        if (profileImgUrl == null || profileImgUrl.isBlank()) {
            profileImgUrl = defaultProfileImageUrl;
        }

        User user = User.createFromOAuth(profileImgUrl);
        return userRepository.save(user);
    }

    @Transactional
    public AuthResponseDTO.TokenInfo reissueToken(String refreshToken) {
        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            throw new GeneralException(AuthErrorStatus.INVALID_TOKEN);
        }

        Long userId = Long.valueOf(tokenProvider.getAuthentication(refreshToken).getName());
        String requestTokenHash = tokenProvider.hashToken(refreshToken);

        SocialAuth socialAuth = socialAuthRepository.findByRefreshTokenHashWithLock(requestTokenHash)
                .filter(auth -> auth.getUser().getUserId().equals(userId))
                .orElseThrow(() -> new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST));

        String newAccessToken = tokenProvider.createAccessToken(userId);
        String newRefreshToken = tokenProvider.createRefreshToken(userId);
        String newRefreshTokenHash = tokenProvider.hashToken(newRefreshToken);

        socialAuth.updateRefreshToken(newRefreshTokenHash, tokenProvider.getRefreshTokenExpiryTime(), socialAuth.getDeviceInfo());

        return AuthResponseDTO.TokenInfo.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresInSeconds(tokenProvider.getAccessTokenExpirationSeconds())
                .build();
    }

    @Transactional
    public void logout(Long userId) {
        List<SocialAuth> socialAuths = socialAuthRepository.findAllByUser_UserId(userId);
        if (socialAuths.isEmpty()) {
            throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
        }

        socialAuths.forEach(SocialAuth::expireToken);
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        List<SocialAuth> socialAuths = socialAuthRepository.findAllByUser_UserId(userId);
        if (!socialAuths.isEmpty()) {
            socialAuths.forEach(SocialAuth::expireToken);
            socialAuthRepository.deleteAll(socialAuths);
        }

        userRepository.delete(user);
    }
}