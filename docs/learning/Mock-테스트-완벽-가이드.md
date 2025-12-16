# Mock 완벽 가이드 - 테스트 코드의 핵심 개념

> 이 문서는 Spring Boot 테스트에서 자주 사용되는 Mock 관련 개념들을 정리한 학습 자료입니다.

## 목차
1. [Mock이란 무엇인가?](#mock이란-무엇인가)
2. [Mock 관련 어노테이션 정리](#mock-관련-어노테이션-정리)
3. [@Mock과 @InjectMocks](#1-mock과-injectmocks-mockito---단위-테스트)
4. [@MockBean](#2-mockbean-spring-boot-test---통합-테스트)
5. [MockMvc](#3-mockmvc-http-요청-시뮬레이션)
6. [테스트 종류별 사용 패턴](#4-테스트-종류별-사용-패턴-정리)
7. [자주 쓰는 Mockito 메서드](#5-자주-쓰는-mockito-메서드)
8. [실제 프로젝트 적용 예시](#6-실제-프로젝트-적용-예시)

---

## Mock이란 무엇인가?

**Mock(모의 객체)**은 실제 객체를 흉내 내는 가짜 객체입니다.

### 왜 Mock이 필요한가?

```
실제 상황: UserService를 테스트하고 싶다
    ↓
문제: UserService가 UserRepository(DB 연결)에 의존한다
    ↓
해결: UserRepository를 "가짜"로 만들어서 DB 없이 테스트
```

**비유로 이해하기:**
- 영화 촬영에서 위험한 장면은 **스턴트맨(Mock)**이 대신한다
- 실제 배우(실제 객체) 대신 스턴트맨이 행동하지만, 영화에선 배우처럼 보인다
- 테스트에서도 실제 DB/외부 API 대신 Mock이 그 역할을 한다

### Mock 사용의 장점

| 장점 | 설명 |
|------|------|
| **독립성** | 외부 시스템(DB, API) 없이 테스트 가능 |
| **속도** | 실제 I/O 없이 메모리에서 빠르게 실행 |
| **제어** | 원하는 시나리오(성공, 실패, 예외)를 쉽게 재현 |
| **결정성** | 테스트 결과가 항상 동일 (외부 요인 제거) |

---

## Mock 관련 어노테이션 정리

| 어노테이션 | 제공 라이브러리 | 용도 | 사용 위치 |
|-----------|---------------|------|----------|
| `@Mock` | Mockito | 가짜 객체 생성 | 단위 테스트 |
| `@InjectMocks` | Mockito | Mock을 주입받는 대상 | 단위 테스트 |
| `@MockBean` | Spring Boot Test | Spring Context에 가짜 빈 등록 | 통합 테스트 |
| `@Autowired MockMvc` | Spring Test | HTTP 요청 시뮬레이션 | 컨트롤러 테스트 |

---

## 1. @Mock과 @InjectMocks (Mockito - 단위 테스트)

### 개념

```
@InjectMocks: "이 객체를 테스트할 거야, 의존성은 Mock으로 채워줘"
@Mock: "이건 가짜 객체야, 내가 동작을 지정해줄게"
```

### 실제 예시 - AuthServiceTest

```java
@ExtendWith(MockitoExtension.class)  // Mockito 활성화
class AuthServiceTest {

    @InjectMocks   // 테스트 대상 - Mock들이 자동 주입됨
    private AuthService authService;

    @Mock          // 가짜 UserRepository
    private UserRepository userRepository;

    @Mock          // 가짜 PasswordEncoder
    private PasswordEncoder passwordEncoder;
```

**구조 시각화:**
```
┌─────────────────────────────────────────┐
│           AuthService (실제)             │
│              @InjectMocks               │
├─────────────────────────────────────────┤
│  ┌─────────────────┐ ┌────────────────┐ │
│  │ UserRepository  │ │ PasswordEncoder│ │
│  │     @Mock       │ │     @Mock      │ │
│  │   (가짜 객체)    │ │   (가짜 객체)   │ │
│  └─────────────────┘ └────────────────┘ │
└─────────────────────────────────────────┘
```

### 동작 지정하기 (given-when-then)

```java
@Test
void 로그인_성공() {
    // given - Mock의 동작을 미리 정의
    given(userRepository.findByEmail("test@example.com"))
        .willReturn(Optional.of(testUser));  // "이 메서드 호출하면 testUser 반환해"

    given(passwordEncoder.matches("password123", "encodedPassword"))
        .willReturn(true);  // "비밀번호 일치한다고 해줘"

    // when - 실제 테스트 대상 메서드 호출
    User result = authService.authenticate("test@example.com", "password123");

    // then - 결과 검증
    assertThat(result.getEmail()).isEqualTo("test@example.com");
}
```

**흐름 다이어그램:**
```
테스트 코드                    AuthService                 Mock 객체들
    │                            │                            │
    │ authenticate() 호출         │                            │
    │ ─────────────────────────► │                            │
    │                            │ findByEmail() 호출          │
    │                            │ ─────────────────────────► │
    │                            │                            │
    │                            │ ◄───── testUser 반환 ────── │
    │                            │                            │
    │                            │ matches() 호출              │
    │                            │ ─────────────────────────► │
    │                            │                            │
    │                            │ ◄────── true 반환 ───────── │
    │                            │                            │
    │ ◄── User 반환 ──────────── │                            │
```

### @ExtendWith(MockitoExtension.class)의 역할

```java
@ExtendWith(MockitoExtension.class)  // 이게 없으면 @Mock, @InjectMocks가 동작 안 함!
class MyServiceTest {
    // ...
}
```

이 어노테이션이 하는 일:
1. `@Mock` 붙은 필드에 Mock 객체 생성
2. `@InjectMocks` 붙은 필드에 Mock들을 주입
3. 각 테스트 후 Mock 상태 초기화

---

## 2. @MockBean (Spring Boot Test - 통합 테스트)

### @Mock vs @MockBean 차이

| 구분 | @Mock | @MockBean |
|------|-------|-----------|
| **컨텍스트** | 순수 Mockito | Spring ApplicationContext |
| **빈 등록** | X | O (Spring 빈으로 등록) |
| **사용처** | 단위 테스트 | 통합 테스트, @WebMvcTest |
| **속도** | 빠름 | 상대적으로 느림 |
| **필요 어노테이션** | @ExtendWith(MockitoExtension.class) | @SpringBootTest 또는 @WebMvcTest |

### 실제 예시 - AuthControllerTest

```java
@WebMvcTest(AuthController.class)  // 컨트롤러만 로드하는 슬라이스 테스트
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;  // HTTP 요청 시뮬레이션

    @MockBean  // Spring Context에 가짜 AuthService 등록
    private AuthService authService;
```

**@MockBean이 필요한 이유:**
```
┌─────────────────── Spring Context ───────────────────┐
│                                                      │
│  AuthController (실제)                                │
│       │                                              │
│       └──── AuthService 필요!                         │
│              │                                       │
│              └─► @MockBean으로 가짜 빈 주입           │
│                                                      │
└──────────────────────────────────────────────────────┘
```

### 언제 @MockBean을 사용하나?

1. **@WebMvcTest** - 컨트롤러 테스트 시 Service를 Mock
2. **@SpringBootTest** - 외부 API 클라이언트를 Mock
3. **슬라이스 테스트** - 테스트 범위 외의 빈을 Mock

### 사용 예시

```java
@Test
void 로그인_API_성공() throws Exception {
    // given - MockBean의 동작 정의
    given(authService.authenticate(anyString(), anyString()))
        .willReturn(testUser);

    LoginRequest request = new LoginRequest("test@example.com", "password123");

    // when & then - HTTP 요청 시뮬레이션
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty());
}
```

---

## 3. MockMvc (HTTP 요청 시뮬레이션)

### 개념

**MockMvc**는 실제 서버를 띄우지 않고 HTTP 요청/응답을 테스트합니다.

```
실제 환경:  클라이언트 → 톰캣 서버 → 컨트롤러
테스트:    MockMvc ─────────────────► 컨트롤러
           (서버 없이 직접 호출)
```

### MockMvc 설정 방법

**방법 1: @WebMvcTest (권장 - 컨트롤러만 테스트)**
```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;  // 자동 주입
}
```

**방법 2: @SpringBootTest + @AutoConfigureMockMvc (통합 테스트)**
```java
@SpringBootTest
@AutoConfigureMockMvc
class IntegrationTest {
    @Autowired
    private MockMvc mockMvc;
}
```

### MockMvc 주요 메서드

```java
// 1. HTTP 메서드
mockMvc.perform(get("/api/users"))       // GET 요청
mockMvc.perform(post("/api/users"))      // POST 요청
mockMvc.perform(put("/api/users/1"))     // PUT 요청
mockMvc.perform(delete("/api/users/1"))  // DELETE 요청
mockMvc.perform(patch("/api/users/1"))   // PATCH 요청

// 2. 요청 설정
.contentType(MediaType.APPLICATION_JSON)  // Content-Type 헤더
.content("{\"email\":\"test@test.com\"}")  // 요청 본문
.header("Authorization", "Bearer token")   // 헤더 추가
.param("page", "1")                        // 쿼리 파라미터
.cookie(new Cookie("session", "abc"))      // 쿠키 추가

// 3. 응답 상태 검증
.andExpect(status().isOk())               // 200 OK
.andExpect(status().isCreated())          // 201 Created
.andExpect(status().isBadRequest())       // 400 Bad Request
.andExpect(status().isUnauthorized())     // 401 Unauthorized
.andExpect(status().isForbidden())        // 403 Forbidden
.andExpect(status().isNotFound())         // 404 Not Found

// 4. JSON 응답 검증 (JsonPath)
.andExpect(jsonPath("$.email").value("test@test.com"))
.andExpect(jsonPath("$.id").isNumber())
.andExpect(jsonPath("$.items").isArray())
.andExpect(jsonPath("$.items.length()").value(3))
.andExpect(jsonPath("$.items[0].name").value("첫번째"))
.andExpect(jsonPath("$.data").isEmpty())
.andExpect(jsonPath("$.data").doesNotExist())

// 5. 응답 헤더 검증
.andExpect(header().string("Location", "/api/users/1"))
.andExpect(header().exists("X-Custom-Header"))

// 6. 디버깅
.andDo(print())  // 요청/응답 상세 출력
```

### 전체 예시

```java
@Test
void 회원가입_유효성_검증_실패() throws Exception {
    // given - 잘못된 이메일 형식
    LoginRequest request = new LoginRequest("invalid-email", "password123");

    // when & then
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())  // 요청/응답 출력 (디버깅용)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
}
```

### print() 출력 예시

```
MockHttpServletRequest:
      HTTP Method = POST
      Request URI = /api/v1/auth/login
       Parameters = {}
          Headers = [Content-Type:"application/json"]
             Body = {"email":"invalid-email","password":"password123"}

MockHttpServletResponse:
           Status = 400
    Error message = null
          Headers = [Content-Type:"application/json"]
             Body = {"code":"VALIDATION_FAILED","message":"이메일 형식이 올바르지 않습니다"}
```

---

## 4. 테스트 종류별 사용 패턴 정리

### 단위 테스트 (Unit Test)

**목적:** 하나의 클래스/메서드를 고립시켜 테스트

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;  // 테스트 대상

    @Mock
    private UserRepository userRepository;  // 의존성은 Mock

    @Test
    void 사용자_조회_성공() {
        // given
        given(userRepository.findById(1L))
            .willReturn(Optional.of(user));

        // when
        User result = userService.getUser(1L);

        // then
        assertThat(result.getId()).isEqualTo(1L);
    }
}
```

**특징:**
- Spring 없이 순수 Java로 빠르게 테스트
- 외부 의존성 완전 차단
- 실행 속도: 밀리초 단위

### 슬라이스 테스트 (@WebMvcTest)

**목적:** 컨트롤러 레이어만 테스트

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;  // HTTP 테스트

    @MockBean
    private UserService userService;  // Service는 Mock

    @Test
    void 사용자_조회_API_성공() throws Exception {
        // given
        given(userService.getUser(1L)).willReturn(user);

        // when & then
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }
}
```

**특징:**
- 컨트롤러 + Spring MVC 설정만 로드
- Service 이하는 MockBean으로 대체
- 실행 속도: 초 단위

### 통합 테스트 (@SpringBootTest)

**목적:** 여러 레이어를 함께 테스트

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExternalApiClient externalApi;  // 외부 API만 Mock

    @Test
    void 사용자_생성_통합테스트() throws Exception {
        // 실제 Service, Repository 사용
        // DB는 H2 또는 Testcontainers 사용
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new@test.com\"}"))
            .andExpect(status().isCreated());
    }
}
```

**특징:**
- 전체 Spring Context 로드
- 실제 빈들이 연동되어 동작
- 외부 의존성(API, 메시지큐)만 Mock
- 실행 속도: 수 초 ~ 수십 초

### 테스트 피라미드

```
        /\
       /  \      E2E 테스트 (적게)
      /____\     - 전체 시스템 테스트
     /      \
    /________\   통합 테스트 (중간)
   /          \  - @SpringBootTest
  /____________\
 /              \ 단위 테스트 (많이)
/________________\- @Mock, @InjectMocks
```

---

## 5. 자주 쓰는 Mockito 메서드

### 동작 지정 (Stubbing)

```java
// BDDMockito 스타일 (권장 - given/when/then과 일관성)
import static org.mockito.BDDMockito.given;

given(repository.findById(1L)).willReturn(Optional.of(user));
given(repository.save(any())).willReturn(savedUser);
given(service.process(anyString())).willThrow(new RuntimeException("에러"));

// 전통적인 Mockito 스타일
import static org.mockito.Mockito.when;

when(repository.findById(1L)).thenReturn(Optional.of(user));
when(repository.save(any())).thenThrow(new IllegalArgumentException());
```

### Argument Matchers (인자 매처)

```java
import static org.mockito.ArgumentMatchers.*;

any()              // 아무 값이나 (null 포함)
any(User.class)    // User 타입 아무거나
anyString()        // 아무 문자열 (null 제외)
anyLong()          // 아무 Long
anyInt()           // 아무 int
anyList()          // 아무 List
anyMap()           // 아무 Map
anySet()           // 아무 Set

eq("specific")     // 정확히 "specific"과 일치
eq(123L)           // 정확히 123L과 일치

isNull()           // null인 경우
isNotNull()        // null이 아닌 경우

// 커스텀 조건
argThat(arg -> arg.length() > 5)           // 길이가 5보다 큰 문자열
argThat(user -> user.getAge() >= 18)       // 나이가 18 이상인 User
```

**주의: Matcher 혼용 규칙**
```java
// ❌ 잘못된 사용 - 일부만 Matcher 사용
given(service.process("literal", anyLong()))  // 컴파일 에러!

// ✅ 올바른 사용 - 모두 Matcher 사용
given(service.process(eq("literal"), anyLong()))  // OK
```

### 호출 검증 (Verification)

```java
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

// 메서드가 호출되었는지 검증
verify(repository).save(any(User.class));          // 1번 호출됨 (기본값)
verify(repository, times(1)).save(any());          // 정확히 1번
verify(repository, times(2)).findById(anyLong());  // 정확히 2번
verify(repository, atLeast(1)).save(any());        // 최소 1번
verify(repository, atMost(3)).save(any());         // 최대 3번
verify(repository, never()).delete(any());         // 호출 안 됨

// 호출 순서 검증
InOrder inOrder = inOrder(repository, eventPublisher);
inOrder.verify(repository).save(any());
inOrder.verify(eventPublisher).publish(any());
```

### ArgumentCaptor (인자 캡처)

```java
@Captor
ArgumentCaptor<User> userCaptor;

@Test
void 저장된_사용자_정보_검증() {
    // when
    userService.createUser("test@test.com");

    // then - 실제로 저장된 객체 캡처
    verify(userRepository).save(userCaptor.capture());

    User savedUser = userCaptor.getValue();
    assertThat(savedUser.getEmail()).isEqualTo("test@test.com");
    assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
}
```

---

## 6. 실제 프로젝트 적용 예시

### LookMarket 프로젝트의 AuthControllerTest 분석

```java
@WebMvcTest(AuthController.class)  // ① 컨트롤러만 로드
@Import({
    JwtTokenProvider.class,        // ② 실제 빈 추가 (토큰 생성 필요)
    GlobalExceptionHandler.class,  // ② 예외 처리기
    SecurityConfig.class,          // ② Security 설정
    JwtAuthenticationFilter.class  // ② JWT 필터
})
@TestPropertySource(properties = {  // ③ 테스트용 설정
    "jwt.secret=test-secret-key-for-jwt-token-generation-must-be-long-enough-for-testing",
    "jwt.access-token-expiration=3600000",
    "jwt.refresh-token-expiration=604800000"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;  // ④ HTTP 테스트 도구

    @Autowired
    private JwtTokenProvider jwtTokenProvider;  // ⑤ 실제 빈 (토큰 생성 필요)

    @MockBean
    private AuthService authService;  // ⑥ 가짜 서비스 (DB 의존성 차단)

    @Test
    void 로그인_성공() throws Exception {
        // ⑦ Mock 동작 지정
        given(authService.authenticate(anyString(), anyString()))
            .willReturn(testUser);

        // ⑧ HTTP 요청 시뮬레이션 & 검증
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
}
```

**포인트 설명:**

| 번호 | 설명 |
|------|------|
| ① | `@WebMvcTest`로 AuthController만 로드 (빠른 테스트) |
| ② | `@Import`로 필요한 실제 빈 추가 (Security, JWT 관련) |
| ③ | `@TestPropertySource`로 테스트용 JWT 설정 주입 |
| ④ | `MockMvc`로 실제 HTTP 요청처럼 테스트 |
| ⑤ | `JwtTokenProvider`는 실제 객체 (토큰 생성/검증 필요) |
| ⑥ | `AuthService`는 MockBean (DB 연결 차단) |
| ⑦ | `given().willReturn()`으로 Mock 동작 정의 |
| ⑧ | `mockMvc.perform()`으로 HTTP 요청 후 검증 |

---

## 핵심 요약 체크리스트

### 상황별 선택 가이드

| 상황 | 사용할 것 |
|------|----------|
| Service 단위 테스트 | `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks` |
| Controller 테스트 | `@WebMvcTest` + `@MockBean` + `MockMvc` |
| Repository 테스트 | `@DataJpaTest` (실제 DB 사용) |
| 통합 테스트 | `@SpringBootTest` + `@AutoConfigureMockMvc` |
| 외부 API Mock | `@MockBean` 또는 WireMock |

### 기억할 핵심 포인트

```
@Mock = 순수 Mockito 가짜 객체 (Spring 없이)
@MockBean = Spring Context에 등록되는 가짜 빈
@InjectMocks = Mock들이 주입되는 테스트 대상
MockMvc = 서버 없이 HTTP 요청/응답 테스트

given().willReturn() = Mock 동작 지정 (BDD 스타일)
verify() = Mock 메서드 호출 검증
```

### 테스트 작성 패턴 (Given-When-Then)

```java
@Test
void 테스트_메서드명은_한글도_OK() {
    // given - 테스트 준비 (Mock 동작 정의)
    given(mockObject.method()).willReturn(value);

    // when - 테스트 대상 실행
    Result result = testTarget.doSomething();

    // then - 결과 검증
    assertThat(result).isEqualTo(expected);
    verify(mockObject).method();  // 호출 검증 (선택)
}
```

---

## 참고 자료

- [Mockito 공식 문서](https://site.mockito.org/)
- [Spring Boot Testing 가이드](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [BDDMockito 사용법](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/BDDMockito.html)
- [MockMvc 공식 문서](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#spring-mvc-test-framework)
