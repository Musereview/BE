package com.mr.domain.user.entity;

import com.mr.domain.user.entity.enums.UserErrorStatus;
import com.mr.global.apipayload.exception.GeneralException;
import com.mr.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    private static final int NICKNAME_MIN_LENGTH = 2;
    private static final int NICKNAME_MAX_LENGTH = 10;
    private static final Pattern NICKNAME_PATTERN =
            Pattern.compile("^[가-힣a-zA-Z0-9]{" + NICKNAME_MIN_LENGTH + "," + NICKNAME_MAX_LENGTH + "}$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Size(
            min = NICKNAME_MIN_LENGTH,
            max = NICKNAME_MAX_LENGTH,
            message = "닉네임은 2자 이상 10자 이하로 입력해야 합니다."
    )
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

    public boolean isOnboardingCompleted() {
        return nickname != null;
    }

    public void updateNickname(String nickname) {
        String trimmedNickname = nickname == null ? null : nickname.trim();
        validateNickname(trimmedNickname);
        this.nickname = trimmedNickname;
    }

    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new GeneralException(UserErrorStatus.NICKNAME_REQUIRED);
        }
        if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new GeneralException(UserErrorStatus.NICKNAME_INVALID_FORMAT);
        }
    }
}