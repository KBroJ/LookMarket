package com.lookmarket.api.user;

import com.lookmarket.domain.user.User;
import com.lookmarket.domain.user.UserRole;
import com.lookmarket.domain.user.UserStatus;

import java.time.LocalDateTime;

/**
 * 사용자 응답 DTO
 *
 * Domain 객체를 직접 노출하지 않고 DTO로 변환
 */
public record UserResponse(
        Long id,
        String email,
        String name,
        String phoneNumber,
        UserRole role,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Domain 객체 → DTO 변환
     */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
