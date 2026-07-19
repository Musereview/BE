package com.mr.domain.backingTrack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(
        name = "chord_progression",
        indexes = {
                @Index(name = "idx_chord_progression_track_seq",
                        columnList = "backing_track_id, sequence_no")
        }
)
public class ChordProgression{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chord_progression_id")
    private Long chordProgressionId;

    // 백킹트랙 아이디
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backing_track_id", nullable = false)
    private BackingTrack backingTrack;

    // 코드 순서
    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    // 마디 번호
    @Column(name = "measure_no", nullable = false)
    private Integer measureNo;

    // 코드명
    @Column(name = "chord_name", nullable = false, length = 30)
    private String chordName;
}
