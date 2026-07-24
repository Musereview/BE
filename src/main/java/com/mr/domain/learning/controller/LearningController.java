package com.mr.domain.learning.controller;

import com.mr.domain.learning.dto.req.LearningResultSaveRequestDTO;
import com.mr.domain.learning.dto.res.LearningPracticeDataResponseDTO;
import com.mr.domain.learning.dto.res.LearningProgressResponseDTO;
import com.mr.domain.learning.dto.res.LearningResultResponseDTO;
import com.mr.domain.learning.dto.res.LearningTheoryListResponseDTO;
import com.mr.domain.learning.service.LearningService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.principal.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learnings")
@Tag(name = "학습(Learning)", description = "학습 홈/목록/커리큘럼/단계별 조회 및 진행 결과 저장 API")
public class LearningController {

    private final LearningService learningService;

    // 학습 결과 저장
    @PostMapping("/{learningId}/result")
    public ApiResponse<LearningResultResponseDTO.SaveResultResultDTO> saveLearningResult(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long learningId,
            @Valid @RequestBody LearningResultSaveRequestDTO.SaveResultDTO request
    ){
        LearningResultResponseDTO.SaveResultResultDTO response = learningService.saveResult(userDetails.getUserId(), learningId, request);

        return ApiResponse.onSuccess(response);
    }

    // 학습 진행률 조회
    @GetMapping("/{learningId}/progress")
    public ApiResponse<LearningProgressResponseDTO.ProgressResultDTO> getLearningProgress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long learningId
    ) {
        LearningProgressResponseDTO.ProgressResultDTO result = learningService.getLearningProgress(userDetails.getUserId(), learningId);
        return ApiResponse.onSuccess(result);
    }

    @Operation(
            summary = "단계별 연습 실행 정보 조회",
            description = "[이 이론으로 실습하기] 클릭 시 필요한 bpm/keySignature/midiData를 조회합니다. "
                    + "채점 후 점수 저장은 이 API가 아니라 POST /{learningId}/result가 담당합니다."
    )
    @GetMapping("/{learningId}/steps/{learningStepId}/practice-data")
    public ApiResponse<LearningPracticeDataResponseDTO.PracticeDataResultDTO> getPracticeData(
            @PathVariable Long learningId,
            @PathVariable Long learningStepId
    ) {
        LearningPracticeDataResponseDTO.PracticeDataResultDTO result =
                learningService.getPracticeData(learningId, learningStepId);
        return ApiResponse.onSuccess(result);
    }

    @Operation(
            summary = "학습 주제(THEORY) 전체보기",
            description = "난이도(difficulty) 탭별로 학습 주제 목록을 조회합니다. difficulty는 필수입니다."
    )
    @GetMapping("/theory")
    public ApiResponse<LearningTheoryListResponseDTO.TheoryListResultDTO> getTheoryList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "BEGINNER | INTERMEDIATE | ADVANCED", required = true)
            @RequestParam(required = false) String difficulty
    ) {
        LearningTheoryListResponseDTO.TheoryListResultDTO result =
                learningService.getTheoryList(userDetails.getUserId(), difficulty);
        return ApiResponse.onSuccess(result);
    }
}
