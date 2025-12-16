package com.lookmarket.api.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 이메일 변경 요청 DTO
 */
public record ChangeEmailRequest(
        @NotBlank(message = "New email is required")
        @Email(message = "Invalid email format")
        String newEmail
) {
}
