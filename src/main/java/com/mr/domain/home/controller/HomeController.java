package com.mr.domain.home.controller;

import com.mr.domain.home.dto.res.HomeResponseDTO;
import com.mr.domain.home.service.HomeService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    public ApiResponse<HomeResponseDTO> getHome() {
        Long userId = SecurityUtil.getCurrentUserId();

        return ApiResponse.onSuccess(homeService.getHome(userId));
    }
}
