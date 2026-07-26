package com.mr.domain.history.controller;

import com.mr.domain.history.dto.req.HistoryPeriod;
import com.mr.domain.history.dto.res.HistoryDetailResponseDTO;
import com.mr.domain.history.dto.res.HistoryListResponseDTO;
import com.mr.domain.history.service.HistoryService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/histories")
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping
    public ApiResponse<HistoryListResponseDTO> getHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) HistoryPeriod period
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        return ApiResponse.onSuccess(
                historyService.getHistories(userId, page, size, period)
        );
    }

    @GetMapping("/{playingId}")
    public ApiResponse<HistoryDetailResponseDTO> getHistoryDetail(
            @PathVariable Long playingId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();

        return ApiResponse.onSuccess(
                historyService.getHistoryDetail(userId, playingId)
        );
    }
}
