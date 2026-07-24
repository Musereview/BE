package com.mr.domain.user.dto;

import com.mr.domain.subscriptions.entity.enums.SubscriptionTier;
import com.mr.domain.user.entity.enums.TheoryLevel;

public class UserProfileRequestDTO {

    public record OnboardingRequest(
            String nickname,
            TheoryLevel skillLevel,
            SubscriptionTier subscriptionTier
    ) {}

    public record UpdateRequest(
            String nickname,
            TheoryLevel skillLevel
    ) {}
}
