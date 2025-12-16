package com.lookmarket.infrastructure.user;

import com.lookmarket.domain.user.User;
import com.lookmarket.domain.user.UserRole;
import com.lookmarket.domain.user.UserStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * User JPA 엔티티 (Infrastructure Layer)
 *
 * Domain의 User 엔티티를 데이터베이스에 매핑하는 JPA 엔티티입니다.
 * Domain 레이어의 독립성을 유지하기 위해 별도로 관리됩니다.
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA 기본 생성자 (protected)
     */
    protected UserEntity() {
    }

    /**
     * 모든 필드를 포함하는 생성자
     */
    private UserEntity (
        Long id, String email, String password, String name,
        String phoneNumber, UserRole role, UserStatus status,
        LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
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
     * Domain User 엔티티에서 JPA Entity로 변환
     *
     * @param user Domain User 엔티티
     * @return UserEntity (JPA)
     */
    public static UserEntity fromDomain(User user) {
        return new UserEntity(
            user.getId(),
            user.getEmail(),
            user.getPassword(),
            user.getName(),
            user.getPhoneNumber(),
            user.getRole(),
            user.getStatus(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    /**
     * JPA Entity를 Domain User 엔티티로 변환
     *
     * @return Domain User 엔티티
     */
    public User toDomain() {
        return User.reconstitute(
            this.id,
            this.email,
            this.password,
            this.name,
            this.phoneNumber,
            this.role,
            this.status,
            this.createdAt,
            this.updatedAt
        );
    }

    /**
     * JPA Lifecycle 콜백: 엔티티 생성 전
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * JPA Lifecycle 콜백: 엔티티 수정 전
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ===== Getters and Setters =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
