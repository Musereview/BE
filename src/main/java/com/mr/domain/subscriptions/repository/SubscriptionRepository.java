package com.mr.domain.subscriptions.repository;

import com.mr.domain.subscriptions.entity.Subscription;
import com.mr.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // 결제 시스템이 없어 만료(end_date)를 검사/집행하지 않기로 팀 확정(docs/학습·사용자관리 API 명세서.md 4장)
    // → 만료일과 무관하게 가장 최근 구독 1건을 그대로 조회
    Optional<Subscription> findFirstByUserOrderByStartDateDesc(User user);

    // TODO: 실제 결제/구독 만료 로직 도입 시 복구 (재도입 시 java.time.LocalDateTime import 필요)
    // Optional<Subscription> findFirstByUserAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
    //         User user, LocalDateTime startDateInclusive, LocalDateTime endDateInclusive);
}
