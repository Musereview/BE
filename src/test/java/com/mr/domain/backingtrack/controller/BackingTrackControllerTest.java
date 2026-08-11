package com.mr.domain.backingtrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.domain.backingtrack.dto.req.BackingTrackSaveRequestDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackCreateResponseDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackDetailResponseDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackListResponseDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackRecommendedResponseDTO;
import com.mr.domain.backingtrack.dto.res.BackingTrackUpdateResponseDTO;
import com.mr.domain.backingtrack.dto.res.PlayCountIncreaseResponseDTO;
import com.mr.domain.backingtrack.entity.enums.AccessLevel;
import com.mr.domain.backingtrack.entity.enums.Level;
import com.mr.domain.backingtrack.entity.enums.ScaleType;
import com.mr.domain.backingtrack.exception.BackingTrackErrorStatus;
import com.mr.domain.backingtrack.service.BackingTrackService;
import com.mr.domain.user.entity.enums.UserRole;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.security.principal.CustomUserDetails;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BackingTrackController.class)
@AutoConfigureMockMvc(addFilters = false)
class BackingTrackControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long BACKING_TRACK_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BackingTrackService backingTrackService;

    @MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails =
                new CustomUserDetails(USER_ID, UserRole.ROLE_STUDENT);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails, "", userDetails.getAuthorities()
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("백킹트랙 생성")
    class CreateBackingTrack {

        @Test
        @DisplayName("유효한 요청이면 백킹트랙을 생성한다")
        void createBackingTrack_success() throws Exception {
            BackingTrackSaveRequestDTO.CreateDTO request = createValidSaveRequest();

            BackingTrackCreateResponseDTO.CreateResultDTO response =
                    BackingTrackCreateResponseDTO.CreateResultDTO.of(
                            BACKING_TRACK_ID, "테스트 트랙", Instant.now()
                    );

            given(backingTrackService.createBackingTrack(anyLong(), any()))
                    .willReturn(response);

            mockMvc.perform(
                            post("/api/backing-tracks")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data.backingTrackId").value(BACKING_TRACK_ID))
                    .andExpect(jsonPath("$.data.title").value("테스트 트랙"));

            then(backingTrackService)
                    .should()
                    .createBackingTrack(USER_ID, request);
        }

        @Test
        @DisplayName("제목이 비어 있으면 요청에 실패한다")
        void createBackingTrack_blankTitle() throws Exception {
            String requestBody = """
                    {
                      "title": "",
                      "genre": "Jazz",
                      "keySignature": "C",
                      "scaleType": "MAJOR",
                      "timeSignature": "4/4",
                      "bpm": 120,
                      "playtimeSec": 180,
                      "accessLevel": "PUBLIC",
                      "level": "BASIC",
                      "chordProgression": [
                        { "measureNo": 1, "sequenceNo": 1, "chordName": "Cmaj7" }
                      ]
                    }
                    """;

            mockMvc.perform(
                            post("/api/backing-tracks")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));

            then(backingTrackService).should(never()).createBackingTrack(anyLong(), any());
        }

        @Test
        @DisplayName("BPM이 허용 범위를 벗어나면 요청에 실패한다")
        void createBackingTrack_invalidBpm() throws Exception {
            String requestBody = """
                    {
                      "title": "테스트 트랙",
                      "genre": "Jazz",
                      "keySignature": "C",
                      "scaleType": "MAJOR",
                      "timeSignature": "4/4",
                      "bpm": 300,
                      "playtimeSec": 180,
                      "accessLevel": "PUBLIC",
                      "level": "BASIC",
                      "chordProgression": [
                        { "measureNo": 1, "sequenceNo": 1, "chordName": "Cmaj7" }
                      ]
                    }
                    """;

            mockMvc.perform(
                            post("/api/backing-tracks")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));

            then(backingTrackService).should(never()).createBackingTrack(anyLong(), any());
        }

        @Test
        @DisplayName("코드 진행 목록이 비어 있으면 요청에 실패한다")
        void createBackingTrack_emptyChordProgression() throws Exception {
            String requestBody = """
                    {
                      "title": "테스트 트랙",
                      "genre": "Jazz",
                      "keySignature": "C",
                      "scaleType": "MAJOR",
                      "timeSignature": "4/4",
                      "bpm": 120,
                      "playtimeSec": 180,
                      "accessLevel": "PUBLIC",
                      "level": "BASIC",
                      "chordProgression": []
                    }
                    """;

            mockMvc.perform(
                            post("/api/backing-tracks")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));

            then(backingTrackService).should(never()).createBackingTrack(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("백킹트랙 수정")
    class UpdateBackingTrack {

        @Test
        @DisplayName("유효한 요청이면 백킹트랙을 수정한다")
        void updateBackingTrack_success() throws Exception {
            BackingTrackSaveRequestDTO.UpdateDTO request = createValidUpdateRequest();

            BackingTrackUpdateResponseDTO.UpdateResultDTO response =
                    BackingTrackUpdateResponseDTO.UpdateResultDTO.of(
                            BACKING_TRACK_ID, "테스트 트랙", Instant.now()
                    );

            given(backingTrackService.updateBackingTrack(anyLong(), anyLong(), any()))
                    .willReturn(response);

            mockMvc.perform(
                            put("/api/backing-tracks/{backingTrackId}", BACKING_TRACK_ID)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data.backingTrackId").value(BACKING_TRACK_ID));
        }

        @Test
        @DisplayName("backingTrackId가 0 이하이면 요청에 실패한다")
        void updateBackingTrack_invalidId() throws Exception {
            BackingTrackSaveRequestDTO.UpdateDTO request = createValidUpdateRequest();

            mockMvc.perform(
                            put("/api/backing-tracks/{backingTrackId}", 0L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));

            then(backingTrackService).should(never()).updateBackingTrack(anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("수정 권한이 없으면 403을 반환한다")
        void updateBackingTrack_forbidden() throws Exception {
            BackingTrackSaveRequestDTO.UpdateDTO request = createValidUpdateRequest();

            given(backingTrackService.updateBackingTrack(anyLong(), anyLong(), any()))
                    .willThrow(new GeneralException(BackingTrackErrorStatus.FORBIDDEN_UPDATE));

            mockMvc.perform(
                            put("/api/backing-tracks/{backingTrackId}", BACKING_TRACK_ID)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }

        @Test
        @DisplayName("존재하지 않는 트랙이면 404를 반환한다")
        void updateBackingTrack_notFound() throws Exception {
            BackingTrackSaveRequestDTO.UpdateDTO request = createValidUpdateRequest();

            given(backingTrackService.updateBackingTrack(anyLong(), anyLong(), any()))
                    .willThrow(new GeneralException(BackingTrackErrorStatus.BACKING_TRACK_NOT_FOUND));

            mockMvc.perform(
                            put("/api/backing-tracks/{backingTrackId}", BACKING_TRACK_ID)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }

    @Nested
    @DisplayName("백킹트랙 재생 수 증가")
    class IncreasePlayCount {

        @Test
        @DisplayName("정상 요청이면 재생 수를 증가시킨다")
        void increasePlayCount_success() throws Exception {
            String requestBody = """
                    {
                      "analysisId": 200
                    }
                    """;

            PlayCountIncreaseResponseDTO.IncreaseResponseDTO response =
                    PlayCountIncreaseResponseDTO.IncreaseResponseDTO.of(BACKING_TRACK_ID, 6);

            given(backingTrackService.increasePlayCount(anyLong(), any(), anyLong()))
                    .willReturn(response);

            mockMvc.perform(
                            patch("/api/backing-tracks/{backingTrackId}/play-count", BACKING_TRACK_ID)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data.playCount").value(6));
        }

        @Test
        @DisplayName("analysisId가 없으면 요청에 실패한다")
        void increasePlayCount_missingAnalysisId() throws Exception {
            String requestBody = """
                    {
                    }
                    """;

            mockMvc.perform(
                            patch("/api/backing-tracks/{backingTrackId}/play-count", BACKING_TRACK_ID)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));

            then(backingTrackService).should(never()).increasePlayCount(anyLong(), any(), anyLong());
        }

        @Test
        @DisplayName("backingTrackId가 0 이하이면 요청에 실패한다")
        void increasePlayCount_invalidId() throws Exception {
            String requestBody = """
                    {
                      "analysisId": 200
                    }
                    """;

            mockMvc.perform(
                            patch("/api/backing-tracks/{backingTrackId}/play-count", 0L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));

            then(backingTrackService).should(never()).increasePlayCount(anyLong(), any(), anyLong());
        }

        @Test
        @DisplayName("분석 결과와 트랙이 일치하지 않으면 400을 반환한다")
        void increasePlayCount_trackMismatch() throws Exception {
            String requestBody = """
                    {
                      "analysisId": 200
                    }
                    """;

            given(backingTrackService.increasePlayCount(anyLong(), any(), anyLong()))
                    .willThrow(new GeneralException(BackingTrackErrorStatus.ANALYSIS_TRACK_MISMATCH));

            mockMvc.perform(
                            patch("/api/backing-tracks/{backingTrackId}/play-count", BACKING_TRACK_ID)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }

    @Nested
    @DisplayName("백킹트랙 목록 조회")
    class GetBackingTracks {

        @Test
        @DisplayName("커서 없이 요청하면 첫 페이지를 조회한다")
        void getBackingTracks_firstPage() throws Exception {
            BackingTrackListResponseDTO.TrackInfo trackInfo =
                    BackingTrackListResponseDTO.TrackInfo.of(
                            1L, "테스트 트랙", "Jazz", "C", "MAJOR", "4/4",
                            List.of("Cmaj7", "Am7"), 120, "BASIC", 180
                    );

            BackingTrackListResponseDTO.ListResponseDTO response =
                    BackingTrackListResponseDTO.ListResponseDTO.of(
                            List.of(trackInfo), 1L, false
                    );

            given(backingTrackService.getBackingTracks(any(), anyLong()))
                    .willReturn(response);

            // cursor 없이 요청
            mockMvc.perform(get("/api/backing-tracks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data.tracks[0].backingTrackId").value(1))
                    .andExpect(jsonPath("$.data.hasNext").value(false));
        }

        @Test
        @DisplayName("cursor가 0 이하이면(@Positive 위반) 요청에 실패한다")
        void getBackingTracks_invalidCursor() throws Exception {

            mockMvc.perform(get("/api/backing-tracks").param("cursor", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));

            then(backingTrackService).should(never()).getBackingTracks(any(), anyLong());
        }

        @Test
        @DisplayName("유효한 cursor로 요청하면 다음 페이지를 조회한다")
        void getBackingTracks_withCursor() throws Exception {

            BackingTrackListResponseDTO.ListResponseDTO response =
                    BackingTrackListResponseDTO.ListResponseDTO.of(List.of(), null, false);

            given(backingTrackService.getBackingTracks(any(), anyLong()))
                    .willReturn(response);

            // 정상 cursor 요청
            mockMvc.perform(get("/api/backing-tracks").param("cursor", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }
    }

    @Nested
    @DisplayName("백킹트랙 상세 조회")
    class GetBackingTrackDetail {

        @Test
        @DisplayName("존재하는 트랙이면 상세 정보를 반환한다")
        void getBackingTrackDetail_success() throws Exception {
            BackingTrackDetailResponseDTO.ChordDetail chordDetail =
                    BackingTrackDetailResponseDTO.ChordDetail.of(1, 1, "Cmaj7");

            BackingTrackDetailResponseDTO.DetailResponseDTO response =
                    BackingTrackDetailResponseDTO.DetailResponseDTO.of(
                            BACKING_TRACK_ID, "테스트 트랙", "Jazz", "C", "MAJOR",
                            "4/4", 120, 180, "BASIC", "홍길동",
                            "https://example.com/audio.mp3", List.of(chordDetail)
                    );

            given(backingTrackService.getBackingTrackDetail(anyLong(), anyLong()))
                    .willReturn(response);

            mockMvc.perform(get("/api/backing-tracks/{backingTrackId}", BACKING_TRACK_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data.backingTrackId").value(BACKING_TRACK_ID))
                    .andExpect(jsonPath("$.data.chordProgression[0].chordName").value("Cmaj7"));
        }

        @Test
        @DisplayName("backingTrackId가 0 이하이면 요청에 실패한다")
        void getBackingTrackDetail_invalidId() throws Exception {
            mockMvc.perform(get("/api/backing-tracks/{backingTrackId}", 0L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));

            then(backingTrackService).should(never()).getBackingTrackDetail(anyLong(), anyLong());
        }

        @Test
        @DisplayName("접근 권한이 없으면 403을 반환한다")
        void getBackingTrackDetail_forbidden() throws Exception {
            given(backingTrackService.getBackingTrackDetail(anyLong(), anyLong()))
                    .willThrow(new GeneralException(BackingTrackErrorStatus.FORBIDDEN_READ));

            mockMvc.perform(get("/api/backing-tracks/{backingTrackId}", BACKING_TRACK_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }

        @Test
        @DisplayName("존재하지 않는 트랙이면 404를 반환한다")
        void getBackingTrackDetail_notFound() throws Exception {
            given(backingTrackService.getBackingTrackDetail(anyLong(), anyLong()))
                    .willThrow(new GeneralException(BackingTrackErrorStatus.BACKING_TRACK_NOT_FOUND));

            mockMvc.perform(get("/api/backing-tracks/{backingTrackId}", BACKING_TRACK_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }

    @Nested
    @DisplayName("추천 백킹트랙 조회")
    class GetRecommendedTracks {

        @Test
        @DisplayName("추천 트랙 목록을 반환한다")
        void getRecommendedTracks_success() throws Exception {
            BackingTrackRecommendedResponseDTO.TrackInfo trackInfo =
                    BackingTrackRecommendedResponseDTO.TrackInfo.of(
                            1L, "테스트 트랙", "Jazz", "C", "MAJOR", "4/4",
                            List.of("Cmaj7", "Am7"), 120, "BASIC", 180, 15
                    );

            BackingTrackRecommendedResponseDTO.RecommendedResponseDTO response =
                    BackingTrackRecommendedResponseDTO.RecommendedResponseDTO.of(List.of(trackInfo));

            given(backingTrackService.getRecommendedTracks())
                    .willReturn(response);

            mockMvc.perform(get("/api/backing-tracks/recommended"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data.recommendedTracks[0].playCount").value(15));
        }

        @Test
        @DisplayName("추천 트랙이 없으면 빈 목록을 반환한다")
        void getRecommendedTracks_empty() throws Exception {
            given(backingTrackService.getRecommendedTracks())
                    .willReturn(BackingTrackRecommendedResponseDTO.RecommendedResponseDTO.of(List.of()));

            mockMvc.perform(get("/api/backing-tracks/recommended"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data.recommendedTracks").isEmpty());
        }
    }

    private BackingTrackSaveRequestDTO.CreateDTO createValidSaveRequest() {
        return new BackingTrackSaveRequestDTO.CreateDTO(
                "테스트 트랙",
                "Jazz",
                "C",
                ScaleType.MAJOR,
                "4/4",
                120,
                180,
                "https://example.com/audio.mp3",
                AccessLevel.PUBLIC,
                Level.BASIC,
                List.of(new BackingTrackSaveRequestDTO.ChordProgressionDTO(1, 1, "Cmaj7"))
        );
    }

    private BackingTrackSaveRequestDTO.UpdateDTO createValidUpdateRequest() {
        return new BackingTrackSaveRequestDTO.UpdateDTO(
                "테스트 트랙",
                "Jazz",
                "C",
                ScaleType.MAJOR,
                "4/4",
                120,
                180,
                AccessLevel.PUBLIC,
                Level.BASIC,
                List.of(
                        new BackingTrackSaveRequestDTO.ChordProgressionDTO(
                                1,
                                1,
                                "Cmaj7"
                        )
                )
        );
    }
}