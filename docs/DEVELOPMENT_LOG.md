# LookMarket 개발 일지

## 프로젝트 정보

- **프로젝트명**: LookMarket (이전: StyleHub)
- **설명**: Java 21 + Spring Boot 3.3 기반 패션 통합 커머스 플랫폼
- **타겟**: 무신사, 29CM, 카카오스타일 등 커머스 기업 백엔드 개발자 포트폴리오
- **시작일**: 2025-12-15

---

## 📅 2025-12-15 (일요일)

### ✅ 완료된 작업

#### 1. 프로젝트 기획 및 스펙 작성 (19:00 - 20:30)
- [x] SuperClaude 브레인스토밍을 통한 프로젝트 주제 선정
- [x] StyleHub 프로젝트 상세 스펙 문서 작성 (4,362 라인)
  - 프로젝트 개요 및 목표
  - 기술 스택 상세 설명 (Java 21, Spring Boot, Kafka, Elasticsearch 등)
  - 시스템 아키텍처 (Hexagonal Architecture)
  - 데이터베이스 설계 (ERD)
  - 핵심 비즈니스 로직 (코드 예제 포함)
  - Kafka 이벤트 아키텍처 (Saga, CDC, SSE, Kafka Streams)
  - API 설계 (REST API 명세)
  - 프론트엔드 구조 (React + TypeScript)
  - 기술적 챌린지와 해결 방안
  - 6주 구현 로드맵
  - 성능 최적화 및 테스트 전략

#### 2. 프로젝트명 변경 결정 (20:40 - 20:45)
- [x] StyleHub → LookMarket 프로젝트명 변경
- **선정 이유**: 룩북(Look) + 마켓플레이스(Market) 개념으로 패션과 커머스 모두 표현

#### 3. 프로젝트 초기 설정 (20:45 - 21:30)

##### 백엔드 멀티 모듈 구조 생성
- [x] Gradle 멀티 모듈 프로젝트 구조 생성
  - `lookmarket-api` - Presentation Layer (REST API)
  - `lookmarket-application` - Application Service
  - `lookmarket-domain` - Domain Model (DDD)
  - `lookmarket-infrastructure` - Infrastructure (JPA, Kafka, Redis, ES)
  - `lookmarket-common` - Common Utilities

##### 설정 파일 작성
- [x] `settings.gradle` - 멀티 모듈 설정
- [x] 루트 `build.gradle` - 공통 의존성 및 Java 21 toolchain 설정
- [x] 각 모듈별 `build.gradle` 작성
  - QueryDSL 5.0.0 설정 (Jakarta 지원)
  - Spring Boot 3.3.0
  - MySQL, Redis, Elasticsearch, Kafka 의존성
  - JWT, Swagger, Actuator 설정
- [x] `application.yml` - Spring Boot 설정
  - MySQL 연결 설정 (DB: lookmarket)
  - Redis 설정
  - Elasticsearch 설정
  - Kafka Consumer/Producer 설정
  - JWT 설정
  - CORS 설정
  - Virtual Threads 활성화
- [x] `LookMarketApplication.java` - Spring Boot 메인 클래스
  - Java 21 Virtual Threads 활성화
  - Hexagonal Architecture 스캔 설정

##### Docker 인프라 환경 구성
- [x] `docker-compose.yml` 작성
  - MySQL 8.0 (lookmarket DB, CDC binlog 설정)
  - Redis 7-alpine
  - Elasticsearch 8.11.0
  - Zookeeper 7.5.0
  - Kafka 7.5.0
  - Kafka Connect (Debezium 2.5) - CDC용
  - Kafka UI (모니터링)
  - 모든 서비스 lookmarket-network 연결

##### Gradle Wrapper 생성
- [x] `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.5)
- [x] `gradlew` (Unix/Linux 실행 스크립트)
- [x] `gradlew.bat` (Windows 실행 스크립트)

##### 프론트엔드 프로젝트 생성
- [x] React 18 + TypeScript 5 + Vite 5 프로젝트 구조
- [x] `package.json` - 의존성 설정
  - React 18.2
  - React Router DOM 6
  - TanStack Query 5
  - Zustand 4
  - Axios
  - Tailwind CSS 3
- [x] `tsconfig.json` - TypeScript 설정 (ES2020, strict mode)
- [x] `vite.config.ts` - Vite 설정 (프록시: /api → http://localhost:8080)
- [x] `tailwind.config.js` - Tailwind CSS 설정
- [x] `src/main.tsx` - React 엔트리 포인트 (React Query, Router 설정)
- [x] `src/App.tsx` - 메인 컴포넌트 (홈페이지 UI)
- [x] `src/styles/index.css` - 글로벌 스타일
- [x] 디렉토리 구조 생성
  - `src/components/` - 재사용 컴포넌트
  - `src/pages/` - 페이지 컴포넌트
  - `src/hooks/` - 커스텀 훅
  - `src/services/` - API 서비스
  - `src/store/` - Zustand 스토어
  - `src/types/` - TypeScript 타입 정의

##### 기타 파일
- [x] `.gitignore` - Git 제외 파일 설정
- [x] `README.md` - 프로젝트 소개 및 실행 가이드

---

## 📋 앞으로 해야 할 일 (TODO)

### 🔴 Phase 0: 환경 검증 (우선순위: 높음)
- [ ] Docker Compose 인프라 환경 실행 테스트
  - [ ] MySQL 컨테이너 정상 구동 확인
  - [ ] Redis 연결 테스트
  - [ ] Elasticsearch 정상 실행 확인
  - [ ] Kafka + Zookeeper 실행 확인
  - [ ] Kafka UI 접속 테스트 (http://localhost:8989)
- [ ] 프론트엔드 실행 테스트
  - [ ] npm install 의존성 설치
  - [ ] npm run dev 개발 서버 실행
  - [ ] http://localhost:5173 접속 확인
- [ ] 백엔드 빌드 및 실행 테스트
  - [ ] Gradle wrapper jar 다운로드
  - [ ] gradlew clean build 빌드 테스트
  - [ ] gradlew :lookmarket-api:bootRun 실행
  - [ ] http://localhost:8080/actuator/health 헬스 체크

### 🟡 Week 1: 기반 구축 (예상 기간: 5일)

#### 도메인 모델 설계 및 구현
- [ ] User (사용자) 도메인
  - [ ] User 엔티티 작성 (JPA)
  - [ ] UserRole (CUSTOMER, SELLER, ADMIN)
  - [ ] UserStatus (ACTIVE, INACTIVE, SUSPENDED)
- [ ] Product (상품) 도메인
  - [ ] Product 엔티티
  - [ ] Category 엔티티 (계층 구조)
  - [ ] Brand 엔티티
  - [ ] ProductImage 엔티티
  - [ ] ProductStatus (SALE, SOLD_OUT, DISCONTINUED)
- [ ] Order (주문) 도메인
  - [ ] Order 엔티티
  - [ ] OrderItem 엔티티
  - [ ] OrderStatus (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
- [ ] Inventory (재고) 도메인
  - [ ] Inventory 엔티티
  - [ ] InventoryTransaction 엔티티 (재고 증감 이력)
  - [ ] 낙관적 락 (@Version) 적용

#### 데이터베이스 마이그레이션 (Flyway)
- [ ] Flyway 마이그레이션 스크립트 작성
  - [ ] V1__create_users_table.sql
  - [ ] V2__create_products_tables.sql
  - [ ] V3__create_categories_table.sql
  - [ ] V4__create_brands_table.sql
  - [ ] V5__create_orders_tables.sql
  - [ ] V6__create_inventory_table.sql
  - [ ] V7__create_indexes.sql
- [ ] 초기 데이터 삽입 스크립트
  - [ ] 샘플 브랜드 데이터
  - [ ] 샘플 카테고리 데이터
  - [ ] 테스트용 사용자 데이터

#### 인증/인가 구현
- [ ] Spring Security 설정
  - [ ] SecurityConfig 작성
  - [ ] CORS 설정
  - [ ] PasswordEncoder (BCrypt)
- [ ] JWT 인증 구현
  - [ ] JwtTokenProvider
  - [ ] JwtAuthenticationFilter
  - [ ] Access Token / Refresh Token 발급
- [ ] 인증 API 구현
  - [ ] POST /api/v1/auth/signup (회원가입)
  - [ ] POST /api/v1/auth/login (로그인)
  - [ ] POST /api/v1/auth/logout (로그아웃)
  - [ ] POST /api/v1/auth/refresh (토큰 갱신)

#### Repository & Service Layer
- [ ] JPA Repository 작성 (Spring Data JPA)
  - [ ] UserRepository
  - [ ] ProductRepository
  - [ ] OrderRepository
  - [ ] InventoryRepository
- [ ] QueryDSL Repository 구현
  - [ ] QProductRepository (복잡한 상품 검색 쿼리)
  - [ ] QOrderRepository (주문 내역 조회)
- [ ] Service Layer 작성
  - [ ] UserService
  - [ ] AuthService
  - [ ] ProductService
  - [ ] OrderService
  - [ ] InventoryService

#### 단위 테스트 작성
- [ ] Domain 계층 테스트
  - [ ] User 도메인 로직 테스트
  - [ ] Order 도메인 로직 테스트
  - [ ] Inventory 재고 차감 로직 테스트
- [ ] Service 계층 테스트 (Mockito)
  - [ ] AuthService 테스트
  - [ ] ProductService 테스트
  - [ ] OrderService 테스트

### 🟡 Week 2: 상품 관리 & 검색 (Elasticsearch) (예상 기간: 5일)

#### Elasticsearch 설정
- [ ] Elasticsearch 인덱스 설계
  - [ ] products 인덱스 매핑 정의
  - [ ] Nori 형태소 분석기 설정 (한글 검색)
- [ ] Spring Data Elasticsearch 설정
  - [ ] ElasticsearchConfig
  - [ ] ProductDocument 엔티티 (@Document)
- [ ] 초기 데이터 색인
  - [ ] MySQL → Elasticsearch 초기 동기화 스크립트

#### 상품 검색 기능 구현
- [ ] ElasticsearchRepository 구현
  - [ ] ProductSearchRepository
  - [ ] 복합 검색 쿼리 (상품명, 브랜드, 카테고리)
  - [ ] 필터링 (가격 범위, 브랜드, 카테고리)
  - [ ] 정렬 (인기순, 가격순, 최신순)
  - [ ] Aggregation (카테고리별 개수, 가격 범위별 개수)
- [ ] 검색 API 구현
  - [ ] GET /api/v1/products/search (상품 검색)
  - [ ] GET /api/v1/products/{id} (상품 상세)
  - [ ] GET /api/v1/products (상품 목록 - 페이징)

#### Redis 캐싱 구현
- [ ] Redis 캐시 설정
  - [ ] RedisConfig
  - [ ] CacheConfig (@EnableCaching)
- [ ] 캐싱 전략 적용
  - [ ] 상품 상세 정보 캐싱 (5분 TTL)
  - [ ] 인기 상품 목록 캐싱 (10분 TTL)
  - [ ] 브랜드/카테고리 목록 캐싱 (1시간 TTL)
- [ ] 캐시 무효화 전략
  - [ ] 상품 수정 시 캐시 무효화
  - [ ] 이벤트 기반 캐시 무효화

#### 상품 관리 API 구현 (관리자)
- [ ] POST /api/v1/admin/products (상품 등록)
- [ ] PUT /api/v1/admin/products/{id} (상품 수정)
- [ ] DELETE /api/v1/admin/products/{id} (상품 삭제)
- [ ] POST /api/v1/admin/products/{id}/images (이미지 업로드)

#### 프론트엔드 - 상품 검색 페이지
- [ ] 상품 검색 UI 구현
  - [ ] 검색 바 컴포넌트
  - [ ] 필터 사이드바 (가격, 브랜드, 카테고리)
  - [ ] 상품 카드 그리드
  - [ ] 페이지네이션
- [ ] 상품 상세 페이지
  - [ ] 상품 정보 표시
  - [ ] 이미지 갤러리
  - [ ] 장바구니 담기 버튼
- [ ] API 연동 (TanStack Query)
  - [ ] useProductSearch 훅
  - [ ] useProductDetail 훅

### 🟡 Week 3: 재고 관리 & 주문 (동시성 제어) (예상 기간: 5일)

#### 동시성 제어 구현
- [ ] 낙관적 락 (Optimistic Lock)
  - [ ] Inventory 엔티티에 @Version 적용
  - [ ] 재고 차감 시 OptimisticLockException 처리
  - [ ] 재시도 로직 구현 (@Retryable)
- [ ] Redis 분산 락 (Distributed Lock)
  - [ ] RedisLockService 구현
  - [ ] Redisson 또는 Lettuce 활용
  - [ ] 선착순 한정판 주문 시 분산 락 적용
- [ ] 동시성 테스트
  - [ ] 멀티 스레드 환경 재고 차감 테스트
  - [ ] 100명 동시 주문 시나리오 테스트

#### 주문 기능 구현
- [ ] 주문 생성 로직
  - [ ] OrderService.createOrder()
  - [ ] 재고 확인 및 차감
  - [ ] 주문 상태 관리 (FSM)
- [ ] 주문 API 구현
  - [ ] POST /api/v1/orders (주문 생성)
  - [ ] GET /api/v1/orders (주문 목록)
  - [ ] GET /api/v1/orders/{id} (주문 상세)
  - [ ] POST /api/v1/orders/{id}/cancel (주문 취소)

#### 재입고 알림 (SSE)
- [ ] SSE 엔드포인트 구현
  - [ ] GET /api/v1/notifications/stock/subscribe (SSE 구독)
  - [ ] SseEmitter 관리
- [ ] 재입고 알림 발송
  - [ ] 재입고 시 구독자에게 실시간 알림
  - [ ] 타임아웃 및 에러 처리

#### 프론트엔드 - 주문 페이지
- [ ] 장바구니 페이지
  - [ ] 장바구니 아이템 관리 (Zustand)
  - [ ] 수량 변경, 삭제
- [ ] 주문 페이지
  - [ ] 주문 정보 입력 폼
  - [ ] 주문 확인 및 결제
- [ ] 주문 내역 페이지
  - [ ] 주문 목록 표시
  - [ ] 주문 상세 보기
  - [ ] 주문 취소

### 🟢 Week 4: Kafka 이벤트 아키텍처 (Saga Pattern) (예상 기간: 5일)

#### Kafka 기본 설정
- [ ] Kafka Producer 설정
  - [ ] KafkaProducerConfig
  - [ ] EventPublisher 인터페이스 및 구현체
- [ ] Kafka Consumer 설정
  - [ ] KafkaConsumerConfig
  - [ ] Virtual Threads 활용 (@KafkaListener)
- [ ] Kafka Topic 생성
  - [ ] order-created
  - [ ] order-confirmed
  - [ ] order-cancelled
  - [ ] inventory-reserved
  - [ ] inventory-released

#### Saga Pattern 구현 (주문-재고-배송)
- [ ] OrderSaga Orchestrator
  - [ ] 주문 생성 이벤트 발행
  - [ ] 재고 예약 이벤트 수신
  - [ ] 보상 트랜잭션 (재고 복구)
- [ ] InventoryEventListener
  - [ ] 주문 생성 이벤트 수신
  - [ ] 재고 예약 처리
  - [ ] 재고 예약 실패 시 보상 이벤트 발행
- [ ] 이벤트 소싱 (Event Sourcing)
  - [ ] OrderEvent 저장 (감사 추적)
  - [ ] 이벤트 재생 기능

#### Outbox Pattern 구현
- [ ] EventOutbox 엔티티
  - [ ] 이벤트 저장 테이블
  - [ ] 발행 상태 관리 (PENDING, PUBLISHED, FAILED)
- [ ] OutboxEventPublisher 스케줄러
  - [ ] 미발행 이벤트 주기적 체크
  - [ ] Kafka 발행 재시도
- [ ] Dead Letter Queue (DLQ) 처리
  - [ ] 실패한 이벤트 DLQ로 이동
  - [ ] 관리자 알림

### 🟢 Week 5: 실시간 알림 & CDC (예상 기간: 5일)

#### Debezium CDC 설정
- [ ] Kafka Connect Connector 설정
  - [ ] MySQL Source Connector 설정 (Debezium)
  - [ ] products 테이블 CDC 활성화
- [ ] CDC 이벤트 처리
  - [ ] CDCEventListener
  - [ ] MySQL → Elasticsearch 자동 동기화
  - [ ] 상품 변경 시 실시간 색인 업데이트

#### SSE 실시간 알림 고도화
- [ ] 알림 타입 확장
  - [ ] 주문 상태 변경 알림
  - [ ] 배송 시작 알림
  - [ ] 재입고 알림
  - [ ] 프로모션 알림
- [ ] NotificationService 구현
  - [ ] 사용자별 SSE 연결 관리
  - [ ] 알림 히스토리 저장
- [ ] 프론트엔드 SSE 연동
  - [ ] useSSE 커스텀 훅
  - [ ] 실시간 알림 UI

### 🟢 Week 6: Kafka Streams & 성능 최적화 (예상 기간: 5일)

#### Kafka Streams 구현
- [ ] 실시간 주문 통계
  - [ ] 시간대별 주문 건수 집계
  - [ ] 상품별 판매량 집계
  - [ ] 브랜드별 매출 집계
- [ ] 실시간 인기 상품 순위
  - [ ] Windowed Aggregation (1시간 윈도우)
  - [ ] Top 10 상품 계산
  - [ ] Redis 캐시 업데이트

#### 성능 최적화
- [ ] N+1 쿼리 해결
  - [ ] Fetch Join 적용
  - [ ] @BatchSize 설정
- [ ] DB 인덱스 최적화
  - [ ] 복합 인덱스 추가
  - [ ] 커버링 인덱스 적용
  - [ ] Slow Query 분석 및 개선
- [ ] Connection Pool 튜닝
  - [ ] HikariCP 설정 최적화
  - [ ] Redis Lettuce Pool 설정

#### 통합 테스트
- [ ] Testcontainers 활용
  - [ ] MySQL 컨테이너 테스트
  - [ ] Redis 컨테이너 테스트
  - [ ] Kafka 컨테이너 테스트
- [ ] API 통합 테스트 (RestAssured)
  - [ ] 주요 API 엔드포인트 테스트
  - [ ] 인증 플로우 테스트
  - [ ] 주문 플로우 E2E 테스트

#### 성능 테스트
- [ ] JMeter 부하 테스트
  - [ ] 상품 검색 API (1000 TPS 목표)
  - [ ] 주문 생성 API (100 TPS 목표)
- [ ] Gatling 시나리오 테스트
  - [ ] 동시 주문 시나리오
  - [ ] 선착순 이벤트 시나리오

### 🔵 추가 고려 사항 (선택)
- [ ] Spring Batch 배치 작업
  - [ ] 주문 통계 일별 집계
  - [ ] 장기 미사용 계정 정리
- [ ] Swagger UI 고도화
  - [ ] API 문서 상세 설명 추가
  - [ ] 예제 요청/응답 추가
- [ ] 프론트엔드 고도화
  - [ ] 반응형 디자인 개선
  - [ ] 로딩 스피너 및 스켈레톤 UI
  - [ ] 에러 바운더리
  - [ ] React.lazy를 통한 코드 스플리팅
- [ ] 모니터링 및 로깅
  - [ ] Actuator + Prometheus 연동
  - [ ] Grafana 대시보드 구성
  - [ ] ELK Stack 로그 수집 (선택)
- [ ] CI/CD 파이프라인
  - [ ] GitHub Actions 워크플로우
  - [ ] Docker 이미지 빌드 자동화
  - [ ] 배포 자동화 (선택)

---

## 📝 메모 및 참고사항

### 핵심 기술 어필 포인트
1. **Java 21 Virtual Threads**: Kafka Listener, 대량 요청 처리에 활용
2. **동시성 제어**: 낙관적 락, Redis 분산 락으로 재고 정합성 보장
3. **Kafka 이벤트 아키텍처**: Saga Pattern, CDC, SSE 실시간 알림
4. **Elasticsearch**: Nori 형태소 분석, 복합 필터링, Aggregation
5. **QueryDSL**: 복잡한 동적 쿼리, N+1 문제 해결
6. **DDD & Hexagonal Architecture**: 도메인 중심 설계, 계층 분리

### 개발 환경
- **IDE**: IntelliJ IDEA (권장) 또는 VS Code
- **Java**: OpenJDK 21
- **Node.js**: 18+ (프론트엔드)
- **Docker**: 최신 버전
- **Git**: 버전 관리

### 참고 문서
- [프로젝트 상세 스펙](./LookMarket_Project_Specification.md) - 4,362라인 상세 설계 문서
- [README.md](./README.md) - 프로젝트 소개 및 실행 가이드

### 다음 세션 시작 시 확인 사항
1. Docker 컨테이너 모두 정상 실행 중인지 확인
2. 프론트엔드 npm 의존성 설치 완료 여부
3. Gradle 빌드 성공 여부
4. 현재 구현 중인 Week 확인

---

## 🎯 단기 목표 (다음 세션)
1. ✅ Docker Compose 실행 및 모든 인프라 컨테이너 정상 동작 확인
2. ✅ 프론트엔드 로컬 실행 성공
3. ✅ 백엔드 빌드 및 기본 헬스 체크 성공
4. ⏭️ Week 1 시작: User 도메인 모델 작성

---

**마지막 업데이트**: 2025-12-15 21:30 (일요일)
**다음 작업 예정**: Week 1 - 기반 구축 (도메인 모델 작성)
