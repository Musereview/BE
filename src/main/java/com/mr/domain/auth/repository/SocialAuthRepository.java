package com.mr.domain.auth.repository;

import com.mr.domain.auth.entity.SocialAuth;
import com.mr.domain.auth.entity.enums.SocialType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SocialAuthRepository extends JpaRepository<SocialAuth, Long> {

    List<SocialAuth> findAllByUser_UserId(Long userId);
    Optional<SocialAuth> findBySocialTypeAndSocialId(SocialType socialType, String socialId);
    Optional<SocialAuth> findByUser_UserIdAndSocialType(Long userId, SocialType socialType);
    Optional<SocialAuth> findByRefreshTokenHash(String refreshTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SocialAuth s WHERE s.refreshTokenHash = :refreshTokenHash")
    Optional<SocialAuth> findByRefreshTokenHashWithLock(@Param("refreshTokenHash") String refreshTokenHash);
}