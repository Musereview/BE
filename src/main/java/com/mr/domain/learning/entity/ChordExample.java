package com.mr.domain.learning.entity;

import com.mr.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.List;

@Entity
@Getter
@Table(
        name = "chord_example"
)
public class ChordExample extends BaseCreatedEntity {

    @Id
    @Column(name = "chord_example_id")
    private Long chordExampleid;

    // 학습 단계 id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_step_id", nullable = false)
    private LearningStep learningStep;

    // 코드명
    @Column(name = "chord_name", nullable = false, length = 100)
    private String chordName;

    // 미디 노트 번호 배열 (DB의 INTEGER[] 타입 매핑)
    // 데이터가 항상 간단한 숫자 배열 구조라면 @ElementCollection이 아주 유용해!
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "chord_example_note",
            joinColumns = @JoinColumn(name = "chord_example_id")
    )
    @Column(name = "note_number", nullable = false)
    private List<Integer> noteNumbers;

    // 설명
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
