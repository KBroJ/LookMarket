# LookMarket - 패션 통합 커머스 플랫폼

> Java 21 + Spring Boot 3.3.x 기반 B2C 이커머스 플랫폼
>
> **타겟**: 무신사, 29CM, 카카오스타일 등 커머스 기업 백엔드 개발자 포트폴리오

## 📌 프로젝트 개요

무신사, 올리브영 스타일의 멀티 브랜드 패션/뷰티 통합 플랫폼으로, 실시간 재고 관리와 이벤트 기반 아키텍처를 통한 확장 가능한 B2C 커머스 시스템

## 🎯 핵심 기술 스택

### Backend
- **Java 21** (Virtual Threads)
- **Spring Boot 3.3.x**
- **MySQL 8.0** (Primary DB)
- **Redis 7.x** (Cache + Distributed Lock)
- **Elasticsearch 8.x** (Search Engine)
- **Apache Kafka 3.6.x** (Event-Driven Architecture)
- **QueryDSL 5.x** (Type-safe Queries)

### Frontend
- **React 18** + **TypeScript 5.x**
- **Vite 5.x**
- **TanStack Query** (Server State)
- **Zustand** (Client State)
- **Tailwind CSS**

### Infrastructure
- **Docker** + **Docker Compose**
- **Flyway** (DB Migration)
- **Testcontainers** (Integration Test)

## 🏗️ 아키텍처

### 멀티 모듈 구조 (Hexagonal Architecture)

```
lookmarket/
├── lookmarket-api/              # Presentation Layer (REST API)
├── lookmarket-application/      # Application Service
├── lookmarket-domain/           # Domain Model (DDD)
├── lookmarket-infrastructure/   # Infrastructure (JPA, Kafka, Redis, ES)
└── lookmarket-common/           # Common Utilities
```

### 주요 기능

1. **상품 관리 & 검색**
   - Elasticsearch 기반 고성능 검색 (Nori 형태소 분석)
   - 복합 필터링 (가격, 브랜드, 카테고리, 평점)
   - Redis 캐싱

2. **실시간 재고 관리**
   - 낙관적 락 (일반 주문)
   - 분산 락 (선착순 한정판)
   - 재입고 알림 (SSE)

3. **주문/결제 시스템**
   - 분산 트랜잭션 (Saga Pattern)
   - 보상 트랜잭션
   - 주문-결제-배송 이벤트 기반 처리

4. **Kafka 이벤트 아키텍처**
   - Saga Pattern (분산 트랜잭션)
   - CDC (Debezium - Elasticsearch 동기화)
   - Event-Driven Notification (SSE)
   - Kafka Streams (실시간 통계)

## 🚀 빠른 시작

### 1. 사전 요구사항

- Java 21
- Docker & Docker Compose
- (선택) Node.js 18+ (프론트엔드)

### 2. 인프라 환경 구성

```bash
cd docker
docker-compose up -d
```

실행되는 서비스:
- MySQL: `localhost:3306`
- Redis: `localhost:6379`
- Elasticsearch: `localhost:9200`
- Kafka: `localhost:9092`
- Kafka Connect: `localhost:8083`
- Kafka UI: `localhost:8989`

### 3. 백엔드 실행

```bash
# 프로젝트 루트에서
./gradlew clean build
./gradlew :lookmarket-api:bootRun
```

애플리케이션: `http://localhost:8080`
API 문서 (Swagger): `http://localhost:8080/swagger-ui.html`

### 4. 프론트엔드 실행 (선택)

```bash
cd lookmarket-frontend
npm install
npm run dev
```

Frontend: `http://localhost:5173`

## 📊 데이터베이스 초기화

Flyway 마이그레이션이 자동으로 실행됩니다.

수동 실행:
```bash
./gradlew :lookmarket-infrastructure:flywayMigrate
```

## 🧪 테스트

```bash
# 전체 테스트
./gradlew test

# 특정 모듈 테스트
./gradlew :lookmarket-domain:test
./gradlew :lookmarket-api:test
```

## 📖 API 문서

Swagger UI: `http://localhost:8080/swagger-ui.html`

주요 엔드포인트:
- 인증: `POST /api/v1/auth/login`
- 상품 검색: `GET /api/v1/products/search`
- 주문 생성: `POST /api/v1/orders`
- 재입고 알림: `GET /api/v1/notifications/stock/subscribe` (SSE)

## 🔥 백엔드 어필 포인트

### 1. Java 21 Virtual Threads
- Kafka Listener에서 Virtual Threads 활용
- 수천 개 동시 요청 블로킹 없이 처리

### 2. 동시성 제어
- 낙관적 락 (@Version)
- Redis 분산 락
- 재고 정합성 보장

### 3. Kafka 이벤트 아키텍처
- **Saga Pattern**: 주문-결제-배송 분산 트랜잭션
- **CDC**: MySQL → Elasticsearch 자동 동기화
- **SSE**: 재입고 알림 실시간 전송
- **Kafka Streams**: 실시간 주문 통계

### 4. Elasticsearch 검색 최적화
- Nori 형태소 분석기 (한글)
- 복합 필터링 & Aggregation
- Redis 캐싱 (5분 TTL)

### 5. QueryDSL 고급 활용
- 복잡한 동적 쿼리
- N+1 문제 해결 (Fetch Join)

### 6. DDD & Clean Architecture
- Hexagonal Architecture
- Domain Event
- Value Object

## 🎓 학습 자료

- [프로젝트 상세 스펙](../StyleHub_Project_Specification.md)
- [아키텍처 다이어그램](./docs/architecture.md) (작성 예정)
- [ERD](./docs/erd.md) (작성 예정)

## 📝 개발 로드맵

- [x] 프로젝트 초기 설정
- [x] Docker Compose 환경 구성
- [ ] Week 1: 기반 구축 (도메인 모델, 인증)
- [ ] Week 2: 상품 관리 & 검색 (Elasticsearch)
- [ ] Week 3: 재고 관리 & 주문 (동시성 제어)
- [ ] Week 4: Kafka 이벤트 아키텍처 (Saga Pattern)
- [ ] Week 5: 실시간 알림 & CDC
- [ ] Week 6: Kafka Streams & 성능 최적화

## 🤝 기여

이슈나 PR은 환영합니다!

## 📄 라이선스

이 프로젝트는 포트폴리오 목적으로 작성되었습니다.

## 📧 문의

- GitHub Issues: 질문이나 제안사항은 이슈로 등록해주세요
