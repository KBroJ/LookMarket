# 아키텍처 강제 규칙 - 상세 예시

> 이 문서는 CLAUDE.md의 "아키텍처 강제 규칙"에 대한 상세 예시와 설명을 제공합니다.
> AI 도구가 코드를 생성할 때 참고하거나, 개발자가 구체적인 패턴을 확인할 때 사용합니다.

## 목차
1. [레이어 간 의존성 방향](#1-레이어-간-의존성-방향)
2. [Import 금지 목록](#2-import-금지-목록)
3. [DDD 패턴 강제 규칙](#3-ddd-패턴-강제-규칙)
4. [Repository 패턴](#4-repository-패턴)
5. [트랜잭션 경계](#5-트랜잭션-경계)
6. [DTO 변환 규칙](#6-dto-변환-규칙)
7. [예외 처리 규칙](#7-예외-처리-규칙)

---

## 1. 레이어 간 의존성 방향

### 허용되는 의존성 방향
```
API → Application → Domain
              ↑
         Infrastructure
```

### ❌ Domain → Infrastructure (가장 흔한 실수)

**잘못된 예: Domain이 JPA에 의존**
```java
package com.lookmarket.domain.user;

import javax.persistence.Entity;  // 금지!
import javax.persistence.Id;      // 금지!

@Entity  // Domain에 JPA 애노테이션 금지
public class User {
    @Id
    private Long id;
}
```

**올바른 예: Domain은 순수 Java 클래스**
```java
package com.lookmarket.domain.user;

public class User {
    private Long id;
    private String email;

    // 순수 비즈니스 로직만
    public void changeEmail(String newEmail) {
        if (newEmail == null || !newEmail.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
        this.email = newEmail;
    }
}
```

### ❌ API → Infrastructure (직접 의존 금지)

**잘못된 예: Controller가 JpaRepository 직접 사용**
```java
package com.lookmarket.api.user;

import com.lookmarket.infrastructure.user.JpaUserRepository;  // 금지!

@RestController
public class UserController {
    private final JpaUserRepository userRepository;  // 금지!

    public UserController(JpaUserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

**올바른 예: Controller는 Application Service만 사용**
```java
package com.lookmarket.api.user;

import com.lookmarket.application.user.UserService;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;  // 올바름

    @GetMapping("/api/v1/users/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        User user = userService.getUser(id);
        return UserResponse.from(user);
    }
}
```

### ❌ Application → API (역방향 금지)

**잘못된 예: Service가 DTO 사용**
```java
package com.lookmarket.application.user;

import com.lookmarket.api.user.dto.UserResponse;  // 금지!

public class UserService {
    public UserResponse getUser(Long id) {  // 금지!
        // Application은 Domain 객체만 반환
    }
}
```

**올바른 예: Service는 Domain 객체 반환**
```java
package com.lookmarket.application.user;

import com.lookmarket.domain.user.User;
import com.lookmarket.domain.user.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User getUser(Long id) {  // 올바름
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
}
```

---

## 2. Import 금지 목록

### Domain 레이어 (lookmarket-domain) - 가장 엄격

**❌ 절대 금지되는 import**
```java
import javax.persistence.*;           // JPA
import org.springframework.data.*;    // Spring Data
import org.springframework.stereotype.*;  // Spring 애노테이션
import com.lookmarket.infrastructure.*;   // Infrastructure 레이어
import com.lookmarket.application.*;      // Application 레이어
import com.lookmarket.api.*;              // API 레이어
```

**✅ 허용되는 import**
```java
import java.time.*;                   // Java 표준 라이브러리
import java.util.*;                   // Java 표준 라이브러리
import java.math.*;                   // BigDecimal 등
import lombok.*;                      // Lombok (불변 객체용)
import com.lookmarket.domain.*;       // 같은 Domain 레이어 내부
```

### Infrastructure 레이어 (lookmarket-infrastructure)

**❌ 금지되는 import**
```java
import com.lookmarket.api.*;              // API 레이어
import com.lookmarket.application.*;      // Application 레이어 (Service 직접 의존 금지)
```

**✅ 허용되는 import**
```java
import javax.persistence.*;               // JPA (Infrastructure에서만 허용)
import org.springframework.data.jpa.*;    // Spring Data JPA
import com.lookmarket.domain.*;           // Domain 레이어 (포트 구현)
import org.apache.kafka.*;                // Kafka
import org.elasticsearch.*;               // Elasticsearch
```

### Application 레이어 (lookmarket-application)

**❌ 금지되는 import**
```java
import com.lookmarket.infrastructure.*;   // Infrastructure 구현체 직접 의존 금지
import com.lookmarket.api.*;              // API 레이어
import javax.persistence.*;               // JPA (Application은 JPA 몰라야 함)
```

**✅ 허용되는 import**
```java
import com.lookmarket.domain.*;           // Domain 레이어
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
```

### API 레이어 (lookmarket-api)

**❌ 금지되는 import**
```java
import com.lookmarket.infrastructure.*;   // Infrastructure 직접 의존 금지
import javax.persistence.*;               // JPA
```

**⚠️ 주의가 필요한 import**
```java
import com.lookmarket.domain.user.User;   // Domain 엔티티 직접 노출 금지 (DTO 변환 필수)
```

**✅ 허용되는 import**
```java
import com.lookmarket.application.*;      // Application Service
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
```

---

## 3. DDD 패턴 강제 규칙

### 애그리게이트 간 참조는 ID만 허용

**❌ 잘못된 예: 다른 애그리게이트 객체 직접 참조**
```java
public class Order {
    private User user;           // 금지!
    private Product product;     // 금지!

    public Order(User user, Product product) {
        this.user = user;
        this.product = product;
    }
}
```

**문제점**:
- Order가 User와 Product의 생명주기에 강하게 결합
- 트랜잭션 경계가 불명확
- N+1 쿼리 문제 발생 가능

**✅ 올바른 예: ID로만 참조**
```java
public class Order {
    private Long userId;         // 올바름
    private Long productId;      // 올바름

    public Order(Long userId, Long productId) {
        this.userId = userId;
        this.productId = productId;
    }
}
```

**장점**:
- 애그리게이트 간 느슨한 결합
- 트랜잭션 경계 명확
- 필요 시 Application 레이어에서 조회

### Value Object는 불변 (Immutable)

**❌ 잘못된 예: Setter 존재**
```java
public class Money {
    private BigDecimal amount;
    private String currency;

    public void setAmount(BigDecimal amount) {  // 금지!
        this.amount = amount;
    }

    public void setCurrency(String currency) {  // 금지!
        this.currency = currency;
    }
}
```

**✅ 올바른 예 1: Record 사용 (Java 14+)**
```java
public record Money(BigDecimal amount, String currency) {

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }
}
```

**✅ 올바른 예 2: Lombok @Value 사용**
```java
import lombok.Value;

@Value
public class Address {
    String city;
    String street;
    String zipCode;

    public Address withCity(String newCity) {
        return new Address(newCity, this.street, this.zipCode);
    }
}
```

### 도메인 이벤트는 Domain 레이어에서 정의

**❌ 잘못된 예: Infrastructure에 이벤트 정의**
```java
package com.lookmarket.infrastructure.user;

public class UserCreatedEvent {  // 금지! (Infrastructure 레이어)
    private Long userId;
    private String email;
}
```

**✅ 올바른 예: Domain에 이벤트 정의**
```java
package com.lookmarket.domain.user;

public record UserCreatedEvent(
    String eventId,
    Long userId,
    String email,
    LocalDateTime occurredAt
) {
    public static UserCreatedEvent of(User user) {
        return new UserCreatedEvent(
            UUID.randomUUID().toString(),
            user.getId(),
            user.getEmail(),
            LocalDateTime.now()
        );
    }
}
```

**발행 위치 (Application 레이어)**:
```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public User registerUser(String email, String password) {
        User user = User.create(email, password);
        User savedUser = userRepository.save(user);

        // 이벤트 발행
        eventPublisher.publishEvent(UserCreatedEvent.of(savedUser));

        return savedUser;
    }
}
```

---

## 4. Repository 패턴

### Repository 인터페이스는 Domain에 정의

**✅ Domain 레이어에 인터페이스 정의**
```java
package com.lookmarket.domain.user;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    void delete(User user);
}
```

### Repository 구현체는 Infrastructure에 위치

**✅ Infrastructure 레이어에 구현**
```java
package com.lookmarket.infrastructure.user;

import com.lookmarket.domain.user.User;
import com.lookmarket.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {
    private final UserJpaRepository jpaRepository;
    private final UserMapper mapper;

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        UserEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
            .map(mapper::toDomain);
    }
}
```

### Spring Data JPA Repository는 직접 노출 금지

**❌ 잘못된 예: JpaRepository를 Domain 인터페이스로 사용**
```java
package com.lookmarket.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {  // 금지!
    // Domain이 Spring Data JPA에 의존하게 됨
}
```

**✅ 올바른 예: Adapter 패턴으로 래핑**
```java
// Infrastructure 내부에서만 사용
package com.lookmarket.infrastructure.user;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
}

// Domain Repository 구현
@Repository
public class UserRepositoryAdapter implements UserRepository {
    private final UserJpaRepository jpaRepository;

    // 매핑 로직
}
```

---

## 5. 트랜잭션 경계

### 트랜잭션은 Application 레이어에서만

**❌ 잘못된 예: Domain에 트랜잭션**
```java
package com.lookmarket.domain.user;

import org.springframework.transaction.annotation.Transactional;

@Transactional  // 금지! (Domain은 Spring 몰라야 함)
public class User {
    public void changePassword(String newPassword) {
        // ...
    }
}
```

**✅ 올바른 예: Application Service에 트랜잭션**
```java
package com.lookmarket.application.user;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public User changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        user.changePassword(newPassword);  // Domain 로직 호출

        return userRepository.save(user);
    }
}
```

**트랜잭션 범위 설정 원칙**:
- **읽기 전용 트랜잭션**: `@Transactional(readOnly = true)`
- **쓰기 트랜잭션**: `@Transactional`
- **트랜잭션 전파**: 필요 시 `propagation` 설정
- **롤백 규칙**: 비즈니스 예외는 명시적 롤백 설정

```java
@Transactional(readOnly = true)
public List<User> getUsers() {
    return userRepository.findAll();
}

@Transactional(rollbackFor = Exception.class)
public void registerUser(UserRegisterRequest request) {
    // 모든 예외에서 롤백
}
```

---

## 6. DTO 변환 규칙

### API DTO는 Domain 객체 직접 노출 금지

**❌ 잘못된 예: Domain 객체 그대로 반환**
```java
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {  // 금지!
    return userService.getUser(id);
}
```

**문제점**:
- Domain 내부 구조가 API에 노출됨
- Domain 변경 시 API 계약이 깨짐
- 순환 참조 문제 발생 가능
- 민감 정보(비밀번호 등) 노출 위험

**✅ 올바른 예: DTO로 변환**
```java
@GetMapping("/users/{id}")
public UserResponse getUser(@PathVariable Long id) {
    User user = userService.getUser(id);
    return UserResponse.from(user);  // DTO 변환
}
```

**DTO 정의 예시**:
```java
package com.lookmarket.api.user.dto;

public record UserResponse(
    Long id,
    String email,
    String name,
    UserStatus status,
    LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getStatus(),
            user.getCreatedAt()
        );
    }
}
```

### Domain 객체는 API 레이어에서 import 금지

**❌ 잘못된 예: Controller가 Domain 객체 import**
```java
package com.lookmarket.api.user;

import com.lookmarket.domain.user.User;  // 금지!

@RestController
public class UserController {
    public User getUser() {  // 금지!
        // ...
    }
}
```

**예외적으로 허용되는 경우**:
- Domain의 Enum 타입 (예: `UserStatus`, `OrderStatus`)
- Domain의 Value Object (예: `Money`, `Address`) - DTO 변환에 사용
- Domain Exception (GlobalExceptionHandler에서만)

---

## 7. 예외 처리 규칙

### Domain Exception은 Domain 레이어에 정의

**✅ Domain 레이어에 비즈니스 예외 정의**
```java
package com.lookmarket.domain.user;

public class UserNotFoundException extends RuntimeException {
    private final Long userId;

    public UserNotFoundException(Long userId) {
        super("User not found: " + userId);
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
```

```java
package com.lookmarket.domain.order;

public class InsufficientStockException extends RuntimeException {
    private final Long productId;
    private final int requested;
    private final int available;

    public InsufficientStockException(Long productId, int requested, int available) {
        super(String.format("Insufficient stock for product %d: requested %d, available %d",
            productId, requested, available));
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }
}
```

### Infrastructure Exception은 Application에서 변환

**✅ Application에서 Infrastructure 예외 → Domain 예외로 변환**
```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User getUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public User registerUser(String email, String password) {
        try {
            User user = User.create(email, password);
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Infrastructure 예외 → Domain 예외로 변환
            throw new DuplicateEmailException(email);
        }
    }
}
```

### Global Exception Handler (API 레이어)

**✅ API 레이어에서 통합 예외 처리**
```java
package com.lookmarket.api.common;

import com.lookmarket.domain.user.UserNotFoundException;
import com.lookmarket.domain.order.InsufficientStockException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleUserNotFound(UserNotFoundException e) {
        return new ErrorResponse(
            "USER_NOT_FOUND",
            e.getMessage(),
            HttpStatus.NOT_FOUND.value()
        );
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInsufficientStock(InsufficientStockException e) {
        return new ErrorResponse(
            "INSUFFICIENT_STOCK",
            e.getMessage(),
            HttpStatus.CONFLICT.value()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(Exception e) {
        return new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred",
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
    }
}
```

---

## 요약

이 문서의 모든 규칙은 **Hexagonal Architecture**와 **DDD 원칙**을 준수하기 위한 것입니다.

**핵심 원칙 3가지**:
1. **Domain은 독립적**: 어떤 레이어나 프레임워크에도 의존하지 않음
2. **의존성은 안쪽으로**: Infrastructure → Domain, API → Application → Domain
3. **명확한 경계**: 레이어 간 통신은 정의된 인터페이스(포트)를 통해서만
