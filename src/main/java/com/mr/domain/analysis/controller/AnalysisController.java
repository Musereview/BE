package com.mr.domain.analysis.controller;

import com.mr.domain.analysis.dto.res.AnalysisResultResponseDTO;
import com.mr.domain.analysis.dto.res.AnalysisStatusResponseDTO;
import com.mr.domain.analysis.service.AnalysisService;
import com.mr.global.apipayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analyses")
public class AnalysisController {

    private static final Long TEMP_USER_ID = 1L;

    private final AnalysisService analysisService;

    @GetMapping("/{analysisId}/status")
    public ApiResponse<AnalysisStatusResponseDTO> getAnalysisStatus(
            @PathVariable Long analysisId
    ) {
        // TODO: SecurityUtil 연동
        Long userId = TEMP_USER_ID;

        return ApiResponse.onSuccess(
                analysisService.getAnalysisStatus(userId, analysisId)
        );
    }

    @GetMapping("/{analysisId}")
    public ApiResponse<AnalysisResultResponseDTO> getAnalysisResult(
            @PathVariable Long analysisId
    ) {
        // TODO: SecurityUtil 연동
        Long userId = TEMP_USER_ID;

        return ApiResponse.onSuccess(
                analysisService.getAnalysisResult(userId, analysisId)
        );
    }
}