package com.mr.domain.subscriptions.enitity;

import com.mr.domain.subscriptions.enitity.enums.SubscriptionTier;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name="subscriptions")
public class Subscription {

    @Id
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
}
