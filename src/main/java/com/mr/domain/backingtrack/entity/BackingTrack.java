package com.mr.domain.backingtrack.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.mr.domain.backingtrack.entity.enums.AccessLevel;
import com.mr.domain.backingtrack.entity.enums.Level;
import com.mr.domain.backingtrack.entity.enums.ScaleType;
import com.mr.domain.user.entity.User;
import com.mr.global.entity.BaseTimeDeletedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(
        name = "backing_track",
        indexes = {
                @Index(name = "idx_bt_genre_play",
                        columnList = "genre, play_count"),
        }
)
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BackingTrack extends BaseTimeDeletedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "backing_track_id")
    private Long id;

    // 유저 아이디
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 학원 아이디
    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    // 트랙 이름
    @Column(name = "title", nullable = false, length = 50)
    private String title;

    // 장르
    @Column(name = "genre", nullable = false, length = 50)
    private String genre;

    // key
    @Column(name = "key_signature", nullable = false, length = 20)
    private String keySignature;

    // 조성
    @Enumerated(EnumType.STRING)
    @Column(name = "scale_type", nullable = false)
    private ScaleType scaleType;

    // 박자
    @Column(name = "time_signature", nullable = false, length = 10)
    private String timeSignature;

    // bpm
    @Column(name = "bpm", nullable = false)
    private Integer bpm;

    // 재생 시간
    @Column(name = "playtime_sec", nullable = false)
    private Integer playtimeSec;

    // 오디오 파일
    @Column(name = "audio_file_url", length = 255)
    private String audioFileUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "midi_data", columnDefinition = "jsonb")
    private JsonNode midiData;

    // 재생 수
    @Column(name = "play_count", nullable = false)
    private Integer playCount;

    // 공개 범위
    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false)
    private AccessLevel accessLevel;

    // 난이도
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private Level level;

    // 양방향 매핑 추가, 영속성 전이랑 고아 객체 제거
    @OneToMany(mappedBy = "backingTrack", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChordProgression> chordProgressions = new ArrayList<>();

    @Builder(access = lombok.AccessLevel.PRIVATE)
    private BackingTrack(User user, Long academyId, String title, String genre,
                         String keySignature, ScaleType scaleType, String timeSignature,
                         Integer bpm, Integer playtimeSec, String audioFileUrl,
                         JsonNode midiData, Integer playCount,
                         AccessLevel accessLevel, Level level) {
        this.user = user;
        this.academyId = academyId;
        this.title = title;
        this.genre = genre;
        this.keySignature = keySignature;
        this.scaleType = scaleType;
        this.timeSignature = timeSignature;
        this.bpm = bpm;
        this.playtimeSec = playtimeSec;
        this.audioFileUrl = audioFileUrl;
        this.midiData = midiData;
        this.playCount = playCount != null ? playCount : 0;
        this.accessLevel = accessLevel != null ? accessLevel : AccessLevel.PRIVATE;
        this.level = level != null ? level : Level.BASIC;
    }

    public static BackingTrack create(User user, Long academyId, String title, String genre,
                                      String keySignature, ScaleType scaleType, String timeSignature,
                                      Integer bpm, Integer playtimeSec, String audioFileUrl,
                                      JsonNode midiData, AccessLevel accessLevel, Level level) {
        return BackingTrack.builder()
                .user(user)
                .academyId(academyId)
                .title(title)
                .genre(genre)
                .keySignature(keySignature)
                .scaleType(scaleType)
                .timeSignature(timeSignature)
                .bpm(bpm)
                .playtimeSec(playtimeSec)
                .audioFileUrl(audioFileUrl)
                .midiData(midiData)
                .playCount(0)
                .accessLevel(accessLevel)
                .level(level)
                .build();
    }

    public void updateTrackInfo(String title, String genre, String keySignature,
                                ScaleType scaleType, String timeSignature, Integer bpm,
                                Integer playtimeSec, String audioFileUrl, AccessLevel accessLevel, Level level) {
        this.title = title;
        this.genre = genre;
        this.keySignature = keySignature;
        this.scaleType = scaleType;
        this.timeSignature = timeSignature;
        this.bpm = bpm;
        this.playtimeSec = playtimeSec;
        this.audioFileUrl = audioFileUrl;
        if (accessLevel != null) this.accessLevel = accessLevel;
        if (level != null) this.level = level;
    }

    // 공개 범위 수정
    public void changeAccessLevel(AccessLevel accessLevel) {
        if (accessLevel != null) {
            this.accessLevel = accessLevel;
        }
    }

    //  코드 진행 추가
    public void addChordProgression(ChordProgression chordProgression) {
        this.chordProgressions.add(chordProgression);
    }

    // 재생 수 증가
    public void increasePlayCount() {
        if (this.playCount == null) {
            this.playCount = 0;
        }
        this.playCount += 1;
    }
}
