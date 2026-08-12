package com.mr.domain.user.controller;

import com.mr.domain.user.dto.res.UserResponseDTO;
import com.mr.domain.user.service.UserService;
import com.mr.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "사용자", description = "사용자 API")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "닉네임 중복 확인 API",
            description = "회원가입 온보딩 단계 또는 프로필 수정 시, 닉네임이 형식 규칙(한글/영문/숫자 2~10자)을 만족하는지, "
                    + "이미 다른 유저가 사용 중인지 확인합니다. 인증이 필요하며, 본인이 이미 쓰고 있는 닉네임은 중복 처리되지 않습니다."
    )
    @GetMapping("/verify-nickname")
    public ApiResponse<UserResponseDTO.NicknameCheckResponse> verifyNickname(
            @Parameter(description = "확인할 닉네임", example = "김뮤즈")
            @RequestParam(required = false) String nickname
    ) {
        return ApiResponse.onSuccess(userService.checkNicknameAvailable(nickname));
    }
}
