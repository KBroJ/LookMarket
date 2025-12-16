# Hexagonal Architecture 설계 결정 가이드

> **작성일**: 2025-12-16
> **목적**: LookMarket 프로젝트의 Hexagonal Architecture 설계 원칙과 Loopers 프로젝트와의 비교 분석

---

## 📋 목차

1. [Domain에서 Lombok을 사용하지 않는 이유](#1-domain에서-lombok을-사용하지-않는-이유)
2. [Loopers vs LookMarket 아키텍처 비교](#2-loopers-vs-lookmarket-아키텍처-비교)
3. [설계 결정 요약](#3-설계-결정-요약)

---

## 1. Domain에서 Lombok을 사용하지 않는 이유

### 1.1 Setter의 근본적 문제

#### ❌ Lombok @Setter 사용 시 (Anti-Pattern)

```java
@Getter
@Setter  // ← 문제 발생!
public class User {
    private String email;
    private UserStatus status;

    // Lombok이 자동 생성:
    // public void setEmail(String email) { this.email = email; }
    // public void setStatus(UserStatus status) { this.status = status; }
}

// 사용 예시 - 비즈니스 규칙 우회 가능!
User user = new User();
user.setEmail("잘못된이메일");  // ← 이메일 검증 없이 설정 가능 😱
user.setStatus(UserStatus.SUSPENDED);  // ← 권한 체크 없이 정지 가능 😱
```

**발생 가능한 문제:**

1. **비즈니스 규칙 우회**
   - 검증 로직을 건너뛸 수 있음
   - 잘못된 데이터가 도메인에 저장됨
   - 예: 이메일 형식 검증 없이 저장

2. **의도 불명확**
   - `setEmail()`이 단순 값 변경인지 비즈니스 의미가 있는지 알 수 없음
   - 코드 리딩 시 혼란 야기
   - 예: "이메일 변경"인지 "초기 설정"인지 구분 불가

3. **불변성 보장 불가**
   - 언제든 어디서든 상태 변경 가능
   - 멀티스레드 환경에서 예측 불가능한 동작
   - 디버깅 어려움

4. **도메인 로직 분산**
   - 비즈니스 규칙이 Service 레이어에 흩어짐
   - Domain 객체가 "빈약한 도메인 모델(Anemic Domain Model)"이 됨

---

#### ✅ Behavior-rich Entity (올바른 패턴)

```java
@Getter  // Getter는 괜찮음
public class User {
    private String email;
    private UserStatus status;
    private LocalDateTime updatedAt;

    // Setter 대신 비즈니스 메서드
    public void changeEmail(String newEmail) {
        // 1. 비즈니스 규칙 검증
        validateEmail(newEmail);

        // 2. 의미 있는 이름 (이메일 "변경")
        if (!this.email.equals(newEmail)) {
            this.email = newEmail;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void suspend() {
        // 3. 상태 전이 규칙 검증
        if (this.status == UserStatus.SUSPENDED) {
            throw new IllegalStateException("이미 정지된 계정입니다");
        }
        if (!canBeSuspended()) {
            throw new IllegalStateException("정지할 수 없는 상태입니다");
        }

        this.status = UserStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일은 필수입니다");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("이메일 형식이 올바르지 않습니다");
        }
    }

    private boolean canBeSuspended() {
        return this.status == UserStatus.ACTIVE;
    }
}

// 사용 예시 - 안전함!
User user = new User(...);
user.changeEmail("new@example.com");  // ← 검증 통과해야만 변경됨 ✅
user.suspend();  // ← 비즈니스 규칙 검사 후 정지 ✅
```

**장점:**

1. **비즈니스 규칙 강제**
   - 검증 로직을 반드시 거침
   - 잘못된 데이터 차단
   - 도메인 불변식(Invariant) 보장

2. **의도 명확**
   - `changeEmail()`은 "이메일을 변경한다"는 명확한 의미
   - 코드 리딩 시 비즈니스 흐름 이해 용이
   - 도메인 전문가와 대화 가능한 코드 (Ubiquitous Language)

3. **불변성 제어**
   - 필요한 필드만 `final`로 선언 가능
   - 상태 변경 시점 명확
   - 추적 가능 (updatedAt 자동 갱신)

4. **도메인 로직 응집**
   - 비즈니스 규칙이 Domain 객체에 집중
   - Rich Domain Model 구현
   - Service 레이어는 오케스트레이션만 담당

---

### 1.2 Getter는 왜 사용 가능한가?

```java
@Getter  // ← 이건 사용해도 됨
public class User {
    private final Long id;
    private String email;
    private UserRole role;

    // Lombok이 생성:
    // public Long getId() { return id; }
    // public String getEmail() { return email; }
    // public UserRole getRole() { return role; }
}
```

**Getter 사용 가능한 이유:**

1. **읽기 전용**: 상태를 변경하지 않음
2. **안전함**: 비즈니스 규칙을 위반할 수 없음
3. **편의성**: 매번 수동 작성하는 것보다 효율적

**주의사항:**

- **민감한 정보는 Getter 노출 주의**
  - 예: `password` 필드는 Getter 제공하지 않거나 마스킹
- **필요한 필드만 노출**
  - `@Getter` 클래스 레벨 대신 필드 레벨로 선택적 적용 권장

---

### 1.3 LookMarket 프로젝트의 선택

#### Domain Layer

```java
// lookmarket-domain/User.java
public class User {
    private final Long id;
    private String email;
    private String password;

    // Lombok 사용 안 함 (명시적 제어)
    public Long getId() { return id; }
    public String getEmail() { return email; }

    // password는 Getter 제공 안 함 (보안)
    // public String getPassword() { return password; }  // ← 제공 안 함

    // Setter 대신 비즈니스 메서드
    public void changeEmail(String newEmail) {
        validateEmail(newEmail);
        this.email = newEmail;
        this.updatedAt = LocalDateTime.now();
    }

    public void changePassword(String newPassword) {
        validatePassword(newPassword);
        this.password = newPassword;
        this.updatedAt = LocalDateTime.now();
    }
}
```

**선택 이유:**

1. **명시적 제어**: 어떤 필드를 노출할지 명확히 결정
2. **보안**: 민감 정보(password) Getter 제공 안 함
3. **가독성**: Lombok 문서 없이도 코드만으로 이해 가능
4. **포트폴리오**: "의도적 설계"를 보여줌

---

#### Infrastructure Layer

```java
// lookmarket-infrastructure/UserEntity.java
@Entity
@Table(name = "users")
@Getter
@Setter  // ← Infrastructure에서는 사용 가능
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // Getter/Setter 자동 생성 (Lombok)
    // 이유: Infrastructure는 단순 데이터 컨테이너 역할
}
```

**Infrastructure에서 Lombok 사용 가능한 이유:**

1. **단순 데이터 컨테이너**: JPA Entity는 비즈니스 로직 없음
2. **JPA 요구사항**: Getter/Setter 필요
3. **코드 간결성**: 보일러플레이트 감소
4. **역할 분리**: 비즈니스 로직은 Domain에만 존재

---

### 1.4 정리: Lombok 사용 원칙

| Layer | Lombok 사용 | 이유 |
|-------|------------|------|
| **Domain** | ❌ 사용 안 함 | Behavior-rich Entity, 비즈니스 로직 명확화 |
| **Domain (Getter)** | △ 선택적 사용 | 필요한 경우 @Getter만 필드 레벨로 사용 |
| **Infrastructure** | ✅ 사용 가능 | 단순 데이터 컨테이너, JPA Entity |
| **Application** | ✅ 사용 가능 | DTO, 요청/응답 객체 |
| **API** | ✅ 사용 가능 | Controller, DTO |

**핵심 원칙:**
- **Domain = 행위 중심 (Behavior-driven)**
- **Infrastructure = 데이터 중심 (Data-driven)**

---

## 2. Loopers vs LookMarket 아키텍처 비교

### 2.1 전체 구조 비교

| 구분 | Loopers | LookMarket |
|------|---------|------------|
| **Domain Entity** | `@Entity` User (JPA 직접 사용) | 순수 Java User (프레임워크 독립) |
| **Infrastructure** | User 직접 사용 | UserEntity + User 분리 |
| **변환 로직** | 불필요 | fromDomain/toDomain 필요 |
| **BaseEntity** | 상속 사용 (`extends BaseEntity`) | 사용 안 함 |
| **Lombok** | `@Getter`만 사용 | Domain은 사용 안 함 |
| **Validation** | 생성자에서 검증 | 생성자 + 비즈니스 메서드 |
| **복잡도** | ⭐⭐ (낮음) | ⭐⭐⭐⭐ (높음) |
| **순수성** | ⭐⭐⭐ (JPA 의존) | ⭐⭐⭐⭐⭐ (완전 독립) |

---

### 2.2 Layer별 상세 비교

#### Domain Layer

**Loopers**

```java
package com.loopers.domain.users;

@Entity
@Getter
@Table(name = "users")
public class User extends BaseEntity {  // ← JPA 의존

    @NotNull
    @Column(name = "user_id")  // ← JPA 어노테이션
    private String userId;

    @NotNull
    private String gender;

    @NotNull
    private LocalDate birthDate;

    @NotNull
    private String email;

    protected User() {}  // JPA 필수 생성자

    public User(String userId, String gender, String birthDateStr, String email) {
        this.userId = validateUserId(userId);
        this.gender = validateGender(gender);
        this.birthDate = validateBirthDate(birthDateStr);
        this.email = validateEmail(email);
    }

    public static User of(String userId, String gender, String birthDate, String email) {
        return new User(userId, gender, birthDate, email);
    }

    private String validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "아이디는 필수입니다");
        }
        if (!userId.matches("^[a-zA-Z0-9]{1,10}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID는 영문 및 숫자 10자 이내");
        }
        return userId;
    }

    // ... 나머지 검증 메서드
}
```

**특징:**
- ✅ **간단함**: 파일 하나로 완결
- ✅ **실용적**: 빠른 개발 가능
- ✅ **Static Factory Method**: `User.of()` 사용
- ✅ **생성자 검증**: 객체 생성 시 검증
- ❌ **JPA 의존**: `@Entity`, `@Column` 등
- ❌ **BaseEntity 상속**: JPA Auditing에 의존
- ❌ **테스트 어려움**: JPA 컨텍스트 필요
- ❌ **비즈니스 메서드 부족**: Setter는 없지만 changeEmail() 같은 메서드도 없음

---

**LookMarket**

```java
package com.lookmarket.domain.user;

// 순수 Java (JPA 어노테이션 없음!)
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

    // package-private 생성자 (외부 직접 생성 방지)
    User(Long id, String email, String password, String name,
         String phoneNumber, UserRole role, UserStatus status,
         LocalDateTime createdAt, LocalDateTime updatedAt) {
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

    // 신규 생성 (Static Factory Method)
    public static User create(String email, String password, String name,
                              String phoneNumber, UserRole role) {
        LocalDateTime now = LocalDateTime.now();
        return new User(null, email, password, name, phoneNumber,
                        role != null ? role : UserRole.CUSTOMER,
                        UserStatus.ACTIVE, now, now);
    }

    // 재구성 (DB 조회 시 사용)
    public static User reconstitute(Long id, String email, String password,
                                   String name, String phoneNumber, UserRole role,
                                   UserStatus status, LocalDateTime createdAt,
                                   LocalDateTime updatedAt) {
        return new User(id, email, password, name, phoneNumber,
                        role, status, createdAt, updatedAt);
    }

    // 비즈니스 메서드
    public void changeEmail(String newEmail) {
        validateEmail(newEmail);
        if (!this.email.equals(newEmail)) {
            this.email = newEmail;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void changePassword(String newPassword) {
        validatePassword(newPassword);
        this.password = newPassword;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        if (this.status == UserStatus.SUSPENDED) {
            throw new IllegalStateException("정지된 계정은 활성화할 수 없습니다");
        }
        this.status = UserStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }

    // ... 검증 메서드 및 Getter
}
```

**특징:**
- ✅ **프레임워크 독립**: 순수 Java
- ✅ **테스트 용이**: Mock 불필요
- ✅ **비즈니스 메서드**: changeEmail(), suspend() 등
- ✅ **명확한 생성 의도**: create() vs reconstitute()
- ✅ **불변성**: id, role, createdAt은 final
- ❌ **복잡함**: Infrastructure에 별도 UserEntity 필요
- ❌ **변환 로직 필요**: fromDomain/toDomain

---

#### Infrastructure Layer

**Loopers**

```java
// UserJpaRepository.java
public interface UserJpaRepository extends JpaRepository<User, Long> {
    boolean existsByUserId(String userId);
    Optional<User> findByUserId(String userId);
}

// UserRepositoryImpl.java
@Component
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);  // ← Domain User 직접 저장
    }

    @Override
    public boolean existsByUserId(String userId) {
        return userJpaRepository.existsByUserId(userId);
    }

    @Override
    public Optional<User> findByUserId(String userId) {
        return userJpaRepository.findByUserId(userId);
    }
}
```

**특징:**
- ✅ **간단함**: Domain User를 직접 사용
- ✅ **변환 불필요**: JPA가 User를 직접 저장
- ❌ **Domain이 JPA에 종속**: User가 `@Entity`여야 함

---

**LookMarket**

```java
// UserEntity.java (Infrastructure)
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    // ... 나머지 필드

    // Domain → JPA Entity 변환
    public static UserEntity fromDomain(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setPassword(user.getPassword());
        // ... 나머지 필드 설정
        return entity;
    }

    // JPA Entity → Domain 변환
    public User toDomain() {
        return User.reconstitute(
            this.id, this.email, this.password,
            this.name, this.phoneNumber, this.role,
            this.status, this.createdAt, this.updatedAt
        );
    }
}

// JpaUserRepository.java
public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}

// UserAdapter.java (변환 담당)
@Component
public class UserAdapter implements UserRepository {
    private final JpaUserRepository jpaUserRepository;

    @Override
    public User save(User user) {
        UserEntity entity = UserEntity.fromDomain(user);  // Domain → JPA
        UserEntity saved = jpaUserRepository.save(entity);
        return saved.toDomain();  // JPA → Domain
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email)
                .map(UserEntity::toDomain);  // 변환
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }
}
```

**특징:**
- ✅ **Domain 독립성**: User는 JPA 몰라도 됨
- ✅ **기술 교체 용이**: JPA → MyBatis 교체 시 Adapter만 수정
- ✅ **역할 분리**: UserEntity는 DB 매핑만, User는 비즈니스 로직만
- ❌ **복잡함**: 3개 파일 + 변환 로직 필요
- ❌ **성능 오버헤드**: 변환 과정 추가

---

#### Application Layer

**Loopers**

```java
@Service
public class UserApplicationService {
    private final UserRepository userRepository;
    private final PointRepository pointRepository;

    @Transactional
    public UserInfo saveUser(String userId, String gender,
                             String birthDate, String email) {
        // 1. 중복 체크
        if (userRepository.existsByUserId(userId)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 ID");
        }

        // 2. Domain 객체 생성
        User user = User.of(userId, gender, birthDate, email);
        User savedUser = userRepository.save(user);

        // 3. 다른 도메인과 상호작용
        Point point = Point.of(savedUser, 0L);
        pointRepository.save(point);

        // 4. DTO로 변환하여 반환
        return UserInfo.from(savedUser);
    }

    @Transactional(readOnly = true)
    public UserInfo getMyInfo(String userId) {
        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new CoreException(
                ErrorType.NOT_FOUND, "사용자 정보를 찾을 수 없습니다"
            ));

        return UserInfo.from(user);
    }
}

// UserInfo.java (Application DTO)
public record UserInfo(
    Long id, String userId, String gender,
    LocalDate birthDate, String email
) {
    public static UserInfo from(User model) {
        return new UserInfo(
            model.getId(), model.getUserId(),
            model.getGender(), model.getBirthDate(),
            model.getEmail()
        );
    }
}
```

**특징:**
- ✅ **명확한 흐름**: 검증 → 생성 → 저장 → 변환
- ✅ **다른 도메인과 조율**: Point 생성 포함
- ✅ **DTO 변환**: Domain → DTO (UserInfo)
- ✅ **트랜잭션 경계**: `@Transactional`

---

**LookMarket** (예상 구현)

```java
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(String email, String rawPassword,
                            String name, String phoneNumber) {
        // 1. 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 존재하는 이메일");
        }

        // 2. 비밀번호 암호화 (Infrastructure 서비스)
        String encryptedPassword = passwordEncoder.encode(rawPassword);

        // 3. Domain 객체 생성 (Static Factory Method)
        User user = User.create(email, encryptedPassword, name,
                                phoneNumber, UserRole.CUSTOMER);

        // 4. 저장 (Adapter가 변환 자동 처리)
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "사용자를 찾을 수 없습니다: " + userId
            ));
    }

    @Transactional
    public void changeUserEmail(Long userId, String newEmail) {
        User user = getUser(userId);

        // 비즈니스 메서드 호출 (도메인 로직)
        user.changeEmail(newEmail);

        userRepository.save(user);
    }
}
```

**특징:**
- ✅ **비즈니스 로직 집중**: Domain 객체의 메서드 활용
- ✅ **변환 로직 숨김**: Adapter가 자동 처리
- ✅ **명확한 의도**: `changeUserEmail()`은 이메일 변경만 수행
- 🤔 **DTO 반환 여부**: Domain 객체 직접 반환 vs DTO 변환 (설계 선택)

---

#### API Layer

**Loopers**

```java
// UsersV1Dto.java
public class UsersV1Dto {
    public record UsersSaveRequest(
        String userId, String gender, String birthDate, String email
    ) {}

    public record UsersResponse(
        Long id, String userId, String gender,
        LocalDate birthDate, String email
    ) {
        public static UsersResponse from(UserInfo info) {
            return new UsersResponse(
                info.id(), info.userId(), info.gender(),
                info.birthDate(), info.email()
            );
        }
    }
}

// UsersV1Controller.java
@RestController
@RequestMapping("/api/v1/users")
public class UsersV1Controller implements UsersV1ApiSpec {
    private final UserApplicationService userApplicationService;

    @PostMapping
    @Override
    public ApiResponse<UsersV1Dto.UsersResponse> saveUser(
        @RequestBody UsersV1Dto.UsersSaveRequest request
    ) {
        UserInfo info = userApplicationService.saveUser(
            request.userId(), request.gender(),
            request.birthDate(), request.email()
        );

        UsersV1Dto.UsersResponse response =
            UsersV1Dto.UsersResponse.from(info);

        return ApiResponse.success(response);
    }

    @GetMapping("/me")
    @Override
    public ApiResponse<UsersV1Dto.UsersResponse> getMyInfo(
        @RequestHeader("X-USER-ID") String userId
    ) {
        UserInfo info = userApplicationService.getMyInfo(userId);
        UsersV1Dto.UsersResponse response =
            UsersV1Dto.UsersResponse.from(info);

        return ApiResponse.success(response);
    }
}
```

**특징:**
- ✅ **Swagger 스펙 분리**: `UsersV1ApiSpec` 인터페이스
- ✅ **DTO 변환**: Application DTO (UserInfo) → API DTO (UsersResponse)
- ✅ **일관된 응답 형식**: `ApiResponse<T>`
- ✅ **Record 사용**: Java 14+ Record 활용

---

**LookMarket** (예상 구현)

```java
// UserController.java (예상)
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    public ApiResponse<UserResponse> register(
        @RequestBody @Valid UserRegisterRequest request
    ) {
        User user = userService.registerUser(
            request.email(), request.password(),
            request.name(), request.phoneNumber()
        );

        UserResponse response = UserResponse.from(user);
        return ApiResponse.success(response);
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo(
        @AuthenticationPrincipal Long userId
    ) {
        User user = userService.getUser(userId);
        UserResponse response = UserResponse.from(user);
        return ApiResponse.success(response);
    }
}

// UserResponse.java (예상)
public record UserResponse(
    Long id, String email, String name,
    UserRole role, UserStatus status
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(), user.getEmail(), user.getName(),
            user.getRole(), user.getStatus()
        );
    }
}
```

**특징:**
- ✅ **DTO 변환**: Domain (User) → API DTO (UserResponse)
- ✅ **Spring Security 통합**: `@AuthenticationPrincipal`
- ✅ **Validation**: `@Valid`
- 🤔 **Swagger 스펙**: 별도 인터페이스 분리 여부는 선택 사항

---

### 2.3 변환 흐름 비교

#### Loopers 변환 흐름

```
API Layer
  UsersSaveRequest
    ↓ (primitive params)
Application Layer
  User.of() → User (Domain, @Entity)
    ↓ (직접 저장)
Infrastructure Layer
  JpaRepository<User> → DB
    ↓ (직접 반환)
Application Layer
  User → UserInfo (Application DTO)
    ↓
API Layer
  UserInfo → UsersResponse (API DTO)
```

**특징:**
- Domain User가 `@Entity`라서 JPA가 직접 저장
- 변환: Domain → Application DTO → API DTO (2단계)

---

#### LookMarket 변환 흐름

```
API Layer
  UserRegisterRequest
    ↓ (primitive params)
Application Layer
  User.create() → User (Domain, 순수 Java)
    ↓
Infrastructure Layer (Adapter)
  User → UserEntity (@Entity)
    ↓
  JpaRepository<UserEntity> → DB
    ↓
  UserEntity → User (Domain)
    ↓
Application Layer
  User (Domain)
    ↓
API Layer
  User → UserResponse (API DTO)
```

**특징:**
- Infrastructure에서 Domain ↔ JPA Entity 변환 자동 처리
- 변환: Domain → JPA Entity → Domain → API DTO (3단계)
- Application과 API는 순수한 Domain 객체만 사용

---

### 2.4 장단점 종합 비교

#### Loopers - "실용적 Hexagonal Architecture"

| 장점 | 단점 |
|------|------|
| ✅ **간단하고 직관적** | ❌ Domain이 JPA에 의존 |
| ✅ **빠른 개발 속도** | ❌ ORM 교체 어려움 |
| ✅ **변환 로직 불필요** | ❌ 단위 테스트 시 JPA Mock 필요 |
| ✅ **파일 수 적음** | ❌ BaseEntity 상속으로 결합도 증가 |
| ✅ **실무에서 흔한 패턴** | ❌ 비즈니스 로직과 기술 세부사항 혼재 |
| ✅ **학습 곡선 낮음** | ❌ Rich Domain Model 구현 어려움 |

**적합한 경우:**
- 중소규모 프로젝트 (1-3개월)
- 빠른 개발이 필요한 경우
- JPA를 계속 사용할 것이 확실한 경우
- 팀원 대부분이 Hexagonal Architecture 경험 부족
- 스타트업 초기 단계

**실무 채택률:** ⭐⭐⭐⭐⭐ (매우 흔함)

---

#### LookMarket - "순수 Hexagonal Architecture"

| 장점 | 단점 |
|------|------|
| ✅ **Domain 완전 독립** (순수 Java) | ❌ 파일 수 증가 (복잡도 상승) |
| ✅ **테스트 용이** (Mock 불필요) | ❌ 변환 로직 오버헤드 |
| ✅ **기술 스택 교체 용이** | ❌ 학습 곡선 높음 |
| ✅ **비즈니스 로직 명확 분리** | ❌ 초기 개발 속도 느림 |
| ✅ **Rich Domain Model 구현** | ❌ 소규모 프로젝트에는 과함 |
| ✅ **포트폴리오 어필 효과** | ❌ 팀원 교육 필요 |

**적합한 경우:**
- 장기 프로젝트 (6개월 이상)
- 기술 스택 변경 가능성
- **포트폴리오 프로젝트** ← **LookMarket의 경우!**
- 높은 테스트 커버리지 목표
- 설계 능력 어필 필요
- 대기업 또는 금융권 프로젝트

**실무 채택률:** ⭐⭐⭐ (대기업, 금융권 일부)

---

### 2.5 실무 관점 평가

#### Loopers 평점

| 평가 항목 | 점수 | 설명 |
|----------|------|------|
| **실용성** | ⭐⭐⭐⭐⭐ | 실무에서 가장 흔한 패턴 |
| **개발 속도** | ⭐⭐⭐⭐⭐ | 매우 빠름 |
| **유지보수성** | ⭐⭐⭐ | 괜찮지만 JPA 의존 |
| **테스트 용이성** | ⭐⭐⭐ | JPA Mock 필요 |
| **확장성** | ⭐⭐⭐ | 중간 |
| **포트폴리오 임팩트** | ⭐⭐⭐ | 평범함 |

**총평:** 실무에서 **가장 효율적**인 설계. 대부분의 스타트업이 채택.

---

#### LookMarket 평점

| 평가 항목 | 점수 | 설명 |
|----------|------|------|
| **실용성** | ⭐⭐⭐ | 대기업/장기 프로젝트 적합 |
| **개발 속도** | ⭐⭐⭐ | 초기 느림, 후반 빠름 |
| **유지보수성** | ⭐⭐⭐⭐⭐ | 매우 우수 |
| **테스트 용이성** | ⭐⭐⭐⭐⭐ | 순수 Java 테스트 가능 |
| **확장성** | ⭐⭐⭐⭐⭐ | 매우 우수 |
| **포트폴리오 임팩트** | ⭐⭐⭐⭐⭐ | **설계 능력 강력 어필** |

**총평:** **포트폴리오**에 최적화된 설계. "원칙을 이해한 개발자" 인상.

---

## 3. 설계 결정 요약

### 3.1 LookMarket이 선택한 설계 원칙

#### 1️⃣ Domain 순수성 우선

```java
// ❌ Loopers 방식
@Entity  // JPA 의존
public class User extends BaseEntity {
    @Column(name = "user_id")
    private String userId;
}

// ✅ LookMarket 방식
public class User {  // 순수 Java
    private final Long id;
    private String email;

    public void changeEmail(String newEmail) {
        validateEmail(newEmail);
        this.email = newEmail;
    }
}
```

**이유:**
- 비즈니스 로직과 기술 세부사항 완전 분리
- 테스트 용이성 극대화
- 기술 스택 교체 용이

---

#### 2️⃣ Behavior-rich Entity

```java
// ❌ Anemic Domain Model
@Getter
@Setter  // Setter 사용
public class User {
    private String email;
}

// 사용 (Service에서 비즈니스 로직)
user.setEmail(newEmail);  // 검증 없음
user.setStatus(UserStatus.ACTIVE);

// ✅ Rich Domain Model
@Getter  // Getter만
public class User {
    private String email;

    // 비즈니스 메서드
    public void changeEmail(String newEmail) {
        validateEmail(newEmail);
        this.email = newEmail;
        this.updatedAt = LocalDateTime.now();
    }
}

// 사용 (Service는 오케스트레이션만)
user.changeEmail(newEmail);  // 검증 포함
user.activate();
```

**이유:**
- 비즈니스 규칙이 Domain에 응집
- Service 레이어는 오케스트레이션만 담당
- DDD 원칙 준수

---

#### 3️⃣ Infrastructure 분리

```java
// Loopers: Domain이 JPA Entity
@Entity
public class User { }  // JPA 의존

// LookMarket: 완전 분리
// Domain
public class User { }  // 순수 Java

// Infrastructure
@Entity
public class UserEntity {
    public static UserEntity fromDomain(User user) { }
    public User toDomain() { }
}
```

**이유:**
- Domain의 프레임워크 독립성
- 변환 로직 Infrastructure에 캡슐화
- 포트-어댑터 패턴 명확화

---

### 3.2 면접 대비 답변 가이드

#### Q1: "왜 Domain과 Infrastructure를 분리했나요?"

**답변:**
> "LookMarket 프로젝트에서는 Domain을 프레임워크로부터 완전히 독립시켜 비즈니스 로직의 순수성을 보장했습니다. 예를 들어, User 엔티티는 순수 Java로 작성하여 JPA에 의존하지 않으며, Infrastructure 레이어의 UserEntity가 JPA 매핑을 담당합니다. 이렇게 하면 JPA를 MyBatis로 교체하더라도 Domain 레이어는 수정할 필요가 없고, 단위 테스트 시 Mock 없이 순수 Java 객체로 테스트할 수 있습니다."

---

#### Q2: "Domain에서 Lombok을 사용하지 않은 이유는?"

**답변:**
> "Behavior-rich Entity 원칙을 따르기 위해서입니다. Lombok의 @Setter를 사용하면 비즈니스 규칙을 우회하여 상태를 변경할 수 있는 위험이 있습니다. 대신 `changeEmail()`, `suspend()` 같은 비즈니스 메서드를 제공하여 검증 로직을 강제하고, 코드만으로도 비즈니스 의도를 명확히 표현했습니다. Infrastructure의 JPA Entity는 단순 데이터 컨테이너이므로 Lombok을 사용하여 보일러플레이트를 줄였습니다."

---

#### Q3: "실무에서 이렇게 복잡하게 하나요?"

**답변:**
> "실무에서는 프로젝트 규모와 요구사항에 따라 다릅니다. 저는 Loopers 프로젝트에서 실용적인 Hexagonal Architecture(Domain에 @Entity 사용)를 경험했고, 이 방식이 빠른 개발에 효과적임을 알고 있습니다. 하지만 LookMarket에서는 장기 유지보수와 설계 능력 향상을 목표로 순수 Hexagonal Architecture를 선택했습니다. 두 방식의 장단점을 모두 이해하고 있으며, 실무에서는 상황에 맞게 적절한 설계를 선택하겠습니다."

---

### 3.3 최종 권장 사항

#### 포트폴리오 관점

**LookMarket 방식 (순수 Hexagonal Architecture)을 강력 추천합니다.**

**이유:**

1. **차별화**
   - 대부분 후보자는 Loopers 방식 사용
   - "설계 원칙을 깊이 이해한 개발자" 인상

2. **학습 효과**
   - Hexagonal Architecture의 본질적 이해
   - DDD, Clean Architecture 개념 학습

3. **면접 대응**
   - "왜 이렇게 설계했나요?" 질문에 자신감 있게 답변
   - Loopers와 비교하여 설명 가능

4. **장기적 성장**
   - 대기업, 금융권에서 선호하는 패턴
   - 아키텍트로 성장하는 데 필요한 사고방식

---

#### 실무 진입 시

**초기 실무에서는 Loopers 방식 사용 가능성 높음**

- 스타트업, 중소기업 대부분 채택
- 빠른 개발 속도 중시
- 팀원 학습 곡선 고려

**하지만:**
- LookMarket에서 배운 원칙은 여전히 유효
- 점진적 리팩터링으로 개선 가능
- 설계 리뷰 시 더 나은 제안 가능

---

### 3.4 혼합 접근 (절충안)

만약 "너무 복잡하다"고 느껴진다면:

**Phase별 적용:**
- **Phase 1 (User)**: 순수 Hexagonal Architecture (학습 + 포트폴리오)
- **Phase 2-3 (Product, Order)**: Loopers 방식 (속도)
- **면접 준비**: "User는 학습 목적, 나머지는 실용성"

**하지만 권장하지 않음:**
- 일관성 부족
- "왜 다르게 했나?" 질문 대응 어려움
- 하나의 방식을 끝까지 가는 것이 학습 효과 큼

---

### 3.5 다음 단계

현재 User Domain Layer 작업을 완료했습니다:

- ✅ Domain: User, UserRole, UserStatus, UserRepository (포트)
- ✅ Infrastructure: UserEntity, JpaUserRepository, UserAdapter
- ✅ Flyway: V1__create_users_table.sql

**다음 작업:**
1. Application Layer: UserService
2. API Layer: UserController, DTO
3. Security: Spring Security + JWT
4. Tests: 단위/통합/E2E

**계속 진행할까요?**

---

## 참고 자료

- **프로젝트 스펙**: `docs/design/LookMarket_Project_Specification.md`
- **아키텍처 규칙**: `docs/architecture/ENFORCEMENT_RULES.md`
- **개발 가이드**: `CLAUDE.md`
- **개발 일지**: `docs/project/DEVELOPMENT_LOG.md`

---

**마지막 업데이트**: 2025-12-16
**작성자**: LookMarket 개발팀
