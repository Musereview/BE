package com.mr.domain.backingTrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.mr.domain.analysis.repository.AnalysisRepository;
import com.mr.domain.backingTrack.dto.req.BackingTrackSaveRequestDTO;
import com.mr.domain.backingTrack.dto.res.BackingTrackCreateResponseDTO;
import com.mr.domain.backingTrack.dto.res.BackingTrackUpdateResponseDTO;
import com.mr.domain.backingTrack.entity.BackingTrack;
import com.mr.domain.backingTrack.entity.enums.AccessLevel;
import com.mr.domain.backingTrack.entity.enums.Level;
import com.mr.domain.backingTrack.entity.enums.ScaleType;
import com.mr.domain.backingTrack.exception.BackingTrackErrorStatus;
import com.mr.domain.backingTrack.repository.BackingTrackRepository;
import com.mr.domain.user.entity.User;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BackingTrackServiceTest {

    @Mock
    private BackingTrackRepository backingTrackRepository;

    @Mock
    private UserRepository userRepository;
    @Mock
    private AnalysisRepository analysisRepository;

    private BackingTrackService backingTrackService;

    @BeforeEach
    void setUp() {
        backingTrackService = new BackingTrackService(backingTrackRepository, userRepository, analysisRepository);
    }

    private User createMockUser(Long userId) {
        User mockUser = mock(User.class);
        lenient().when(mockUser.getUserId()).thenReturn(userId);
        return mockUser;
    }

    private BackingTrackSaveRequestDTO.SaveDTO createValidRequest() {
        return new BackingTrackSaveRequestDTO.SaveDTO(
                "새로운 트랙", "Jazz", "C", ScaleType.MAJOR, "4/4", 120, 180, "http://audio.url",
                AccessLevel.PUBLIC, Level.BASIC,
                // 💡 확실하게 1개만 들어가도록 설정
                List.of(new BackingTrackSaveRequestDTO.ChordProgressionDTO(1, 1, "CM7"))
        );
    }

    @Test
    @DisplayName("createBackingTrack - 정상적으로 백킹트랙을 생성한다")
    void createBackingTrack_success() {
        // given
        Long userId = 1L;
        User user = createMockUser(userId);
        BackingTrackSaveRequestDTO.SaveDTO request = createValidRequest();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        given(backingTrackRepository.save(any(BackingTrack.class))).willAnswer(invocation -> {
            BackingTrack saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        // when
        BackingTrackCreateResponseDTO.CreateResultDTO response = backingTrackService.createBackingTrack(userId, request);

        // then
        ArgumentCaptor<BackingTrack> captor = ArgumentCaptor.forClass(BackingTrack.class);

        // 💡 verify 구문 안에 captor.capture()가 들어가도록 수정
        verify(backingTrackRepository).save(captor.capture());

        BackingTrack capturedTrack = captor.getValue();
        assertThat(capturedTrack.getTitle()).isEqualTo("새로운 트랙");
        assertThat(capturedTrack.getChordProgressions()).hasSize(1);

        assertThat(response.backingTrackId()).isEqualTo(100L);
        assertThat(response.title()).isEqualTo("새로운 트랙");
    }

    @Test
    @DisplayName("updateBackingTrack - 작성자 본인이면 정상적으로 업데이트한다")
    void updateBackingTrack_success() {
        // given
        Long userId = 1L;
        Long trackId = 100L;
        User user = createMockUser(userId);
        BackingTrackSaveRequestDTO.SaveDTO request = createValidRequest();

        BackingTrack existingTrack = BackingTrack.create(
                user, 1L, "기존 제목", "Pop", "G", ScaleType.MAJOR, "4/4", 100, 200, null, null, AccessLevel.PRIVATE, Level.BASIC
        );
        ReflectionTestUtils.setField(existingTrack, "id", trackId);

        given(backingTrackRepository.findByIdAndDeletedAtIsNull(trackId)).willReturn(Optional.of(existingTrack));

        // when
        BackingTrackUpdateResponseDTO.UpdateResultDTO response = backingTrackService.updateBackingTrack(userId, trackId, request);

        // then
        assertThat(existingTrack.getTitle()).isEqualTo("새로운 트랙");

        assertThat(existingTrack.getChordProgressions()).hasSize(1);

        assertThat(response.backingTrackId()).isEqualTo(trackId);
        assertThat(response.title()).isEqualTo("새로운 트랙");
    }

    @Test
    @DisplayName("updateBackingTrack - 작성자가 다르면 예외가 발생한다")
    void updateBackingTrack_fail_forbidden() {
        Long requesterId = 2L;
        Long ownerId = 1L;
        Long trackId = 100L;

        User owner = createMockUser(ownerId);
        BackingTrackSaveRequestDTO.SaveDTO request = createValidRequest();

        BackingTrack existingTrack = BackingTrack.create(
                owner, 1L, "기존 제목", "Pop", "G", ScaleType.MAJOR, "4/4", 100, 200, null, null, AccessLevel.PRIVATE, Level.BASIC
        );
        ReflectionTestUtils.setField(existingTrack, "id", trackId);

        given(backingTrackRepository.findByIdAndDeletedAtIsNull(trackId)).willReturn(Optional.of(existingTrack));

        GeneralException exception = assertThrows(GeneralException.class,
                () -> backingTrackService.updateBackingTrack(requesterId, trackId, request)
        );

        assertThat(exception.getErrorReason().code())
                .isEqualTo(BackingTrackErrorStatus.FORBIDDEN_UPDATE.getCode());
    }

    @Test
    @DisplayName("validateChordDuplicates - 중복된 코드 위치가 있으면 예외가 발생한다")
    void validateChordDuplicates_fail() {
        Long userId = 1L;
        User user = createMockUser(userId);

        BackingTrackSaveRequestDTO.SaveDTO invalidRequest = new BackingTrackSaveRequestDTO.SaveDTO(
                "새로운 트랙", "Jazz", "C", ScaleType.MAJOR, "4/4", 120, 180, null, AccessLevel.PUBLIC, Level.BASIC,
                List.of(
                        new BackingTrackSaveRequestDTO.ChordProgressionDTO(1, 1, "CM7"),
                        new BackingTrackSaveRequestDTO.ChordProgressionDTO(1, 1, "Am7") // 중복
                )
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        GeneralException exception = assertThrows(GeneralException.class,
                () -> backingTrackService.createBackingTrack(userId, invalidRequest)
        );

        assertThat(exception.getErrorReason().code())
                .isEqualTo(BackingTrackErrorStatus.DUPLICATE_CHORD_POSITION.getCode());
    }
}