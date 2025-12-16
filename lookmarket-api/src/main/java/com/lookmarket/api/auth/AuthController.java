package com.lookmarket.api.auth;

import com.lookmarket.api.security.JwtTokenProvider;
import com.lookmarket.application.auth.AuthService;
import com.lookmarket.domain.user.User;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 REST API Controller
 *
 * 엔드포인트:
 * - POST /api/v1/auth/login: 로그인 (토큰 발급)
 * - POST /api/v1/auth/refresh: 토큰 갱신
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 로그인 API
     *
     * @param request 로그인 요청 (이메일, 비밀번호)
     * @return 토큰 응답 (accessToken, refreshToken)
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("로그인 시도: email={}", request.email());

        // 사용자 인증
        User user = authService.authenticate(request.email(), request.password());

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        log.info("로그인 성공: userId={}", user.getId());

        return ResponseEntity.ok(TokenResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiration()
        ));
    }

    /**
     * 토큰 갱신 API
     *
     * @param request 토큰 갱신 요청 (refreshToken)
     * @return 새로운 토큰 응답
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        String refreshToken = request.refreshToken();

        // Refresh Token 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new com.lookmarket.application.auth.AuthenticationException("유효하지 않은 Refresh Token입니다.");
        }

        // 토큰 타입 확인
        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new com.lookmarket.application.auth.AuthenticationException("Refresh Token이 아닙니다.");
        }

        // 사용자 검증
        Long userId = jwtTokenProvider.getUserId(refreshToken);
        User user = authService.validateUserForRefresh(userId);

        // 새 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        log.info("토큰 갱신 성공: userId={}", userId);

        return ResponseEntity.ok(TokenResponse.of(
                newAccessToken,
                newRefreshToken,
                jwtTokenProvider.getAccessTokenExpiration()
        ));
    }
}
