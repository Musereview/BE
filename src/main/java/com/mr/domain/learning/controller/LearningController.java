package com.mr.domain.learning.controller;

import com.mr.domain.learning.dto.req.LearningResultSaveRequestDTO;
import com.mr.domain.learning.dto.res.LearningProgressResponseDTO;
import com.mr.domain.learning.dto.res.LearningResultResponseDTO;
import com.mr.domain.learning.service.LearningService;
import com.mr.global.apipayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
            @PathVariable Long learningId,  // 임시
            @Valid @RequestBody LearningResultSaveRequestDTO.SaveResultDTO request
    ){
        Long userId = 1L;   // 임시
        LearningResultResponseDTO.SaveResultResultDTO response =
                learningService.saveResult(userId, learningId, request);

        return ApiResponse.onSuccess(response);
    }

    // 학습 진행률 조회
    @GetMapping("/{learningId}/progress")
    public ApiResponse<LearningProgressResponseDTO.ProgressResultDTO> getLearningProgress(
            @PathVariable Long learningId
    ) {
        LearningProgressResponseDTO.ProgressResultDTO result = learningService.getLearningProgress(learningId);
        return ApiResponse.onSuccess(result);
    }
}
