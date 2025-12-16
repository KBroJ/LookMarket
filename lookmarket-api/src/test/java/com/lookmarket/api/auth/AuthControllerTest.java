package com.lookmarket.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lookmarket.api.security.JwtTokenProvider;
import com.lookmarket.application.auth.AuthService;
import com.lookmarket.application.auth.AuthenticationException;
import com.lookmarket.domain.user.User;
import com.lookmarket.domain.user.UserRole;
import com.lookmarket.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController 통합 테스트")
@WebMvcTest(AuthController.class)
@Import({JwtTokenProvider.class, com.lookmarket.api.config.GlobalExceptionHandler.class, com.lookmarket.api.config.SecurityConfig.class, com.lookmarket.api.security.JwtAuthenticationFilter.class})
@org.springframework.test.context.TestPropertySource(properties = {
        "jwt.secret=test-secret-key-for-jwt-token-generation-must-be-long-enough-for-testing",
        "jwt.access-token-expiration=3600000",
        "jwt.refresh-token-expiration=604800000"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        testUser = User.reconstitute(
                1L,
                "test@example.com",
                "encodedPassword",
                "Test User",
                "010-1234-5678",
                UserRole.CUSTOMER,
                UserStatus.ACTIVE,
                now,
                now
        );
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("로그인 성공 시 토큰을 반환한다")
        void login_success() throws Exception {
            // given
            LoginRequest request = new LoginRequest("test@example.com", "password123");

            given(authService.authenticate(anyString(), anyString())).willReturn(testUser);

            // when & then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").isNumber());
        }

        @Test
        @DisplayName("잘못된 인증 정보로 로그인 시 401을 반환한다")
        void login_invalidCredentials_returns401() throws Exception {
            // given
            LoginRequest request = new LoginRequest("test@example.com", "wrongPassword");

            given(authService.authenticate(anyString(), anyString()))
                    .willThrow(new AuthenticationException("이메일 또는 비밀번호가 올바르지 않습니다."));

            // when & then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                    .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
        }

        @Test
        @DisplayName("이메일 형식이 잘못되면 400을 반환한다")
        void login_invalidEmailFormat_returns400() throws Exception {
            // given
            LoginRequest request = new LoginRequest("invalid-email", "password123");

            // when & then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("이메일이 비어있으면 400을 반환한다")
        void login_emptyEmail_returns400() throws Exception {
            // given
            LoginRequest request = new LoginRequest("", "password123");

            // when & then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("비밀번호가 비어있으면 400을 반환한다")
        void login_emptyPassword_returns400() throws Exception {
            // given
            LoginRequest request = new LoginRequest("test@example.com", "");

            // when & then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("유효한 Refresh Token으로 새 토큰을 발급받는다")
        void refresh_success() throws Exception {
            // given
            String refreshToken = jwtTokenProvider.createRefreshToken(1L);
            TokenRefreshRequest request = new TokenRefreshRequest(refreshToken);

            given(authService.validateUserForRefresh(anyLong())).willReturn(testUser);

            // when & then
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("유효하지 않은 Refresh Token이면 401을 반환한다")
        void refresh_invalidToken_returns401() throws Exception {
            // given
            TokenRefreshRequest request = new TokenRefreshRequest("invalid.refresh.token");

            // when & then
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
        }

        @Test
        @DisplayName("Access Token으로 갱신 시도하면 401을 반환한다")
        void refresh_withAccessToken_returns401() throws Exception {
            // given
            String accessToken = jwtTokenProvider.createAccessToken(1L, "test@example.com", "USER");
            TokenRefreshRequest request = new TokenRefreshRequest(accessToken);

            // when & then
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                    .andExpect(jsonPath("$.message").value("Refresh Token이 아닙니다."));
        }

        @Test
        @DisplayName("Refresh Token이 비어있으면 400을 반환한다")
        void refresh_emptyToken_returns400() throws Exception {
            // given
            TokenRefreshRequest request = new TokenRefreshRequest("");

            // when & then
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("비활성 사용자의 Refresh Token이면 401을 반환한다")
        void refresh_inactiveUser_returns401() throws Exception {
            // given
            String refreshToken = jwtTokenProvider.createRefreshToken(1L);
            TokenRefreshRequest request = new TokenRefreshRequest(refreshToken);

            given(authService.validateUserForRefresh(anyLong()))
                    .willThrow(new AuthenticationException("비활성 상태의 계정입니다."));

            // when & then
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                    .andExpect(jsonPath("$.message").value("비활성 상태의 계정입니다."));
        }
    }
}
