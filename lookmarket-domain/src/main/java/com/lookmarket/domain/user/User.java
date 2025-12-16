package com.lookmarket.domain.user;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 사용자 도메인 엔티티 (순수 Java, 프레임워크 의존성 없음)
 *
 * Hexagonal Architecture의 Domain Layer에 위치하여
 * 비즈니스 로직과 불변식을 캡슐화합니다.
 */
public class User {

    private final Long id;
    private String email;
    private String password;
    private String name;
    private String phoneNumber;
    private final UserRole role;
    private UserStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * User 엔티티 생성자 (package-private)
     * 외부에서 직접 생성하지 못하도록 제한
     */
    User (
        Long id, String email, String password, String name, String phoneNumber,
        UserRole role, UserStatus status, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        validateEmail(email);
        validatePassword(password);
        validateName(name);

        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 사용자 생성 (Static Factory Method)
     *
     * @param email 이메일 (필수, 중복 불가)
     * @param password 암호화된 비밀번호
     * @param name 사용자 이름 (필수)
     * @param phoneNumber 전화번호 (선택)
     * @param role 사용자 역할 (기본값: CUSTOMER)
     * @return 생성된 User 엔티티
     */
    public static User create (
        String email, String password, String name,
        String phoneNumber, UserRole role
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new User(
            null,  // ID는 데이터베이스에서 생성
            email,
            password,
            name,
            phoneNumber,
            role != null ? role : UserRole.CUSTOMER,
            UserStatus.ACTIVE,
            now,
            now
        );
    }

    /**
     * 기존 사용자 재구성 (데이터베이스에서 조회 시 사용)
     */
    public static User reconstitute(
        Long id, String email, String password, String name,
        String phoneNumber, UserRole role, UserStatus status,
        LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        return new User(id, email, password, name, phoneNumber, role, status, createdAt, updatedAt);
    }

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 이메일 변경
     *
     * @param newEmail 새 이메일 주소
     */
    public void changeEmail(String newEmail) {

        validateEmail(newEmail);

        if (!this.email.equals(newEmail)) {
            this.email = newEmail;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 이름 변경
     *
     * @param newName 새 이름
     */
    public void changeName(String newName) {

        validateName(newName);

        if (!this.name.equals(newName)) {
            this.name = newName;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 전화번호 변경
     *
     * @param newPhoneNumber 새 전화번호
     */
    public void changePhoneNumber(String newPhoneNumber) {
        this.phoneNumber = newPhoneNumber;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 비밀번호 변경
     *
     * @param newPassword 암호화된 새 비밀번호
     */
    public void changePassword(String newPassword) {
        validatePassword(newPassword);
        this.password = newPassword;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 계정 활성화
     */
    public void activate() {
        if (this.status == UserStatus.SUSPENDED) {
            throw new IllegalStateException("정지된 계정은 활성화할 수 없습니다. 관리자에게 문의하세요.");
        }
        this.status = UserStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 계정 비활성화 (휴면 상태로 전환)
     */
    public void deactivate() {
        this.status = UserStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 계정 정지 (관리자 권한 필요)
     */
    public void suspend() {
        this.status = UserStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 활성 상태 확인
     */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    /**
     * 관리자 권한 확인
     */
    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }

    /**
     * 판매자 권한 확인
     */
    public boolean isSeller() {
        return this.role == UserRole.SELLER || this.role == UserRole.ADMIN;
    }

    // ===== Validation 메서드 =====
    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일은 필수 입력 항목입니다.");
        }
        // 간단한 이메일 형식 검증
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 필수 입력 항목입니다.");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("이름은 필수 입력 항목입니다.");
        }
    }

    // ===== Getters (Behavior-rich Entities 원칙에 따라 필요한 것만 제공) =====

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ===== equals & hashCode (ID 기반) =====

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", role=" + role +
                ", status=" + status +
                '}';
    }
}
