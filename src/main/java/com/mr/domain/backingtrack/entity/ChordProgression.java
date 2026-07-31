package com.mr.domain.backingtrack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "chord_progression",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chord_progression_track_measure_seq",
                        columnNames = {"backing_track_id", "measure_no", "sequence_no"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChordProgression{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chord_progression_id")
    private Long id;

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

    @Builder(access = AccessLevel.PRIVATE)
    private ChordProgression(BackingTrack backingTrack, Integer sequenceNo, Integer measureNo, String chordName) {
        this.backingTrack = backingTrack;
        this.sequenceNo = sequenceNo;
        this.measureNo = measureNo;
        this.chordName = chordName;
    }

    public static ChordProgression create(BackingTrack backingTrack, Integer sequenceNo, Integer measureNo, String chordName) {
        ChordProgression chordProgression = ChordProgression.builder()
                .backingTrack(backingTrack)
                .sequenceNo(sequenceNo)
                .measureNo(measureNo)
                .chordName(chordName)
                .build();
        // 양방향 매핑 추가 하고 연결하게
        if (backingTrack != null) {
            backingTrack.addChordProgression(chordProgression);
        }
        return chordProgression;
    }

    public void updateChordInfo(Integer sequenceNo, Integer measureNo, String chordName) {
        this.sequenceNo = sequenceNo;
        this.measureNo = measureNo;
        this.chordName = chordName;
    }
}
