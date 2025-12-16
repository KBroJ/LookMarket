# ADR-001: User 도메인 설계 결정

**상태**: 승인됨
**날짜**: 2024-12-16
**범위**: User 도메인 (lookmarket-domain, lookmarket-infrastructure, lookmarket-application, lookmarket-api)

---

## 개요

이 문서는 User 도메인의 수직적 슬라이스(Vertical Slice) 구현에서 내린 설계 결정들과 그 이유를 기록합니다.

---

## 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────────┐
│                         API Layer (api)                             │
│  UserController ← UserRequest/UserResponse (DTO)                    │
│  - HTTP 요청/응답 처리, 입력 검증                                      │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Application Layer (application)                  │
│  UserService (@Transactional)                                       │
│  - 유즈케이스 오케스트레이션, 트랜잭션 경계                              │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Domain Layer (domain)                          │
│  User (엔티티) + UserRepository (인터페이스/포트)                       │
│  - 순수 비즈니스 로직, 프레임워크 의존성 없음                             │
└─────────────────────────────────────────────────────────────────────┘
                                  ▲
                                  │ 구현
┌─────────────────────────────────────────────────────────────────────┐
│                  Infrastructure Layer (infrastructure)              │
│  UserAdapter ← JpaUserRepository ← UserEntity                       │
│  - JPA, 데이터베이스 접근                                              │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 1. Domain Layer 설계 결정

### 1.1 순수 Java 클래스로 User 엔티티 구현

#### 결정

```java
// 선택한 방식: 순수 Java
public class User {
    private final Long id;
    private String email;
    // ... JPA 어노테이션 없음!
}
```

#### 고려한 대안

| 대안 | 장점 | 단점 |
|------|------|------|
| **순수 Java (선택)** | 프레임워크 독립적, 테스트 용이 | 변환 로직 필요 |
| JPA 엔티티 직접 사용 | 단순함, 변환 불필요 | Domain이 JPA에 의존 |

#### 결정 이유

- **Hexagonal Architecture 원칙 준수**: Domain 레이어가 Infrastructure(JPA)에 의존하면 안 됨
- **테스트 용이성**: `UserTest.java`에서 보듯이 Spring 없이 순수 Java로 테스트 가능
- **프레임워크 교체 유연성**: 나중에 JPA → jOOQ나 다른 ORM으로 변경해도 Domain은 수정 불필요

---

### 1.2 Static Factory Method 패턴

#### 결정

```java
// 선택: Static Factory Method
public static User create(String email, String password, ...) {
    return new User(null, email, password, ...);
}

public static User reconstitute(Long id, String email, ...) {
    return new User(id, email, ...);
}
```

#### 고려한 대안

```java
// 대안: public 생성자
public User(String email, String password, ...) { }
```

#### 각 메서드의 목적

| 패턴 | 목적 |
|------|------|
| `User.create()` | **신규 생성** - ID=null, status=ACTIVE, 검증 포함 |
| `User.reconstitute()` | **DB 복원** - 기존 데이터 그대로 재구성 |

#### 결정 이유

- **의도 명확화**: 생성자 vs 복원의 목적이 메서드명으로 드러남
- **생성자 캡슐화**: package-private 생성자로 외부에서 직접 생성 방지
- **일관된 초기 상태**: 신규 생성 시 항상 `ACTIVE` 상태로 시작

---

### 1.3 Behavior-rich Entity (행위 중심 엔티티)

#### 결정

```java
// 선택: 행위 메서드
public void changeEmail(String newEmail) {
    validateEmail(newEmail);
    if (!this.email.equals(newEmail)) {
        this.email = newEmail;
        this.updatedAt = LocalDateTime.now();
    }
}
```

#### 고려한 대안

```java
// 대안: Anemic Domain Model (Setter)
public void setEmail(String email) {
    this.email = email;
}
```

#### 비교

| 방식 | 비즈니스 로직 위치 | 문제점 |
|------|-------------------|--------|
| **행위 중심 (선택)** | Domain 내부 | - |
| Setter 방식 | Service 레이어 | 로직 분산, Domain이 단순 데이터 구조가 됨 |

#### 결정 이유

- **DDD 원칙**: 도메인 객체가 자신의 비즈니스 규칙을 알아야 함
- **검증 캡슐화**: `changeEmail()` 내부에서 이메일 형식 검증
- **상태 일관성**: 변경 시 `updatedAt` 자동 갱신

---

### 1.4 Repository 인터페이스를 Domain에 정의

#### 결정

```java
// Domain Layer에 위치
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    // ... JpaRepository를 상속하지 않음!
}
```

#### 의존성 방향

```
❌ 잘못된 방식:
Domain → extends JpaRepository → Spring Data JPA 의존

✅ 올바른 방식 (선택):
Domain: UserRepository (인터페이스/포트)
    ↑ implements
Infrastructure: UserAdapter (어댑터)
```

#### 결정 이유

- **의존성 역전 (DIP)**: Domain이 Infrastructure를 모름
- **포트 & 어댑터 패턴**: Repository 인터페이스가 "포트" 역할
- **테스트 용이성**: Mock Repository로 Domain 독립 테스트 가능

---

## 2. Infrastructure Layer 설계 결정

### 2.1 Domain Entity와 JPA Entity 분리

#### 결정

```java
// Domain Layer
public class User { ... }  // 순수 Java

// Infrastructure Layer
@Entity
@Table(name = "users")
public class UserEntity { ... }  // JPA 전용
```

#### 고려한 대안

| 방식 | 장점 | 단점 |
|------|------|------|
| **분리 (선택)** | 관심사 분리, Domain 순수성 유지 | 변환 코드 필요 |
| 단일 엔티티 | 단순함 | Domain이 JPA에 오염 |

#### 결정 이유

- **Domain 순수성**: `@Entity`, `@Column` 같은 JPA 어노테이션이 Domain에 침투하지 않음
- **각 레이어의 책임 명확화**:
  - `User`: 비즈니스 로직
  - `UserEntity`: DB 매핑

---

### 2.2 변환 메서드 패턴 (fromDomain / toDomain)

#### 결정

```java
// UserEntity.java
public static UserEntity fromDomain(User user) {
    return new UserEntity(user.getId(), user.getEmail(), ...);
}

public User toDomain() {
    return User.reconstitute(this.id, this.email, ...);
}
```

#### 고려한 대안

| 방식 | 위치 | 장단점 |
|------|------|--------|
| **Entity에 변환 메서드 (선택)** | Infrastructure | Entity가 Domain을 알아야 함 (OK) |
| 별도 Mapper 클래스 | Infrastructure | 코드 분산, 파일 증가 |
| MapStruct 등 라이브러리 | Infrastructure | 의존성 추가, 설정 복잡 |

#### 결정 이유

- **단순성**: 별도 Mapper 없이 Entity 내에서 변환
- **응집도**: 변환 로직이 변환 대상과 함께 위치
- **의존 방향 유지**: Infrastructure → Domain (올바른 방향)

---

### 2.3 Adapter 패턴 적용 (UserAdapter)

#### 결정

```java
@Component
public class UserAdapter implements UserRepository {  // Domain의 포트 구현

    private final JpaUserRepository jpaUserRepository;  // Spring Data JPA

    @Override
    public User save(User user) {
        UserEntity entity = UserEntity.fromDomain(user);
        UserEntity saved = jpaUserRepository.save(entity);
        return saved.toDomain();  // Domain 객체로 변환하여 반환
    }
}
```

#### 구조

```
Domain Layer:        UserRepository (인터페이스)
                           ↑ implements
Infrastructure:      UserAdapter (어댑터)
                           │ 사용
                     JpaUserRepository (Spring Data JPA)
                           │
                     UserEntity (JPA Entity)
```

#### 결정 이유

- **포트 & 어댑터 패턴**: Domain의 포트를 Infrastructure가 구현
- **Spring Data JPA 캡슐화**: `JpaUserRepository`는 내부 구현 세부사항
- **변환 책임 집중**: 모든 Domain ↔ Entity 변환이 Adapter에서 수행

---

## 3. Application Layer 설계 결정

### 3.1 Orchestration Only (오케스트레이션만)

#### 결정

```java
@Transactional
public User changeEmail(Long userId, String newEmail) {
    // 1. 조회 (Repository 호출)
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    // 2. 중복 체크 (Repository 호출)
    if (userRepository.existsByEmail(newEmail)) {
        throw new IllegalArgumentException("Email already exists");
    }

    // 3. 도메인 로직 실행 (Domain에 위임)
    user.changeEmail(newEmail);  // 검증은 Domain에서!

    // 4. 저장
    return userRepository.save(user);
}
```

#### Service의 역할 범위

| Service가 해야 할 것 | Service가 하면 안 되는 것 |
|---------------------|------------------------|
| ✅ 트랜잭션 경계 관리 | ❌ 이메일 형식 검증 |
| ✅ Repository 호출 순서 조율 | ❌ 비즈니스 규칙 직접 구현 |
| ✅ 외부 서비스 호출 (암호화 등) | ❌ 도메인 객체 상태 직접 변경 |
| ✅ 예외 처리 및 변환 | ❌ 도메인 불변식 검증 |
| ✅ 이벤트 발행 조율 | ❌ 복잡한 계산 로직 |

#### 결정 이유

- **단일 책임**: Service는 흐름만 제어, 로직은 Domain에
- **테스트 용이성**: Domain 로직은 독립 테스트, Service는 통합 테스트
- **로직 분산 방지**: 비즈니스 규칙이 여러 곳에 흩어지지 않음

---

### 3.2 @Transactional(readOnly = true) 기본 설정

#### 결정

```java
@Service
@Transactional(readOnly = true)  // 클래스 레벨: 기본 읽기 전용
public class UserService {

    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);  // 읽기 전용 트랜잭션
    }

    @Transactional  // 메서드 레벨: 쓰기 트랜잭션으로 오버라이드
    public User register(...) { ... }
}
```

#### 효과

| 설정 | 효과 |
|------|------|
| `readOnly = true` | Hibernate flush 비활성화, 성능 최적화 |
| 쓰기 메서드에 `@Transactional` | 필요한 곳에만 쓰기 트랜잭션 |

#### 결정 이유

- **실수 방지**: 읽기 메서드에서 실수로 데이터 변경 방지
- **성능**: 읽기 전용 시 Dirty Checking 비활성화
- **명시적**: 쓰기 작업이 필요한 곳이 명확히 드러남

---

## 4. API Layer 설계 결정

### 4.1 Domain 객체 직접 노출 금지

#### 결정

```java
// 선택한 방식: DTO로 변환
@GetMapping("/{userId}")
public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
    User user = userService.getUserById(userId)...;
    return ResponseEntity.ok(UserResponse.from(user));
}
```

#### 고려한 대안

```java
// 대안: Domain 직접 반환
@GetMapping("/{userId}")
public User getUser(@PathVariable Long userId) {
    return userService.getUserById(userId);
}
```

#### 결정 이유

| 이유 | 설명 |
|------|------|
| **보안** | `password` 필드가 응답에 노출되지 않음 |
| **계약 분리** | API 스펙 변경이 Domain에 영향 안 줌 |
| **유연성** | 클라이언트에 맞게 필드 추가/제거 가능 |

---

### 4.2 Java Record를 DTO로 사용

#### 결정

```java
public record UserRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password,
    @NotBlank String name,
    @Pattern(regexp = "...") String phoneNumber,
    @NotNull UserRole role
) { }

public record UserResponse(
    Long id, String email, String name, ...
) {
    public static UserResponse from(User user) { ... }
}
```

#### 고려한 대안

| 방식 | 장점 | 단점 |
|------|------|------|
| **Record (선택)** | 불변, 간결, equals/hashCode 자동 | Java 16+ 필요 |
| 일반 Class + Lombok | 호환성 | 보일러플레이트, 실수로 mutable 가능 |

#### 결정 이유

- **Java 21 프로젝트**: Record 사용 가능
- **불변 보장**: DTO는 변경될 이유가 없음
- **간결함**: getter, equals, hashCode 자동 생성

---

### 4.3 RESTful API 설계

#### 결정

```
POST   /api/v1/users                    // 회원가입
GET    /api/v1/users/{userId}           // 조회
PATCH  /api/v1/users/{userId}/email     // 이메일 변경
PATCH  /api/v1/users/{userId}/password  // 비밀번호 변경
POST   /api/v1/users/{userId}/activate  // 활성화 (상태 변경 = 행위)
POST   /api/v1/users/{userId}/suspend   // 정지
```

#### 결정 이유

| 결정 | 이유 |
|------|------|
| `PATCH` for 부분 수정 | `PUT`은 전체 교체, `PATCH`는 부분 수정에 적합 |
| `POST` for 상태 변경 | 활성화/정지는 "행위"이므로 POST가 적합 |
| `/email`, `/password` 분리 | 각 작업의 검증 로직이 다름 (현재 비밀번호 확인 등) |

---

## 5. 테스트 전략

### Domain 테스트: 순수 단위 테스트

```java
// UserTest.java - Spring 없이 실행
@Test
void createUser_Success() {
    User user = User.create("test@example.com", "password", "홍길동", null, UserRole.CUSTOMER);
    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
}
```

#### 결정 이유

- **빠른 실행**: Spring Context 로드 불필요
- **독립적**: DB, 외부 서비스 없이 비즈니스 로직만 검증
- **Domain 순수성 증명**: 프레임워크 없이 테스트 가능 = Domain이 순수함

---

## 설계 결정 요약표

| 결정 사항 | 선택 | 핵심 이유 |
|-----------|------|-----------|
| Domain Entity | 순수 Java | Hexagonal Architecture, 테스트 용이성 |
| 생성 패턴 | Static Factory Method | 의도 명확화, 상태 일관성 |
| Entity 스타일 | Behavior-rich | DDD 원칙, 비즈니스 로직 캡슐화 |
| Repository | Domain에 인터페이스 | 의존성 역전, 포트 & 어댑터 |
| Domain/JPA Entity | 분리 | 관심사 분리, Domain 순수성 |
| 변환 방식 | Entity 내 메서드 | 단순성, 응집도 |
| Infrastructure 연결 | Adapter 패턴 | JPA 캡슐화, 변환 책임 집중 |
| Service 역할 | Orchestration Only | 단일 책임, 로직 분산 방지 |
| 트랜잭션 기본값 | readOnly = true | 성능, 실수 방지 |
| API 응답 | DTO 변환 | 보안, 계약 분리 |
| DTO 구현 | Java Record | 불변, 간결 |

---

## 참고

- [CLAUDE.md - 아키텍처 강제 규칙](../../../CLAUDE.md)
- [ENFORCEMENT_RULES.md - 상세 규칙](../ENFORCEMENT_RULES.md)
- [핵심 설계 패턴 개념 정리](../../learning/핵심-설계-패턴-개념-정리.md)
