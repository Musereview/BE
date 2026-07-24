package com.mr.domain.user.repository;

import com.mr.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

// NOTE: feat/#32-learning-status-api(PR #46, 동균 강)에도 동일한 이름의 빈 UserRepository가 있음.
// 그쪽이 먼저 머지되면 병합 시 이 파일(메서드 포함된 상위호환 버전)로 유지하면 됨.
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByNicknameAndUserIdNot(String nickname, Long userId);
}
