package com.mr.domain.analysis.controller;

import com.mr.domain.analysis.dto.res.AnalysisResultResponseDTO;
import com.mr.domain.analysis.dto.res.AnalysisStatusResponseDTO;
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

    @GetMapping("/{analysisId}/status")
    public ApiResponse<AnalysisStatusResponseDTO> getAnalysisStatus(
            @PathVariable Long analysisId
    ) {
        return ApiResponse.onSuccess(null);
    }

    @GetMapping("/{analysisId}")
    public ApiResponse<AnalysisResultResponseDTO> getAnalysisResult(
            @PathVariable Long analysisId
    ) {
        return ApiResponse.onSuccess(null);
    }
}