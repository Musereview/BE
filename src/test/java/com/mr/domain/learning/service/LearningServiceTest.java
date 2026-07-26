package com.mr.domain.learning.service;

import com.mr.domain.learning.dto.res.LearningTheoryListResponseDTO;
import com.mr.domain.learning.entity.enums.LearningCategory;
import com.mr.domain.learning.entity.enums.LearningDifficulty;
import com.mr.domain.learning.exception.LearningErrorStatus;
import com.mr.domain.learning.repository.LearningRepository;
import com.mr.domain.learning.repository.LearningStepRepository;
import com.mr.domain.learning.repository.PlayingExampleRepository;
import com.mr.domain.learning.repository.UserLearningProgressRepository;
import com.mr.domain.user.repository.UserRepository;
import com.mr.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningServiceTest {

    @Mock
    private UserLearningProgressRepository userLearningProgressRepository;
    @Mock
    private LearningStepRepository learningStepRepository;
    @Mock
    private LearningRepository learningRepository;
    @Mock
    private PlayingExampleRepository playingExampleRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LearningService learningService;

    @Test
    void 소문자와_공백이_포함된_difficulty도_정상_변환() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(learningRepository.findByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(
                LearningCategory.THEORY, LearningDifficulty.BEGINNER))
                .thenReturn(Collections.emptyList());

        LearningTheoryListResponseDTO.TheoryListResultDTO result =
                learningService.getTheoryList(1L, " beginner ");

        assertThat(result.items()).isEmpty();
        verify(learningRepository).findByCategoryAndDifficultyAndIsActiveTrueOrderByTitleAsc(
                LearningCategory.THEORY, LearningDifficulty.BEGINNER);
    }

    @Test
    void 존재하지_않는_difficulty값은_400() {
        assertThatThrownBy(() -> learningService.getTheoryList(1L, "invalid"))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> assertThat(((GeneralException) e).getCode())
                        .isEqualTo(LearningErrorStatus.INVALID_DIFFICULTY));
    }
}
