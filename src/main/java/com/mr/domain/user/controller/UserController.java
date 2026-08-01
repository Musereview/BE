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
@Tag(name = "사용자", description = "인증 불필요한 사용자 관련 API")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "닉네임 중복 확인",
            description = "닉네임이 형식 규칙(한글/영문/숫자 2~10자)을 만족하는지, 이미 사용 중인지 확인합니다. "
                    + "인증 없이 호출 가능합니다."
    )
    @GetMapping("/verify-nickname")
    public ApiResponse<UserResponseDTO.NicknameCheckResponse> verifyNickname(
            @Parameter(description = "확인할 닉네임", example = "김뮤즈")
            @RequestParam String nickname
    ) {
        return ApiResponse.onSuccess(userService.checkNicknameAvailable(nickname));
    }
}
