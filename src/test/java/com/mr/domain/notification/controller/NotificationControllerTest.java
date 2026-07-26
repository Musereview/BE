package com.mr.domain.notification.controller;

import com.mr.domain.notification.dto.res.NotificationListResponseDTO;
import com.mr.domain.notification.service.NotificationService;
import com.mr.global.security.principal.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@MockBean(JpaMetamodelMappingContext.class) // JPA Auditing 에러 방지
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        // SecurityContext에 가짜(Mock) CustomUserDetails 세팅
        CustomUserDetails mockUserDetails = mock(CustomUserDetails.class);
        when(mockUserDetails.getUserId()).thenReturn(1L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(mockUserDetails, null, Collections.emptyList())
        );
    }

    @Test
    @DisplayName("알림 목록 조회 API")
    void getNotifications() throws Exception {
        // given
        NotificationListResponseDTO mockResponse = NotificationListResponseDTO.builder()
                .listSize(0)
                .hasNext(false)
                .isFirst(true)
                .isLast(true)
                .build();

        when(notificationService.getNotificationList(eq(1L), any())).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    @DisplayName("알림 단건 읽음 처리 API")
    void readNotification() throws Exception {
        // given
        doNothing().when(notificationService).readNotification(1L, 100L);

        // when & then
        mockMvc.perform(patch("/api/notifications/{notificationId}/read", 100L)
                        .with(csrf())) // Spring Security CSRF 토큰 우회
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    @DisplayName("알림 전체 읽음 처리 API")
    void readAllNotifications() throws Exception {
        // given
        doNothing().when(notificationService).readAllNotifications(1L);

        // when & then
        mockMvc.perform(patch("/api/notifications/read-all")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    @DisplayName("안 읽은 알림 상태 확인 API (뱃지용)")
    void checkUnreadStatus() throws Exception {
        // given
        when(notificationService.checkUnreadNotification(1L)).thenReturn(true);

        // when & then
        mockMvc.perform(get("/api/notifications/unread-status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }
}