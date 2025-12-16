package com.lookmarket.api.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenProvider 단위 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // 테스트용 설정값
    private static final String SECRET = "test-secret-key-for-jwt-token-generation-must-be-long-enough";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000L;  // 1시간
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L;  // 7일

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);
    }

    @Nested
    @DisplayName("Access Token 생성")
    class CreateAccessToken {

        @Test
        @DisplayName("유효한 Access Token을 생성한다")
        void createAccessToken_success() {
            // given
            Long userId = 1L;
            String email = "test@example.com";
            String role = "USER";

            // when
            String token = jwtTokenProvider.createAccessToken(userId, email, role);

            // then
            assertThat(token).isNotNull();
            assertThat(token.split("\\.")).hasSize(3);  // JWT는 3부분으로 구성
        }

        @Test
        @DisplayName("Access Token에 올바른 Claims가 포함된다")
        void createAccessToken_containsCorrectClaims() {
            // given
            Long userId = 1L;
            String email = "test@example.com";
            String role = "USER";

            // when
            String token = jwtTokenProvider.createAccessToken(userId, email, role);
            Claims claims = jwtTokenProvider.getClaims(token);

            // then
            assertThat(claims.getSubject()).isEqualTo(String.valueOf(userId));
            assertThat(claims.get("email", String.class)).isEqualTo(email);
            assertThat(claims.get("role", String.class)).isEqualTo(role);
            assertThat(claims.get("type", String.class)).isEqualTo("access");
        }
    }

    @Nested
    @DisplayName("Refresh Token 생성")
    class CreateRefreshToken {

        @Test
        @DisplayName("유효한 Refresh Token을 생성한다")
        void createRefreshToken_success() {
            // given
            Long userId = 1L;

            // when
            String token = jwtTokenProvider.createRefreshToken(userId);

            // then
            assertThat(token).isNotNull();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("Refresh Token에 올바른 Claims가 포함된다")
        void createRefreshToken_containsCorrectClaims() {
            // given
            Long userId = 1L;

            // when
            String token = jwtTokenProvider.createRefreshToken(userId);
            Claims claims = jwtTokenProvider.getClaims(token);

            // then
            assertThat(claims.getSubject()).isEqualTo(String.valueOf(userId));
            assertThat(claims.get("type", String.class)).isEqualTo("refresh");
            assertThat(claims.get("email")).isNull();  // Refresh Token에는 email 없음
        }
    }

    @Nested
    @DisplayName("토큰 검증")
    class ValidateToken {

        @Test
        @DisplayName("유효한 토큰은 true를 반환한다")
        void validateToken_validToken_returnsTrue() {
            // given
            String token = jwtTokenProvider.createAccessToken(1L, "test@example.com", "USER");

            // when
            boolean isValid = jwtTokenProvider.validateToken(token);

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("잘못된 형식의 토큰은 false를 반환한다")
        void validateToken_malformedToken_returnsFalse() {
            // given
            String malformedToken = "invalid.token.format";

            // when
            boolean isValid = jwtTokenProvider.validateToken(malformedToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("빈 토큰은 false를 반환한다")
        void validateToken_emptyToken_returnsFalse() {
            // given
            String emptyToken = "";

            // when
            boolean isValid = jwtTokenProvider.validateToken(emptyToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("다른 비밀키로 생성된 토큰은 false를 반환한다")
        void validateToken_differentSecretKey_returnsFalse() {
            // given
            JwtTokenProvider otherProvider = new JwtTokenProvider(
                    "different-secret-key-that-is-also-long-enough-for-testing",
                    ACCESS_TOKEN_EXPIRATION,
                    REFRESH_TOKEN_EXPIRATION
            );
            String tokenFromOther = otherProvider.createAccessToken(1L, "test@example.com", "USER");

            // when
            boolean isValid = jwtTokenProvider.validateToken(tokenFromOther);

            // then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("토큰에서 정보 추출")
    class ExtractFromToken {

        @Test
        @DisplayName("토큰에서 userId를 추출한다")
        void getUserId_success() {
            // given
            Long userId = 123L;
            String token = jwtTokenProvider.createAccessToken(userId, "test@example.com", "USER");

            // when
            Long extractedUserId = jwtTokenProvider.getUserId(token);

            // then
            assertThat(extractedUserId).isEqualTo(userId);
        }

        @Test
        @DisplayName("토큰에서 email을 추출한다")
        void getEmail_success() {
            // given
            String email = "test@example.com";
            String token = jwtTokenProvider.createAccessToken(1L, email, "USER");

            // when
            String extractedEmail = jwtTokenProvider.getEmail(token);

            // then
            assertThat(extractedEmail).isEqualTo(email);
        }

        @Test
        @DisplayName("토큰에서 role을 추출한다")
        void getRole_success() {
            // given
            String role = "ADMIN";
            String token = jwtTokenProvider.createAccessToken(1L, "test@example.com", role);

            // when
            String extractedRole = jwtTokenProvider.getRole(token);

            // then
            assertThat(extractedRole).isEqualTo(role);
        }

        @Test
        @DisplayName("Access Token의 타입은 'access'이다")
        void getTokenType_accessToken() {
            // given
            String token = jwtTokenProvider.createAccessToken(1L, "test@example.com", "USER");

            // when
            String tokenType = jwtTokenProvider.getTokenType(token);

            // then
            assertThat(tokenType).isEqualTo("access");
        }

        @Test
        @DisplayName("Refresh Token의 타입은 'refresh'이다")
        void getTokenType_refreshToken() {
            // given
            String token = jwtTokenProvider.createRefreshToken(1L);

            // when
            String tokenType = jwtTokenProvider.getTokenType(token);

            // then
            assertThat(tokenType).isEqualTo("refresh");
        }
    }

    @Nested
    @DisplayName("Authentication 객체 생성")
    class GetAuthentication {

        @Test
        @DisplayName("토큰에서 Authentication 객체를 생성한다")
        void getAuthentication_success() {
            // given
            Long userId = 1L;
            String email = "test@example.com";
            String role = "USER";
            String token = jwtTokenProvider.createAccessToken(userId, email, role);

            // when
            Authentication authentication = jwtTokenProvider.getAuthentication(token);

            // then
            assertThat(authentication).isNotNull();
            assertThat(authentication.isAuthenticated()).isTrue();

            JwtUserDetails userDetails = (JwtUserDetails) authentication.getPrincipal();
            assertThat(userDetails.getUserId()).isEqualTo(userId);
            assertThat(userDetails.getEmail()).isEqualTo(email);
            assertThat(userDetails.getRole()).isEqualTo(role);
        }

        @Test
        @DisplayName("Authentication 객체에 올바른 권한이 설정된다")
        void getAuthentication_hasCorrectAuthority() {
            // given
            String role = "ADMIN";
            String token = jwtTokenProvider.createAccessToken(1L, "test@example.com", role);

            // when
            Authentication authentication = jwtTokenProvider.getAuthentication(token);

            // then
            assertThat(authentication.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_ADMIN");
        }
    }

    @Nested
    @DisplayName("만료 시간 설정")
    class TokenExpiration {

        @Test
        @DisplayName("Access Token 만료 시간을 반환한다")
        void getAccessTokenExpiration() {
            // when
            long expiration = jwtTokenProvider.getAccessTokenExpiration();

            // then
            assertThat(expiration).isEqualTo(ACCESS_TOKEN_EXPIRATION);
        }

        @Test
        @DisplayName("Refresh Token 만료 시간을 반환한다")
        void getRefreshTokenExpiration() {
            // when
            long expiration = jwtTokenProvider.getRefreshTokenExpiration();

            // then
            assertThat(expiration).isEqualTo(REFRESH_TOKEN_EXPIRATION);
        }
    }
}
