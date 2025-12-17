# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 리포지토리에서 작업할 때 참고하는 가이드입니다.

## 프로젝트 개요

LookMarket은 Java 21과 Spring Boot 3.3.x 기반의 멀티 브랜드 패션/뷰티 통합 커머스 플랫폼입니다. 무신사, 29CM, 카카오스타일과 같은 B2C 커머스 시스템을 목표로 하며, 이벤트 기반 아키텍처, 분산 시스템, 고성능 검색 등 고급 백엔드 개발 역량을 보여줍니다.

**핵심 기술 스택**: Java 21 (Virtual Threads), Spring Boot 3.3.x, MySQL 8.0, Redis 7.x, Elasticsearch 8.x, Apache Kafka 3.6.x, QueryDSL 5.x, React 18 + TypeScript

## 구현 전략 및 현재 상태

### 개발 방식
- **수직적 슬라이스 (Vertical Slice) + 도메인 완성 우선**
- 각 도메인: **기본 구현 → 테스트 → 프론트엔드 연동** 순서로 완성
- **User 도메인을 레퍼런스로 삼아 Product, Order 도메인 설계/구현**
- **고급 기술(Redis, Elasticsearch, Kafka)은 모든 도메인 완성 후 마지막에 적용**
- 이벤트 처리: Phase 1-3은 Spring Event 사용, Phase 5에서 Kafka로 전환

### 현재 진행 상황
- **완료**: Phase 0 (환경 검증), Phase 1 (User 기본 구현), Phase 1-B (JWT 인증)
- **다음 단계**: Phase 1-C (User 테스트 보강)
- **상세 진행 현황**: [docs/project/TODO.md](docs/project/TODO.md) 참조

### Phase별 로드맵

**기본 기능 구현 (고급 기술 없이 동작하는 커머스)**:
- **Phase 1**: User 도메인 (기본 구현 ✅ → JWT ✅ → 테스트 → 프론트 연동)
- **Phase 2**: Product 도메인 (기본 CRUD → 테스트 → 프론트 연동)
- **Phase 3**: Order 도메인 (기본 주문/재고 → 테스트 → 프론트 연동)
- **Phase 4**: 프론트엔드 통합 (전체 플로우 시연)

**고급 기술 통합 (학습 후 적용)**:
- **Phase 5**: 고급 기술 통합
  - Redis: 캐싱, Refresh Token, 분산 락
  - Elasticsearch: 전문 검색, 자동완성
  - Kafka: 이벤트 기반, Saga, CDC, SSE

## 아키텍처

### Hexagonal Architecture (포트 & 어댑터)

명확한 계층 분리를 가진 멀티 모듈 구조:

```
lookmarket/
├── lookmarket-api/              # Presentation Layer (Controllers, DTOs, Security Config)
├── lookmarket-application/      # Application Service (Use Cases, Facades)
├── lookmarket-domain/           # Domain Model (Entities, Value Objects, Domain Events)
├── lookmarket-infrastructure/   # Infrastructure (JPA Repositories, Kafka, Redis, Elasticsearch)
└── lookmarket-common/           # Shared Utilities
```

**의존성 흐름**: API → Application → Domain ← Infrastructure
- Domain 레이어는 다른 레이어에 대한 의존성이 없음 (순수 비즈니스 로직)
- Infrastructure는 Domain에서 정의한 포트를 구현
- API 레이어는 Application 서비스를 통해 유즈케이스를 조율

### 모듈별 책임

**lookmarket-domain**:
- 핵심 비즈니스 엔티티 (User, Product, Order, Inventory)
- 도메인 이벤트 (OrderCreatedEvent, StockRestoredEvent)
- 비즈니스 규칙 및 불변식 (예: Order.create()는 재고 가용성 검증)
- Repository 인터페이스 (포트)

**lookmarket-infrastructure**:
- JPA 엔티티 및 QueryDSL 리포지토리
- Kafka 프로듀서/컨슈머
- Redis 캐시 구현
- Elasticsearch 인덱싱 및 검색
- Flyway 데이터베이스 마이그레이션 (src/main/resources/db/migration/)

**lookmarket-application**:
- 여러 도메인 작업을 조율하는 애플리케이션 서비스
- 트랜잭션 경계
- 이벤트 발행 오케스트레이션

**lookmarket-api**:
- REST 컨트롤러
- DTO 및 요청/응답 매핑
- Spring Security 설정 (JWT 인증)
- Swagger/OpenAPI 문서
- 예외 핸들러

---

## 아키텍처 강제 규칙 (Architecture Enforcement Rules)

> **중요**: 이 규칙들은 반드시 준수해야 하는 **강제 제약사항**입니다.
>
> **상세 예시 및 설명**: [docs/architecture/ENFORCEMENT_RULES.md](docs/architecture/ENFORCEMENT_RULES.md)

### 핵심 원칙

| 규칙 | 설명 | 예시 |
|------|------|------|
| **Domain 독립성** | Domain은 어떤 레이어/프레임워크에도 의존하지 않음 | ✅ 순수 Java / ❌ import javax.persistence.* |
| **의존성 방향** | Infrastructure → Domain, API → Application → Domain | ✅ Hexagonal Architecture / ❌ Domain → Infrastructure |
| **레이어 격리** | 각 레이어는 정해진 책임만 가짐 | ✅ Domain = 비즈니스 로직 / ❌ Domain에 @Transactional |

### A. Domain Model 규칙

| 규칙 | 설명 | 허용 (✅) | 금지 (❌) |
|------|------|----------|----------|
| **Behavior-rich Entities** | 엔티티는 행위(Behavior)를 가져야 함 | `user.changeEmail(newEmail)` | `user.setEmail(email)` |
| **Aggregate References** | 애그리게이트 간 참조는 ID만 | `Order(userId, productId)` | `Order(User user, Product product)` |
| **Value Object Immutability** | Value Object는 불변 | `record Money(BigDecimal amount)` | `money.setAmount(100)` |
| **Domain Event in Domain** | 도메인 이벤트는 Domain 레이어에 정의 | `domain/UserCreatedEvent` | `infrastructure/UserCreatedEvent` |

### B. Ports & Adapters 규칙

| 규칙 | 설명 | 허용 (✅) | 금지 (❌) |
|------|------|----------|----------|
| **Repository Interface** | Repository 인터페이스는 Domain에 | `domain/UserRepository.java` | `infrastructure/UserRepository.java` |
| **Repository Implementation** | Repository 구현체는 Infrastructure에 | `infrastructure/JpaUserRepository` | `domain/JpaUserRepository` |
| **No Direct JPA in Domain** | Domain에서 Spring Data JPA 직접 사용 금지 | `UserRepository { User save(); }` | `UserRepository extends JpaRepository` |

### C. Service Layer 규칙

| 규칙 | 설명 | 허용 (✅) | 금지 (❌) |
|------|------|----------|----------|
| **Orchestration Only** | Application Service는 오케스트레이션만 | `user.changeEmail()` 호출 | Service에서 직접 validation |
| **Transaction Boundary** | 트랜잭션은 Application 레이어에만 | `@Transactional` in Service | `@Transactional` in Domain |
| **Return Domain Objects** | Service는 Domain 객체 반환 | `User getUser()` | `UserResponse getUser()` (DTO는 API에서) |

### D. Dependency Inversion 규칙

| 규칙 | 설명 | 허용 (✅) | 금지 (❌) |
|------|------|----------|----------|
| **Infrastructure → Domain** | Infrastructure가 Domain에 의존 | `import com.lookmarket.domain.*` | `domain에서 import javax.persistence.*` |
| **API → Application** | API가 Application에 의존 | `import com.lookmarket.application.*` | `import com.lookmarket.infrastructure.*` |
| **DTO Conversion** | API에서 Domain 직접 노출 금지 | `UserResponse.from(user)` | `return user` (Domain 객체) |

---

## 코드 품질 규칙

> **상세 가이드**: [docs/architecture/ENFORCEMENT_RULES.md](docs/architecture/ENFORCEMENT_RULES.md)

| 카테고리 | 규칙 | 기준 |
|---------|------|------|
| **네이밍** | 명확하고 의미 있는 이름 | 메서드: `getUser`, `createOrder` / 변수: `userId`, `orderItems` |
| **크기 제한** | 클래스/메서드 길이 제한 | 클래스 300줄, 메서드 50줄, 파라미터 3개 이내 |
| **복잡도** | Cyclomatic Complexity 제한 | 10 이하 (복잡한 로직은 메서드 분리) |
| **주석** | 복잡한 비즈니스 로직에만 작성 | "왜(Why)"를 설명, "무엇(What)"은 코드로 |
| **매직 넘버** | 하드코딩 금지 | 상수로 정의 또는 설정 파일 사용 |
| **Null 처리** | Null 반환 금지 | `Optional` 사용 또는 예외 발생 |
| **로깅** | 민감 정보 로깅 금지 | 비밀번호, 카드번호 마스킹 처리 |

---

## RESTful API 설계 규칙

> **중요**: 모든 API는 RESTful 원칙을 준수해야 합니다.
>
> **상세 가이드**: [docs/architecture/decisions/ADR-002-RESTful-API-설계-원칙.md](docs/architecture/decisions/ADR-002-RESTful-API-설계-원칙.md)
>
> **학습 자료**: [docs/learning/251217_RESTful-API-설계-완벽-가이드.md](docs/learning/251217_RESTful-API-설계-완벽-가이드.md)

### API 버저닝

**URL Path 버저닝** 채택: `/api/v1/...`

```
/api/v1/users
/api/v1/products
/api/v1/orders
```

### URL 설계 원칙

| 원칙 | 규칙 | 좋은 예 | 나쁜 예 |
|------|------|---------|---------
| 리소스 중심 | URL은 명사(리소스) | `/users` | `/getUsers` |
| HTTP 메서드 | 행위는 메서드로 | `DELETE /users/1` | `POST /deleteUser` |
| 복수형 | 컬렉션은 복수 | `/users` | `/user` |
| 계층 구조 | 소속 관계 표현 | `/users/1/orders` | `/userOrders` |
| 소문자 | URL은 소문자 | `/users` | `/Users` |
| 케밥 케이스 | 단어 연결 시 | `/order-items` | `/orderItems` |

### HTTP 메서드 사용

| 메서드 | 용도 | 멱등성 | 예시 |
|--------|------|--------|------|
| GET | 조회 | O | `GET /users/1` |
| POST | 생성 | X | `POST /users` |
| PUT | 전체 수정 | O | `PUT /users/1` |
| PATCH | 부분 수정 | O | `PATCH /users/1` |
| DELETE | 삭제 | O | `DELETE /users/1` |

### 상태 변경 패턴 (Sub-resource)

**동사 대신 상태를 리소스로 취급**:
```
# 나쁜 예 (동사 사용)
POST /users/1/activate
POST /users/1/suspend

# 좋은 예 (상태 리소스)
PATCH /users/1/status
Body: { "status": "ACTIVE" | "SUSPENDED" | "INACTIVE" }
```

### HTTP 상태 코드

| 코드 | 의미 | 사용 시점 |
|------|------|----------|
| 200 | OK | 조회, 수정 성공 |
| 201 | Created | 생성 성공 |
| 204 | No Content | 삭제 성공 |
| 400 | Bad Request | 잘못된 요청 (validation 실패) |
| 401 | Unauthorized | 인증 필요 |
| 403 | Forbidden | 권한 없음 |
| 404 | Not Found | 리소스 없음 |
| 409 | Conflict | 충돌 (중복 이메일 등) |
| 500 | Internal Server Error | 서버 오류 |

### 응답 형식

#### 성공 응답
```json
// 단일 리소스
{
  "id": 1,
  "email": "user@example.com",
  "name": "홍길동"
}

// 컬렉션 (페이징)
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

#### 에러 응답
```json
{
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "사용자를 찾을 수 없습니다.",
    "details": { "userId": 999 }
  },
  "timestamp": "2025-12-17T10:30:00Z",
  "path": "/api/v1/users/999"
}
```

### 예외 허용

복잡한 경우 RESTful 원칙의 예외 허용:
- 복잡한 검색: `POST /api/v1/products/search` (body에 조건)
- 벌크 작업: `POST /api/v1/products/bulk-delete`

---

## 주요 개발 명령어

### 인프라 환경 구성

모든 인프라 서비스 시작 (MySQL, Redis, Elasticsearch, Kafka):
```bash
cd docker
docker-compose up -d
```

모든 서비스 중지:
```bash
cd docker
docker-compose down
```

특정 서비스 로그 확인:
```bash
docker-compose logs -f kafka        # Kafka 로그
docker-compose logs -f mysql        # MySQL 로그
```

### 빌드 & 실행

전체 프로젝트 빌드:
```bash
./gradlew clean build
```

특정 모듈 빌드:
```bash
./gradlew :lookmarket-domain:build
./gradlew :lookmarket-api:build
```

애플리케이션 실행 (API 모듈):
```bash
./gradlew :lookmarket-api:bootRun
```

### 테스트

전체 테스트 실행:
```bash
./gradlew test
```

특정 모듈 테스트:
```bash
./gradlew :lookmarket-domain:test
./gradlew :lookmarket-infrastructure:test
```

단일 테스트 클래스 실행:
```bash
./gradlew :lookmarket-domain:test --tests "com.lookmarket.domain.order.OrderTest"
```

통합 테스트 실행 (Testcontainers 필요):
```bash
./gradlew :lookmarket-infrastructure:test --tests "*IntegrationTest"
```

### 데이터베이스 마이그레이션

Flyway 마이그레이션 수동 실행:
```bash
./gradlew :lookmarket-infrastructure:flywayMigrate
```

마이그레이션 상태 확인:
```bash
./gradlew :lookmarket-infrastructure:flywayInfo
```

### 프론트엔드

프론트엔드는 `lookmarket-frontend/`에 위치 (React + TypeScript + Vite):
```bash
cd lookmarket-frontend
npm install
npm run dev          # 개발 서버 (http://localhost:5173)
npm run build        # 프로덕션 빌드
```

## 핵심 아키텍처 패턴

### 1. 동시성 제어

**낙관적 락 (Optimistic Locking)** - 일반 재고 관리:
- 도메인 엔티티에 `@Version` 애노테이션 사용
- 재시도 로직으로 동시 재고 차감 처리
- 예시: domain 레이어의 `Inventory` 엔티티

**분산 락 (Distributed Locking, Redis)** - 한정판 상품:
- infrastructure 레이어의 `RedisLockService`
- 높은 수요 시나리오에서 경쟁 상태 방지
- 원자적 락 해제를 위해 Lua 스크립트 사용

### 2. 이벤트 기반 아키텍처 (Kafka)

4가지 주요 패턴 구현:

**Saga Pattern** - 주문/결제/배송의 분산 트랜잭션:
- OrderSagaOrchestrator가 다단계 워크플로우 조율
- 실패 시 보상 트랜잭션 (예: 재고 복원)

**CDC (Change Data Capture)** - MySQL → Elasticsearch 동기화:
- Debezium 커넥터가 데이터베이스 변경 캡처
- Elasticsearch 인덱스 자동 업데이트
- 토픽: `mysql.lookmarket.products`

**이벤트 기반 알림** - SSE를 통한 실시간 알림:
- 재입고 이벤트가 사용자 알림 트리거
- Server-Sent Events (SSE)로 실시간 푸시

**Kafka Streams** - 실시간 분석:
- 주문 통계를 위한 윈도우 집계
- 상품 판매 순위

### 3. 검색 아키텍처 (Elasticsearch)

- **Nori 분석기** - 한글 형태소 분석
- **멀티 필드 검색** - 필드 부스팅 (상품명, 브랜드, 설명)
- **집계 (Aggregations)** - 패싯 검색 (가격 범위, 브랜드, 카테고리)
- **Redis 캐싱** - 검색 결과 캐싱 (5분 TTL)

### 4. 도메인 주도 설계 (DDD)

**애그리게이트**: Order, Product, Inventory가 애그리게이트 루트
- 각 애그리게이트는 자체 일관성 경계 유지
- 애그리게이트 간 참조는 직접 객체 참조가 아닌 ID 사용

**값 객체 (Value Objects)**: Money, Address, ProductOption
- 도메인 개념을 나타내는 불변 객체

**도메인 이벤트**: OrderCreatedEvent, PaymentCompletedEvent
- 성공적인 도메인 작업 후 발행
- 이벤트 핸들러가 비동기로 소비

## 기술별 가이드라인

### Java 21 기능

I/O 바운드 작업에 **Virtual Threads** 사용:
- Virtual thread 실행자로 구성된 Kafka 리스너
- `CompletableFuture`를 사용한 비동기 주문 처리

DTO 및 불변 데이터에 **Records** 사용:
```java
public record ProductSearchRequest(String keyword, Long categoryId, List<Long> brandIds) {}
```

적절한 곳에 **Sequenced Collections** 사용:
```java
List<Product> products = getProducts();
Product latest = products.getLast();  // Java 21 API
```

### QueryDSL

QueryDSL은 `lookmarket-infrastructure`에 설정됨:
- 생성된 Q-클래스 위치: `build/generated/sources/annotationProcessor/java/main`
- 타입 안전성이 필요한 복잡한 쿼리에 QueryDSL 사용
- 예시: 다중 조건 상품 검색, 동적 필터링

#### N+1 쿼리 방지 패턴

**Fetch Join 사용**:
```java
// N+1 문제 해결: Order → OrderItem → ProductOption 한 번에 조회
public List<Order> findOrdersWithItems(Long userId) {
    return queryFactory
        .selectFrom(order)
        .join(order.items, orderItem).fetchJoin()
        .join(orderItem.productOption, productOption).fetchJoin()
        .join(productOption.product, product).fetchJoin()
        .where(order.userId.eq(userId))
        .fetch();
}
```

**페이징 + Fetch Join** (컬렉션 Fetch Join은 페이징 불가):
```java
// 1. 주문 ID만 페이징 조회
List<Long> orderIds = queryFactory
    .select(order.id)
    .from(order)
    .where(order.userId.eq(userId))
    .offset(pageable.getOffset())
    .limit(pageable.getPageSize())
    .fetch();

// 2. ID로 Fetch Join 조회
List<Order> orders = queryFactory
    .selectFrom(order)
    .join(order.items, orderItem).fetchJoin()
    .where(order.id.in(orderIds))
    .fetch();
```

**동적 WHERE 절 패턴**:
```java
private BooleanExpression categoryEq(Long categoryId) {
    return categoryId != null ? product.category.id.eq(categoryId) : null;
}

private BooleanExpression brandIn(List<Long> brandIds) {
    return brandIds != null && !brandIds.isEmpty()
        ? product.brand.id.in(brandIds)
        : null;
}
```

### Spring Boot 설정

애플리케이션 설정 위치: `lookmarket-api/src/main/resources/application.yml`
- Virtual Threads: `spring.threads.virtual.enabled=true`
- Database: localhost:3306의 `lookmarket` 데이터베이스
- Redis: localhost:6379
- Elasticsearch: localhost:9200
- Kafka: localhost:9092

#### Spring Security JWT 구현

**Security 설정**:
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

**JWT 필터**:
```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            Authentication auth = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

**토큰 갱신 (Refresh Token)**:
- Access Token 만료 시 Refresh Token으로 재발급
- Refresh Token은 Redis에 저장 (7일 TTL)
- 로그아웃 시 Refresh Token을 Redis 블랙리스트에 추가

#### Elasticsearch 인덱스 설정

**인덱스 설정 파일 위치**: `lookmarket-infrastructure/src/main/resources/elasticsearch/`

**네이밍 규칙**: `{index-name}-settings.json`, `{index-name}-mappings.json`

**products 인덱스 매핑 예시**:
```json
{
  "settings": {
    "analysis": {
      "analyzer": {
        "nori": {
          "type": "custom",
          "tokenizer": "nori_tokenizer",
          "filter": ["nori_part_of_speech"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "name": {
        "type": "text",
        "analyzer": "nori",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "brand_name": { "type": "keyword" },
      "category_id": { "type": "long" },
      "base_price": { "type": "long" },
      "rating": { "type": "scaled_float", "scaling_factor": 100 }
    }
  }
}
```

**인덱스 생성 시점**: 애플리케이션 시작 시 `ElasticsearchConfig`에서 자동 생성

**재인덱싱 필요 시점**:
- 필드 타입 변경 (text → keyword)
- 분석기 변경 (standard → nori)
- 매핑 수정 (기존 문서에는 적용 안 됨)

### Testcontainers를 사용한 테스트

통합 테스트는 MySQL, Kafka, Elasticsearch용 Testcontainers 사용:
- 추상 기본 테스트 클래스가 컨테이너 구성
- 성능을 위해 테스트 간 컨테이너 재사용
- 예시: `@Container` 필드와 함께 `@Testcontainers` 애노테이션

#### Testcontainers 초기 설정

**필수 요구사항**:
- Docker Desktop 설치 및 실행 상태 확인
- Windows: WSL2 활성화 필요

**컨테이너 재사용 설정** (`~/.testcontainers.properties`):
```properties
testcontainers.reuse.enable=true
```

**추상 테스트 클래스**:
```java
@SpringBootTest
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withReuse(true);  // 테스트 간 재사용

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
}
```

**주의사항**:
- Docker Desktop이 실행 중이 아니면 `ContainerLaunchException` 발생
- CI/CD 환경에서는 Docker-in-Docker 설정 필요
- 테스트 실행 시간 단축을 위해 `withReuse(true)` 설정 권장

## 개발 워크플로우

### 개발 접근 방식: 수직적 슬라이스 (Vertical Slice)

이 프로젝트는 **수평적 레이어 구현** 대신 **수직적 슬라이스** 방식을 채택합니다.

#### 수직적 슬라이스란?

**정의**: 한 도메인(예: User)의 전체 레이어를 Domain → Application → Infrastructure → API까지 완전히 구현하는 방식

**수평적 접근 vs 수직적 슬라이스**:

| 구분 | 수평적 접근 (Horizontal) | 수직적 슬라이스 (Vertical) |
|------|-------------------------|--------------------------|
| 진행 방식 | Week 1: 모든 도메인 엔티티<br>Week 2: 모든 Repository<br>Week 3: 모든 Service | Week 1-2: User 전체 레이어<br>Week 3-4: Product 전체 레이어<br>Week 5-6: Order 전체 레이어 |
| 검증 시점 | 모든 레이어 완성 후 | 각 도메인 완성 시점마다 |
| 학습 효과 | 레이어별 집중 | 전체 흐름 이해 |
| 테스트 | 후반부에 통합 테스트 | 각 슬라이스마다 E2E 테스트 |

#### 왜 수직적 슬라이스를 선택했는가?

**1. 조기 검증**
- User 도메인 완성 후 실제 API 테스트 가능
- 설계 문제를 조기에 발견하여 다른 도메인에 적용 전 수정
- 예: User에서 N+1 문제 발견 → Product, Order에는 처음부터 올바른 패턴 적용

**2. 학습 효과 (개인 프로젝트 특화)**
- 전체 아키텍처 흐름을 한 사이클로 경험
- Domain → Application → Infrastructure → API의 의존성 방향 체득
- 첫 번째 슬라이스(User)에서 패턴 확립 → 이후 슬라이스에서 재사용

**3. 명확한 진척도**
- User 완료 → Product 완료 → Order 완료로 명확한 성취감
- 각 단계마다 "동작하는 소프트웨어" 결과물
- 포트폴리오 시연 시 단계별 진행 상황 명확히 설명 가능

**4. 테스트 용이성**
- 각 도메인마다 단위 → 통합 → E2E 테스트 작성
- Postman이나 프론트엔드로 실제 동작 확인
- 테스트 패턴을 첫 슬라이스에서 확립 후 재사용

**5. 리스크 감소**
- 도메인 간 의존성 순서로 구현 (User → Product → Order)
- 각 슬라이스 완료 시점에 통합 이슈 해결
- 후반부 도메인은 앞선 슬라이스의 검증된 패턴 활용

#### Hexagonal Architecture와의 호환성

Hexagonal Architecture는 **레이어 분리**를 강조하지 **구현 순서**를 강제하지 않습니다.

- 핵심 원칙: **의존성 방향** (Domain ← Infrastructure, API → Application → Domain)
- Walking Skeleton 방법론과 일치: 한 기능을 end-to-end로 먼저 구현 (얇게) → 점진적으로 확장
- 수직적 슬라이스로 구현하더라도 각 레이어의 의존성 방향만 지키면 아키텍처 원칙 준수

#### 구현 순서 (도메인 완성 우선)

**Phase 1: User 도메인**
```
User 도메인 완성
├── 1-A: 기본 구현 ✅
│   ├── Domain Layer: User 엔티티, UserRole/UserStatus enum
│   ├── Infrastructure Layer: JpaUserRepository, UserEntity
│   ├── Application Layer: UserService
│   └── API Layer: UserController, DTO
├── 1-B: JWT 인증 ✅
│   └── Spring Security, JwtTokenProvider, AuthController
├── 1-C: 테스트 보강 (다음 작업)
│   ├── 통합 테스트 (Testcontainers)
│   └── E2E 테스트 (MockMvc/REST Assured)
└── 1-D: 프론트엔드 연동
    └── 로그인, 회원가입, 프로필 페이지
```

**Phase 2: Product 도메인**
```
Product 도메인 완성 (Elasticsearch 없이)
├── 2-A: 기본 구현 (CRUD, LIKE 검색)
├── 2-B: 테스트
└── 2-C: 프론트엔드 연동
```

**Phase 3: Order 도메인**
```
Order 도메인 완성 (Redis 분산락 없이 낙관적 락만)
├── 3-A: 기본 구현 (주문/재고)
├── 3-B: 테스트
└── 3-C: 프론트엔드 연동
```

**Phase 4: 프론트엔드 통합**
- 전체 플로우 시연 (회원가입 → 상품검색 → 주문)
- UI/UX 완성

**Phase 5: 고급 기술 통합 (학습 & 적용)**
```
고급 기술 학습 후 점진적 적용
├── 5-A: Redis
│   ├── Refresh Token 저장 (메모리 → Redis)
│   ├── 상품 캐싱 (응답 속도 전후 비교)
│   └── 분산 락 (낙관적 락 → Redis 락)
├── 5-B: Elasticsearch
│   ├── 상품 검색 (LIKE → 전문 검색)
│   ├── 자동완성
│   └── 패싯 검색
└── 5-C: Kafka
    ├── Spring Event → Kafka 전환
    ├── Saga Pattern (주문-결제-재고)
    ├── CDC (MySQL → ES 동기화)
    └── SSE (실시간 알림)
```

#### 힌트 기반 학습 모드 (Product, Order 도메인)

> **적용 대상**: Phase 2 (Product), Phase 3 (Order) 백엔드 구현
> **목적**: User 도메인을 레퍼런스로 삼아 직접 구현하며 학습

```
1단계: 힌트 제공
├── 구현할 레이어와 클래스 안내
├── User 도메인 참고 포인트 제시
├── 필요한 필드/메서드 힌트
└── "어떤 비즈니스 메서드가 필요할까요?" 같은 질문으로 사고 유도

2단계: 직접 구현
└── User.java, UserService.java 등을 참고하여 코드 작성

3단계: 리뷰 & 피드백
├── 작성한 코드 검토
├── 개선점 제안
├── 아키텍처 규칙 준수 여부 확인
└── 추가 학습 포인트 설명

4단계: 다음 레이어로 이동
└── Domain → Infrastructure → Application → API 순서로 반복
```

**예시 (Product 도메인 시작 시)**:
```
"이번에는 Product 엔티티를 만들어볼까요?
User.java를 참고해서 Behavior-rich 패턴으로 작성해보세요.

필요한 필드 힌트:
- name, description, basePrice, status
- sellerId (판매자 ID 참조)
- brandId, categoryId (ID로 참조)

생각해볼 질문:
- 상품 가격 변경은 어떤 비즈니스 규칙이 필요할까요?
- 상품 상태(판매중/품절/단종)는 어떻게 전환되어야 할까요?"
```

#### 이벤트 처리 전략

**Phase 1-4: Spring Event 사용**
- 도메인 이벤트를 메모리 내에서 발행/소비 (`@EventListener`)
- Kafka 복잡도 없이 이벤트 기반 패턴 학습
- 장점: 간단하고 빠른 검증, 도메인 이벤트 개념 익히기 좋음

**Phase 5: Kafka 전환**
- Spring Event → Kafka 토픽으로 전환
- 모든 도메인이 완성되어 있으므로 이벤트 구독자 구현 가능
- Saga Pattern, CDC 등 고급 패턴 적용
- **적용 전후 비교**: 동기 vs 비동기 처리 차이 체험

### 새 기능 추가 (수직적 슬라이스 방식)

1. **Domain First**: `lookmarket-domain`에서 엔티티, 값 객체, 비즈니스 규칙 정의
2. **Repository 인터페이스**: domain 레이어에서 repository 포트 정의
3. **Infrastructure**: `lookmarket-infrastructure`에서 JPA 엔티티 및 리포지토리 구현
4. **Application Service**: `lookmarket-application`에서 유즈케이스 오케스트레이션 생성
5. **API Layer**: `lookmarket-api`에 컨트롤러 및 DTO 추가
6. **Tests**: 단위 테스트(domain), 통합 테스트(infrastructure), API 테스트(REST Assured) 작성

**중요**: 각 단계를 순차적으로 완료하되, 한 도메인의 전체 레이어를 완성한 후 다음 도메인으로 이동합니다.

---

### 기능 개발 프로세스 (구현 → 테스트 → 커밋 → PR)

> **핵심 원칙**: **테스트 없는 커밋 금지, 빌드 실패 상태 커밋 금지**

#### 수직적 슬라이스 개발 순서

| 단계 | 작업 내용 | 커밋 단위 |
|------|---------|---------|
| **1. Domain** | 엔티티, Repository 인터페이스, 단위 테스트 | `feat(domain): Add User entity and repository interface` |
| **2. Infrastructure** | JPA 구현체, Flyway 마이그레이션, 통합 테스트 | `feat(infrastructure): Implement JPA UserRepository with Flyway migration` |
| **3. Application** | Application Service, 통합 테스트 | `feat(application): Add user registration and login service` |
| **4. API** | Controller, DTO, E2E 테스트 | `feat(api): Add user registration and login endpoints` |
| **5. PR 생성** | 전체 수직적 슬라이스 완성 후 PR 생성 | `feat: Implement user registration feature` |

**각 단계마다 반드시**:
1. 코드 작성
2. 테스트 작성
3. `./gradlew :모듈명:test` 실행 및 통과 확인
4. `git commit` (Conventional Commits 형식)

---

### 커밋 규칙 (Commit Convention)

#### 커밋 메시지 언어 규칙

- **타입 접두사**: 영어 (Conventional Commits 표준)
- **제목 및 본문**: 한글 (가독성 및 프로젝트 일관성)

```
feat: 회원가입 기능 구현

- 이메일 중복 검증 추가
- BCrypt 비밀번호 암호화 적용
```

#### Atomic Commit 원칙

**1커밋 = 1레이어 완성 + 해당 테스트**

| Type | Scope | 예시 |
|------|-------|------|
| `feat` | `domain`, `infrastructure`, `application`, `api` | `feat(domain): User 엔티티 및 값 객체 추가` |
| `fix` | `domain`, `infrastructure`, `application`, `api` | `fix(api): UserController 유효성 검증 오류 수정` |
| `refactor` | `domain`, `infrastructure`, `application`, `api` | `refactor(application): 사용자 검증 로직 분리` |
| `test` | `domain`, `infrastructure`, `application`, `api` | `test(domain): User 엔티티 엣지 케이스 테스트 추가` |
| `docs` | - | `docs: Phase 1 진행 상황 개발 로그 업데이트` |
| `chore` | - | `chore: Gradle 의존성 업데이트` |

#### 커밋 전 체크리스트

- [ ] `./gradlew build` 성공
- [ ] `./gradlew :모듈명:test` 통과
- [ ] 불필요한 코드 제거 (주석, import 정리)
- [ ] 아키텍처 규칙 준수 확인

**테스트 실패 시**: 수정 후 통과해야만 커밋 (절대 WIP 커밋 금지)

---

### PR (Pull Request) 생성 및 리뷰 규칙

#### PR 생성 시점 및 크기

| 항목 | 기준 |
|------|------|
| **생성 시점** | 수직적 슬라이스 완성 (Domain + Infrastructure + Application + API + Tests) |
| **적정 크기** | 300~500 라인 (테스트 포함) |
| **최대 크기** | 1,000 라인 (초과 시 분할 고려) |

#### PR 제목 및 본문

**제목**: Conventional Commits 형식 (한글)
```
feat: 회원가입 기능 구현
```

**본문**: PR 템플릿 사용 (한글)
```markdown
## Summary
User 도메인 구현: 회원가입, 로그인 기능

## Changes
- Domain: User 엔티티, UserRepository 인터페이스
- Infrastructure: JPA UserRepository 구현, Flyway 마이그레이션
- Application: UserService
- API: UserController

## Test Plan
- [x] 모든 테스트 통과: ./gradlew test

## Self-Review Checklist
- [x] 아키텍처 규칙 준수
- [x] 코드 품질 규칙 준수
- [x] 테스트 커버리지 달성
- [x] docs/project/DEVELOPMENT_LOG.md 업데이트
```

#### PR 라벨 (Labels)

PR 생성 시 적절한 라벨을 붙여 분류합니다.

**타입 라벨** (필수 - 1개 선택):

| 라벨 | 색상 | 용도 |
|------|------|------|
| `feature` | 🟢 녹색 | 새 기능 구현 |
| `bug` | 🔴 빨간색 | 버그 수정 |
| `docs` | 🔵 파란색 | 문서 작업 |
| `refactor` | 🟡 노란색 | 리팩토링 |
| `test` | 🟣 보라색 | 테스트 추가/수정 |

**도메인 라벨** (해당 시 - 복수 선택 가능):

| 라벨 | 용도 |
|------|------|
| `domain:user` | User 도메인 관련 |
| `domain:product` | Product 도메인 관련 |
| `domain:order` | Order 도메인 관련 |

**PR 생성 명령어 예시**:
```bash
gh pr create --title "feat: 회원가입 기능 구현" \
  --label "feature" --label "domain:user" \
  --body "..."
```

#### 셀프 리뷰 체크리스트 (간소화)

| 카테고리 | 확인 항목 |
|---------|---------|
| **아키텍처** | Domain 독립성, 의존성 방향, 레이어별 import 규칙 준수 |
| **코드 품질** | 네이밍, 크기 제한, 복잡도, DRY 원칙 |
| **테스트** | 단위/통합/E2E 테스트 작성 완료, 모든 테스트 통과 |
| **보안** | SQL Injection, XSS, 비밀번호 암호화, 민감 정보 로깅 금지 |
| **문서** | docs/project/DEVELOPMENT_LOG.md 업데이트, API 문서 (필요 시) |

#### PR 머지 후

```bash
git checkout main && git pull origin main
git branch -d feature/user-registration
git push origin --delete feature/user-registration
```

---

### 데이터베이스 스키마 변경

1. `lookmarket-infrastructure/src/main/resources/db/migration/`에 새 Flyway 마이그레이션 생성
2. 네이밍 규칙: `V{version}__{description}.sql` (예: `V1__create_users_table.sql`)
3. 마이그레이션 실행: `./gradlew :lookmarket-infrastructure:flywayMigrate`
4. infrastructure 레이어의 해당 JPA 엔티티 업데이트

#### Flyway 마이그레이션 주의사항

**롤백 불가능한 작업 처리**:
- Flyway는 기본적으로 롤백을 지원하지 않음 (Flyway Pro 제외)
- 데이터 손실 가능성이 있는 작업은 별도 백업 후 실행

**대용량 테이블 ALTER 시**:
```sql
-- 나쁜 예: 대용량 테이블에 직접 ALTER (락 발생)
ALTER TABLE orders ADD COLUMN new_column VARCHAR(50);

-- 좋은 예: 새 테이블 생성 후 데이터 복사
CREATE TABLE orders_new LIKE orders;
ALTER TABLE orders_new ADD COLUMN new_column VARCHAR(50);
INSERT INTO orders_new SELECT *, NULL FROM orders;
RENAME TABLE orders TO orders_old, orders_new TO orders;
DROP TABLE orders_old;
```

**인덱스 추가 시**:
```sql
-- ONLINE DDL 사용으로 락 최소화 (MySQL 8.0+)
CREATE INDEX idx_user_status ON orders(user_id, status) ALGORITHM=INPLACE, LOCK=NONE;
```

**외래 키 제약조건**:
- 외래 키는 성능에 영향을 줄 수 있으므로 신중히 사용
- 애플리케이션 레벨에서 참조 무결성 보장 고려

### 이벤트 처리

새 도메인 이벤트 추가 시:
1. `lookmarket-domain`에서 이벤트 정의 (예: `OrderCancelledEvent`)
2. 상태 변경 후 도메인 서비스에서 이벤트 발행
3. `lookmarket-infrastructure`에서 `@KafkaListener`로 Kafka 리스너 생성
4. `application.yml`에서 토픽명 설정
5. 멱등성 처리 (이벤트는 여러 번 전달될 수 있음)

#### Kafka 리스너 멱등성 보장

**이벤트 ID 기반 중복 처리 방지** (권장):
```java
@KafkaListener(topics = "order-events", groupId = "payment-processor")
public void handleOrderCreated(OrderCreatedEvent event) {
    String eventId = event.getEventId();  // 모든 이벤트는 고유 ID 필요

    // Redis Set으로 중복 체크
    Boolean isNew = redisTemplate.opsForSet()
        .add("processed:events", eventId);

    if (Boolean.FALSE.equals(isNew)) {
        log.warn("Duplicate event ignored: eventId={}", eventId);
        return;
    }

    // TTL 설정 (24시간 후 자동 삭제)
    redisTemplate.expire("processed:events", Duration.ofHours(24));

    // 이벤트 처리 로직
    processOrder(event);
}
```

**데이터베이스 유니크 제약조건 활용**:
```java
@KafkaListener(topics = "order-events")
public void handleOrderCreated(OrderCreatedEvent event) {
    try {
        // 주문 번호에 UNIQUE 제약조건이 있으면 중복 삽입 방지됨
        Order order = Order.create(event);
        orderRepository.save(order);
    } catch (DataIntegrityViolationException e) {
        log.warn("Duplicate order creation ignored: orderNumber={}",
            event.getOrderNumber());
    }
}
```

**Kafka Idempotent Producer 설정**:
```yaml
# application.yml
spring:
  kafka:
    producer:
      enable-idempotence: true  # Exactly-once 시맨틱 (성능 약간 저하)
```

**선택 기준**:
- 간단한 이벤트: Redis Set (빠르고 간편)
- 중요한 트랜잭션: DB 유니크 제약 + Redis 조합
- 완벽한 Exactly-once 필요: Kafka Idempotent Producer + 트랜잭션

## 개발 원칙 및 규칙

### 테스트 코드 작성 의무화

**모든 기능 구현 완료 시 반드시 테스트 코드를 작성해야 합니다.**

#### 테스트 레벨별 요구사항

**1. 단위 테스트 (Unit Tests)**
- **대상**: 도메인 로직, 유틸리티 함수, 값 객체
- **위치**: 각 모듈의 `src/test/java`
- **필수 작성 대상**:
  - 도메인 엔티티의 비즈니스 규칙 (예: `Order.create()`, `Inventory.deduct()`)
  - 값 객체의 불변성 및 유효성 검증
  - 도메인 이벤트 발행 로직
- **도구**: JUnit 5, AssertJ, Mockito
- **예시**:
  ```java
  @Test
  void 재고_차감_성공() {
      // given
      Inventory inventory = new Inventory(productId, 10);

      // when
      inventory.deduct(5);

      // then
      assertThat(inventory.getQuantity()).isEqualTo(5);
  }
  ```

**2. 통합 테스트 (Integration Tests)**
- **대상**: Repository, Kafka 리스너, Elasticsearch 검색, Redis 캐시
- **위치**: `lookmarket-infrastructure/src/test/java`
- **필수 작성 대상**:
  - JPA Repository의 복잡한 쿼리 (QueryDSL 포함)
  - Kafka 메시지 발행 및 소비
  - Elasticsearch 인덱싱 및 검색
  - Redis 캐시 동작 및 분산 락
- **도구**: Testcontainers, Spring Boot Test, @DataJpaTest
- **예시**:
  ```java
  @SpringBootTest
  @Testcontainers
  class ProductRepositoryIntegrationTest {
      @Container
      static MySQLContainer mysql = new MySQLContainer("mysql:8.0");

      @Test
      void 복합조건_상품검색_성공() {
          // 실제 데이터베이스를 사용한 통합 테스트
      }
  }
  ```

**3. API 테스트 (E2E Tests)**
- **대상**: REST API 엔드포인트
- **위치**: `lookmarket-api/src/test/java`
- **필수 작성 대상**:
  - 주요 API 엔드포인트의 정상 플로우
  - 에러 케이스 및 예외 처리
  - 인증/인가 동작
- **도구**: REST Assured, MockMvc, Spring Security Test
- **예시**:
  ```java
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  class OrderApiTest {
      @Test
      void 주문생성_API_성공() {
          given()
              .contentType(ContentType.JSON)
              .body(orderRequest)
          .when()
              .post("/api/v1/orders")
          .then()
              .statusCode(201)
              .body("orderId", notNullValue());
      }
  }
  ```

#### 테스트 커버리지 목표 (Phase별)

**Phase 1 - User 도메인** (Week 1-2):
- 도메인 레이어: 70% 이상
- 인프라스트럭처 레이어: 60% 이상
- API 레이어: 50% 이상
- 목표: 첫 슬라이스에서 테스트 패턴 확립, 이후 재사용

**Phase 2 - Product 도메인** (Week 3-4):
- 도메인 레이어: 75% 이상
- 인프라스트럭처 레이어: 65% 이상
- API 레이어: 55% 이상
- 목표: User에서 확립한 패턴 적용, Elasticsearch 테스트 추가

**Phase 3 - Order 도메인** (Week 5-6):
- 도메인 레이어: 80% 이상
- 인프라스트럭처 레이어: 70% 이상
- API 레이어: 60% 이상
- 목표: 통합 시나리오 테스트 추가 (User-Product-Order 연동)

**Phase 4 - Kafka 통합** (Week 7):
- 이벤트 발행/소비 테스트: 70% 이상
- Saga Pattern 테스트: 80% 이상 (보상 트랜잭션 포함)
- 목표: 이벤트 기반 아키텍처 검증

**목표 설정 이유**: 수직적 슬라이스 방식에서는 각 도메인마다 완전한 테스트 작성이 필요합니다. 첫 슬라이스(User)에서 패턴을 확립하면 이후 슬라이스는 빠르게 진행됩니다.

#### 테스트 작성 시점
- **기능 구현 중**: TDD 방식 권장 (Red → Green → Refactor)
- **기능 커밋 전**: 최소한 단위 테스트 1개 이상 작성 필수
- **PR 생성 전**: 통합 테스트 및 API 테스트 작성 완료
- **기존 코드 수정 시**: 영향받는 테스트 수정 및 추가 테스트 작성

### 학습 중심 개발 문화

**이 프로젝트는 포트폴리오이자 학습 목적입니다. 모든 구현에는 "왜 이렇게 했는지" 설명이 필요합니다.**

#### 결정 사항 설명 원칙

**1. 코드 주석에 의도 명시**
- 복잡한 비즈니스 로직이나 기술적 결정에는 주석으로 이유 설명
- "왜(Why)"를 중심으로 작성 ("무엇(What)"은 코드로 충분)
- 예시:
  ```java
  /**
   * 낙관적 락 대신 분산 락을 사용하는 이유:
   * - 한정판 상품은 동시 접속이 매우 많아 낙관적 락의 재시도가 과도하게 발생
   * - Redis 분산 락으로 요청을 순차 처리하여 데이터베이스 부하 감소
   * - 락 대기 시간(3초)을 설정하여 무한 대기 방지
   */
  public void deductLimitedStock(Long optionId, int quantity) {
      redisLockService.executeWithLock(lockKey, 3000, () -> {
          // 재고 차감 로직
      });
  }
  ```

**2. 기술 선택 이유를 docs에 문서화**
- 새로운 라이브러리나 패턴 도입 시 `docs/architecture/decisions/` 디렉토리에 ADR (Architecture Decision Record) 작성
- 파일명 형식: `ADR-{번호}-{제목}.md` (예: `ADR-001-왜-QueryDSL을-선택했는가.md`)
- 포함 내용:
  - **상황(Context)**: 어떤 문제를 해결하려고 했는가?
  - **고려한 옵션들**: 어떤 선택지들이 있었는가?
  - **결정**: 최종적으로 무엇을 선택했는가?
  - **이유**: 왜 이것을 선택했는가?
  - **결과**: 예상되는 장단점은?

**3. 설명 시 학습자 관점 유지**
- 전문 용어 사용 시 간단한 설명 추가
- 복잡한 개념은 예시와 함께 설명
- "이렇게 하면 X가 Y되기 때문에 Z한 장점이 있습니다" 형식으로 설명

### 문서화 규칙

**진행 상황, 문제 해결, 학습 내용을 지속적으로 문서화합니다.**

#### 1. 개발 로그 (DEVELOPMENT_LOG.md)

**위치**: `docs/project/DEVELOPMENT_LOG.md`

**작성 시점**:
- 매 작업 세션 종료 시
- 중요한 기능 완료 시
- 문제 해결 시

**작성 내용**:
```markdown
## 📅 YYYY-MM-DD (요일)

### ✅ 완료된 작업
- [x] User 도메인 엔티티 작성
- [x] UserRepository 단위 테스트 작성 (커버리지 85%)

### 🔧 기술적 결정
- **QueryDSL 동적 쿼리 사용 결정**
  - 이유: 검색 조건이 10개 이상으로 JPQL로는 가독성이 떨어짐
  - 결과: 타입 안전성과 가독성 확보

### 🐛 문제 및 해결
- **문제**: Testcontainers MySQL 컨테이너가 시작되지 않는 오류
- **원인**: Docker Desktop이 실행되지 않음
- **해결**: Docker Desktop 실행 후 테스트 재실행
- **참고**: `docs/troubleshooting/testcontainers-mysql-issue.md`

### 📚 학습 내용
- **JPA N+1 문제**
  - Fetch Join과 Entity Graph의 차이점 학습
  - Fetch Join은 JPQL에서 명시적, Entity Graph는 애노테이션으로 선언적
  - 프로젝트에서는 QueryDSL Fetch Join 사용하기로 결정

### 📋 다음 작업
- [ ] Product 도메인 엔티티 작성
- [ ] Category 계층 구조 설계
```

#### 2. 문제 해결 문서 (Troubleshooting)

**위치**: `docs/troubleshooting/{문제-제목}.md`

**작성 시점**: 다음 조건 중 하나라도 해당하는 경우
- 3시간 이상 소요된 문제
- 재발 가능성이 높은 문제
- 팀원(또는 미래의 자신)이 참고할 만한 가치가 있는 문제

**작성 내용**:
```markdown
# [문제 제목]

## 발생 일시
2025-12-16 14:30

## 증상
Kafka 컨슈머가 메시지를 소비하지 못하는 문제 발생

## 에러 메시지
```
org.apache.kafka.common.errors.TimeoutException: Topic not found
```

## 원인 분석
1. Kafka 토픽이 자동 생성되지 않음
2. `auto.create.topics.enable=false`로 설정되어 있음

## 해결 방법
1. Kafka Connect에서 토픽 수동 생성
```bash
kafka-topics.sh --create --topic order-events --bootstrap-server localhost:9092
```

2. 또는 docker-compose.yml에서 자동 생성 활성화
```yaml
KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
```

## 교훈
- 로컬 개발 환경에서는 토픽 자동 생성 활성화가 편리
- 프로덕션에서는 명시적 토픽 생성 권장
```

#### 3. 질문과 답변 문서 (Q&A)

**위치**: `docs/learning/qna/{주제}.md`

**작성 시점**: 궁금증이 생겨 학습하고 이해한 내용

**작성 내용**:
```markdown
# Q&A: 왜 Hexagonal Architecture를 선택했나요?

## 질문
왜 일반적인 Layered Architecture 대신 Hexagonal Architecture를 사용하나요?

## 답변

### 1. 도메인 중심 설계
- 비즈니스 로직이 Infrastructure에 의존하지 않음
- 데이터베이스나 프레임워크 변경이 도메인에 영향 없음

### 2. 테스트 용이성
- 도메인 로직을 독립적으로 테스트 가능
- Mock 없이 순수 Java 객체로 테스트

### 3. 확장성
- 포트-어댑터 패턴으로 새로운 인프라 추가 용이
- 예: MySQL → PostgreSQL 전환 시 Infrastructure만 변경

### 예시 코드
```java
// Domain Layer (순수 비즈니스 로직)
public interface OrderRepository {  // 포트
    Order save(Order order);
}

// Infrastructure Layer (어댑터)
@Repository
public class JpaOrderRepository implements OrderRepository {
    // JPA 구현
}
```

### 참고 자료
- [Hexagonal Architecture 설명](https://alistair.cockburn.us/hexagonal-architecture/)
```

#### 4. 앞으로 해야 할 일 (TODO)

**위치**: `docs/project/TODO.md`

**작성 시점**: 새로운 작업이 생길 때마다 업데이트

**작성 내용**:
```markdown
# 앞으로 해야 할 일

## 🔴 우선순위 높음
- [ ] User 인증/인가 구현 (JWT)
  - [ ] UserService 작성
  - [ ] JWT 토큰 발급/검증 로직
  - [ ] Spring Security 설정
  - [ ] 단위/통합/API 테스트 작성

## 🟡 우선순위 중간
- [ ] Product 검색 기능 (Elasticsearch)
  - [ ] Nori 분석기 설정
  - [ ] 검색 쿼리 작성
  - [ ] 통합 테스트 작성

## 🟢 우선순위 낮음
- [ ] 성능 최적화
  - [ ] N+1 쿼리 개선
  - [ ] Redis 캐싱 추가
```

#### 5. 학습 문서 (docs/learning/)

**위치**: `docs/learning/YYMMDD_문서명.md`

**파일명 규칙**:
- 형식: `YYMMDD_문서명.md` (예: `251216_JWT-인증-완벽-가이드.md`)
- 작성 날짜를 파일명 앞에 표기하여 언제 작성되었는지 파악 가능

**문서 상단 필수 항목**:
```markdown
# 문서 제목

> **작성일시**: YYYY-MM-DD HH:MM
>
> 문서 설명
```

**예시**:
```markdown
# JWT 인증 완벽 가이드 (초보자용)

> **작성일시**: 2025-12-16 23:46
>
> LookMarket 프로젝트의 JWT 인증 구현을 처음부터 쉽게 설명하는 문서입니다.
```

**작성 시점**: 새로운 개념을 학습하고 정리할 때

#### 문서화 체크리스트

기능 구현 완료 시 다음 항목 확인:

**필수 항목** (모든 작업 세션마다):
- [ ] `docs/project/DEVELOPMENT_LOG.md`에 작업 내용 기록 (완료 작업, 기술적 결정, 문제 해결, 학습 내용)
- [ ] `docs/project/TODO.md` 업데이트 (완료 항목 체크, 새로운 작업 추가)

**선택 항목** (해당 시):
- [ ] 중요한 기술적 결정 시 `docs/architecture/decisions/ADR-xxx.md` 작성 (기본 기술 스택 외 새 라이브러리 도입, 설계 패턴 변경 등)
- [ ] 3시간 이상 소요되었거나 재발 가능성이 높은 문제는 `docs/troubleshooting/` 문서화
- [ ] 학습한 내용 중 나중에 참고할 만한 가치가 있는 것은 `docs/learning/qna/` 문서화

**목표**: 문서 작성 시간이 개발 시간의 20%를 넘지 않도록 조절합니다.

## API 엔드포인트

Base URL: `http://localhost:8080`

**API 문서**: `http://localhost:8080/swagger-ui.html`

**헬스 체크**: `http://localhost:8080/actuator/health`

**Prometheus 메트릭**: `http://localhost:8080/actuator/prometheus`

### 주요 엔드포인트 패턴

- 인증: `/api/v1/auth/*`
- 상품: `/api/v1/products/*`
- 주문: `/api/v1/orders/*`
- 관리자: `/api/v1/admin/*`
- 알림 (SSE): `/api/v1/notifications/stock/subscribe`

## 모니터링 & 인프라

**Kafka UI**: http://localhost:8989 (토픽, 컨슈머, 메시지 모니터링)

**Elasticsearch**: http://localhost:9200 (인덱스 확인, 쿼리 실행)

**MySQL**: localhost:3306
- Database: `lookmarket`
- User: `lookmarket` / Password: `lookmarket1234`

**Redis**: localhost:6379 (개발 환경에서는 인증 없음)

## 중요 제약사항

### 동시성 & 트랜잭션

- **낙관적 락**은 엔티티에 `@Version` 필드 필요
- **분산 락**은 데드락 방지를 위해 타임아웃 지정 필수
- **Kafka 리스너**는 멱등성 보장 (중복 메시지 처리)
- **Saga 보상 트랜잭션**은 일관성 유지를 위해 신중하게 설계 필요

### 성능 고려사항

- **N+1 쿼리**: 연관 엔티티에 대해 fetch join을 사용한 QueryDSL 사용
- **Elasticsearch 캐싱**: 검색 결과에 5분 TTL로 부하 감소
- **커넥션 풀링**: MySQL용 HikariCP 설정 (기본 설정)
- **Kafka 파티셔닝**: 중요 토픽은 엔티티 ID로 파티셔닝 필요

### 보안

- **JWT 토큰**: Access token (15분 만료), Refresh token (7일)
- **비밀번호 인코딩**: BCrypt (strength 10)
- **CORS**: 프론트엔드 origin (http://localhost:5173) 설정됨

## 참고 문서

### 핵심 문서
- **프로젝트 상세 스펙**: `docs/design/LookMarket_Project_Specification.md` (4,362라인 포괄적 설계 문서)
- **개발 로그**: `docs/project/DEVELOPMENT_LOG.md` (구현 진행 상황 및 결정사항)
- **README**: `README.md` (빠른 시작 가이드)
- **문서 구조 가이드**: `docs/README.md` (전체 문서 구조 및 사용법)

### 아키텍처 규칙 (Architecture Rules)
- **강제 규칙**: `docs/architecture/ENFORCEMENT_RULES.md` (반드시 준수해야 할 아키텍처 제약사항)
- **설계 결정 기록**: `docs/architecture/decisions/` (ADR - Architecture Decision Records)

### 학습 자료 (Learning Materials)
- **Hexagonal Architecture 가이드**: `docs/learning/251216_Hexagonal-Architecture-Domain-구현-방식-비교.md` (설계 원칙 및 Loopers 프로젝트 비교)
- **RESTful API 설계 가이드**: `docs/learning/251217_RESTful-API-설계-완벽-가이드.md` (API 버저닝, 토큰 갱신, RESTful 원칙)
- **질의응답**: `docs/learning/qna/` (학습 과정에서 생긴 질문과 답변)

### 실용 가이드 (Guides)
- **멀티모듈 아키텍처**: `docs/guides/멀티모듈-아키텍처-가이드.md`
- **Docker Compose 설정**: `docs/guides/Docker-Compose-설정-가이드.md`

### 기타
- **환경 설정**: `docs/setup/` (개발 환경 구성 가이드)
- **문제 해결**: `docs/troubleshooting/` (트러블슈팅 문서)
- **보관 문서**: `docs/archive/` (참고용 구버전 문서)


# 프로젝트 Git 워크플로우 규칙

## 1. Repository
- **GitHub Repository:** `KBroJ/LookMarket`
- **Main Branch:** `main`

## 2. Branching Strategy
- 모든 기능 개발은 `feature/[이슈번호]-[간단-설명-kebab-case]` 형식의 브랜치에서 진행한다.
- 이슈 번호가 없는 간단한 수정은 `fix/[간단-설명]` 또는 `chore/[간단-설명]` 브랜치를 사용한다.

## 3. Commit Message Convention
- 모든 커밋 메시지는 **Conventional Commits** 명세를 따른다.
- 형식: `<type>: <subject>` (예: `feat: Add user authentication`, `fix: Resolve N+1 query in OrderRepository`)
- 커밋 본문에는 변경 이유를 명확히 서술한다.
- GitHub 이슈 연결: 중요한 기능이나 버그 수정 시 `Closes #[이슈번호]` 권장 (간단한 수정은 생략 가능)

## 4. Pull Request (PR) Process
- **PR 작성 필수**: 모든 기능은 PR을 통해 `main` 브랜치로 머지
- **셀프 리뷰 허용**: 혼자 개발하는 프로젝트이므로 셀프 리뷰 후 머지 가능
- **PR 템플릿 준수**: `.github/PULL_REQUEST_TEMPLATE.md`를 사용하여 변경 사항 요약 작성
- **PR 제목**: 커밋 메시지와 동일한 Conventional Commits 형식 사용

**PR 작성 이유**: 변경 사항을 체계적으로 정리하고 포트폴리오 검토 시 개발 과정을 쉽게 추적할 수 있습니다.

## 5. AI 도구 사용 명시
- README.md에 "이 프로젝트는 Claude Code를 활용하여 개발되었습니다" 명시
- 커밋 메시지나 코드에는 자동 생성 문구 제거 (예: "🤖 Generated with Claude Code" 제거)
- 학습 포트폴리오에서 AI 도구 활용을 투명하게 공개하는 것이 진정성 있는 접근입니다
