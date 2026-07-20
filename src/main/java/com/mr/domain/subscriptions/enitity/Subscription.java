package com.mr.domain.subscriptions.enitity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
}
