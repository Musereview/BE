package com.mr.domain.statistics.controller;

import com.mr.domain.statistics.dto.req.StatisticsPeriod;
import com.mr.domain.statistics.dto.res.StatisticsResponseDTO;
import com.mr.domain.statistics.service.StatisticsService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.SecurityUtil;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    // MVP: period/from/to 값과 무관하게 이번 주 vs 지난 주 기준으로 고정 집계
    @GetMapping
    public ApiResponse<StatisticsResponseDTO> getStatistics(
            @RequestParam(required = false) StatisticsPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        return ApiResponse.onSuccess(
                statisticsService.getStatistics(userId, period, from, to)
        );
    }
}
