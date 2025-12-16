package com.lookmarket.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * User 도메인 엔티티 단위 테스트
 *
 * 비즈니스 로직 검증 (순수 Java, 프레임워크 의존 없음)
 */
class UserTest {

    // ===== User.create() 테스트 =====

    @Test
    @DisplayName("신규 사용자 생성 성공")
    void createUser_Success() {
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
    @DisplayName("신규 사용자 생성 - phoneNumber null 허용")
    void createUser_WithoutPhoneNumber() {
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
    @DisplayName("신규 사용자 생성 - role null이면 기본값 CUSTOMER")
    void createUser_DefaultRole() {
        // when
        User user = User.create("test@example.com", "password", "홍길동", null, null);

        // then
        assertThat(user.getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("신규 사용자 생성 실패 - 이메일이 null")
    void createUser_FailWithNullEmail() {
        // when & then
        assertThatThrownBy(() ->
                User.create(null, "password", "홍길동", null, UserRole.CUSTOMER)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일은 필수 입력 항목입니다.");
    }

    @Test
    @DisplayName("신규 사용자 생성 실패 - 이메일이 빈 문자열")
    void createUser_FailWithEmptyEmail() {
        // when & then
        assertThatThrownBy(() ->
                User.create("", "password", "홍길동", null, UserRole.CUSTOMER)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일은 필수 입력 항목입니다.");
    }

    @Test
    @DisplayName("신규 사용자 생성 실패 - 잘못된 이메일 형식")
    void createUser_FailWithInvalidEmailFormat() {
        // when & then
        assertThatThrownBy(() ->
                User.create("invalid-email", "password", "홍길동", null, UserRole.CUSTOMER)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("올바른 이메일 형식이 아닙니다.");
    }

    @Test
    @DisplayName("신규 사용자 생성 실패 - 비밀번호가 null")
    void createUser_FailWithNullPassword() {
        // when & then
        assertThatThrownBy(() ->
                User.create("test@example.com", null, "홍길동", null, UserRole.CUSTOMER)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호는 필수 입력 항목입니다.");
    }

    @Test
    @DisplayName("신규 사용자 생성 실패 - 이름이 null")
    void createUser_FailWithNullName() {
        // when & then
        assertThatThrownBy(() ->
                User.create("test@example.com", "password", null, null, UserRole.CUSTOMER)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이름은 필수 입력 항목입니다.");
    }

    // ===== changeEmail() 테스트 =====

    @Test
    @DisplayName("이메일 변경 성공")
    void changeEmail_Success() {
        // given
        User user = User.create("old@example.com", "password", "홍길동", null, UserRole.CUSTOMER);
        String newEmail = "new@example.com";

        // when
        user.changeEmail(newEmail);

        // then
        assertThat(user.getEmail()).isEqualTo(newEmail);
    }

    @Test
    @DisplayName("이메일 변경 실패 - 잘못된 형식")
    void changeEmail_FailWithInvalidFormat() {
        // given
        User user = User.create("test@example.com", "password", "홍길동", null, UserRole.CUSTOMER);

        // when & then
        assertThatThrownBy(() -> user.changeEmail("invalid-email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("올바른 이메일 형식이 아닙니다.");
    }

    @Test
    @DisplayName("이메일 변경 - 동일한 이메일은 변경 안 함")
    void changeEmail_SameEmailNoChange() {
        // given
        User user = User.create("test@example.com", "password", "홍길동", null, UserRole.CUSTOMER);
        String originalEmail = user.getEmail();

        // when
        user.changeEmail("test@example.com");

        // then
        assertThat(user.getEmail()).isEqualTo(originalEmail);
    }

    // ===== changePassword() 테스트 =====

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePassword_Success() {
        // given
        User user = User.create("test@example.com", "oldPassword", "홍길동", null, UserRole.CUSTOMER);
        String newPassword = "newEncodedPassword";

        // when
        user.changePassword(newPassword);

        // then
        assertThat(user.getPassword()).isEqualTo(newPassword);
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - null 비밀번호")
    void changePassword_FailWithNull() {
        // given
        User user = User.create("test@example.com", "oldPassword", "홍길동", null, UserRole.CUSTOMER);

        // when & then
        assertThatThrownBy(() -> user.changePassword(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호는 필수 입력 항목입니다.");
    }

    // ===== changeName() 테스트 =====

    @Test
    @DisplayName("이름 변경 성공")
    void changeName_Success() {
        // given
        User user = User.create("test@example.com", "password", "홍길동", null, UserRole.CUSTOMER);
        String newName = "김철수";

        // when
        user.changeName(newName);

        // then
        assertThat(user.getName()).isEqualTo(newName);
    }

    // ===== changePhoneNumber() 테스트 =====

    @Test
    @DisplayName("전화번호 변경 성공")
    void changePhoneNumber_Success() {
        // given
        User user = User.create("test@example.com", "password", "홍길동", "010-1234-5678", UserRole.CUSTOMER);
        String newPhoneNumber = "010-9999-9999";

        // when
        user.changePhoneNumber(newPhoneNumber);

        // then
        assertThat(user.getPhoneNumber()).isEqualTo(newPhoneNumber);
    }

    // ===== 상태 전환 테스트 =====

    @Test
    @DisplayName("계정 활성화 성공")
    void activate_Success() {
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
    @DisplayName("계정 활성화 실패 - SUSPENDED 상태에서는 활성화 불가")
    void activate_FailWhenSuspended() {
        // given
        User user = User.create("test@example.com", "password", "홍길동", null, UserRole.CUSTOMER);
        user.suspend();

        // when & then
        assertThatThrownBy(() -> user.activate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("정지된 계정은 활성화할 수 없습니다. 관리자에게 문의하세요.");
    }

    @Test
    @DisplayName("계정 비활성화 성공")
    void deactivate_Success() {
        // given
        User user = User.create("test@example.com", "password", "홍길동", null, UserRole.CUSTOMER);

        // when
        user.deactivate();

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(user.isActive()).isFalse();
    }

    @Test
    @DisplayName("계정 정지 성공")
    void suspend_Success() {
        // given
        User user = User.create("test@example.com", "password", "홍길동", null, UserRole.CUSTOMER);

        // when
        user.suspend();

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(user.isActive()).isFalse();
    }

    // ===== 권한 확인 테스트 =====

    @Test
    @DisplayName("관리자 권한 확인")
    void isAdmin_ReturnsTrueForAdmin() {
        // given
        User admin = User.create("admin@example.com", "password", "관리자", null, UserRole.ADMIN);

        // then
        assertThat(admin.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("일반 사용자는 관리자 권한 없음")
    void isAdmin_ReturnsFalseForCustomer() {
        // given
        User customer = User.create("customer@example.com", "password", "고객", null, UserRole.CUSTOMER);

        // then
        assertThat(customer.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("판매자 권한 확인 - SELLER")
    void isSeller_ReturnsTrueForSeller() {
        // given
        User seller = User.create("seller@example.com", "password", "판매자", null, UserRole.SELLER);

        // then
        assertThat(seller.isSeller()).isTrue();
    }

    @Test
    @DisplayName("판매자 권한 확인 - ADMIN도 판매자 권한 가짐")
    void isSeller_ReturnsTrueForAdmin() {
        // given
        User admin = User.create("admin@example.com", "password", "관리자", null, UserRole.ADMIN);

        // then
        assertThat(admin.isSeller()).isTrue();
    }

    @Test
    @DisplayName("일반 고객은 판매자 권한 없음")
    void isSeller_ReturnsFalseForCustomer() {
        // given
        User customer = User.create("customer@example.com", "password", "고객", null, UserRole.CUSTOMER);

        // then
        assertThat(customer.isSeller()).isFalse();
    }

    // ===== reconstitute() 테스트 =====

    @Test
    @DisplayName("기존 사용자 재구성 성공")
    void reconstitute_Success() {
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
