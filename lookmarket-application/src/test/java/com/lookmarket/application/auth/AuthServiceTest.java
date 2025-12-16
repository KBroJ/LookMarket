package com.lookmarket.application.auth;

import com.lookmarket.domain.user.User;
import com.lookmarket.domain.user.UserRepository;
import com.lookmarket.domain.user.UserRole;
import com.lookmarket.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@DisplayName("AuthService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private User activeUser;
    private User inactiveUser;
    private User suspendedUser;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        // 활성 사용자
        activeUser = User.reconstitute(
                1L,
                "active@example.com",
                "encodedPassword",
                "Active User",
                "010-1234-5678",
                UserRole.CUSTOMER,
                UserStatus.ACTIVE,
                now,
                now
        );

        // 비활성 사용자
        inactiveUser = User.reconstitute(
                2L,
                "inactive@example.com",
                "encodedPassword",
                "Inactive User",
                "010-1234-5678",
                UserRole.CUSTOMER,
                UserStatus.INACTIVE,
                now,
                now
        );

        // 정지된 사용자
        suspendedUser = User.reconstitute(
                3L,
                "suspended@example.com",
                "encodedPassword",
                "Suspended User",
                "010-1234-5678",
                UserRole.CUSTOMER,
                UserStatus.SUSPENDED,
                now,
                now
        );
    }

    @Nested
    @DisplayName("로그인 인증 (authenticate)")
    class Authenticate {

        @Test
        @DisplayName("이메일과 비밀번호가 일치하면 사용자를 반환한다")
        void authenticate_success() {
            // given
            String email = "active@example.com";
            String rawPassword = "password123";

            given(userRepository.findByEmail(email)).willReturn(Optional.of(activeUser));
            given(passwordEncoder.matches(rawPassword, activeUser.getPassword())).willReturn(true);

            // when
            User result = authService.authenticate(email, rawPassword);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo(email);
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 예외가 발생한다")
        void authenticate_userNotFound_throwsException() {
            // given
            String email = "notfound@example.com";
            String rawPassword = "password123";

            given(userRepository.findByEmail(email)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.authenticate(email, rawPassword))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 예외가 발생한다")
        void authenticate_wrongPassword_throwsException() {
            // given
            String email = "active@example.com";
            String wrongPassword = "wrongPassword";

            given(userRepository.findByEmail(email)).willReturn(Optional.of(activeUser));
            given(passwordEncoder.matches(wrongPassword, activeUser.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.authenticate(email, wrongPassword))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        @Test
        @DisplayName("비활성화된 계정이면 예외가 발생한다")
        void authenticate_inactiveUser_throwsException() {
            // given
            String email = "inactive@example.com";
            String rawPassword = "password123";

            given(userRepository.findByEmail(email)).willReturn(Optional.of(inactiveUser));

            // when & then
            assertThatThrownBy(() -> authService.authenticate(email, rawPassword))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("비활성화된 계정입니다");
        }

        @Test
        @DisplayName("정지된 계정이면 예외가 발생한다")
        void authenticate_suspendedUser_throwsException() {
            // given
            String email = "suspended@example.com";
            String rawPassword = "password123";

            given(userRepository.findByEmail(email)).willReturn(Optional.of(suspendedUser));

            // when & then
            assertThatThrownBy(() -> authService.authenticate(email, rawPassword))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("정지된 계정입니다");
        }
    }

    @Nested
    @DisplayName("토큰 갱신 검증 (validateUserForRefresh)")
    class ValidateUserForRefresh {

        @Test
        @DisplayName("활성 사용자는 검증을 통과한다")
        void validateUserForRefresh_activeUser_success() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));

            // when
            User result = authService.validateUserForRefresh(userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
            assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("존재하지 않는 사용자는 예외가 발생한다")
        void validateUserForRefresh_userNotFound_throwsException() {
            // given
            Long userId = 999L;
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.validateUserForRefresh(userId))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("비활성 사용자는 예외가 발생한다")
        void validateUserForRefresh_inactiveUser_throwsException() {
            // given
            Long userId = 2L;
            given(userRepository.findById(userId)).willReturn(Optional.of(inactiveUser));

            // when & then
            assertThatThrownBy(() -> authService.validateUserForRefresh(userId))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("비활성 상태의 계정입니다");
        }

        @Test
        @DisplayName("정지된 사용자는 예외가 발생한다")
        void validateUserForRefresh_suspendedUser_throwsException() {
            // given
            Long userId = 3L;
            given(userRepository.findById(userId)).willReturn(Optional.of(suspendedUser));

            // when & then
            assertThatThrownBy(() -> authService.validateUserForRefresh(userId))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("비활성 상태의 계정입니다");
        }
    }
}
