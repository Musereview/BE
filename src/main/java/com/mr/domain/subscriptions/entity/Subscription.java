package com.mr.domain.subscriptions.entity;

import com.mr.domain.subscriptions.exception.SubscriptionErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name="subscriptions"
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long id;

    // 유저 아이디
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 구독 등급
    @Column(name = "tier", nullable = false, length = 20)
    private String tier;

    // 구독 시작일
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    // 구독 종료일
    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Builder(access = AccessLevel.PRIVATE)
    private Subscription(Long userId, String tier, LocalDateTime startDate, LocalDateTime endDate) {
        this.userId = userId;
        this.tier = tier;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // 정적 팩토리 메서드 (구독 생성)
    public static Subscription create(Long userId, String tier, LocalDateTime startDate, LocalDateTime endDate) {
        validateDates(startDate, endDate);
        return Subscription.builder()
                .userId(userId)
                .tier(tier)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private static void validateDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new GeneralException(SubscriptionErrorStatus.INVALID_SUBSCRIPTION_DATE);
        }
    }

    // 현재 구독 유효 여부 확인 (현재 시간이 시작일과 종료일 사이인지)
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(this.startDate) && !now.isAfter(this.endDate);
    }

    // 구독 기간 연장
    public void extendSubscription(LocalDateTime newEndDate) {
        if (newEndDate == null || !newEndDate.isAfter(this.endDate)) {
            throw new GeneralException(SubscriptionErrorStatus.INVALID_SUBSCRIPTION_EXTENSION_DATE);
        }
        this.endDate = newEndDate;
    }

    // 구독 티어 변경
    public void changeTier(String newTier) {
        if (newTier != null && !newTier.isBlank()) {
            this.tier = newTier;
        }
    }
}
