package com.mr.domain.backingTrack.entity;

import com.mr.domain.backingTrack.entity.enums.AccessLevel;
import com.mr.domain.backingTrack.entity.enums.Level;
import com.mr.domain.backingTrack.entity.enums.ScaleType;
import com.mr.domain.backingTrack.entity.enums.TrackType;
import com.mr.global.entity.BaseTimeDeletedEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(
        name = "backing_track"
)
public class BackingTrack extends BaseTimeDeletedEntity {

    @Id
    @Column(name = "backing_track_id")
    private Long id;

    // 유저 아이디
    @JoinColumn(name = "user_id", nullable = false)
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

    // 트랙 유형
    @Enumerated(EnumType.STRING)
    @Column(name = "track_type", nullable = false, columnDefinition = "ENUM('SYSTEM', 'USER_AUDIO') DEFAULT 'SYSTEM'")
    private TrackType trackType;

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
}
