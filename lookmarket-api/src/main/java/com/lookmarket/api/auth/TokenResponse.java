package com.lookmarket.api.auth;

/**
 * 토큰 응답 DTO
 *
 * 로그인 성공 또는 토큰 갱신 시 반환됩니다.
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    /**
     * Bearer 토큰 타입으로 응답 생성
     */
    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
