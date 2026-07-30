package com.mr.domain.statistics.controller;

import com.mr.domain.statistics.dto.res.StatisticsResponseDTO;
import com.mr.domain.statistics.service.StatisticsService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/statistics")
@Tag(name = "통계", description = "현재 로그인한 사용자의 연습 및 분석 통계 조회 API")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(
            summary = "통계 화면 조회",
            description = "현재 로그인한 사용자의 이번 주 요약, 지난주 대비 영역별 성장, 최근 4주 점수 추이를 조회합니다. "
                    + "조회 기간은 월요일 00시를 기준으로 고정되며 별도의 기간 쿼리 파라미터를 받지 않습니다."
    )
    @GetMapping
    public ApiResponse<StatisticsResponseDTO> getStatistics() {
        Long userId = SecurityUtil.getCurrentUserId();

        return ApiResponse.onSuccess(
                statisticsService.getStatistics(userId)
        );
    }
}
