package com.mr.domain.backingTrack.entity;

import com.mr.domain.backingTrack.entity.enums.AccessLevel;
import com.mr.domain.backingTrack.entity.enums.Level;
import com.mr.domain.backingTrack.entity.enums.ScaleType;
import com.mr.global.entity.BaseTimeDeletedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 학원 아이디
    @Column(name = "academy_id")
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
    @Column(name = "time", nullable = false, length = 10)
    private String time;

    // bpm
    @Column(name = "bpm", nullable = false)
    private Integer bpm;

    // 재생 시간
    @Column(name = "playtime_sec", nullable = false)
    private Integer playtimeSec;

    // 오디오 파일
    @Column(name = "audio_file_url", length = 255)
    private String audioFileUrl;

    @Column(name = "midi_file_url", columnDefinition = "JSON")
    private String midiFileUrl;

    // 재생 수
    @Column(name = "play_count", nullable = false)
    private Integer playCount;

    // 공개 범위
    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, columnDefinition = "ENUM('PRIVATE', 'ACADEMY', 'PUBLIC') DEFAULT 'PRIVATE'")
    private AccessLevel accessLevel;

    // 난이도
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, columnDefinition = "ENUM('BASIC', 'MED', 'ADVANCED') DEFAULT 'BASIC'")
    private Level level;

    @Builder(access = lombok.AccessLevel.PRIVATE)
    private BackingTrack(Long userId, Long academyId, String title, String genre,
                         String keySignature, ScaleType scaleType, String time,
                         Integer bpm, Integer playtimeSec, String audioFileUrl,
                         String midiFileUrl, Integer playCount,
                         AccessLevel accessLevel, Level level) {
        this.userId = userId;
        this.academyId = academyId;
        this.title = title;
        this.genre = genre;
        this.keySignature = keySignature;
        this.scaleType = scaleType;
        this.time = time;
        this.bpm = bpm;
        this.playtimeSec = playtimeSec;
        this.audioFileUrl = audioFileUrl;
        this.midiFileUrl = midiFileUrl;
        this.playCount = playCount != null ? playCount : 0;
        this.accessLevel = accessLevel != null ? accessLevel : AccessLevel.PRIVATE;
        this.level = level != null ? level : Level.BASIC;
    }

    public static BackingTrack create(Long userId, Long academyId, String title, String genre,
                                      String keySignature, ScaleType scaleType, String time,
                                      Integer bpm, Integer playtimeSec, String audioFileUrl,
                                      String midiFileUrl, AccessLevel accessLevel, Level level) {
        return BackingTrack.builder()
                .userId(userId)
                .academyId(academyId)
                .title(title)
                .genre(genre)
                .keySignature(keySignature)
                .scaleType(scaleType)
                .time(time)
                .bpm(bpm)
                .playtimeSec(playtimeSec)
                .audioFileUrl(audioFileUrl)
                .midiFileUrl(midiFileUrl)
                .playCount(0)
                .accessLevel(accessLevel)
                .level(level)
                .build();
    }

    public void updateTrackInfo(String title, String genre, String keySignature,
                                ScaleType scaleType, String time, Integer bpm,
                                Integer playtimeSec, AccessLevel accessLevel, Level level) {
        this.title = title;
        this.genre = genre;
        this.keySignature = keySignature;
        this.scaleType = scaleType;
        this.time = time;
        this.bpm = bpm;
        this.playtimeSec = playtimeSec;
        if (accessLevel != null) this.accessLevel = accessLevel;
        if (level != null) this.level = level;
    }

    // 재생 수 1 증가
    public void incrementPlayCount() {
        this.playCount++;
    }

    // 공개 범위 수정
    public void changeAccessLevel(AccessLevel accessLevel) {
        if (accessLevel != null) {
            this.accessLevel = accessLevel;
        }
    }
}
