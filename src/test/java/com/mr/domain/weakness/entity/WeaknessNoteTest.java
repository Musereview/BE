package com.mr.domain.weakness.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.mr.domain.analysis.entity.Analysis;
import com.mr.domain.learning.entity.Learning;
import com.mr.domain.user.entity.User;
import com.mr.domain.weakness.entity.enums.Severity;
import com.mr.domain.weakness.exception.WeaknessErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WeaknessNoteTest {

    @Test
    @DisplayName("create - user가 null이면 예외가 발생한다")
    void create_userNull_throwsException() {
        assertThatThrownBy(() -> WeaknessNote.create(
                null, "CODE", "이름", "TYPE", mock(Analysis.class), mock(Learning.class)))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("code", WeaknessErrorStatus.WEAKNESS_INVALID_REQUEST);
    }

    @Test
    @DisplayName("create - lastAnalysis/recommendedLearningContent가 없어도 정상 생성된다")
    void create_optionalFieldsNull_success() {
        WeaknessNote weaknessNote = WeaknessNote.create(
                mock(User.class), "CODE", "이름", "TYPE", null, null);

        assertThat(weaknessNote.getOccurrenceCount()).isEqualTo(1);
        assertThat(weaknessNote.getSeverity()).isEqualTo(Severity.LOW);
        assertThat(weaknessNote.getLastAnalysis()).isNull();
        assertThat(weaknessNote.getRecommendedLearningContent()).isNull();
    }
}
