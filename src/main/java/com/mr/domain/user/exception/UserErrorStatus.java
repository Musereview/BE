package com.mr.domain.user.exception;

import com.mr.global.apipayload.code.BaseCode;
import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserErrorStatus implements BaseCode {

    NICKNAME_REQUIRED(HttpStatus.BAD_REQUEST, "USER_400_01", "닉네임은 필수입니다."),
    NICKNAME_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "USER_400_02", "닉네임은 한글, 영어, 숫자 2~10자로 입력해야 합니다."),
    PROFILE_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "USER_400_03", "프로필 이미지 URL은 필수입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_01", "존재하지 않는 사용자입니다."),
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "USER_409_01", "이미 사용 중인 닉네임입니다."),
    ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "USER_409_02", "이미 온보딩을 완료한 사용자입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}