package com.mr.domain.auth.service;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final SocialAuthRepository socialAuthRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final OAuthClientService oAuthClientService;
    private final AuthTransactionService authTransactionService;

    public AuthResponseDTO.LoginResponse socialLogin(SocialType socialType, String accessToken, String deviceInfo) {
        OAuthUserInfo userInfo = oAuthClientService.getUserInfo(socialType, accessToken);

        try {
            return authTransactionService.executeSocialLogin(socialType, userInfo, deviceInfo);
        } catch (DataIntegrityViolationException e) {
            return authTransactionService.executeSocialLoginForExistingUser(socialType, userInfo, deviceInfo);
        }
    }

    public AuthResponseDTO.LoginResponse socialLoginByCode(SocialType socialType, String code, String redirectUri, String deviceInfo) {
        OAuthUserInfo userInfo = oAuthClientService.getUserInfoByCode(socialType, code, redirectUri);

        try {
            return authTransactionService.executeSocialLogin(socialType, userInfo, deviceInfo);
        } catch (DataIntegrityViolationException e) {
            return authTransactionService.executeSocialLoginForExistingUser(socialType, userInfo, deviceInfo);
        }
    }

    public AuthResponseDTO.TokenInfo linkSocialAccount(Long userId, SocialType socialType, String accessToken, String deviceInfo) {
        OAuthUserInfo userInfo = oAuthClientService.getUserInfo(socialType, accessToken);
        return authTransactionService.executeLinkSocialAccount(userId, socialType, userInfo, deviceInfo);
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

    private static final long TEMP_CODE_EXPIRATION_SECONDS = 120;
    private final java.util.concurrent.ConcurrentHashMap<String, TempExchangeData> tempCodeStore = new java.util.concurrent.ConcurrentHashMap<>();

    private record TempExchangeData(AuthResponseDTO.LoginResponse loginResponse, LocalDateTime expiresAt) {}

    public String generateTempExchangeCode(AuthResponseDTO.LoginResponse loginResponse) {
        cleanExpiredTempCodes();
        String tempCode = java.util.UUID.randomUUID().toString();
        tempCodeStore.put(tempCode, new TempExchangeData(loginResponse, LocalDateTime.now().plusSeconds(TEMP_CODE_EXPIRATION_SECONDS)));
        return tempCode;
    }

    public AuthResponseDTO.LoginResponse exchangeTempCode(String tempCode) {
        cleanExpiredTempCodes();
        if (tempCode == null || tempCode.isBlank()) {
            throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
        }
        TempExchangeData data = tempCodeStore.remove(tempCode.trim());
        if (data == null || LocalDateTime.now().isAfter(data.expiresAt())) {
            throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
        }
        return data.loginResponse();
    }

    private void cleanExpiredTempCodes() {
        LocalDateTime now = LocalDateTime.now();
        tempCodeStore.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }
}