package com.mr.domain.mentor.controller;

import com.mr.domain.mentor.dto.req.MentorQuestionRequestDTO;
import com.mr.domain.mentor.dto.res.MentorMessageHistoryResponseDTO;
import com.mr.domain.mentor.service.MentorQuestionService;
import com.mr.domain.mentor.service.MentorService;
import com.mr.domain.mentor.service.MentorStreamingService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analyses")
@Tag(name = "AI 멘토", description = "AI 멘토 대화 API")
public class MentorController {

    private final MentorService mentorService;
    private final MentorQuestionService mentorQuestionService;
    private final MentorStreamingService mentorStreamingService;

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

    @PostMapping(
            value = "/{analysisId}/mentor/messages",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    @Operation(summary = "AI 멘토 질문 전송 API", description = "질문을 저장하고 Gemini 답변을 SSE로 실시간 전송합니다.")
    public SseEmitter sendQuestion(
            @PathVariable Long analysisId,
            @RequestBody MentorQuestionRequestDTO request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        MentorQuestionService.PreparedQuestion prepared =
                mentorQuestionService.prepare(userId, analysisId, request.content());
        return mentorStreamingService.stream(prepared);
    }
}
