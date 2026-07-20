package com.mr.domain.learning.entity;

import com.mr.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Builder;

@Entity
@Getter
@Table(
        name = "playing_example"
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayingExample extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_example_id")
    private Long id;

    // 학습 단계 엔티티와 일대일(1:1) 매핑 (외래 키 관리자)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_step_id", nullable = false)
    private LearningStep learningStep;

    // 제목
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    // 미디 파일 데이터
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "midi_file_url", nullable = false, columnDefinition = "JSON")
    private String midiFileUrl;

    // 오디오 파일
    @Column(name = "audio_file_url", nullable = false, length = 255)
    private String audioFileUrl;

    // bpm
    @Column(name = "bpm")
    private Integer bpm;

    // key
    @Column(name = "key_signature", length = 20)
    private String keySignature;

    // 설명
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // 재생 시간 (초 단위 저장)
    @Column(name = "playing_seconds")
    private Long playingSeconds;

    @Builder(access = AccessLevel.PRIVATE)
    private PlayingExample(LearningStep learningStep, String title, String midiFileUrl,
                           String audioFileUrl, Integer bpm, String keySignature,
                           String description, Long playingSeconds) {
        this.learningStep = learningStep;
        this.title = title;
        this.midiFileUrl = midiFileUrl;
        this.audioFileUrl = audioFileUrl;
        this.bpm = bpm;
        this.keySignature = keySignature;
        this.description = description;
        this.playingSeconds = playingSeconds;
    }

    public static PlayingExample create(LearningStep learningStep, String title, String midiFileUrl,
                                        String audioFileUrl, Integer bpm, String keySignature,
                                        String description, Long playingSeconds) {
        return PlayingExample.builder()
                .learningStep(learningStep)
                .title(title)
                .midiFileUrl(midiFileUrl)
                .audioFileUrl(audioFileUrl)
                .bpm(bpm)
                .keySignature(keySignature)
                .description(description)
                .playingSeconds(playingSeconds)
                .build();
    }

    public void updatePlayingExample(String title, String midiFileUrl, String audioFileUrl,
                                     Integer bpm, String keySignature, String description,
                                     Long playingSeconds) {
        this.title = title;
        this.midiFileUrl = midiFileUrl;
        this.audioFileUrl = audioFileUrl;
        this.bpm = bpm;
        this.keySignature = keySignature;
        this.description = description;
        this.playingSeconds = playingSeconds;
    }
}
