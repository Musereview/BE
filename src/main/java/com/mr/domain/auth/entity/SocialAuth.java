package com.mr.domain.auth.entity;

import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.exception.AuthErrorStatus;
import com.mr.domain.user.entity.User;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "social_auth",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_social_auth_type_id", columnNames = {"social_type", "social_id"}),
                @UniqueConstraint(name = "uk_social_auth_user_type", columnNames = {"user_id", "social_type"})
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAuth extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_auth_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false, length = 20)
    private SocialType socialType;

    @Column(name = "social_id", nullable = false, length = 100)
    private String socialId;

    @Column(name = "refresh_token_hash", length = 64, unique = true)
    private String refreshTokenHash;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Builder(access = AccessLevel.PRIVATE)
    private SocialAuth(User user, SocialType socialType, String socialId,
                       String refreshTokenHash, LocalDateTime expiredAt, String deviceInfo) {

        validateUser(user);
        validateSocialType(socialType);
        validateSocialId(socialId);

        this.user = user;
        this.socialType = socialType;
        this.socialId = socialId;
        this.refreshTokenHash = refreshTokenHash;
        this.expiredAt = expiredAt;
        this.deviceInfo = deviceInfo;
    }

    public static SocialAuth create(User user, SocialType socialType, String socialId,
                                    String tokenHash, LocalDateTime expiredAt, String deviceInfo) {

        validateTokenValue(tokenHash);
        validateExpiryTime(expiredAt);

        return SocialAuth.builder()
                .user(user)
                .socialType(socialType)
                .socialId(socialId)
                .refreshTokenHash(tokenHash)
                .expiredAt(expiredAt)
                .deviceInfo(deviceInfo)
                .build();
    }

    private static void validateUser(User user) {
        if (user == null) {
            throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
        }
    }

    private static void validateSocialType(SocialType socialType) {
        if (socialType == null) {
            throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
        }
    }

    private static void validateSocialId(String socialId) {
        if (socialId == null || socialId.isBlank()) {
            throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
        }
    }

    private static void validateTokenValue(String token) {
        if (token == null || token.isBlank()) {
            throw new GeneralException(AuthErrorStatus.TOKEN_MISSING);
        }
    }

    private static void validateExpiryTime(LocalDateTime expiredAt) {
        if (expiredAt == null || !expiredAt.isAfter(LocalDateTime.now(ZoneId.of("Asia/Seoul")))) {
            throw new GeneralException(AuthErrorStatus.INVALID_TOKEN_EXPIRY);
        }
    }

    public void updateRefreshToken(String tokenHash, LocalDateTime newExpiredAt, String deviceInfo) {
        validateTokenValue(tokenHash);
        validateExpiryTime(newExpiredAt);

        this.refreshTokenHash = tokenHash;
        this.expiredAt = newExpiredAt;
        this.deviceInfo = deviceInfo;
    }

    public void expireToken() {
        this.refreshTokenHash = null;
        this.expiredAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}