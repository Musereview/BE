package com.mr.domain.home.controller;

import com.mr.domain.home.dto.res.HomeResponseDTO;
import com.mr.domain.home.service.HomeService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
@Tag(name = "홈", description = "홈 API")
public class HomeController {

    private final HomeService homeService;

    @Operation(
            summary = "홈 화면 요약 조회 API",
            description = "현재 로그인한 사용자의 프로필 요약, 연속 출석 현황, 주/월간 연습 시간, 진행 중인 학습(재도전 포함), "
                    + "추천 학습, 최근 완료한 연습 목록을 한 번에 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_200",
                                    summary = "홈 화면 요약 조회 성공 예시",
                                    value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON_200",
                                      "message": "요청에 성공하였습니다.",
                                      "data": {
                                        "user": {
                                          "userId": 1,
                                          "nickname": "김뮤즈",
                                          "profileImgUrl": "https://cdn.example.com/profile/1.png",
                                          "skillLevel": "INTERMEDIATE",
                                          "instrumentType": "KEYBOARD"
                                        },
                                        "streak": {
                                          "currentDays": 4,
                                          "message": "4일 연속 학습 중이에요!",
                                          "weeklyAttendance": [
                                            { "dayOfWeek": "MON", "label": "월", "status": "COMPLETED" },
                                            { "dayOfWeek": "TUE", "label": "화", "status": "COMPLETED" },
                                            { "dayOfWeek": "WED", "label": "수", "status": "MISSED" },
                                            { "dayOfWeek": "THU", "label": "목", "status": "COMPLETED" },
                                            { "dayOfWeek": "FRI", "label": "금", "status": "TODAY_COMPLETED" },
                                            { "dayOfWeek": "SAT", "label": "토", "status": "EMPTY" },
                                            { "dayOfWeek": "SUN", "label": "일", "status": "EMPTY" }
                                          ]
                                        },
                                        "practiceSummary": {
                                          "weeklyPracticeHours": 21,
                                          "monthlyPracticeHours": 60,
                                          "monthLabel": "8월"
                                        },
                                        "currentLearning": {
                                          "learningId": 5,
                                          "title": "Tension Notes",
                                          "subtitle": "11th 텐션 노트 활용하기",
                                          "level": "ADVANCED",
                                          "progressRate": 10,
                                          "nextStepId": 13
                                        },
                                        "recommendedLearnings": [
                                          {
                                            "learningId": 5,
                                            "title": "Tension Notes",
                                            "subtitle": "13th 텐션 노트 활용하기",
                                            "level": "ADVANCED",
                                            "nextStepId": 14
                                          }
                                        ],
                                        "recentPlayings": [
                                          {
                                            "playingId": 31,
                                            "title": "Jazz Standard Practice",
                                            "genre": "JAZZ",
                                            "key": "C Major",
                                            "bpm": 120,
                                            "playedAt": "2026-08-11T14:32:00",
                                            "relativeTime": "오늘",
                                            "durationMinutes": 12
                                          }
                                        ]
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (로그인 토큰 누락/만료)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_401_01",
                                    summary = "인증 토큰 누락/만료",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON_401_01",
                                      "message": "인증이 필요합니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "USER_404_01",
                                    summary = "탈퇴 등으로 존재하지 않는 사용자",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "USER_404_01",
                                      "message": "존재하지 않는 사용자입니다.",
                                      "data": null
                                    }
                                    """
                            )
                    )
            )
    })
    @GetMapping
    public ApiResponse<HomeResponseDTO> getHome() {
        Long userId = SecurityUtil.getCurrentUserId();

        return ApiResponse.onSuccess(homeService.getHome(userId));
    }
}
