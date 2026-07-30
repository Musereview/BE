package com.mr.domain.auth.service;

import com.mr.domain.auth.dto.OAuthCredential;
import com.mr.domain.auth.dto.OAuthUserInfo;
import com.mr.domain.auth.dto.res.AuthResponseDTO;
import com.mr.domain.auth.entity.SocialAuth;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.exception.AuthErrorStatus;
import com.mr.domain.auth.repository.SocialAuthRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.exception.UserErrorStatus;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate transactionTemplate;

    @Value("${app.profile.default-image-url}")
    private String defaultProfileImageUrl;

    @Transactional
    public AuthResponseDTO.LoginResponse socialLogin(SocialType socialType, OAuthCredential credential, String redirectUri, String deviceInfo) {
        OAuthUserInfo userInfo = fetchOAuthUserInfo(socialType, credential, redirectUri);

        try {
            return executeSocialLogin(socialType, userInfo, deviceInfo);
        } catch (DataIntegrityViolationException e) {
            return executeSocialLoginForExistingUser(socialType, userInfo, deviceInfo);
        }
    }

    private OAuthUserInfo fetchOAuthUserInfo(SocialType socialType, OAuthCredential credential, String redirectUri) {
        if (credential.type() == OAuthCredential.CredentialType.ACCESS_TOKEN) {
            if (redirectUri != null && !redirectUri.isBlank()) {
                return oAuthClientService.getUserInfo(socialType, credential.value(), redirectUri);
            }
            return oAuthClientService.getUserInfo(socialType, credential.value());
        }
        if (redirectUri != null && !redirectUri.isBlank()) {
            return oAuthClientService.getUserInfo(socialType, credential, redirectUri);
        }
        return oAuthClientService.getUserInfo(socialType, credential);
    }

    public AuthResponseDTO.LoginResponse socialLogin(SocialType socialType, String codeOrToken, String redirectUri, String deviceInfo) {
        return socialLogin(socialType, new OAuthCredential(OAuthCredential.CredentialType.ACCESS_TOKEN, codeOrToken), redirectUri, deviceInfo);
    }

    public AuthResponseDTO.LoginResponse socialLogin(SocialType socialType, String codeOrToken, String deviceInfo) {
        return socialLogin(socialType, codeOrToken, null, deviceInfo);
    }

    private AuthResponseDTO.LoginResponse executeSocialLogin(SocialType socialType, OAuthUserInfo userInfo, String deviceInfo) {
        return transactionTemplate.execute(status -> {
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
                socialAuthRepository.saveAndFlush(socialAuth);
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
        });
    }

    private AuthResponseDTO.LoginResponse executeSocialLoginForExistingUser(SocialType socialType, OAuthUserInfo userInfo, String deviceInfo) {
        return transactionTemplate.execute(status -> {
            SocialAuth socialAuth = socialAuthRepository.findBySocialTypeAndSocialId(socialType, userInfo.socialId())
                    .orElseThrow(() -> new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST));

            User user = socialAuth.getUser();

            String newAccessToken = tokenProvider.createAccessToken(user.getUserId());
            String newRefreshToken = tokenProvider.createRefreshToken(user.getUserId());
            String refreshTokenHash = tokenProvider.hashToken(newRefreshToken);
            LocalDateTime expiryTime = tokenProvider.getRefreshTokenExpiryTime();

            socialAuth.updateRefreshToken(refreshTokenHash, expiryTime, deviceInfo);

            AuthResponseDTO.TokenResponse tokenResponse = AuthResponseDTO.TokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .accessTokenExpiresInSeconds(tokenProvider.getAccessTokenExpirationSeconds())
                    .build();

            return AuthResponseDTO.LoginResponse.builder()
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .isNewUser(false)
                    .isOnboardingCompleted(user.isOnboardingCompleted())
                    .tokenInfo(tokenResponse)
                    .build();
        });
    }

    public AuthResponseDTO.TokenInfo linkSocialAccount(Long userId, SocialType socialType, OAuthCredential credential, String redirectUri, String deviceInfo) {
        OAuthUserInfo userInfo = fetchOAuthUserInfo(socialType, credential, redirectUri);

        return transactionTemplate.execute(status -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

            Optional<SocialAuth> existingSocialAuth = socialAuthRepository.findBySocialTypeAndSocialId(socialType, userInfo.socialId());
            if (existingSocialAuth.isPresent()) {
                SocialAuth socialAuth = existingSocialAuth.get();
                if (!socialAuth.getUser().getUserId().equals(userId)) {
                    throw new GeneralException(AuthErrorStatus.ALREADY_LINKED_SOCIAL_ACCOUNT);
                }
            }

            String newAccessToken = tokenProvider.createAccessToken(userId);
            String newRefreshToken = tokenProvider.createRefreshToken(userId);
            String refreshTokenHash = tokenProvider.hashToken(newRefreshToken);
            LocalDateTime expiryTime = tokenProvider.getRefreshTokenExpiryTime();

            if (existingSocialAuth.isPresent()) {
                SocialAuth socialAuth = existingSocialAuth.get();
                socialAuth.updateRefreshToken(refreshTokenHash, expiryTime, deviceInfo);
            } else {
                SocialAuth newSocialAuth = SocialAuth.create(
                        user,
                        socialType,
                        userInfo.socialId(),
                        refreshTokenHash,
                        expiryTime,
                        deviceInfo
                );
                socialAuthRepository.save(newSocialAuth);
            }

            return AuthResponseDTO.TokenInfo.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .accessTokenExpiresInSeconds(tokenProvider.getAccessTokenExpirationSeconds())
                    .build();
        });
    }

    public AuthResponseDTO.TokenInfo linkSocialAccount(Long userId, SocialType socialType, String codeOrToken, String deviceInfo) {
        return linkSocialAccount(userId, socialType, new OAuthCredential(OAuthCredential.CredentialType.ACCESS_TOKEN, codeOrToken), null, deviceInfo);
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

        Long userId = tokenProvider.getUserIdFromToken(refreshToken);
        String requestTokenHash = tokenProvider.hashToken(refreshToken);

        SocialAuth socialAuth = socialAuthRepository.findByRefreshTokenHashWithLock(requestTokenHash)
                .filter(auth -> auth.getUser().getUserId().equals(userId))
                .filter(auth -> !auth.isExpired())
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
    public void logout(Long userId, String refreshToken) {
        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            throw new GeneralException(AuthErrorStatus.INVALID_TOKEN);
        }

        String requestTokenHash = tokenProvider.hashToken(refreshToken);

        SocialAuth socialAuth = socialAuthRepository.findByRefreshTokenHashWithLock(requestTokenHash)
                .filter(auth -> auth.getUser().getUserId().equals(userId))
                .orElseThrow(() -> new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST));

        socialAuth.expireToken();
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