package com.mr.domain.learning.entity;

import com.mr.global.entity.BaseCreatedEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@Table(
        name = "chord_example"
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChordExample extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chord_example_id")
    private Long chordExampleid;

    // 학습 단계 id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_step_id", nullable = false)
    private LearningStep learningStep;

    // 코드명
    @Column(name = "chord_name", nullable = false, length = 100)
    private String chordName;

    // 미디 노트 번호 목록 (chord_example_note 보조 테이블에 매핑)
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
