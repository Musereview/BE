package com.mr.domain.notification.service;

import com.mr.domain.notification.dto.res.NotificationListResponseDTO;
import com.mr.domain.notification.entity.Notification;
import com.mr.domain.notification.exception.NotificationErrorStatus;
import com.mr.domain.notification.repository.NotificationRepository;
import com.mr.domain.user.entity.User;
import com.mr.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    private User user;
    private Notification notification;

    @BeforeEach
    void setUp() {
        // 테스트용 임시 유저 및 알림 엔티티 생성 (리플렉션으로 ID 주입)
        user = User.builder().build(); // User 엔티티 구조에 맞게 수정 필요
        ReflectionTestUtils.setField(user, "userId", 1L);

        notification = Notification.create(user, "테스트 알림 제목", "테스트 알림 내용");
        ReflectionTestUtils.setField(notification, "id", 100L);
    }

    @Test
    @DisplayName("알림 목록 조회 (Slice) 성공")
    void getNotificationList_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Slice<Notification> mockSlice = new SliceImpl<>(List.of(notification), pageRequest, false);
        when(notificationRepository.findAllByUser_UserIdAndDeletedAtIsNull(eq(1L), any())).thenReturn(mockSlice);

        // when
        NotificationListResponseDTO result = notificationService.getNotificationList(1L, pageRequest);

        // then
        assertThat(result.getNotificationList()).hasSize(1);
        assertThat(result.getNotificationList().get(0).getTitle()).isEqualTo("테스트 알림 제목");
        assertThat(result.getHasNext()).isFalse();
    }

    @Test
    @DisplayName("알림 단건 읽음 처리 성공")
    void readNotification_Success() {
        // given
        when(notificationRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(notification));

        // when
        notificationService.readNotification(1L, 100L);

        // then
        assertThat(notification.isRead()).isTrue();
    }

    @Test
    @DisplayName("알림 단건 읽음 처리 실패 - 권한 없음(다른 유저의 알림)")
    void readNotification_Fail_Forbidden() {
        // given
        User otherUser = User.builder().build();
        ReflectionTestUtils.setField(otherUser, "userId", 2L);
        Notification otherNotification = Notification.create(otherUser, "제목", "내용");

        when(notificationRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(otherNotification));

        // when & then
        assertThatThrownBy(() -> notificationService.readNotification(1L, 100L))
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(NotificationErrorStatus.FORBIDDEN_NOTIFICATION.getMessage());
    }

    @Test
    @DisplayName("알림 모두 읽음 처리 성공")
    void readAllNotifications_Success() {
        // when
        notificationService.readAllNotifications(1L);

        // then
        verify(notificationRepository, times(1)).bulkMarkAllAsReadByUserId(1L);
    }

    @Test
    @DisplayName("안 읽은 알림 존재 여부 확인 성공")
    void checkUnreadNotification_Success() {
        // given
        when(notificationRepository.existsByUserUserIdAndIsReadFalseAndDeletedAtIsNull(1L)).thenReturn(true);

        // when
        boolean result = notificationService.checkUnreadNotification(1L);

        // then
        assertThat(result).isTrue();
    }
}