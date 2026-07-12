package com.mr.domain.user.entity;

import com.mr.domain.user.entity.enums.InstrumentType;
import com.mr.domain.user.entity.enums.SkillLevel;
import com.mr.domain.user.entity.enums.UserRole;
import com.mr.global.entity.BaseTimeDeletedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeDeletedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "nickname", unique = true, length = 10)
    private String nickname;

    @Column(name = "email", length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.GENERAL;

    @Column(name = "profile_img_url", length = 255)
    private String profileImgUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", length = 20)
    private SkillLevel skillLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "instrument_type", nullable = false, length = 20)
    private InstrumentType instrumentType = InstrumentType.KEYBOARD;

    public void updateProfile(String nickname, SkillLevel skillLevel) {
        this.nickname = nickname;
        this.skillLevel = skillLevel;
    }
}