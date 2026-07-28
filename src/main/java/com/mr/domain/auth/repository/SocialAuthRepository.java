package com.mr.domain.auth.repository;

import com.mr.domain.auth.entity.SocialAuth;
import com.mr.domain.auth.entity.enums.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialAuthRepository extends JpaRepository<SocialAuth, Long> {

    List<SocialAuth> findAllByUser_UserId(Long userId);
    Optional<SocialAuth> findBySocialTypeAndSocialId(SocialType socialType, String socialId);
    Optional<SocialAuth> findByUser_UserIdAndSocialType(Long userId, SocialType socialType);
    Optional<SocialAuth> findByRefreshTokenHash(String refreshTokenHash);
}