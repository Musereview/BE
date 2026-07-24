//package com.mr.domain.auth.service;
//
//import com.mr.domain.auth.dto.AuthResponseDTO;
//import com.mr.domain.auth.entity.SocialAuth;
//import com.mr.domain.auth.entity.enums.SocialType;
//import com.mr.domain.auth.repository.SocialAuthRepository;
//import com.mr.domain.user.entity.User;
//import com.mr.domain.user.repository.UserRepository;
//import com.mr.global.security.jwt.JwtTokenProvider;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//@Transactional(readOnly = true)
//public class AuthService {
//
//    private final JwtTokenProvider jwtTokenProvider;
//   // private final UserRepository userRepository;
//    //private final SocialAuthRepository socialAuthRepository;
//
//    @Transactional
//    public AuthResponseDTO.LoginResponse socialLogin(SocialType socialType, String accessToken) {
//        // 1. TODO: 추후 외부 소셜 API(카카오/구글) 파싱 연동
//        String socialId = "12345678";
//        String profileImgUrl = "https://example.com/default_profile.png";
//
//        // 2. DB에서 유저 조회 또는 신규 가입 (User.createFromOAuth 연동)
//       // Optional<SocialAuth> optionalSocialAuth = socialAuthRepository.findBySocialTypeAndSocialId(socialType, socialId);
//        //boolean isNewUser = optionalSocialAuth.isEmpty();
//
//        User user;
//        SocialAuth socialAuth;
//
//        if (isNewUser) {
//            // 신규 유저 생성 (User 엔티티 PK 필드명이 userId)
//            user = userRepository.save(User.createFromOAuth(profileImgUrl));
//
//            // 임시 토큰 값 및 만료시간 세팅 (추후 소셜 Refresh Token 수신 시 적용)
//            String dummyEncryptedToken = "encrypted_refresh_token_sample";
//            String dummyTokenHash = "hash_value_sample_64_characters_hash_string_sample_hash_value_123";
//            LocalDateTime dummyExpiredAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusDays(14);
//
//            socialAuth = socialAuthRepository.save(
//                    SocialAuth.create(
//                            user.getUserId(), // ★ user.getId() -> user.getUserId() 로 수정!
//                            socialType,
//                            socialId,
//                            dummyEncryptedToken,
//                            dummyTokenHash,
//                            dummyExpiredAt,
//                            "Web"
//                    )
//            );
//        } else {
//            socialAuth = optionalSocialAuth.get();
//            user = userRepository.findById(socialAuth.getUserId())
//                    .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
//
//            // 기존 토큰 정보 갱신 (SocialAuth.updateRefreshToken 연동)
//            String updatedEncryptedToken = "updated_encrypted_refresh_token";
//            String updatedTokenHash = "updated_hash_value_sample_64_characters_hash_string_sample_1234";
//            LocalDateTime updatedExpiredAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusDays(14);
//
//            socialAuth.updateRefreshToken(updatedEncryptedToken, updatedTokenHash, updatedExpiredAt, "Web");
//        }
//
//        // 3. 앱 서비스 전용 JWT 토큰 생성 (user.getUserId() 사용)
//        String appAccessToken = jwtTokenProvider.createAccessToken(user.getUserId());
//        String appRefreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());
//
//        AuthResponseDTO.TokenResponse tokenResponse = AuthResponseDTO.TokenResponse.builder()
//                .accessToken(appAccessToken)
//                .refreshToken(appRefreshToken)
//                .tokenExpirationTime(3600L)
//                .build();
//
//        return AuthResponseDTO.LoginResponse.builder()
//                .userId(user.getUserId()) // ★ user.getUserId()
//                .nickname(user.getNickname()) // 최초 가입 시 null일 수 있음 (온보딩 여부 확인용)
//                .isNewUser(isNewUser)
//                .tokenInfo(tokenResponse)
//                .build();
//    }
//}