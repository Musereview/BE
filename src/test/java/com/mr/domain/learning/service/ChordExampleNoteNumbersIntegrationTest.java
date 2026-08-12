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
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 서승기님이 지적한 서버 크래시 재현/검증용 통합 테스트.
 * noteNumbers는 @ElementCollection이라 실제로는 Hibernate가 관리하는 컬렉션(PersistentBag)으로 로드되는데,
 * Mockito mock(ChordExample.class)로는 이 타입을 재현할 수 없어 실제로 DB에서 다시 읽어온 컬렉션으로 검증한다.
 * 클래스 레벨 @Transactional을 걸지 않아서(걸면 getStepDetail() 호출 이후에도 테스트 자신의 트랜잭션이
 * 계속 열려있게 되어 세션 종료 후 접근하는 실제 장애 시나리오를 재현하지 못한다) 저장/서비스 호출이 각자
 * 별도 트랜잭션으로 커밋·종료되게 하고, 검증은 서비스 트랜잭션이 이미 닫힌 뒤에 이뤄지도록 한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class ChordExampleNoteNumbersIntegrationTest {

    @Autowired
    private LearningService learningService;
    @Autowired
    private LearningRepository learningRepository;
    @Autowired
    private LearningStepRepository learningStepRepository;
    @Autowired
    private ChordExampleRepository chordExampleRepository;

    private Long learningId;
    private Long stepId;
    private Long chordExampleId;

    @AfterEach
    void tearDown() {
        if (chordExampleId != null) {
            chordExampleRepository.deleteById(chordExampleId);
        }
        if (stepId != null) {
            learningStepRepository.deleteById(stepId);
        }
        if (learningId != null) {
            learningRepository.deleteById(learningId);
        }
    }

    @Test
    @DisplayName("getStepDetail - 실제 Hibernate 컬렉션으로 로드된 noteNumbers도 불변 리스트로 반환된다")
    void getStepDetail_realHibernateCollection_returnsImmutableNoteNumbers() {
        Learning learning = learningRepository.save(Learning.create(
                "Tension Notes", LearningCategory.THEORY, LearningDifficulty.ADVANCED,
                "요약", "본문", "팁", 10, "PIANO", true));
        learningId = learning.getId();

        LearningStep step = learningStepRepository.save(
                LearningStep.create(learning, 1, "11th 텐션 노트 활용하기", "설명", "이론 설명", "연습 팁", 10));
        stepId = step.getId();

        ChordExample chordExample = chordExampleRepository.save(
                ChordExample.create(step, "Cmaj7", List.of(4, 11), "F(11th) 주의 - E(3rd)와 충돌"));
        chordExampleId = chordExample.getId();

        // 여기까지의 save() 호출들은 각자 별도 트랜잭션으로 이미 커밋·종료됐고,
        // getStepDetail()도 자신의 트랜잭션을 새로 열어 반환 시점에 커밋·세션 종료한다.
        LearningStepDetailResponseDTO.StepDetailResultDTO result =
                learningService.getStepDetail(learningId, stepId);

        // 이 시점엔 서비스 트랜잭션도 이미 닫혀 있다 - 실제 장애 시나리오(세션 종료 후 접근)와 동일한 조건에서 검증
        assertThat(result.chordExamples()).hasSize(1);
        List<Integer> noteNumbers = result.chordExamples().get(0).noteNumbers();
        assertThat(noteNumbers).containsExactly(4, 11);
        assertThatThrownBy(() -> noteNumbers.add(1))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
