package com.lookmarket.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * User 도메인 엔티티 단위 테스트
 *
 * 비즈니스 로직 검증 (순수 Java, 프레임워크 의존 없음)
 */
@DisplayName("User 도메인 단위 테스트")
class UserTest {

    @Nested
    @DisplayName("create() - 사용자 생성")
    class Create {

        @Test
        @DisplayName("유효한 정보로 사용자를 생성할 수 있다")
        void success() {
            // given
            String email = "test@example.com";
            String password = "encodedPassword123";
            String name = "홍길동";
            String phoneNumber = "010-1234-5678";
            UserRole role = UserRole.CUSTOMER;

            // when
            User user = User.create(email, password, name, phoneNumber, role);

            // then
            assertThat(user).isNotNull();
            assertThat(user.getId()).isNull();  // ID는 데이터베이스에서 생성
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getPassword()).isEqualTo(password);
            assertThat(user.getName()).isEqualTo(name);
            assertThat(user.getPhoneNumber()).isEqualTo(phoneNumber);
            assertThat(user.getRole()).isEqualTo(role);
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.getCreatedAt()).isNotNull();
            assertThat(user.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("phoneNumber가 null이어도 사용자를 생성할 수 있다")
        void withoutPhoneNumber() {
            // given
            String email = "test@example.com";
            String password = "encodedPassword123";
            String name = "홍길동";
            UserRole role = UserRole.CUSTOMER;

            // when
            User user = User.create(email, password, name, null, role);

            // then
            assertThat(user).isNotNull();
            assertThat(user.getPhoneNumber()).isNull();
        }

        @Test
        @DisplayName("role이 null이면 기본값 CUSTOMER가 설정된다")
        void defaultRole() {
            // when
            User user = User.create("test@example.com", "password", "홍길동", null, null);

            // then
            assertThat(user.getRole()).isEqualTo(UserRole.CUSTOMER);
        }

        @Test
        @DisplayName("이메일이 null이면 예외가 발생한다")
        void failWithNullEmail() {
            // when & then
            assertThatThrownBy(() ->
                    User.create(null, "password", "홍길동", null, UserRole.CUSTOMER)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이메일은 필수 입력 항목입니다.");
        }

        @Test
        @DisplayName("이메일이 빈 문자열이면 예외가 발생한다")
        void failWithEmptyEmail() {
            // when & then
            assertThatThrownBy(() ->
                    User.create("", "password", "홍길동", null, UserRole.CUSTOMER)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이메일은 필수 입력 항목입니다.");
        }

        @Test
        @DisplayName("잘못된 이메일 형식이면 예외가 발생한다")
        void failWithInvalidEmailFormat() {
            // when & then
            assertThatThrownBy(() ->
                    User.create("invalid-email", "password", "홍길동", null, UserRole.CUSTOMER)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("올바른 이메일 형식이 아닙니다.");
        }

        @Test
        @DisplayName("비밀번호가 null이면 예외가 발생한다")
        void failWithNullPassword() {
            // when & then
            assertThatThrownBy(() ->
                    User.create("test@example.com", null, "홍길동", null, UserRole.CUSTOMER)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("비밀번호는 필수 입력 항목입니다.");
        }

        @Test
        @DisplayName("이름이 null이면 예외가 발생한다")
        void failWithNullName() {
            // when & then
            assertThatThrownBy(() ->
                    User.create("test@example.com", "password", null, null, UserRole.CUSTOMER)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이름은 필수 입력 항목입니다.");
        }
    }

    @Nested
    @DisplayName("changeEmail() - 이메일 변경")
    class ChangeEmail {

        @Test
        @DisplayName("유효한 이메일로 변경할 수 있다")
        void success() {
            // given
            User user = User.create("old@example.com", "password", "홍길동", null, UserRole.CUSTOMER);
            String newEmail = "new@example.com";

            // when
            user.changeEmail(newEmail);

            // then
            assertThat(user.getEmail()).isEqualTo(newEmail);
        }

        @Test
        @DisplayName("잘못된 형식의 이메일로 변경하면 예외가 발생한다")
        void failWithInvalidFormat() {
            // given
            User user = User.create("test@example.com", "password", "홍길동", null, UserRole.CUSTOMER);

            // when & then
            assertThatThrownBy(() -> user.changeEmail("invalid-email"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("올바른 이메일 형식이 아닙니다.");
        }

        @Test
        @DisplayName("동일한 이메일로 변경해도 정상 처리된다")
        void sameEmailNoChange() {
            // given
            User user = User.create("test@example.com", "password", "홍길동", null, UserRole.CUSTOMER);
            String originalEmail = user.getEmail();

            // when
            user.changeEmail("test@example.com");

            // then
            assertThat(user.getEmail()).isEqualTo(originalEmail);
        }
    }

    @Nested
    @DisplayName("changePassword() - 비밀번호 변경")
    class ChangePassword {

        @Test
        @DisplayName("새 비밀번호로 변경할 수 있다")
        void success() {
            // given
            User user = User.create("test@example.com", "oldPassword", "홍길동", null, UserRole.CUSTOMER);
            String newPassword = "newEncodedPassword";

            // when
            user.changePassword(newPassword);

            // then
            assertThat(user.getPassword()).isEqualTo(newPassword);
        }

        @Test
        @DisplayName("null 비밀번호로 변경하면 예외가 발생한다")
        void failWithNull() {
            // given
            User user = User.create("test@example.com", "oldPassword", "홍길동", null, UserRole.CUSTOMER);

            // when & then
            assertThatThrownBy(() -> user.changePassword(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("비밀번호는 필수 입력 항목입니다.");
        }
    }

    @Nested
    @DisplayName("changeName() - 이름 변경")
    class ChangeName {

        @Test
        @DisplayName("새 이름으로 변경할 수 있다")
        void success() {
            // given
            User user = User.create("test@example.com", "password", "홍길동", null, UserRole.CUSTOMER);
            String newName = "김철수";

            // when
            user.changeName(newName);

            // then
            assertThat(user.getName()).isEqualTo(newName);
        }
    }

    @Nested
    @DisplayName("changePhoneNumber() - 전화번호 변경")
    class ChangePhoneNumber {

        @Test
        @DisplayName("새 전화번호로 변경할 수 있다")
        void success() {
            // given
            User user = User.create("test@example.com", "password", "홍길동", "010-1234-5678", UserRole.CUSTOMER);
            String newPhoneNumber = "010-9999-9999";

            // when
            user.changePhoneNumber(newPhoneNumber);

            // then
            assertThat(user.getPhoneNumber()).isEqualTo(newPhoneNumber);
        }
    }

    @Nested
    @DisplayName("상태 전환 - activate/deactivate/suspend")
    class StatusTransition {

        @Test
        @DisplayName("INACTIVE 상태에서 activate하면 ACTIVE가 된다")
        void activate_success() {
            // given
            User user = User.create("test@example.com", "password", "홍길동", null, UserRole.CUSTOMER);
            user.deactivate();

            // when
            user.activate();

            // then
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.isActive()).isTrue();
        }

        @Test
        @DisplayName("SUSPENDED 상태에서 activate하면 예외가 발생한다")
        void activate_failWhenSuspended() {
            // given
            User user = User.create("test@example.com", "password", "홍길동", null, UserRole.CUSTOMER);
            user.suspend();

            // when & then
            assertThatThrownBy(() -> user.activate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("정지된 계정은 활성화할 수 없습니다. 관리자에게 문의하세요.");
        }

        @Test
        @DisplayName("deactivate하면 INACTIVE 상태가 된다")
        void deactivate_success() {
            // given
            User user = User.create("test@example.com", "password", "홍길동", null, UserRole.CUSTOMER);

            // when
            user.deactivate();

            // then
            assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
            assertThat(user.isActive()).isFalse();
        }

        @Test
        @DisplayName("suspend하면 SUSPENDED 상태가 된다")
        void suspend_success() {
            // given
            User user = User.create("test@example.com", "password", "홍길동", null, UserRole.CUSTOMER);

            // when
            user.suspend();

            // then
            assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
            assertThat(user.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("권한 확인 - isAdmin/isSeller")
    class RoleCheck {

        @Test
        @DisplayName("ADMIN 사용자는 isAdmin()이 true다")
        void isAdmin_returnsTrue() {
            // given
            User admin = User.create("admin@example.com", "password", "관리자", null, UserRole.ADMIN);

            // then
            assertThat(admin.isAdmin()).isTrue();
        }

        @Test
        @DisplayName("CUSTOMER 사용자는 isAdmin()이 false다")
        void isAdmin_returnsFalse() {
            // given
            User customer = User.create("customer@example.com", "password", "고객", null, UserRole.CUSTOMER);

            // then
            assertThat(customer.isAdmin()).isFalse();
        }

        @Test
        @DisplayName("SELLER 사용자는 isSeller()가 true다")
        void isSeller_returnsTrue_forSeller() {
            // given
            User seller = User.create("seller@example.com", "password", "판매자", null, UserRole.SELLER);

            // then
            assertThat(seller.isSeller()).isTrue();
        }

        @Test
        @DisplayName("ADMIN 사용자도 isSeller()가 true다")
        void isSeller_returnsTrue_forAdmin() {
            // given
            User admin = User.create("admin@example.com", "password", "관리자", null, UserRole.ADMIN);

            // then
            assertThat(admin.isSeller()).isTrue();
        }

        @Test
        @DisplayName("CUSTOMER 사용자는 isSeller()가 false다")
        void isSeller_returnsFalse() {
            // given
            User customer = User.create("customer@example.com", "password", "고객", null, UserRole.CUSTOMER);

            // then
            assertThat(customer.isSeller()).isFalse();
        }
    }

    @Nested
    @DisplayName("reconstitute() - 기존 엔티티 재구성")
    class Reconstitute {

        @Test
        @DisplayName("모든 필드를 포함한 사용자를 재구성할 수 있다")
        void success() {
            // given
            Long id = 1L;
            String email = "test@example.com";
            String password = "password";
            String name = "홍길동";
            String phoneNumber = "010-1234-5678";
            UserRole role = UserRole.CUSTOMER;
            UserStatus status = UserStatus.ACTIVE;

            // when
            User user = User.reconstitute(
                    id, email, password, name, phoneNumber,
                    role, status,
                    java.time.LocalDateTime.now(),
                    java.time.LocalDateTime.now()
            );

            // then
            assertThat(user.getId()).isEqualTo(id);
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getName()).isEqualTo(name);
            assertThat(user.getPhoneNumber()).isEqualTo(phoneNumber);
            assertThat(user.getRole()).isEqualTo(role);
            assertThat(user.getStatus()).isEqualTo(status);
        }
    }
}
