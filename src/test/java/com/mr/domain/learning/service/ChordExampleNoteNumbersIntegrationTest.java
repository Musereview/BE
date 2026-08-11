package com.mr.domain.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mr.domain.learning.dto.res.LearningStepDetailResponseDTO;
import com.mr.domain.learning.entity.ChordExample;
import com.mr.domain.learning.entity.Learning;
import com.mr.domain.learning.entity.LearningStep;
import com.mr.domain.learning.entity.enums.LearningCategory;
import com.mr.domain.learning.entity.enums.LearningDifficulty;
import com.mr.domain.learning.repository.ChordExampleRepository;
import com.mr.domain.learning.repository.LearningRepository;
import com.mr.domain.learning.repository.LearningStepRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 서승기님이 지적한 서버 크래시 재현/검증용 통합 테스트.
 * noteNumbers는 @ElementCollection이라 실제로는 Hibernate가 관리하는 컬렉션(PersistentBag)으로 로드되는데,
 * Mockito mock(ChordExample.class)로는 이 타입을 재현할 수 없어 flush+clear로 영속성 컨텍스트를 비우고
 * 실제로 DB에서 다시 읽어온 컬렉션으로 검증한다.
 */
@SpringBootTest
@Transactional
class ChordExampleNoteNumbersIntegrationTest {

    @Autowired
    private LearningService learningService;
    @Autowired
    private LearningRepository learningRepository;
    @Autowired
    private LearningStepRepository learningStepRepository;
    @Autowired
    private ChordExampleRepository chordExampleRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("getStepDetail - 실제 Hibernate 컬렉션으로 로드된 noteNumbers도 불변 리스트로 반환된다")
    void getStepDetail_realHibernateCollection_returnsImmutableNoteNumbers() {
        Learning learning = learningRepository.save(Learning.create(
                "Tension Notes", LearningCategory.THEORY, LearningDifficulty.ADVANCED,
                "요약", "본문", "팁", 10, "PIANO", true));

        LearningStep step = learningStepRepository.save(
                LearningStep.create(learning, 1, "11th 텐션 노트 활용하기", "설명", "이론 설명", "연습 팁", 10));

        chordExampleRepository.save(
                ChordExample.create(step, "Cmaj7", List.of(4, 11), "F(11th) 주의 - E(3rd)와 충돌"));

        // 방금 만든 자바 객체가 아니라 DB에서 진짜로 다시 읽어온 Hibernate 컬렉션을 태우기 위해 영속성 컨텍스트를 비운다
        entityManager.flush();
        entityManager.clear();

        LearningStepDetailResponseDTO.StepDetailResultDTO result =
                learningService.getStepDetail(learning.getId(), step.getId());

        assertThat(result.chordExamples()).hasSize(1);
        List<Integer> noteNumbers = result.chordExamples().get(0).noteNumbers();
        assertThat(noteNumbers).containsExactly(4, 11);
        assertThatThrownBy(() -> noteNumbers.add(1))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
