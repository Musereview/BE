package com.mr.domain.user.repository;

import com.mr.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByNicknameAndUserIdNot(String nickname, Long userId);

    // 유저 단위로 쓰기 작업을 직렬화하기 위한 비관적 락 조회.
    // 학습 결과 저장(saveResult)에서 같은 유저의 동시 요청 간 TOCTOU를 막는 데 사용 —
    // 패키지(Learning) 행이 아니라 유저 행에 걸어서, 서로 다른 유저끼리는 잠기지 않게 한다.
    // 락 대기가 무한정 걸리지 않도록 3초 타임아웃 설정 (초과 시 PessimisticLockException)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("select u from User u where u.userId = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);
}