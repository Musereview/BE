package com.mr.domain.auth.entity;

import com.mr.domain.auth.exception.AuthErrorStatus;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
// TODO: 추후 User 도메인 완성 시 단방향/양방향 인덱스 추가
@Table(
        name = "social_auth",
        indexes = {
                @Index(name = "idx_social_auth_token_hash", columnList = "refresh_token_hash")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAuth extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_auth_id")
    private Long id;

    // TODO: User 엔티티 연관관계 연결 예정
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false, length = 20)
    private SocialType socialType;

    @Column(name = "social_id", nullable = false, length = 100)
    private String socialId;

    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;

    @Column(name = "refresh_token_hash", length = 64, unique = true)
    private String refreshTokenHash;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Builder(access = AccessLevel.PRIVATE)
    private SocialAuth(Long userId, SocialType socialType, String socialId, String refreshToken,
                       String refreshTokenHash, LocalDateTime expiredAt, String deviceInfo) {
        validateRequiredField(userId);
        validateRequiredField(socialType);
        validateRequiredField(socialId);

        this.userId = userId;
        this.socialType = socialType;
        this.socialId = socialId;
        this.refreshToken = refreshToken;
        this.refreshTokenHash = refreshTokenHash;
        this.expiredAt = expiredAt;
        this.deviceInfo = deviceInfo;
    }

    public static SocialAuth create(Long userId, SocialType socialType, String socialId,
                                    String encryptedToken, String tokenHash, LocalDateTime expiredAt, String deviceInfo) {
        validateRequiredField(encryptedToken);
        validateRequiredField(tokenHash);
        validateExpiryTime(expiredAt);

        return SocialAuth.builder()
                .userId(userId)
                .socialType(socialType)
                .socialId(socialId)
                .refreshToken(encryptedToken)
                .refreshTokenHash(tokenHash)
                .expiredAt(expiredAt)
                .deviceInfo(deviceInfo)
                .build();
    }
    private static void validateRequiredField(Object value) {
        if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
            throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
        }
    }
    private static void validateExpiryTime(LocalDateTime expiredAt) {
        if (expiredAt == null || expiredAt.isBefore(LocalDateTime.now())) {
            throw new GeneralException(AuthErrorStatus.INVALID_TOKEN_EXPIRY);
        }
    }

    public void updateRefreshToken(String encryptedToken, String tokenHash, LocalDateTime newExpiredAt, String deviceInfo) {
        validateRequiredField(encryptedToken);
        validateRequiredField(tokenHash);
        validateExpiryTime(newExpiredAt);

        this.refreshToken = encryptedToken;
        this.refreshTokenHash = tokenHash;
        this.expiredAt = newExpiredAt;
        this.deviceInfo = deviceInfo;
    }

    public void expireToken() {
        this.refreshToken = null;
        this.refreshTokenHash = null;
        this.expiredAt = null;
    }
}