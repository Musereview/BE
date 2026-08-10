package com.mr.domain.home.controller;

import com.mr.domain.home.dto.res.HomeResponseDTO;
import com.mr.domain.home.service.HomeService;
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
@RequestMapping("/api/home")
@Tag(name = "홈", description = "홈 대시보드 요약 조회 API")
public class HomeController {

    private final HomeService homeService;

    @Operation(
            summary = "홈 화면 요약 조회",
            description = "현재 로그인한 사용자의 프로필 요약, 연속 출석 현황, 주/월간 연습 시간, 진행 중인 학습, "
                    + "가장 최근에 시도한 학습 단계(재도전 포함), 추천 학습, 최근 완료한 연습 목록을 조회합니다."
    )
    @GetMapping
    public ApiResponse<HomeResponseDTO> getHome() {
        Long userId = SecurityUtil.getCurrentUserId();

        return ApiResponse.onSuccess(homeService.getHome(userId));
    }
}
