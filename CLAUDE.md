# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 리포지토리에서 작업할 때 참고하는 가이드입니다.

## 프로젝트 개요

LookMarket은 Java 21과 Spring Boot 3.3.x 기반의 멀티 브랜드 패션/뷰티 통합 커머스 플랫폼입니다. 무신사, 29CM, 카카오스타일과 같은 B2C 커머스 시스템을 목표로 하며, 이벤트 기반 아키텍처, 분산 시스템, 고성능 검색 등 고급 백엔드 개발 역량을 보여줍니다.

**핵심 기술 스택**: Java 21 (Virtual Threads), Spring Boot 3.3.x, MySQL 8.0, Redis 7.x, Elasticsearch 8.x, Apache Kafka 3.6.x, QueryDSL 5.x, React 18 + TypeScript

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

### Spring Boot 설정

애플리케이션 설정 위치: `lookmarket-api/src/main/resources/application.yml`
- Virtual Threads: `spring.threads.virtual.enabled=true`
- Database: localhost:3306의 `lookmarket` 데이터베이스
- Redis: localhost:6379
- Elasticsearch: localhost:9200
- Kafka: localhost:9092

### Testcontainers를 사용한 테스트

통합 테스트는 MySQL, Kafka, Elasticsearch용 Testcontainers 사용:
- 추상 기본 테스트 클래스가 컨테이너 구성
- 성능을 위해 테스트 간 컨테이너 재사용
- 예시: `@Container` 필드와 함께 `@Testcontainers` 애노테이션

## 개발 워크플로우

### 새 기능 추가

1. **Domain First**: `lookmarket-domain`에서 엔티티, 값 객체, 비즈니스 규칙 정의
2. **Repository 인터페이스**: domain 레이어에서 repository 포트 정의
3. **Infrastructure**: `lookmarket-infrastructure`에서 JPA 엔티티 및 리포지토리 구현
4. **Application Service**: `lookmarket-application`에서 유즈케이스 오케스트레이션 생성
5. **API Layer**: `lookmarket-api`에 컨트롤러 및 DTO 추가
6. **Tests**: 단위 테스트(domain), 통합 테스트(infrastructure), API 테스트(REST Assured) 작성

### 데이터베이스 스키마 변경

1. `lookmarket-infrastructure/src/main/resources/db/migration/`에 새 Flyway 마이그레이션 생성
2. 네이밍 규칙: `V{version}__{description}.sql` (예: `V1__create_users_table.sql`)
3. 마이그레이션 실행: `./gradlew :lookmarket-infrastructure:flywayMigrate`
4. infrastructure 레이어의 해당 JPA 엔티티 업데이트

### 이벤트 처리

새 도메인 이벤트 추가 시:
1. `lookmarket-domain`에서 이벤트 정의 (예: `OrderCancelledEvent`)
2. 상태 변경 후 도메인 서비스에서 이벤트 발행
3. `lookmarket-infrastructure`에서 `@KafkaListener`로 Kafka 리스너 생성
4. `application.yml`에서 토픽명 설정
5. 멱등성 처리 (이벤트는 여러 번 전달될 수 있음)

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

**프로젝트 상세 스펙**: `docs/LookMarket_Project_Specification.md` (4,362라인 포괄적 설계 문서)

**개발 로그**: `docs/DEVELOPMENT_LOG.md` (구현 진행 상황 및 결정사항)

**README**: `README.md` (빠른 시작 가이드)


# 프로젝트 Git 워크플로우 규칙

## 1. Repository
- **GitHub Repository:** `KBroJ/LookMarket`
- **Main Branch:** `main`

## 2. Branching Strategy
- 모든 기능 개발은 `feature/[이슈번호]-[간단-설명-kebab-case]` 형식의 브랜치에서 진행한다.
- 이슈 번호가 없는 간단한 수정은 `fix/[간단-설명]` 또는 `chore/[간단-설명]` 브랜치를 사용한다.

## 3. Commit Message Convention
- 모든 커밋 메시지는 **Conventional Commits** 명세를 따른다.
- (예: `feat: Add author profile component`, `fix: Correct typo in footer`)
- 커밋 본문에는 변경 이유를 명확히 서술하고, 관련된 GitHub 이슈를 `Closes #[이슈번호]` 형식으로 반드시 포함한다.

## 4. Pull Request (PR) Process
- 모든 코드는 `main` 브랜치로 직접 푸시할 수 없으며, 반드시 PR을 통해 코드 리뷰를 받아야 한다.
- PR 제목은 커밋 메시지와 동일한 형식을 따른다.
- PR 본문은 `.github/PULL_REQUEST_TEMPLATE.md` 템플릿을 사용한다.

## 5. 클로드의 모든 흔적 남기지 않기
 - 커밋 메시지 또는 PR 생성 시 클로드 코드를 사용한 흔적을 절대 남기지 않는다.
