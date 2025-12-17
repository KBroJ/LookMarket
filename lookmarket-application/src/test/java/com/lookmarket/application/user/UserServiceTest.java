package com.lookmarket.application.user;

import com.lookmarket.domain.user.User;
import com.lookmarket.domain.user.UserRepository;
import com.lookmarket.domain.user.UserRole;
import com.lookmarket.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * UserService 단위 테스트
 *
 * Mock을 사용하여 의존성을 격리한 테스트
 */
@DisplayName("UserService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.create(
                "test@example.com",
                "encodedPassword",
                "홍길동",
                "010-1234-5678",
                UserRole.CUSTOMER
        );
    }

    @Nested
    @DisplayName("register() - 회원가입")
    class Register {

        @Test
        @DisplayName("새 사용자를 등록할 수 있다")
        void success() {
            // given
            String email = "new@example.com";
            String plainPassword = "password123";
            String encodedPassword = "encodedPassword123";
            String name = "신규회원";
            String phoneNumber = "010-9999-9999";
            UserRole role = UserRole.CUSTOMER;

            given(userRepository.existsByEmail(email)).willReturn(false);
            given(passwordEncoder.encode(plainPassword)).willReturn(encodedPassword);
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            User savedUser = userService.register(email, plainPassword, name, phoneNumber, role);

            // then
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getEmail()).isEqualTo(email);
            assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
            assertThat(savedUser.getName()).isEqualTo(name);
            assertThat(savedUser.getPhoneNumber()).isEqualTo(phoneNumber);
            assertThat(savedUser.getRole()).isEqualTo(role);
            assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);

            verify(userRepository).existsByEmail(email);
            verify(passwordEncoder).encode(plainPassword);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("중복된 이메일로 등록하면 예외가 발생한다")
        void failWithDuplicateEmail() {
            // given
            String email = "duplicate@example.com";
            given(userRepository.existsByEmail(email)).willReturn(true);

            // when & then
            assertThatThrownBy(() ->
                    userService.register(email, "password", "이름", null, UserRole.CUSTOMER)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email already exists: " + email);

            verify(userRepository).existsByEmail(email);
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("getUserById() - ID로 조회")
    class GetUserById {

        @Test
        @DisplayName("존재하는 ID로 사용자를 조회할 수 있다")
        void success() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            Optional<User> foundUser = userService.getUserById(userId);

            // then
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get()).isEqualTo(testUser);

            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 빈 Optional을 반환한다")
        void notFound() {
            // given
            Long userId = 999L;
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when
            Optional<User> foundUser = userService.getUserById(userId);

            // then
            assertThat(foundUser).isEmpty();

            verify(userRepository).findById(userId);
        }
    }

    @Nested
    @DisplayName("getUserByEmail() - 이메일로 조회")
    class GetUserByEmail {

        @Test
        @DisplayName("존재하는 이메일로 사용자를 조회할 수 있다")
        void success() {
            // given
            String email = "test@example.com";
            given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));

            // when
            Optional<User> foundUser = userService.getUserByEmail(email);

            // then
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getEmail()).isEqualTo(email);

            verify(userRepository).findByEmail(email);
        }
    }

    @Nested
    @DisplayName("changeEmail() - 이메일 변경")
    class ChangeEmail {

        @Test
        @DisplayName("새 이메일로 변경할 수 있다")
        void success() {
            // given
            Long userId = 1L;
            String newEmail = "newemail@example.com";

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(userRepository.existsByEmail(newEmail)).willReturn(false);
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            User updatedUser = userService.changeEmail(userId, newEmail);

            // then
            assertThat(updatedUser.getEmail()).isEqualTo(newEmail);

            verify(userRepository).findById(userId);
            verify(userRepository).existsByEmail(newEmail);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("존재하지 않는 사용자의 이메일을 변경하면 예외가 발생한다")
        void failUserNotFound() {
            // given
            Long userId = 999L;
            String newEmail = "newemail@example.com";

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    userService.changeEmail(userId, newEmail)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found: " + userId);

            verify(userRepository).findById(userId);
            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("중복된 이메일로 변경하면 예외가 발생한다")
        void failWithDuplicateEmail() {
            // given
            Long userId = 1L;
            String newEmail = "duplicate@example.com";

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(userRepository.existsByEmail(newEmail)).willReturn(true);

            // when & then
            assertThatThrownBy(() ->
                    userService.changeEmail(userId, newEmail)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email already exists: " + newEmail);

            verify(userRepository).findById(userId);
            verify(userRepository).existsByEmail(newEmail);
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("changePassword() - 비밀번호 변경")
    class ChangePassword {

        @Test
        @DisplayName("현재 비밀번호가 일치하면 새 비밀번호로 변경할 수 있다")
        void success() {
            // given
            Long userId = 1L;
            String currentPassword = "currentPassword";
            String newPassword = "newPassword123";
            String encodedNewPassword = "encodedNewPassword123";
            String originalPassword = testUser.getPassword();

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(currentPassword, originalPassword)).willReturn(true);
            given(passwordEncoder.encode(newPassword)).willReturn(encodedNewPassword);
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            User updatedUser = userService.changePassword(userId, currentPassword, newPassword);

            // then
            assertThat(updatedUser.getPassword()).isEqualTo(encodedNewPassword);

            verify(userRepository).findById(userId);
            verify(passwordEncoder).matches(currentPassword, originalPassword);
            verify(passwordEncoder).encode(newPassword);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("현재 비밀번호가 일치하지 않으면 예외가 발생한다")
        void failWithWrongCurrentPassword() {
            // given
            Long userId = 1L;
            String wrongPassword = "wrongPassword";
            String newPassword = "newPassword123";

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(wrongPassword, testUser.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() ->
                    userService.changePassword(userId, wrongPassword, newPassword)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Current password does not match");

            verify(userRepository).findById(userId);
            verify(passwordEncoder).matches(wrongPassword, testUser.getPassword());
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("상태 변경 - activate/suspend/deactivate")
    class StatusChange {

        @Test
        @DisplayName("사용자를 활성화할 수 있다")
        void activateUser_success() {
            // given
            Long userId = 1L;
            testUser.deactivate();

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            User activatedUser = userService.activateUser(userId);

            // then
            assertThat(activatedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);

            verify(userRepository).findById(userId);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("사용자를 정지할 수 있다")
        void suspendUser_success() {
            // given
            Long userId = 1L;

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            User suspendedUser = userService.suspendUser(userId);

            // then
            assertThat(suspendedUser.getStatus()).isEqualTo(UserStatus.SUSPENDED);

            verify(userRepository).findById(userId);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("사용자를 비활성화할 수 있다")
        void deactivateUser_success() {
            // given
            Long userId = 1L;

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            User deactivatedUser = userService.deactivateUser(userId);

            // then
            assertThat(deactivatedUser.getStatus()).isEqualTo(UserStatus.INACTIVE);

            verify(userRepository).findById(userId);
            verify(userRepository).save(testUser);
        }
    }
}
