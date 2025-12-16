# JWT 인증 완벽 가이드 (초보자용)

> LookMarket 프로젝트의 JWT 인증 구현을 처음부터 쉽게 설명하는 문서입니다.

## 목차
1. [JWT란 무엇인가?](#1-jwt란-무엇인가)
2. [왜 JWT를 사용하는가?](#2-왜-jwt를-사용하는가)
3. [Access Token vs Refresh Token](#3-access-token-vs-refresh-token)
4. [전체 인증 흐름](#4-전체-인증-흐름)
5. [코드 상세 설명](#5-코드-상세-설명)
6. [핵심 용어 정리](#6-핵심-용어-정리)
7. [실제 사용 예시](#7-실제-사용-예시)

---

## 1. JWT란 무엇인가?

### 정의
**JWT (JSON Web Token)** = 사용자 정보를 담은 암호화된 "통행증"

```
┌─────────────────────────────────────────────────────────────┐
│  JWT = 출입증 같은 것                                         │
├─────────────────────────────────────────────────────────────┤
│  - 회사 출입증에 이름, 부서, 권한이 적혀있는 것처럼              │
│  - JWT에는 userId, email, role 등이 담겨있음                  │
│  - 위조 방지를 위해 "서명(signature)"이 포함됨                 │
└─────────────────────────────────────────────────────────────┘
```

### JWT 구조 (3부분으로 구성)
```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJ0ZXN0QGV4YW1wbGUuY29tIiwicm9sZSI6IlVTRVIifQ.abc123...
└──────── Header ────────┘.└────────────────── Payload ──────────────────┘.└─ Signature ─┘
```

| 부분 | 설명 | 예시 |
|------|------|------|
| **Header** | 어떤 알고리즘으로 암호화했는지 | `{"alg": "HS256"}` |
| **Payload** | 실제 데이터 (userId, email, role, 만료시간 등) | `{"sub": "1", "email": "..."}` |
| **Signature** | 위조 방지용 서명 (비밀키로 생성) | 서버만 검증 가능 |

---

## 2. 왜 JWT를 사용하는가?

### 기존 방식 (세션) vs JWT

```
┌─────────────── 세션 방식 ───────────────┐    ┌─────────────── JWT 방식 ───────────────┐
│                                         │    │                                         │
│  [사용자] → 로그인 → [서버]              │    │  [사용자] → 로그인 → [서버]              │
│             ↓                           │    │             ↓                           │
│       세션ID 발급 (JSESSIONID)          │    │       JWT 토큰 발급                      │
│             ↓                           │    │             ↓                           │
│       서버에 세션 저장 (메모리/DB)        │    │       서버에 저장 안함!                   │
│             ↓                           │    │             ↓                           │
│  [사용자] → 요청 시 세션ID 첨부          │    │  [사용자] → 요청 시 JWT 첨부             │
│             ↓                           │    │             ↓                           │
│       서버가 세션 조회해서 확인          │    │       서버가 서명만 검증하면 끝          │
│                                         │    │                                         │
│  단점: 서버 확장 시 세션 공유 문제       │    │  장점: 서버 확장해도 문제 없음           │
└─────────────────────────────────────────┘    └─────────────────────────────────────────┘
```

### 핵심 차이점

| 항목 | 세션 방식 | JWT 방식 |
|------|----------|----------|
| 상태 저장 | 서버에 저장 (Stateful) | 저장 안함 (Stateless) |
| 서버 확장 | 세션 공유 필요 (Redis 등) | 그냥 확장 가능 |
| 검증 방식 | DB/메모리 조회 | 서명 검증만 |
| 로그아웃 | 세션 삭제로 즉시 무효화 | 토큰 만료까지 유효 |

**핵심**: JWT는 **Stateless** (서버가 상태를 저장하지 않음)

---

## 3. Access Token vs Refresh Token

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│  Access Token (출입증)              Refresh Token (출입증 재발급권)   │
│  ─────────────────────              ─────────────────────────────   │
│  • 실제 API 호출에 사용              • Access Token 만료 시 사용      │
│  • 짧은 수명 (1시간)                 • 긴 수명 (7일)                  │
│  • 탈취되면 피해 기간 짧음           • 탈취되면 피해 클 수 있음        │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 왜 두 개를 사용하는가?

| 시나리오 | 문제점 |
|----------|--------|
| Access Token만 사용 (긴 수명) | 탈취 시 오래 악용됨 |
| Access Token만 사용 (짧은 수명) | 매번 로그인해야 함 (불편) |
| **둘 다 사용** | 편의성 + 보안 모두 확보! |

### LookMarket 설정

```yaml
# application.yml
jwt:
  access-token-expiration: 3600000    # 1시간 (밀리초)
  refresh-token-expiration: 604800000 # 7일 (밀리초)
```

---

## 4. 전체 인증 흐름

### 4.1 로그인 흐름

```
[사용자/프론트엔드]                    [서버]
       │                                │
       │  1. POST /api/v1/auth/login    │
       │  { email, password }           │
       │ ──────────────────────────────>│
       │                                │
       │                    2. AuthController.login()
       │                       - AuthService.authenticate() 호출
       │                       - DB에서 사용자 조회
       │                       - 비밀번호 검증 (BCrypt)
       │                                │
       │                    3. JwtTokenProvider
       │                       - createAccessToken()
       │                       - createRefreshToken()
       │                                │
       │  4. 토큰 응답                   │
       │  { accessToken, refreshToken } │
       │ <──────────────────────────────│
       │                                │
       │  5. 토큰을 localStorage 등에 저장
       │                                │
```

### 4.2 API 요청 흐름 (인증된 요청)

```
[사용자/프론트엔드]                    [서버]
       │                                │
       │  1. GET /api/v1/users/me       │
       │  Header: Authorization:        │
       │          Bearer eyJhbGci...    │
       │ ──────────────────────────────>│
       │                                │
       │                    2. JwtAuthenticationFilter
       │                       - Authorization 헤더에서 토큰 추출
       │                       - "Bearer " 제거
       │                                │
       │                    3. JwtTokenProvider.validateToken()
       │                       - 서명 검증
       │                       - 만료 확인
       │                                │
       │                    4. SecurityContext에 인증정보 저장
       │                       - 이제 "로그인된 사용자"로 인식
       │                                │
       │                    5. Controller 실행
       │                       - @AuthenticationPrincipal로 사용자 정보 접근
       │                                │
       │  6. 응답                        │
       │  { id: 1, email: "..." }       │
       │ <──────────────────────────────│
```

### 4.3 토큰 갱신 흐름

```
[사용자/프론트엔드]                    [서버]
       │                                │
       │  Access Token 만료됨! (1시간 후)
       │                                │
       │  1. POST /api/v1/auth/refresh  │
       │  { refreshToken: "eyJ..." }    │
       │ ──────────────────────────────>│
       │                                │
       │                    2. Refresh Token 검증
       │                    3. 사용자 상태 확인 (탈퇴/정지 여부)
       │                    4. 새 Access Token + Refresh Token 발급
       │                                │
       │  5. 새 토큰 응답                │
       │  { accessToken, refreshToken } │
       │ <──────────────────────────────│
```

---

## 5. 코드 상세 설명

### 5.1 JwtTokenProvider - 토큰 생성/검증 담당

**위치**: `lookmarket-api/src/main/java/com/lookmarket/api/security/JwtTokenProvider.java`

```java
@Component  // Spring이 자동으로 관리하는 Bean으로 등록
public class JwtTokenProvider {

    private final SecretKey secretKey;  // 서명에 사용할 비밀키

    // application.yml에서 설정값 주입
    public JwtTokenProvider(
        @Value("${jwt.secret}") String secret,  // 비밀키 문자열
        @Value("${jwt.access-token-expiration}") long accessTokenExpiration  // 만료시간
    ) {
        // 비밀키를 SecretKey 객체로 변환 (HMAC-SHA 알고리즘용)
        this.secretKey = Keys.hmacShaKeyFor(...);
    }
}
```

**@Value 어노테이션**: application.yml의 값을 코드로 가져옴
```yaml
jwt:
  secret: lookmarket-secret-key...  # → @Value("${jwt.secret}")로 주입
  access-token-expiration: 3600000  # → @Value("${jwt.access-token-expiration}")로 주입
```

#### 토큰 생성 메서드

```java
public String createAccessToken(Long userId, String email, String role) {
    Date now = new Date();                              // 현재 시간
    Date expiry = new Date(now.getTime() + 3600000);    // 1시간 후 만료

    return Jwts.builder()
        .subject(String.valueOf(userId))  // sub: "1" (사용자 ID)
        .claim("email", email)            // email: "test@example.com"
        .claim("role", role)              // role: "USER" 또는 "ADMIN"
        .claim("type", "access")          // type: "access" (토큰 종류 구분)
        .issuedAt(now)                    // iat: 발급 시간
        .expiration(expiry)               // exp: 만료 시간
        .signWith(secretKey)              // 비밀키로 서명 (위조 방지)
        .compact();                       // 최종 JWT 문자열 생성
}
```

**생성된 JWT의 Payload 예시** (디코딩 시):
```json
{
  "sub": "1",
  "email": "test@example.com",
  "role": "USER",
  "type": "access",
  "iat": 1702713600,
  "exp": 1702717200
}
```

#### 토큰 검증 메서드

```java
public boolean validateToken(String token) {
    try {
        Jwts.parser()
            .verifyWith(secretKey)        // 우리 비밀키로 서명 검증
            .build()
            .parseSignedClaims(token);    // 토큰 파싱 (실패하면 예외 발생)
        return true;                       // 여기까지 오면 유효한 토큰
    } catch (ExpiredJwtException e) {
        log.warn("만료된 JWT 토큰입니다");     // 시간 지남
    } catch (MalformedJwtException e) {
        log.warn("잘못된 형식의 JWT 토큰입니다"); // 토큰 형식 오류
    } catch (SecurityException e) {
        log.warn("JWT 서명 검증에 실패했습니다"); // 위조된 토큰!
    }
    return false;  // 검증 실패
}
```

---

### 5.2 JwtAuthenticationFilter - 모든 요청을 가로채서 토큰 확인

**위치**: `lookmarket-api/src/main/java/com/lookmarket/api/security/JwtAuthenticationFilter.java`

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // OncePerRequestFilter: 요청당 딱 한 번만 실행되는 필터
}
```

#### 핵심 메서드: doFilterInternal

```java
@Override
protected void doFilterInternal(
    HttpServletRequest request,    // 들어온 HTTP 요청
    HttpServletResponse response,  // 나갈 HTTP 응답
    FilterChain filterChain        // 다음 필터로 넘기는 역할
) {
    // 1. 요청 헤더에서 토큰 추출
    String token = resolveToken(request);

    // 2. 토큰이 있고 유효하면
    if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {

        // 3. Access Token인지 확인 (Refresh Token으로 API 호출 방지)
        String tokenType = jwtTokenProvider.getTokenType(token);
        if ("access".equals(tokenType)) {

            // 4. 토큰에서 Authentication 객체 생성
            Authentication authentication = jwtTokenProvider.getAuthentication(token);

            // 5. SecurityContext에 저장 → "이 사용자는 인증됨!"
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }

    // 6. 다음 필터 또는 Controller로 요청 전달
    filterChain.doFilter(request, response);
}
```

#### 토큰 추출 메서드

```java
private String resolveToken(HttpServletRequest request) {
    // HTTP 헤더에서 "Authorization" 값 가져오기
    // 예: "Bearer eyJhbGciOiJIUzI1NiJ9..."
    String bearerToken = request.getHeader("Authorization");

    // "Bearer " 접두사 제거하고 순수 토큰만 반환
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
        return bearerToken.substring(7);  // "Bearer " = 7글자
    }
    return null;
}
```

---

### 5.3 SecurityConfig - Spring Security 설정

**위치**: `lookmarket-api/src/main/java/com/lookmarket/api/config/SecurityConfig.java`

```java
@Configuration        // 설정 클래스임을 명시
@EnableWebSecurity    // Spring Security 활성화
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 - JWT 사용 시 불필요
            // (CSRF는 쿠키 기반 인증에서 필요, JWT는 헤더 기반)
            .csrf(AbstractHttpConfigurer::disable)

            // 세션 사용 안함 - JWT는 Stateless
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // URL별 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                // 로그인 API는 누구나 접근 가능 (토큰 없이)
                .requestMatchers("/api/v1/auth/**").permitAll()

                // Swagger 문서도 공개
                .requestMatchers("/swagger-ui/**").permitAll()

                // 관리자 API는 ADMIN 역할만
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // 나머지는 로그인 필요
                .anyRequest().authenticated()
            )

            // JWT 필터를 기존 인증 필터 앞에 추가
            .addFilterBefore(jwtAuthenticationFilter,
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

#### 필터 체인 순서

```
HTTP 요청 들어옴
       ↓
┌─────────────────────────────────────┐
│  JwtAuthenticationFilter (우리 필터) │  ← 토큰 검증
└─────────────────────────────────────┘
       ↓
┌─────────────────────────────────────┐
│  UsernamePasswordAuthenticationFilter│  ← 기존 Spring 필터 (사용 안함)
└─────────────────────────────────────┘
       ↓
┌─────────────────────────────────────┐
│  AuthorizationFilter                 │  ← URL별 권한 체크
└─────────────────────────────────────┘
       ↓
Controller 도착
```

---

### 5.4 AuthController - 로그인/토큰갱신 API

**위치**: `lookmarket-api/src/main/java/com/lookmarket/api/auth/AuthController.java`

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {

        // 1. 사용자 인증 (이메일/비밀번호 확인)
        User user = authService.authenticate(request.email(), request.password());

        // 2. Access Token 생성
        String accessToken = jwtTokenProvider.createAccessToken(
            user.getId(),
            user.getEmail(),
            user.getRole().name()  // UserRole.USER → "USER"
        );

        // 3. Refresh Token 생성
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 4. 응답
        return ResponseEntity.ok(TokenResponse.of(accessToken, refreshToken, 3600000));
    }
}
```

---

### 5.5 AuthService - 비즈니스 로직 (Application Layer)

**위치**: `lookmarket-application/src/main/java/com/lookmarket/application/auth/AuthService.java`

```java
@Service
public class AuthService {

    public User authenticate(String email, String rawPassword) {
        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AuthenticationException("이메일 또는 비밀번호가 올바르지 않습니다."));
            // ↑ 보안: "이메일이 없다"고 알려주면 안됨 (이메일 존재 여부 노출)

        // 2. 계정 상태 확인
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AuthenticationException("비활성화된 계정입니다.");
        }

        // 3. 비밀번호 검증 (BCrypt)
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            // rawPassword: "mypassword123" (사용자가 입력한 것)
            // user.getPassword(): "$2a$10$..." (DB에 저장된 해시값)
            // matches()가 내부적으로 해시 비교
            throw new AuthenticationException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return user;  // 인증 성공!
    }
}
```

---

## 6. 핵심 용어 정리

| 용어 | 설명 |
|------|------|
| **JWT** | JSON Web Token. 사용자 정보를 담은 암호화된 문자열 |
| **Access Token** | API 호출에 사용하는 토큰 (짧은 수명) |
| **Refresh Token** | Access Token 재발급에 사용하는 토큰 (긴 수명) |
| **Stateless** | 서버가 사용자 상태를 저장하지 않는 방식 |
| **Claims** | JWT에 담긴 데이터 (userId, email, role 등) |
| **SecretKey** | 토큰 서명에 사용하는 비밀키 (서버만 알고 있음) |
| **SecurityContext** | Spring Security가 인증된 사용자 정보를 저장하는 곳 |
| **Filter** | 요청이 Controller에 도달하기 전에 가로채서 처리하는 것 |
| **BCrypt** | 비밀번호 해시 알고리즘 (단방향 암호화) |
| **Bearer** | HTTP Authorization 헤더의 인증 스킴 (토큰 앞에 붙이는 접두사) |

---

## 7. 실제 사용 예시

### 프론트엔드 (JavaScript)

```javascript
// ========== 로그인 ==========
const loginResponse = await fetch('/api/v1/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'test@example.com',
    password: 'mypassword123'
  })
});

const { accessToken, refreshToken } = await loginResponse.json();

// 토큰 저장 (localStorage 또는 더 안전한 저장소)
localStorage.setItem('accessToken', accessToken);
localStorage.setItem('refreshToken', refreshToken);


// ========== 인증이 필요한 API 호출 ==========
const userData = await fetch('/api/v1/users/me', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  }
});

const user = await userData.json();
console.log(user);  // { id: 1, email: "test@example.com", ... }


// ========== Access Token 만료 시 갱신 ==========
const refreshResponse = await fetch('/api/v1/auth/refresh', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    refreshToken: localStorage.getItem('refreshToken')
  })
});

const newTokens = await refreshResponse.json();
localStorage.setItem('accessToken', newTokens.accessToken);
localStorage.setItem('refreshToken', newTokens.refreshToken);
```

### cURL (터미널에서 테스트)

```bash
# 로그인
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"mypassword123"}'

# 인증된 API 호출
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."

# 토큰 갱신
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"eyJhbGciOiJIUzI1NiJ9..."}'
```

---

## 8. 파일 구조 요약

```
lookmarket-api/src/main/java/com/lookmarket/api/
├── auth/
│   ├── AuthController.java      # 로그인/토큰갱신 API
│   ├── LoginRequest.java        # 로그인 요청 DTO
│   ├── TokenResponse.java       # 토큰 응답 DTO
│   └── TokenRefreshRequest.java # 토큰 갱신 요청 DTO
├── config/
│   ├── SecurityConfig.java      # Spring Security 설정
│   └── GlobalExceptionHandler.java  # 전역 예외 처리
└── security/
    ├── JwtTokenProvider.java    # JWT 생성/검증
    ├── JwtUserDetails.java      # 사용자 정보 래퍼
    └── JwtAuthenticationFilter.java  # 토큰 검증 필터

lookmarket-application/src/main/java/com/lookmarket/application/
└── auth/
    ├── AuthService.java         # 인증 비즈니스 로직
    └── AuthenticationException.java  # 인증 예외
```

---

## 9. 보안 고려사항

### 현재 구현된 보안 기능

| 항목 | 설명 |
|------|------|
| 비밀번호 해싱 | BCrypt (단방향 암호화) |
| 토큰 서명 | HMAC-SHA 알고리즘 |
| 토큰 분리 | Access Token (짧은 수명) / Refresh Token (긴 수명) |
| 토큰 타입 검증 | Refresh Token으로 API 호출 차단 |
| 계정 상태 확인 | 비활성/정지 계정 로그인 차단 |
| 일반적인 에러 메시지 | "이메일 또는 비밀번호가 올바르지 않습니다" (정보 노출 방지) |

### 추후 보완 가능한 사항

| 항목 | 설명 |
|------|------|
| Refresh Token Redis 저장 | 로그아웃 시 즉시 무효화 가능 |
| Token Blacklist | 탈취된 토큰 즉시 차단 |
| Rate Limiting | 로그인 시도 횟수 제한 |
| IP 검증 | 토큰 발급 IP와 사용 IP 비교 |

---

## 참고 자료

- [JWT 공식 사이트](https://jwt.io/)
- [Spring Security 공식 문서](https://docs.spring.io/spring-security/reference/)
- [jjwt 라이브러리 GitHub](https://github.com/jwtk/jjwt)
