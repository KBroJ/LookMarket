package com.lookmarket.api.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lookmarket.api.security.JwtTokenProvider;
import com.lookmarket.application.user.UserService;
import com.lookmarket.domain.user.User;
import com.lookmarket.domain.user.UserRole;
import com.lookmarket.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController E2E 테스트
 *
 * MockMvc를 사용하여 HTTP 요청/응답을 검증하는 테스트
 */
@DisplayName("UserController E2E 테스트")
@WebMvcTest(UserController.class)
@Import({
        JwtTokenProvider.class,
        com.lookmarket.api.config.GlobalExceptionHandler.class,
        com.lookmarket.api.config.SecurityConfig.class,
        com.lookmarket.api.security.JwtAuthenticationFilter.class
})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-for-jwt-token-generation-must-be-long-enough-for-testing",
        "jwt.access-token-expiration=3600000",
        "jwt.refresh-token-expiration=604800000"
})
class UserControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserService userService;

    private User testUser;
    private String validAccessToken;

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

        // 테스트용 JWT 토큰 생성
        validAccessToken = jwtTokenProvider.createAccessToken(
                testUser.getId(),
                testUser.getEmail(),
                testUser.getRole().name()
        );
    }

    @Nested
    @DisplayName("POST /api/v1/users - 회원가입")
    class Register {

        @Test
        @DisplayName("회원가입 성공 시 201과 사용자 정보를 반환한다")
        void success() throws Exception {
            // given
            UserRequest request = new UserRequest(
                    "newuser@example.com",
                    "password123",
                    "New User",
                    "010-9876-5432",
                    UserRole.CUSTOMER
            );

            User newUser = User.reconstitute(
                    2L,
                    request.email(),
                    "encodedPassword",
                    request.name(),
                    request.phoneNumber(),
                    request.role(),
                    UserStatus.ACTIVE,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            given(userService.register(
                    anyString(), anyString(), anyString(), anyString(), any(UserRole.class)
            )).willReturn(newUser);

            // when & then
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.email").value("newuser@example.com"))
                    .andExpect(jsonPath("$.name").value("New User"))
                    .andExpect(jsonPath("$.phoneNumber").value("010-9876-5432"))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("이메일이 이미 존재하면 400을 반환한다")
        void duplicateEmail_returns400() throws Exception {
            // given
            UserRequest request = new UserRequest(
                    "existing@example.com",
                    "password123",
                    "Test User",
                    "010-1234-5678",
                    UserRole.CUSTOMER
            );

            given(userService.register(
                    anyString(), anyString(), anyString(), anyString(), any(UserRole.class)
            )).willThrow(new IllegalArgumentException("Email already exists: existing@example.com"));

            // when & then
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.message").value("Email already exists: existing@example.com"));
        }

        @Test
        @DisplayName("이메일 형식이 잘못되면 400을 반환한다")
        void invalidEmailFormat_returns400() throws Exception {
            // given
            UserRequest request = new UserRequest(
                    "invalid-email",
                    "password123",
                    "Test User",
                    "010-1234-5678",
                    UserRole.CUSTOMER
            );

            // when & then
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("비밀번호가 8자 미만이면 400을 반환한다")
        void shortPassword_returns400() throws Exception {
            // given
            UserRequest request = new UserRequest(
                    "test@example.com",
                    "short",
                    "Test User",
                    "010-1234-5678",
                    UserRole.CUSTOMER
            );

            // when & then
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("이름이 비어있으면 400을 반환한다")
        void emptyName_returns400() throws Exception {
            // given
            UserRequest request = new UserRequest(
                    "test@example.com",
                    "password123",
                    "",
                    "010-1234-5678",
                    UserRole.CUSTOMER
            );

            // when & then
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("역할이 없으면 400을 반환한다")
        void nullRole_returns400() throws Exception {
            // given - JSON 직접 생성 (role: null)
            String requestJson = """
                    {
                        "email": "test@example.com",
                        "password": "password123",
                        "name": "Test User",
                        "phoneNumber": "010-1234-5678",
                        "role": null
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId} - 사용자 조회")
    class GetUser {

        @Test
        @DisplayName("인증된 사용자가 자신의 정보를 조회하면 200을 반환한다")
        void success() throws Exception {
            // given
            given(userService.getUserById(1L)).willReturn(Optional.of(testUser));

            // when & then
            mockMvc.perform(get("/api/v1/users/1")
                            .header("Authorization", "Bearer " + validAccessToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.name").value("Test User"))
                    .andExpect(jsonPath("$.phoneNumber").value("010-1234-5678"))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 400을 반환한다")
        void userNotFound_returns400() throws Exception {
            // given
            given(userService.getUserById(999L)).willReturn(Optional.empty());

            // when & then
            mockMvc.perform(get("/api/v1/users/999")
                            .header("Authorization", "Bearer " + validAccessToken))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.message").value("User not found: 999"));
        }

        @Test
        @DisplayName("인증 없이 조회하면 401을 반환한다")
        void withoutAuth_returns401() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/users/1"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/users/{userId}/email - 이메일 변경")
    class ChangeEmail {

        @Test
        @DisplayName("이메일 변경 성공 시 200과 업데이트된 정보를 반환한다")
        void success() throws Exception {
            // given
            ChangeEmailRequest request = new ChangeEmailRequest("newemail@example.com");

            User updatedUser = User.reconstitute(
                    1L,
                    "newemail@example.com",
                    "encodedPassword",
                    "Test User",
                    "010-1234-5678",
                    UserRole.CUSTOMER,
                    UserStatus.ACTIVE,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            given(userService.changeEmail(1L, "newemail@example.com")).willReturn(updatedUser);

            // when & then
            mockMvc.perform(patch("/api/v1/users/1/email")
                            .header("Authorization", "Bearer " + validAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("newemail@example.com"));
        }

        @Test
        @DisplayName("이메일이 이미 존재하면 400을 반환한다")
        void duplicateEmail_returns400() throws Exception {
            // given
            ChangeEmailRequest request = new ChangeEmailRequest("existing@example.com");

            given(userService.changeEmail(1L, "existing@example.com"))
                    .willThrow(new IllegalArgumentException("Email already exists: existing@example.com"));

            // when & then
            mockMvc.perform(patch("/api/v1/users/1/email")
                            .header("Authorization", "Bearer " + validAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("존재하지 않는 사용자의 이메일 변경 시 400을 반환한다")
        void userNotFound_returns400() throws Exception {
            // given
            ChangeEmailRequest request = new ChangeEmailRequest("newemail@example.com");

            given(userService.changeEmail(999L, "newemail@example.com"))
                    .willThrow(new IllegalArgumentException("User not found: 999"));

            // when & then
            mockMvc.perform(patch("/api/v1/users/999/email")
                            .header("Authorization", "Bearer " + validAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("이메일 형식이 잘못되면 400을 반환한다")
        void invalidEmailFormat_returns400() throws Exception {
            // given
            ChangeEmailRequest request = new ChangeEmailRequest("invalid-email");

            // when & then
            mockMvc.perform(patch("/api/v1/users/1/email")
                            .header("Authorization", "Bearer " + validAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("인증 없이 이메일 변경하면 401을 반환한다")
        void withoutAuth_returns401() throws Exception {
            // given
            ChangeEmailRequest request = new ChangeEmailRequest("newemail@example.com");

            // when & then
            mockMvc.perform(patch("/api/v1/users/1/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/users/{userId}/password - 비밀번호 변경")
    class ChangePassword {

        @Test
        @DisplayName("비밀번호 변경 성공 시 200을 반환한다")
        void success() throws Exception {
            // given
            ChangePasswordRequest request = new ChangePasswordRequest("currentPassword", "newPassword123");

            given(userService.changePassword(1L, "currentPassword", "newPassword123"))
                    .willReturn(testUser);

            // when & then
            mockMvc.perform(patch("/api/v1/users/1/password")
                            .header("Authorization", "Bearer " + validAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("현재 비밀번호가 틀리면 400을 반환한다")
        void wrongCurrentPassword_returns400() throws Exception {
            // given
            ChangePasswordRequest request = new ChangePasswordRequest("wrongPassword", "newPassword123");

            given(userService.changePassword(1L, "wrongPassword", "newPassword123"))
                    .willThrow(new IllegalArgumentException("Current password is incorrect"));

            // when & then
            mockMvc.perform(patch("/api/v1/users/1/password")
                            .header("Authorization", "Bearer " + validAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("새 비밀번호가 8자 미만이면 400을 반환한다")
        void shortNewPassword_returns400() throws Exception {
            // given
            ChangePasswordRequest request = new ChangePasswordRequest("currentPassword", "short");

            // when & then
            mockMvc.perform(patch("/api/v1/users/1/password")
                            .header("Authorization", "Bearer " + validAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("인증 없이 비밀번호 변경하면 401을 반환한다")
        void withoutAuth_returns401() throws Exception {
            // given
            ChangePasswordRequest request = new ChangePasswordRequest("currentPassword", "newPassword123");

            // when & then
            mockMvc.perform(patch("/api/v1/users/1/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/{userId}/activate - 계정 활성화")
    class ActivateUser {

        @Test
        @DisplayName("계정 활성화 성공 시 200과 업데이트된 정보를 반환한다")
        void success() throws Exception {
            // given
            User activatedUser = User.reconstitute(
                    1L,
                    "test@example.com",
                    "encodedPassword",
                    "Test User",
                    "010-1234-5678",
                    UserRole.CUSTOMER,
                    UserStatus.ACTIVE,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            given(userService.activateUser(1L)).willReturn(activatedUser);

            // when & then
            mockMvc.perform(post("/api/v1/users/1/activate")
                            .header("Authorization", "Bearer " + validAccessToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 활성화 시 400을 반환한다")
        void userNotFound_returns400() throws Exception {
            // given
            given(userService.activateUser(999L))
                    .willThrow(new IllegalArgumentException("User not found: 999"));

            // when & then
            mockMvc.perform(post("/api/v1/users/999/activate")
                            .header("Authorization", "Bearer " + validAccessToken))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("인증 없이 계정 활성화하면 401을 반환한다")
        void withoutAuth_returns401() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/users/1/activate"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/{userId}/suspend - 계정 정지")
    class SuspendUser {

        @Test
        @DisplayName("계정 정지 성공 시 200과 업데이트된 정보를 반환한다")
        void success() throws Exception {
            // given
            User suspendedUser = User.reconstitute(
                    1L,
                    "test@example.com",
                    "encodedPassword",
                    "Test User",
                    "010-1234-5678",
                    UserRole.CUSTOMER,
                    UserStatus.SUSPENDED,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            given(userService.suspendUser(1L)).willReturn(suspendedUser);

            // when & then
            mockMvc.perform(post("/api/v1/users/1/suspend")
                            .header("Authorization", "Bearer " + validAccessToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUSPENDED"));
        }

        @Test
        @DisplayName("인증 없이 계정 정지하면 401을 반환한다")
        void withoutAuth_returns401() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/users/1/suspend"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/{userId}/deactivate - 계정 비활성화")
    class DeactivateUser {

        @Test
        @DisplayName("계정 비활성화 성공 시 200과 업데이트된 정보를 반환한다")
        void success() throws Exception {
            // given
            User deactivatedUser = User.reconstitute(
                    1L,
                    "test@example.com",
                    "encodedPassword",
                    "Test User",
                    "010-1234-5678",
                    UserRole.CUSTOMER,
                    UserStatus.INACTIVE,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            given(userService.deactivateUser(1L)).willReturn(deactivatedUser);

            // when & then
            mockMvc.perform(post("/api/v1/users/1/deactivate")
                            .header("Authorization", "Bearer " + validAccessToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("INACTIVE"));
        }

        @Test
        @DisplayName("인증 없이 계정 비활성화하면 401을 반환한다")
        void withoutAuth_returns401() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/users/1/deactivate"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }
}
