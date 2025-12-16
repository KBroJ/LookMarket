package com.lookmarket.api.user;

import com.lookmarket.application.user.UserService;
import com.lookmarket.domain.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * User API 컨트롤러
 *
 * 책임:
 * - HTTP 요청/응답 처리
 * - DTO ↔ Domain 변환
 * - 입력 검증 (@Valid)
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 회원가입
     *
     * POST /api/v1/users
     */
    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        User user = userService.register(
                request.email(),
                request.password(),
                request.name(),
                request.phoneNumber(),
                request.role()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(UserResponse.from(user));
    }

    /**
     * 사용자 조회 (ID)
     *
     * GET /api/v1/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * 이메일 변경
     *
     * PATCH /api/v1/users/{userId}/email
     */
    @PatchMapping("/{userId}/email")
    public ResponseEntity<UserResponse> changeEmail(
            @PathVariable Long userId,
            @Valid @RequestBody ChangeEmailRequest request
    ) {
        User user = userService.changeEmail(userId, request.newEmail());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * 비밀번호 변경
     *
     * PATCH /api/v1/users/{userId}/password
     */
    @PatchMapping("/{userId}/password")
    public ResponseEntity<UserResponse> changePassword(
            @PathVariable Long userId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        User user = userService.changePassword(
                userId,
                request.currentPassword(),
                request.newPassword()
        );
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * 계정 활성화
     *
     * POST /api/v1/users/{userId}/activate
     */
    @PostMapping("/{userId}/activate")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long userId) {
        User user = userService.activateUser(userId);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * 계정 정지
     *
     * POST /api/v1/users/{userId}/suspend
     */
    @PostMapping("/{userId}/suspend")
    public ResponseEntity<UserResponse> suspendUser(@PathVariable Long userId) {
        User user = userService.suspendUser(userId);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * 계정 비활성화
     *
     * POST /api/v1/users/{userId}/deactivate
     */
    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long userId) {
        User user = userService.deactivateUser(userId);
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
