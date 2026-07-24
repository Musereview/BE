package com.mr.domain.subscriptions.repository;

import com.mr.domain.subscriptions.entity.Subscription;
import com.mr.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findFirstByUserAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
            User user, LocalDateTime startDateInclusive, LocalDateTime endDateInclusive);
}
