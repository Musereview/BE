package com.mr.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.auth.entity.enums.SocialType;
import com.mr.domain.auth.exception.AuthErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OAuthTempCodeStore {

    private static final String KEY_PREFIX = "oauth:temp-code:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public OAuthTempCodeStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${oauth.temp-code-ttl:2m}") Duration ttl
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("OAuth 임시 코드 TTL은 0보다 커야 합니다.");
        }
        this.ttl = ttl;
    }

    public void save(String tempCode, TempExchangeData data) {
        if (tempCode == null || tempCode.isBlank() || data == null) {
            throw new GeneralException(AuthErrorStatus.INVALID_AUTH_REQUEST);
        }

        try {
            redisTemplate.opsForValue().set(key(tempCode), objectMapper.writeValueAsString(data), ttl);
        } catch (JsonProcessingException | DataAccessException exception) {
            throw new GeneralException(AuthErrorStatus.TEMP_CODE_STORE_UNAVAILABLE);
        }
    }

    public Optional<TempExchangeData> consume(String tempCode) {
        if (tempCode == null || tempCode.isBlank()) {
            return Optional.empty();
        }

        try {
            String value = redisTemplate.opsForValue().getAndDelete(key(tempCode.trim()));
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, TempExchangeData.class));
        } catch (JsonProcessingException | DataAccessException exception) {
            throw new GeneralException(AuthErrorStatus.TEMP_CODE_STORE_UNAVAILABLE);
        }
    }

    private String key(String tempCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(tempCode.trim().getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", exception);
        }
    }

    public record TempExchangeData(
            Long userId,
            SocialType socialType,
            String socialId,
            String profileImgUrl,
            String deviceInfo
    ) {}
}
