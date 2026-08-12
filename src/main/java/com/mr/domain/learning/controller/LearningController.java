package com.mr.domain.learning.controller;

import com.mr.domain.learning.dto.req.LearningResultSaveRequestDTO;
import com.mr.domain.learning.dto.res.LearningAccompanimentListResponseDTO;
import com.mr.domain.learning.dto.res.LearningCurriculumResponseDTO;
import com.mr.domain.learning.dto.res.LearningHomeResponseDTO;
import com.mr.domain.learning.dto.res.LearningPracticeDataResponseDTO;
import com.mr.domain.learning.dto.res.LearningProgressResponseDTO;
import com.mr.domain.learning.dto.res.LearningResultResponseDTO;
import com.mr.domain.learning.dto.res.LearningStepDetailResponseDTO;
import com.mr.domain.learning.dto.res.LearningTheoryListResponseDTO;
import com.mr.domain.learning.service.LearningService;
import com.mr.global.apipayload.ApiResponse;
import com.mr.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learnings")
@Tag(name = "학습", description = "학습 API")
public class LearningController {

    private final LearningService learningService;

    @Operation(
            summary = "학습 홈 조회 API",
            description = "최근 학습 이어서 하기(currentLearning), 난이도별 대표 학습 주제(최대 3개, 데이터 없는 난이도는 제외), 실전 반주법 대표 3개를 조회합니다."
    )
    @GetMapping("/home")
    public ApiResponse<LearningHomeResponseDTO.HomeResultDTO> getHome() {
        LearningHomeResponseDTO.HomeResultDTO result = learningService.getHome(SecurityUtil.getCurrentUserId());
        return ApiResponse.onSuccess(result);
    }

    @Operation(
            summary = "학습 결과 저장 API",
            description = "학습 단계(learningStepId)의 채점 점수를 저장합니다. 기존 진행 기록이 있으면 갱신하고, 없으면 새로 생성합니다."
    )
    @PostMapping("/{learningId}/result")
    public ApiResponse<LearningResultResponseDTO.SaveResultResultDTO> saveLearningResult(
            @PathVariable Long learningId,
            @Valid @RequestBody LearningResultSaveRequestDTO.SaveResultDTO request
    ){
        LearningResultResponseDTO.SaveResultResultDTO response =
                learningService.saveResult(SecurityUtil.getCurrentUserId(), learningId, request);

        return ApiResponse.onSuccess(response);
    }

    @Operation(
            summary = "학습 진행률 조회 API",
            description = "완료(score>=90) 단계 수 / 전체 단계 수 기준으로 패키지 단위 진행률(progressRate, 0~100)을 조회합니다."
    )
    @GetMapping("/{learningId}/progress")
    public ApiResponse<LearningProgressResponseDTO.ProgressResultDTO> getLearningProgress(
            @PathVariable Long learningId
    ) {
        LearningProgressResponseDTO.ProgressResultDTO result =
                learningService.getLearningProgress(SecurityUtil.getCurrentUserId(), learningId);
        return ApiResponse.onSuccess(result);
    }

    @Operation(
            summary = "학습 커리큘럼 조회 API",
            description = "패키지 상세 정보, 전체 진행률(progress), 단계별 목록(상태·점수 포함)을 조회합니다."
    )
    @GetMapping("/{learningId}")
    public ApiResponse<LearningCurriculumResponseDTO.CurriculumResultDTO> getCurriculum(
            @PathVariable Long learningId
    ) {
        LearningCurriculumResponseDTO.CurriculumResultDTO result =
                learningService.getCurriculum(SecurityUtil.getCurrentUserId(), learningId);
        return ApiResponse.onSuccess(result);
    }

    @Operation(
            summary = "학습 단계별 조회 API",
            description = "단계별 이론 설명, 연습 팁, 모범 연주 예시(있으면), 코드 예시를 조회합니다."
    )
    @GetMapping("/{learningId}/steps/{learningStepId}")
    public ApiResponse<LearningStepDetailResponseDTO.StepDetailResultDTO> getStepDetail(
            @PathVariable Long learningId,
            @PathVariable Long learningStepId
    ) {
        LearningStepDetailResponseDTO.StepDetailResultDTO result =
                learningService.getStepDetail(learningId, learningStepId);
        return ApiResponse.onSuccess(result);
    }

    @Operation(
            summary = "단계별 연습 실행 정보 조회 API",
            description = "[이 이론으로 실습하기] 클릭 시 필요한 bpm/keySignature/midiData를 조회합니다. "
                    + "채점 후 점수 저장은 이 API가 아니라 POST /{learningId}/result가 담당합니다."
    )
    @GetMapping("/{learningId}/steps/{learningStepId}/practice-data")
    public ApiResponse<LearningPracticeDataResponseDTO.PracticeDataResultDTO> getPracticeData(
            @PathVariable Long learningId,
            @PathVariable Long learningStepId
    ) {
        LearningPracticeDataResponseDTO.PracticeDataResultDTO result =
                learningService.getPracticeData(learningId, learningStepId);
        return ApiResponse.onSuccess(result);
    }

    @Operation(
            summary = "학습 주제 전체보기 API",
            description = "난이도(difficulty) 탭별로 학습 주제 목록을 조회합니다. difficulty는 필수입니다."
    )
    @GetMapping("/theory")
    public ApiResponse<LearningTheoryListResponseDTO.TheoryListResultDTO> getTheoryList(
            @Parameter(description = "BEGINNER | INTERMEDIATE | ADVANCED", required = true)
            @RequestParam(required = false) String difficulty
    ) {
        LearningTheoryListResponseDTO.TheoryListResultDTO result =
                learningService.getTheoryList(SecurityUtil.getCurrentUserId(), difficulty);
        return ApiResponse.onSuccess(result);
    }

    @Operation(
            summary = "실전 반주법 패키지 전체보기 API",
            description = "난이도 구분 없이 전체 목록을 제목 기준 이름순으로 조회합니다. 각 항목의 progressRate는 진행률(%)만 반환합니다."
    )
    @GetMapping("/accompaniment")
    public ApiResponse<LearningAccompanimentListResponseDTO.AccompanimentListResultDTO> getAccompanimentList() {
        LearningAccompanimentListResponseDTO.AccompanimentListResultDTO result =
                learningService.getAccompanimentList(SecurityUtil.getCurrentUserId());
        return ApiResponse.onSuccess(result);
    }
}
