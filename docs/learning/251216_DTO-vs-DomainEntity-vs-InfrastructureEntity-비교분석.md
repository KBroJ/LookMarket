# DTO vs Domain Entity vs Infrastructure Entity 비교 분석

> **작성일시**: 2025-12-16 18:00
>
> LookMarket 프로젝트에서 사용하는 세 가지 엔티티/객체 타입의 차이점과 역할

**카테고리**: 아키텍처

---

## 📋 목차

1. [개요](#개요)
2. [세 가지 개념 정의](#세-가지-개념-정의)
3. [역할과 책임 비교](#역할과-책임-비교)
4. [코드 예시 비교](#코드-예시-비교)
5. [변환 흐름](#변환-흐름)
6. [왜 분리하는가?](#왜-분리하는가)
7. [장단점 비교](#장단점-비교)
8. [실무 적용 가이드](#실무-적용-가이드)
9. [면접 대비 답변](#면접-대비-답변)

---

## 개요

### 자주 하는 오해

**"User.java는 DTO 아닌가요?"** ❌

- Domain Entity는 **비즈니스 로직을 포함**하므로 DTO가 아닙니다
- DTO는 **순수 데이터 전송**만 담당합니다
- Infrastructure Entity는 **영속성 매핑**을 담당합니다

### LookMarket 프로젝트의 User 관련 객체

```
1. UserRequest (DTO) → API 요청
2. UserResponse (DTO) → API 응답
3. User (Domain Entity) → 비즈니스 로직
4. UserEntity (Infrastructure Entity) → 데이터베이스 매핑
```

---

## 세 가지 개념 정의

### 1. DTO (Data Transfer Object)

**정의**: 계층 간 데이터 전송을 위한 객체

**특징**:
- ❌ 비즈니스 로직 없음
- ❌ 영속성 매핑 없음
- ✅ 순수 데이터 전송만
- ✅ 불변 객체 (Java Record 사용)

**위치**: `lookmarket-api` 모듈

**예시**: `UserRequest`, `UserResponse`, `ChangeEmailRequest`

---

### 2. Domain Entity

**정의**: 비즈니스 로직과 규칙을 캡슐화한 도메인 모델

**특징**:
- ✅ 비즈니스 로직 포함 (`changeEmail()`, `activate()`)
- ✅ 불변식(Invariant) 보호
- ✅ 프레임워크 독립적 (순수 Java)
- ❌ 영속성 매핑 없음 (JPA 애노테이션 없음)

**위치**: `lookmarket-domain` 모듈

**예시**: `User` (Domain)

---

### 3. Infrastructure Entity

**정의**: 데이터베이스 영속성을 위한 JPA 엔티티

**특징**:
- ✅ JPA 애노테이션 포함 (`@Entity`, `@Table`, `@Column`)
- ✅ 데이터베이스 매핑
- ✅ Domain Entity와 변환 (fromDomain/toDomain)
- ❌ 비즈니스 로직 없음

**위치**: `lookmarket-infrastructure` 모듈

**예시**: `UserEntity` (Infrastructure)

---

## 역할과 책임 비교

### 비교 표

| 항목 | DTO | Domain Entity | Infrastructure Entity |
|------|-----|---------------|----------------------|
| **목적** | 데이터 전송 | 비즈니스 로직 | 데이터베이스 매핑 |
| **로직** | ❌ 없음 | ✅ 있음 | ❌ 없음 |
| **검증** | 형식 검증만 (@NotBlank) | 비즈니스 규칙 검증 | ❌ 없음 |
| **불변성** | ✅ 불변 (Record) | ⚠️ 가변 (상태 변경 허용) | ⚠️ 가변 (JPA 요구사항) |
| **의존성** | 없음 | 없음 | JPA, Database |
| **변경 빈도** | API 변경 시 | 비즈니스 규칙 변경 시 | 테이블 구조 변경 시 |
| **위치** | API Layer | Domain Layer | Infrastructure Layer |
| **예시** | `UserRequest` | `User` | `UserEntity` |

---

## 코드 예시 비교

### 1. DTO (Data Transfer Object)

#### UserRequest.java (API 요청)

```java
package com.lookmarket.api.user;

import jakarta.validation.constraints.*;

/**
 * DTO: 순수 데이터 전송
 * - 로직 없음
 * - 형식 검증만 (Bean Validation)
 */
public record UserRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100)
        String password,

        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Role is required")
        UserRole role
) {
    // 로직 없음! 단순 데이터 컨테이너
}
```

#### UserResponse.java (API 응답)

```java
package com.lookmarket.api.user;

import com.lookmarket.domain.user.User;

/**
 * DTO: Domain 객체를 직접 노출하지 않고 변환
 */
public record UserResponse(
        Long id,
        String email,
        String name,
        UserRole role,
        UserStatus status,
        LocalDateTime createdAt
) {
    /**
     * Domain → DTO 변환 (API 레이어에서만 사용)
     */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
```

**특징**:
- ✅ `record` 사용 (불변 객체)
- ✅ 형식 검증만 (`@NotBlank`, `@Email`)
- ❌ 비즈니스 로직 없음
- ✅ Domain 객체 → DTO 변환 메서드 (`from()`)

---

### 2. Domain Entity (비즈니스 로직)

#### User.java (Domain)

```java
package com.lookmarket.domain.user;

import java.time.LocalDateTime;

/**
 * Domain Entity: 비즈니스 로직과 불변식을 캡슐화
 * - 프레임워크 독립적 (순수 Java)
 * - JPA 애노테이션 없음
 */
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

    /**
     * package-private 생성자
     * 외부에서 직접 생성 불가 → 불변식 보호
     */
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

    /**
     * Static Factory Method: 신규 사용자 생성
     */
    public static User create(String email, String password, String name,
                              String phoneNumber, UserRole role) {
        LocalDateTime now = LocalDateTime.now();
        return new User(
            null,  // ID는 데이터베이스에서 생성
            email,
            password,
            name,
            phoneNumber,
            role != null ? role : UserRole.CUSTOMER,
            UserStatus.ACTIVE,
            now,
            now
        );
    }

    /**
     * Static Factory Method: 기존 사용자 재구성
     * (데이터베이스에서 조회 시 사용)
     */
    public static User reconstitute(Long id, String email, String password,
                                    String name, String phoneNumber,
                                    UserRole role, UserStatus status,
                                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new User(id, email, password, name, phoneNumber, role, status, createdAt, updatedAt);
    }

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 이메일 변경 (비즈니스 규칙 포함)
     */
    public void changeEmail(String newEmail) {
        validateEmail(newEmail);  // ✅ 비즈니스 규칙 검증

        if (!this.email.equals(newEmail)) {
            this.email = newEmail;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 계정 활성화 (비즈니스 규칙 포함)
     */
    public void activate() {
        // ✅ 불변식: 정지된 계정은 활성화 불가
        if (this.status == UserStatus.SUSPENDED) {
            throw new IllegalStateException("정지된 계정은 활성화할 수 없습니다.");
        }
        this.status = UserStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 계정 정지
     */
    public void suspend() {
        this.status = UserStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 활성 상태 확인
     */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    /**
     * 관리자 권한 확인
     */
    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }

    // ===== Validation 메서드 (private) =====

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일은 필수 입력 항목입니다.");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 필수 입력 항목입니다.");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("이름은 필수 입력 항목입니다.");
        }
    }

    // ===== Getters (Setter 없음!) =====

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

**특징**:
- ✅ **비즈니스 로직 포함** (`changeEmail()`, `activate()`, `suspend()`)
- ✅ **불변식 보호** (package-private 생성자, 검증 메서드)
- ✅ **의도가 명확한 메서드** (setter 대신 `changeEmail()`, `activate()`)
- ✅ **프레임워크 독립적** (JPA 애노테이션 없음)
- ❌ **Setter 없음** (상태 변경은 비즈니스 메서드로만)

---

### 3. Infrastructure Entity (영속성 매핑)

#### UserEntity.java (Infrastructure)

```java
package com.lookmarket.infrastructure.user;

import com.lookmarket.domain.user.User;
import com.lookmarket.domain.user.UserRole;
import com.lookmarket.domain.user.UserStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Infrastructure Entity: JPA 매핑
 * - Domain Entity와 별도로 관리
 * - Domain 독립성 유지
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA 기본 생성자 (protected)
     */
    protected UserEntity() {
    }

    /**
     * 모든 필드 생성자 (private)
     */
    private UserEntity(Long id, String email, String password, String name,
                       String phoneNumber, UserRole role, UserStatus status,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
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

    /**
     * Domain → Infrastructure 변환
     */
    public static UserEntity fromDomain(User user) {
        return new UserEntity(
            user.getId(),
            user.getEmail(),
            user.getPassword(),
            user.getName(),
            user.getPhoneNumber(),
            user.getRole(),
            user.getStatus(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    /**
     * Infrastructure → Domain 변환
     */
    public User toDomain() {
        return User.reconstitute(
            this.id,
            this.email,
            this.password,
            this.name,
            this.phoneNumber,
            this.role,
            this.status,
            this.createdAt,
            this.updatedAt
        );
    }

    /**
     * JPA Lifecycle 콜백
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ===== Getters and Setters (JPA 요구사항) =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    // ... 나머지 getter/setter
}
```

**특징**:
- ✅ **JPA 애노테이션** (`@Entity`, `@Table`, `@Column`)
- ✅ **Domain ↔ Infrastructure 변환** (`fromDomain()`, `toDomain()`)
- ✅ **JPA Lifecycle 콜백** (`@PrePersist`, `@PreUpdate`)
- ✅ **Setter 포함** (JPA 요구사항)
- ❌ **비즈니스 로직 없음** (순수 영속성 매핑만)

---

## 변환 흐름

### 전체 흐름도

```
[Client]
   │
   │ HTTP Request (JSON)
   ↓
[API Layer]
   UserRequest (DTO)
   │
   │ Controller → Service 호출
   ↓
[Application Layer]
   │
   │ Service → Domain 로직 실행
   ↓
[Domain Layer]
   User (Domain Entity)
   │ ← 비즈니스 로직 실행 (changeEmail, activate 등)
   │
   │ Repository.save(user)
   ↓
[Infrastructure Layer]
   UserEntity.fromDomain(user)  ← Domain → Infrastructure 변환
   │
   │ JpaRepository.save(userEntity)
   ↓
[Database]
   users 테이블에 저장
   │
   │ 조회 시
   ↓
[Infrastructure Layer]
   UserEntity.toDomain()  ← Infrastructure → Domain 변환
   │
   ↓
[Domain Layer]
   User (Domain Entity)
   │
   ↓
[API Layer]
   UserResponse.from(user)  ← Domain → DTO 변환
   │
   │ HTTP Response (JSON)
   ↓
[Client]
```

### 코드로 보는 변환 흐름

```java
// 1. Client → API Layer (HTTP Request → DTO)
UserRequest request = new UserRequest("test@example.com", "password123", "홍길동", UserRole.CUSTOMER);

// 2. API → Application Layer (DTO → Service 파라미터)
@PostMapping
public ResponseEntity<UserResponse> register(@RequestBody UserRequest request) {
    User user = userService.register(
        request.email(),      // DTO에서 값 추출
        request.password(),
        request.name(),
        request.role()
    );
    return ResponseEntity.ok(UserResponse.from(user));  // 8단계로 점프
}

// 3. Application Layer (Service → Domain 생성)
@Service
public class UserService {
    public User register(String email, String password, String name, UserRole role) {
        String encodedPassword = passwordEncoder.encode(password);

        // 4. Domain Layer (Domain Entity 생성)
        User user = User.create(email, encodedPassword, name, null, role);

        // 5. Application → Infrastructure (Domain → 저장)
        return userRepository.save(user);
    }
}

// 6. Infrastructure Layer (Domain → Infrastructure Entity 변환)
@Component
public class UserAdapter implements UserRepository {
    @Override
    public User save(User user) {
        // 변환: Domain → Infrastructure Entity
        UserEntity entity = UserEntity.fromDomain(user);

        // 7. JPA로 저장
        UserEntity saved = jpaUserRepository.save(entity);

        // 변환: Infrastructure Entity → Domain
        return saved.toDomain();
    }
}

// 8. API Layer (Domain → DTO 변환)
UserResponse response = UserResponse.from(user);
// → { "id": 1, "email": "test@example.com", "name": "홍길동", ... }
```

---

## 왜 분리하는가?

### 1. DTO를 사용하는 이유

**문제**: Domain Entity를 직접 노출하면?

```java
// ❌ 나쁜 예: Domain을 직접 반환
@GetMapping("/{userId}")
public ResponseEntity<User> getUser(@PathVariable Long userId) {
    return ResponseEntity.ok(userService.getUser(userId));
}
```

**문제점**:
1. **보안 문제**: 민감한 정보 노출 (`password` 필드까지 전부 노출)
2. **순환 참조**: Jackson 직렬화 시 무한 루프 가능
3. **API 변경 어려움**: Domain 변경 → API 자동 변경 (하위 호환 깨짐)
4. **과도한 정보**: 클라이언트에게 불필요한 필드까지 전송

**해결**: DTO로 변환

```java
// ✅ 좋은 예: DTO로 변환
@GetMapping("/{userId}")
public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
    User user = userService.getUser(userId);
    return ResponseEntity.ok(UserResponse.from(user));  // Domain → DTO
}

// UserResponse는 필요한 필드만 노출
public record UserResponse(
    Long id,
    String email,
    String name,
    UserRole role
    // password는 노출 안 함! ✅
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole()
        );
    }
}
```

**장점**:
- ✅ 보안: 민감한 정보 노출 방지
- ✅ 유연성: API 형식 독립적 변경 가능
- ✅ 명확성: 클라이언트에게 필요한 데이터만 전달

---

### 2. Domain과 Infrastructure Entity를 분리하는 이유

**문제**: Domain에 JPA 애노테이션을 넣으면?

```java
// ❌ 나쁜 예: Domain에 JPA 애노테이션
package com.lookmarket.domain.user;

import jakarta.persistence.*;  // ❌ Domain이 JPA에 의존!

@Entity  // ❌
@Table(name = "users")  // ❌
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    // ...
}
```

**문제점**:
1. **프레임워크 의존성**: Domain이 JPA에 의존 → 테스트 어려움
2. **변경 전파**: DB 스키마 변경 → Domain 변경 (관심사 분리 위배)
3. **단위 테스트 어려움**: JPA 없이 Domain 로직만 테스트 불가능
4. **영속성 교체 어려움**: JPA → 다른 DB로 전환 시 Domain까지 수정

**해결**: Domain과 Infrastructure Entity 분리

```java
// ✅ Domain Layer: 순수 Java (JPA 의존성 없음)
package com.lookmarket.domain.user;

public class User {
    private final Long id;
    private String email;

    public void changeEmail(String newEmail) {
        validateEmail(newEmail);
        this.email = newEmail;
    }
}

// ✅ Infrastructure Layer: JPA 매핑
package com.lookmarket.infrastructure.user;

import jakarta.persistence.*;  // ✅ Infrastructure만 JPA 의존

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    // Domain ↔ Infrastructure 변환
    public static UserEntity fromDomain(User user) { ... }
    public User toDomain() { ... }
}
```

**장점**:
- ✅ **Domain 독립성**: JPA, Hibernate 없이도 Domain 로직 테스트 가능
- ✅ **변경 격리**: DB 스키마 변경 → Infrastructure만 수정
- ✅ **영속성 교체 용이**: JPA → MongoDB로 전환 시 Domain 변경 불필요
- ✅ **테스트 용이**: Domain 단위 테스트가 빠르고 간단

---

## 장단점 비교

### 분리 전략의 장단점

#### LookMarket 방식 (완전 분리)

**구조**:
```
DTO (API) ↔ Domain Entity ↔ Infrastructure Entity (JPA)
```

**장점**:
- ✅ Domain 완전 독립 (프레임워크 없이 테스트 가능)
- ✅ 영속성 교체 용이 (JPA → MongoDB)
- ✅ 변경 격리 (DB 변경 → Infrastructure만)
- ✅ 계층 간 책임 명확

**단점**:
- ❌ 초기 설정 복잡
- ❌ 변환 코드 필요 (fromDomain/toDomain)
- ❌ 보일러플레이트 코드 증가

**적합한 경우**:
- 중대형 프로젝트
- 장기 유지보수
- 영속성 변경 가능성
- 포트폴리오 (기술력 어필)

---

#### Loopers 방식 (Domain에 @Entity)

**구조**:
```
DTO (API) ↔ Domain Entity (JPA 포함)
```

**장점**:
- ✅ 간단한 구조
- ✅ 변환 코드 불필요
- ✅ 빠른 개발 속도

**단점**:
- ❌ Domain이 JPA에 의존
- ❌ 단위 테스트 어려움
- ❌ 영속성 교체 어려움

**적합한 경우**:
- 소규모 프로젝트
- 단기 프로젝트
- 영속성 변경 없음

---

#### 단일 객체 방식 (분리 없음)

**구조**:
```
User (API + Domain + JPA 모두 포함)
```

**장점**:
- ✅ 가장 간단
- ✅ 변환 없음

**단점**:
- ❌ 관심사 분리 없음
- ❌ 테스트 어려움
- ❌ 보안 문제 (password 노출)
- ❌ API 변경 어려움

**적합한 경우**:
- 프로토타입
- 간단한 CRUD

---

## 실무 적용 가이드

### 언제 어떤 방식을 선택할까?

#### 1. LookMarket 방식 (완전 분리) 선택 기준

**추천하는 경우**:
- ✅ 포트폴리오 프로젝트 (기술력 어필)
- ✅ 장기 유지보수 예정
- ✅ 영속성 변경 가능성
- ✅ 복잡한 비즈니스 로직
- ✅ 팀 프로젝트 (3명 이상)

**예시 프로젝트**:
- E-commerce 플랫폼
- 금융 서비스
- 대규모 백엔드 시스템

---

#### 2. Loopers 방식 (Domain + JPA) 선택 기준

**추천하는 경우**:
- ✅ 중소규모 프로젝트
- ✅ 단기 개발 (1~3개월)
- ✅ JPA 외 다른 영속성 사용 안 함
- ✅ 빠른 MVP 개발

**예시 프로젝트**:
- 사내 관리 도구
- 중소기업 웹사이트
- MVP 프로토타입

---

#### 3. 단일 객체 방식 선택 기준

**추천하는 경우**:
- ✅ 프로토타입
- ✅ 학습용 토이 프로젝트
- ✅ 간단한 CRUD만

**예시 프로젝트**:
- 개인 블로그
- To-Do 앱
- 간단한 게시판

---

## 면접 대비 답변

### Q1. "User.java는 DTO인가요?"

**답변**:

"아니요, User.java는 DTO가 아니라 **Domain Entity**입니다.

**DTO와 Domain Entity의 핵심 차이**는:
1. DTO는 **순수 데이터 전송**만 담당하고 로직이 없습니다
2. Domain Entity는 **비즈니스 로직을 포함**합니다

예를 들어, User.java에는 `changeEmail()`, `activate()`, `suspend()` 같은 비즈니스 로직이 있습니다. 또한 `validateEmail()` 같은 불변식 검증도 포함되어 있습니다.

반면 UserRequest나 UserResponse 같은 DTO는 순수하게 데이터 전송만 담당하고, 비즈니스 로직이 전혀 없습니다."

---

### Q2. "왜 Domain과 Infrastructure Entity를 분리하나요?"

**답변**:

"Domain의 **프레임워크 독립성**을 유지하기 위해서입니다.

**세 가지 주요 이유**가 있습니다:

1. **테스트 용이성**: Domain 로직을 JPA나 Hibernate 없이 순수 Java로 테스트할 수 있습니다. 이렇게 하면 테스트가 빠르고 간단해집니다.

2. **변경 격리**: DB 스키마가 변경되어도 Domain은 영향을 받지 않습니다. Infrastructure만 수정하면 됩니다.

3. **영속성 교체 용이성**: 예를 들어 JPA에서 MongoDB로 전환하거나, Redis를 추가하더라도 Domain 로직은 변경할 필요가 없습니다.

이는 Hexagonal Architecture의 핵심 원칙인 **'Domain을 중심에 두고 외부 의존성을 격리한다'**를 구현한 것입니다."

---

### Q3. "변환 코드가 많아서 복잡한데, 꼭 필요한가요?"

**답변**:

"변환 코드는 초기에는 번거롭지만, **장기적으로 큰 이점**이 있습니다.

**실무 경험상**:
1. **초기 비용**: 변환 코드 작성에 추가 시간 소요 (약 20% 증가)
2. **장기 이익**:
   - 변경 시 영향 범위가 명확히 격리됨
   - 테스트 시간 단축 (Domain 단위 테스트가 빠름)
   - 리팩토링 용이 (한 레이어만 변경 가능)

**트레이드오프**:
- 소규모 프로젝트나 MVP에서는 오버엔지니어링일 수 있습니다
- 하지만 포트폴리오나 장기 프로젝트에서는 투자 가치가 있습니다

저희 LookMarket 프로젝트는 포트폴리오 목적이고 대기업 면접을 준비하기 때문에, **아키텍처 강제**와 **설계 역량 어필**을 위해 완전 분리 방식을 선택했습니다."

---

### Q4. "실무에서는 어떤 방식을 주로 사용하나요?"

**답변**:

"실무에서는 **프로젝트 규모와 목적에 따라 다릅니다**:

**대기업 / 대규모 시스템**:
- 완전 분리 방식 (Domain + Infrastructure 분리)
- 예: 쿠팡, 배민 같은 대규모 커머스
- 이유: 장기 유지보수, 복잡한 비즈니스 로직, 팀 규모 큼

**중소기업 / 중규모 시스템**:
- Domain + JPA 혼합 방식 (실용적 접근)
- 예: 사내 관리 시스템, B2B 서비스
- 이유: 빠른 개발, 적절한 복잡도

**스타트업 / MVP**:
- 단일 객체 또는 간단한 분리
- 예: 초기 프로토타입
- 이유: 빠른 검증, 최소 복잡도

제가 LookMarket에서 완전 분리 방식을 선택한 이유는, **대기업 취업을 목표**로 하고 있어서 실무 수준의 아키텍처 경험을 보여주고 싶었기 때문입니다."

---

## 참고 자료

- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Martin Fowler - Anemic Domain Model](https://martinfowler.com/bliki/AnemicDomainModel.html)
- [LookMarket: Loopers-vs-LookMarket-비교분석.md](./Loopers-vs-LookMarket-비교분석.md)

---

**마지막 업데이트**: 2025-12-16
**관리자**: LookMarket 개발팀
