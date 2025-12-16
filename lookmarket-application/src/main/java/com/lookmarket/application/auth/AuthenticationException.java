package com.lookmarket.application.auth;

/**
 * 인증 실패 시 발생하는 예외
 *
 * - 잘못된 이메일/비밀번호
 * - 비활성 계정 (탈퇴, 정지)
 * - 유효하지 않은 토큰
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
