package com.mr.domain.learning.controller;

import com.mr.domain.learning.dto.req.LearningResultSaveRequestDTO;
import com.mr.domain.learning.dto.res.LearningProgressResponseDTO;
import com.mr.domain.learning.dto.res.LearningResultResponseDTO;
import com.mr.domain.learning.service.LearningService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.principal.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learnings")
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
}
