package com.mr.domain.analysis.controller;

import com.mr.domain.analysis.dto.req.AnalysisCreateRequestDTO;
import com.mr.domain.analysis.dto.res.AnalysisCreateResponseDTO;
import com.mr.domain.analysis.dto.res.AnalysisResultResponseDTO;
import com.mr.domain.analysis.dto.res.AnalysisStatusResponseDTO;
import com.mr.domain.analysis.service.AnalysisService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/analyses")
@Tag(name = "분석", description = "연주 분석 요청 및 결과 조회 API")
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping
    @Operation(
            summary = "분석 요청 생성 API",
            description = "연주 데이터를 기반으로 AI 분석을 비동기로 요청합니다."
    )
    public ApiResponse<AnalysisCreateResponseDTO> createAnalysis(
            @Valid @RequestBody AnalysisCreateRequestDTO request
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.onSuccess(analysisService.createAnalysis(userId, request));
    }

    @GetMapping("/{analysisId}/status")
    @Operation(
            summary = "분석 상태 조회 API",
            description = "분석 요청의 현재 처리 상태를 조회합니다."
    )
    public ApiResponse<AnalysisStatusResponseDTO> getAnalysisStatus(
            @PathVariable Long analysisId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        return ApiResponse.onSuccess(
                analysisService.getAnalysisStatus(userId, analysisId)
        );
    }

    @GetMapping("/{analysisId}")
    @Operation(
            summary = "분석 결과 조회 API",
            description = "완료된 분석 결과와 생성된 리포트를 조회합니다."
    )
    public ApiResponse<AnalysisResultResponseDTO> getAnalysisResult(
            @PathVariable Long analysisId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        return ApiResponse.onSuccess(
                analysisService.getAnalysisResult(userId, analysisId)
        );
    }
}
