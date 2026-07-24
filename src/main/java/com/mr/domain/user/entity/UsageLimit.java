package com.mr.domain.user.entity;

import com.mr.domain.user.exception.UserUsageErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "usage_limit",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_usage_limit_user_date", columnNames = {"user_id", "limit_date"})
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsageLimit extends BaseCreatedEntity {

    private static final int DEFAULT_MAX_FREE_COUNT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_limit_id")
    private Long id;

    // TODO: User 엔티티 연관관계 연결 예정
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "limit_date", nullable = false)
    private LocalDate limitDate;

    @Column(name = "remaining_count", nullable = false)
    private Integer remainingCount;

    @Column(name = "max_count", nullable = false)
    private Integer maxCount;

    //낙관적 락 버전
    @Version
    private Long version;

    @Builder(access = AccessLevel.PRIVATE)
    private UsageLimit(Long userId, LocalDate limitDate, Integer remainingCount, Integer maxCount) {
        validateRequired(userId);
        validateRequired(limitDate);
        validatePositiveOrZero(remainingCount);
        validatePositiveOrZero(maxCount);
        validateCountRange(remainingCount, maxCount);

        this.userId = userId;
        this.limitDate = limitDate;
        this.remainingCount = remainingCount;
        this.maxCount = maxCount;
    }

    public static UsageLimit createDefault(Long userId, LocalDate limitDate) {
        return UsageLimit.builder()
                .userId(userId)
                .limitDate(limitDate)
                .maxCount(DEFAULT_MAX_FREE_COUNT)
                .remainingCount(DEFAULT_MAX_FREE_COUNT)
                .build();
    }

    private static void validateRequired(Object value) {
        if (value == null) {
            throw new GeneralException(UserUsageErrorStatus.REQUIRED_FIELD_MISSING);
        }
    }
    private static void validatePositiveOrZero(Integer value) {
        if (value == null || value < 0) {
            throw new GeneralException(UserUsageErrorStatus.INVALID_USAGE_COUNT_RANGE);
        }
    }
    private static void validateCountRange(Integer remainingCount, Integer maxCount) {
        if (remainingCount != null && maxCount != null && remainingCount > maxCount) {
            throw new GeneralException(UserUsageErrorStatus.EXCEEDED_MAX_COUNT);
        }
    }

    // 분석 요청 시 카운트 로직
    public void consume() {
        if (this.remainingCount <= 0) {
            throw new GeneralException(UserUsageErrorStatus.USAGE_LIMIT_EXCEEDED);
        }
        this.remainingCount--;
    }

    // 관리자 기능 - 잔여 횟수 수동 조절
    public void updateRemainingCount(Integer newCount) {
        validatePositiveOrZero(newCount);
        validateCountRange(newCount, this.maxCount);
        this.remainingCount = newCount;
    }
    public void updateMaxCount(Integer newMaxCount) {
        validatePositiveOrZero(newMaxCount);
        this.maxCount = newMaxCount;

        // 상한선이 깎여서 현재 잔여량이 상한보다 커진 경우 정정 보정 로직 포함
        if (this.remainingCount > this.maxCount) {
            this.remainingCount = this.maxCount;
        }
    }
}