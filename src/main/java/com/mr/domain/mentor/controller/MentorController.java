package com.mr.domain.mentor.controller;

import com.mr.domain.mentor.dto.res.MentorMessageHistoryResponseDTO;
import com.mr.domain.mentor.service.MentorService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analyses")
@Tag(name = "AI 멘토", description = "AI 멘토 대화 API")
public class MentorController {

    private final MentorService mentorService;

    @GetMapping("/{analysisId}/mentor/messages")
    @Operation(
            summary = "AI 멘토 대화 내역 조회 API",
            description = "분석 결과에 연결된 AI 멘토 질문과 답변을 조회합니다."
    )
    public ApiResponse<MentorMessageHistoryResponseDTO> getMessageHistory(
            @PathVariable Long analysisId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.onSuccess(mentorService.getMessageHistory(userId, analysisId));
    }
}
