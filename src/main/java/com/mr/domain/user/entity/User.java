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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Size(max = 10)
    @Column(name = "nickname", unique = true, length = 10)
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
        this.nickname = nickname;
    }
}