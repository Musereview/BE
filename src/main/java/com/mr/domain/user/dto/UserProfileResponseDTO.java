package com.mr.domain.user.dto;

import com.mr.domain.user.entity.enums.TheoryLevel;
import java.time.LocalDateTime;
import lombok.Builder;

public class UserProfileResponseDTO {

    @Builder
    public record ProfileResponse(
            String nickname,
            String profileImgUrl,
            String instrumentType,
            TheoryLevel skillLevel,
            String subscriptionTier,
            StatisticsResponse statistics
    ) {}

    @Builder
    public record StatisticsResponse(
            Long practiceSessionCount,
            Long totalPracticeMinutes,
            Long completedLearningCount
    ) {}

    @Builder
    public record OnboardingResponse(
            Long userId,
            String nickname,
            TheoryLevel skillLevel,
            String instrumentType,
            String subscriptionTier,
            String profileImgUrl
    ) {}

    @Builder
    public record UpdateResponse(
            Long userId,
            String nickname,
            TheoryLevel skillLevel,
            LocalDateTime updatedAt
    ) {}
}
