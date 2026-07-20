package com.mr.domain.user.entity;

import com.mr.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    private static final int NICKNAME_MAX_LENGTH = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Size(max = NICKNAME_MAX_LENGTH)
    @Column(name = "nickname", unique = true, length = NICKNAME_MAX_LENGTH)
    private String nickname;

    @Column(name = "profile_img_url", nullable = false, length = 255)
    private String profileImgUrl;

    @Builder(access = AccessLevel.PRIVATE)
    private User(String profileImgUrl) {
        this.profileImgUrl = profileImgUrl;
    }

    public static User createFromOAuth(String profileImgUrl) {
        return User.builder()
                .profileImgUrl(profileImgUrl)
                .build();
    }

    public void updateNickname(String nickname) {
        validateNickname(nickname);
        this.nickname = nickname;
    }

    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }
        if (nickname.length() > NICKNAME_MAX_LENGTH) {
            throw new IllegalArgumentException("닉네임은 " + NICKNAME_MAX_LENGTH + "자를 초과할 수 없습니다.");
        }
    }
}