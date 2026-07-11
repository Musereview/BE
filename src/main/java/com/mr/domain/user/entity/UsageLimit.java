package com.mr.domain.user.entity;

import com.mr.domain.user.entity.enums.UserUsageErrorStatus;
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
        indexes = {
                @Index(name = "idx_usage_limit_user_date", columnList = "user_id, limit_date", unique = true)
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsageLimit extends BaseCreatedEntity {

    private static final int DEFAULT_MAX_FREE_COUNT = 3; // Free 3회 제한

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

    @Builder(access = AccessLevel.PRIVATE)
    private UsageLimit(Long userId, LocalDate limitDate, Integer remainingCount, Integer maxCount) {
        validateRequired(userId, "userId");
        validateRequired(limitDate, "limitDate");
        validatePositiveOrZero(remainingCount, "remainingCount");
        validatePositiveOrZero(maxCount, "maxCount");

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

    public static void validateRequired(Object value, String fieldName) {
        if (value == null) {
            throw new GeneralException(UserUsageErrorStatus.INVALID_USAGE_COUNT);
        }
    }

    private static void validatePositiveOrZero(Integer value, String fieldName) {
        if (value == null || value < 0) {
            throw new GeneralException(UserUsageErrorStatus.INVALID_USAGE_COUNT);
        }
    }

    // 분석 요청 시 카운트 로직
    public void consume() {
        if (this.remainingCount <= 0) {
            throw new GeneralException(UserUsageErrorStatus.USAGE_LIMIT_EXCEEDED);
        }
        this.remainingCount--;
    }

    // 관리자 기능으로 잔여 횟수 수동 조절할 때 사용
    public void updateRemainingCount(Integer newCount) {
        validatePositiveOrZero(newCount, "newCount");
        if (newCount > this.maxCount) {
            throw new GeneralException(UserUsageErrorStatus.INVALID_USAGE_COUNT);
        }
        this.remainingCount = newCount;
    }
}