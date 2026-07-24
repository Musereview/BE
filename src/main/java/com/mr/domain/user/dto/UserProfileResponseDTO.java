package com.mr.domain.user.dto;

import com.mr.domain.user.entity.enums.TheoryLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;

public class UserProfileResponseDTO {

    @Builder
    @Schema(description = "프로필 조회 응답")
    public record ProfileResponse(
            @Schema(description = "닉네임", example = "김뮤즈")
            String nickname,

            @Schema(description = "프로필 이미지 URL", example = "https://musereview-storage-526426842030-ap-northeast-2.s3.ap-northeast-2.amazonaws.com/profile/default-profile.png")
            String profileImgUrl,

            @Schema(description = "대표 악기 (MVP는 PIANO 고정)", example = "PIANO")
            String instrumentType,

            @Schema(description = "화성학 숙련도", example = "INTERMEDIATE")
            TheoryLevel skillLevel,

            @Schema(description = "구독 등급", example = "PRO")
            String subscriptionTier,

            StatisticsResponse statistics
    ) {}

    @Builder
    @Schema(description = "누적 학습 통계")
    public record StatisticsResponse(
            @Schema(description = "누적 연주 세션 수", example = "24")
            Long practiceSessionCount,

            @Schema(description = "누적 연습 시간(분)", example = "350")
            Long totalPracticeMinutes,

            @Schema(description = "완료한 학습 개수 (score 90점 이상)", example = "8")
            Long completedLearningCount
    ) {}

    @Builder
    @Schema(description = "프로필 최초 등록(온보딩) 응답")
    public record OnboardingResponse(
            @Schema(description = "사용자 ID", example = "1")
            Long userId,

            @Schema(description = "닉네임", example = "김뮤즈")
            String nickname,

            @Schema(description = "화성학 숙련도", example = "INTERMEDIATE")
            TheoryLevel skillLevel,

            @Schema(description = "대표 악기", example = "PIANO")
            String instrumentType,

            @Schema(description = "구독 등급", example = "PRO")
            String subscriptionTier,

            @Schema(description = "프로필 이미지 URL", example = "https://musereview-storage-526426842030-ap-northeast-2.s3.ap-northeast-2.amazonaws.com/profile/default-profile.png")
            String profileImgUrl
    ) {}

    @Builder
    @Schema(description = "프로필 수정 응답")
    public record UpdateResponse(
            @Schema(description = "사용자 ID", example = "1")
            Long userId,

            @Schema(description = "수정된 닉네임", example = "새로운뮤즈")
            String nickname,

            @Schema(description = "수정된 화성학 숙련도", example = "ADVANCED")
            TheoryLevel skillLevel,

            @Schema(description = "처리 완료 시각", example = "2026-07-24T01:30:00")
            LocalDateTime updatedAt
    ) {}
}
