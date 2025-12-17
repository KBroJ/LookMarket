# LookMarket TODO 리스트

> **마지막 업데이트**: 2025-12-17
>
> **개발 방식**: 수직적 슬라이스 (Vertical Slice) + 도메인 완성 우선
> - 각 도메인: 기본 구현 → 테스트 → 프론트엔드 연동 순서로 완성
> - 고급 기술(Redis, Elasticsearch, Kafka)은 모든 도메인 완성 후 마지막에 적용
> - User 도메인을 레퍼런스로 삼아 Product, Order 도메인 설계/구현
> - **4단계 학습 흐름** (Product, Order 도메인):
>   1. **힌트 확인**: 구현할 클래스, 필요한 필드/메서드 파악
>   2. **직접 구현**: User.java 등 레퍼런스 참고하며 코드 작성
>   3. **리뷰 & 개선**: 코드 검토, 아키텍처 규칙 준수 확인
>   4. **다음 레이어**: Domain → Infrastructure → Application → API 순서 반복

---

## 📊 전체 진행 현황

| Phase | 내용 | 상태 | 비고 |
|-------|------|:----:|------|
| Phase 0 | 환경 검증 | ✅ | 2025-12-16 완료 |
| Phase 1 | User 도메인 기본 구현 | ✅ | PR #4 머지 |
| Phase 1-B | JWT 인증 | ✅ | PR #5 머지 |
| **Phase 1-C** | **User 테스트 보강** | 🟡 | **다음 작업** |
| Phase 1-D | User 프론트 연동 | ⏳ | 대기 |
| Phase 2 | Product 도메인 | ⏳ | 대기 |
| Phase 3 | Order 도메인 | ⏳ | 대기 |
| Phase 4 | 프론트엔드 통합 | ⏳ | 대기 |
| Phase 5 | 고급 기술 통합 | ⏳ | Redis, ES, Kafka |

---

## 🔴 즉시 처리 (현재 세션)

- [x] 현재 staged 변경사항 커밋 (문서 정리)
- [x] 개발 순서 관련 문서 정합성 맞추기
- [x] 4단계 학습 흐름 모든 문서에 반영

---

## ✅ Phase 0: 환경 검증 (완료)

> **완료일**: 2025-12-16

### Docker Compose 인프라
- [x] MySQL 8.0 컨테이너 구동
- [x] Redis 7-alpine 연결 테스트
- [x] Elasticsearch 8.11.0 실행
- [x] Kafka + Zookeeper 실행
- [x] Kafka Connect (Debezium) 설정
- [x] Kafka UI 접속 (http://localhost:8989)

### 백엔드
- [x] Gradle 멀티 모듈 빌드 성공
- [x] Spring Boot 애플리케이션 실행
- [x] Actuator 헬스 체크 확인

### 프론트엔드
- [x] npm install 의존성 설치
- [x] Vite 개발 서버 실행 (http://localhost:5173)

---

## ✅ Phase 1: User 도메인 기본 구현 (완료)

> **완료일**: 2025-12-16 | **PR**: #4

### Domain Layer
- [x] User 엔티티 (Behavior-rich Entity 패턴)
  - [x] Static Factory Methods (create, reconstitute)
  - [x] 비즈니스 메서드 (changeEmail, changePassword, activate, suspend, deactivate)
  - [x] Validation 로직 내장
- [x] UserRole enum (ADMIN, SELLER, CUSTOMER)
- [x] UserStatus enum (ACTIVE, INACTIVE, SUSPENDED)
- [x] UserRepository 인터페이스 (포트)

### Infrastructure Layer
- [x] V1__create_users_table.sql (Flyway 마이그레이션)
- [x] UserEntity (JPA 엔티티)
- [x] JpaUserRepository (어댑터)
  - [x] fromDomain / toDomain 변환

### Application Layer
- [x] UserService
  - [x] register (회원가입, 이메일 중복 체크, BCrypt 암호화)
  - [x] getUserById, getUserByEmail (조회)
  - [x] changeEmail (이메일 변경)
  - [x] changePassword (비밀번호 변경)
  - [x] activateUser, suspendUser, deactivateUser (상태 관리)

### API Layer
- [x] UserController
  - [x] POST /api/v1/users - 회원가입
  - [x] GET /api/v1/users/{userId} - 사용자 조회
  - [x] PATCH /api/v1/users/{userId}/email - 이메일 변경
  - [x] PATCH /api/v1/users/{userId}/password - 비밀번호 변경
  - [x] POST /api/v1/users/{userId}/activate - 계정 활성화
  - [x] POST /api/v1/users/{userId}/suspend - 계정 정지
  - [x] POST /api/v1/users/{userId}/deactivate - 계정 비활성화
- [x] DTO (UserRequest, UserResponse, ChangeEmailRequest, ChangePasswordRequest)

### Tests (기본)
- [x] UserTest (25개 단위 테스트)
- [x] UserServiceTest (13개 단위 테스트, Mockito)

---

## ✅ Phase 1-B: JWT 인증 (완료)

> **완료일**: 2025-12-17 | **PR**: #5

### Spring Security
- [x] SecurityConfig
  - [x] CSRF 비활성화 (Stateless)
  - [x] 세션 정책: STATELESS
  - [x] 공개 엔드포인트 설정 (/api/v1/auth/**, Swagger, Actuator)
  - [x] JWT 필터 등록

### JWT
- [x] JwtTokenProvider
  - [x] Access Token 발급 (15분 만료)
  - [x] Refresh Token 발급 (7일 만료)
  - [x] 토큰 검증 및 클레임 추출
- [x] JwtAuthenticationFilter
  - [x] Authorization 헤더에서 Bearer 토큰 추출
  - [x] SecurityContext에 인증 정보 설정
- [x] JwtUserDetails (Spring Security UserDetails 구현)

### API
- [x] AuthController
  - [x] POST /api/v1/auth/login - 로그인
  - [x] POST /api/v1/auth/refresh - 토큰 갱신
- [x] DTO (LoginRequest, TokenResponse, TokenRefreshRequest)

### 예외 처리
- [x] GlobalExceptionHandler
  - [x] 도메인 예외 처리
  - [x] JWT 예외 처리
  - [x] Validation 예외 처리

### 문서
- [x] ADR-002-RESTful-API-설계-원칙.md
- [x] 251217_RESTful-API-설계-완벽-가이드.md

---

## 🟡 Phase 1-C: User 테스트 보강 (다음 단계)

> **목표**: User 도메인 테스트 커버리지 확대

### 통합 테스트 (Testcontainers)
- [ ] UserRepository 통합 테스트
  - [ ] save, findById, findByEmail 테스트
  - [ ] 이메일 중복 체크 테스트
- [ ] AuthService 통합 테스트
  - [ ] 로그인 성공/실패 테스트
  - [ ] 토큰 발급 및 검증 테스트

### E2E 테스트 (REST Assured 또는 MockMvc)
- [ ] UserController API 테스트
  - [ ] 회원가입 API (성공, 이메일 중복, validation 실패)
  - [ ] 사용자 조회 API (성공, 404)
  - [ ] 이메일/비밀번호 변경 API
  - [ ] 상태 변경 API
- [ ] AuthController API 테스트
  - [ ] 로그인 API (성공, 실패)
  - [ ] 토큰 갱신 API (성공, 만료)
  - [ ] 인증 필요 API 접근 테스트

### JWT 단위 테스트
- [ ] JwtTokenProvider 테스트
  - [ ] 토큰 생성 테스트
  - [ ] 토큰 검증 테스트 (유효, 만료, 잘못된 서명)
  - [ ] 클레임 추출 테스트

---

## ⏳ Phase 1-D: User 프론트엔드 연동

> **목표**: User 도메인 프론트엔드 페이지 구현

### 페이지 구현
- [ ] 로그인 페이지
  - [ ] 이메일/비밀번호 입력 폼
  - [ ] 로그인 API 호출
  - [ ] 토큰 저장 (localStorage 또는 httpOnly cookie)
  - [ ] 에러 핸들링 (잘못된 자격 증명)
- [ ] 회원가입 페이지
  - [ ] 회원가입 폼 (이메일, 비밀번호, 이름)
  - [ ] 이메일 중복 체크
  - [ ] validation 에러 표시
- [ ] 프로필 페이지
  - [ ] 사용자 정보 조회
  - [ ] 이메일 변경
  - [ ] 비밀번호 변경

### 인증 관리
- [ ] AuthContext 또는 Zustand 스토어
  - [ ] 로그인 상태 관리
  - [ ] 토큰 갱신 로직
  - [ ] 로그아웃 처리
- [ ] ProtectedRoute 컴포넌트
  - [ ] 인증 필요 페이지 보호
  - [ ] 미인증 시 로그인 페이지 리다이렉트

### API 연동
- [ ] TanStack Query 훅
  - [ ] useLogin, useRegister, useLogout
  - [ ] useUser, useUpdateEmail, useUpdatePassword

---

## ⏳ Phase 2: Product 도메인

> **목표**: 상품 CRUD 기본 기능 구현 (Elasticsearch 없이)

### 2-A: 기본 구현

#### Domain Layer
- [ ] Product 엔티티
  - [ ] Behavior-rich Entity 패턴 적용
  - [ ] Static Factory Methods (create, reconstitute)
  - [ ] 비즈니스 메서드 (changePrice, updateStock, discontinue 등)
- [ ] Category 엔티티
  - [ ] 계층 구조 (parent_id)
  - [ ] 재귀적 자식 조회
- [ ] Brand 엔티티
- [ ] ProductStatus enum (ON_SALE, SOLD_OUT, DISCONTINUED)
- [ ] ProductRepository, CategoryRepository, BrandRepository 인터페이스

#### Infrastructure Layer
- [ ] Flyway 마이그레이션
  - [ ] V2__create_brands_table.sql
  - [ ] V3__create_categories_table.sql
  - [ ] V4__create_products_table.sql
  - [ ] V5__create_product_options_table.sql
  - [ ] V6__create_product_images_table.sql
- [ ] JPA 엔티티 및 Repository 구현

#### Application Layer
- [ ] ProductService (CRUD)
- [ ] CategoryService
- [ ] BrandService

#### API Layer
- [ ] ProductController (CRUD API)
- [ ] CategoryController
- [ ] BrandController
- [ ] DTO 작성

### 2-B: 테스트
- [ ] Domain 단위 테스트 (ProductTest, CategoryTest, BrandTest)
- [ ] Application 단위 테스트 (ProductServiceTest)
- [ ] 통합 테스트 (Testcontainers)
- [ ] E2E 테스트 (API 테스트)

### 2-C: 프론트엔드 연동
- [ ] 상품 목록 페이지 (LIKE 검색, 필터, 페이징)
- [ ] 상품 상세 페이지
- [ ] 관리자 상품 등록/수정 페이지

---

## ⏳ Phase 3: Order 도메인

> **목표**: 주문 생성/조회/취소 기본 기능 (분산락 없이 낙관적 락만)

### 3-A: 기본 구현

#### Domain Layer
- [ ] Order 엔티티 (Aggregate Root)
  - [ ] 주문 생성, 취소, 결제 완료, 배송 시작
- [ ] OrderItem 엔티티
- [ ] OrderStatus enum
- [ ] Inventory 엔티티
  - [ ] 재고 차감/복원 (낙관적 락 @Version)
- [ ] 도메인 이벤트 (Spring Event)
  - [ ] OrderCreatedEvent, OrderCancelledEvent

#### Infrastructure Layer
- [ ] Flyway 마이그레이션 (orders, order_items, inventory)
- [ ] JPA 엔티티 및 Repository 구현

#### Application Layer
- [ ] OrderService (주문 생성/조회/취소)
- [ ] InventoryService (재고 관리, 낙관적 락)

#### API Layer
- [ ] OrderController
- [ ] DTO 작성

### 3-B: 테스트
- [ ] Order 도메인 단위 테스트
- [ ] Inventory 동시성 테스트 (멀티 스레드)
- [ ] 통합 테스트 및 E2E 테스트

### 3-C: 프론트엔드 연동
- [ ] 장바구니 페이지
- [ ] 주문 페이지
- [ ] 주문 내역 페이지

---

## ⏳ Phase 4: 프론트엔드 통합 및 완성

> **목표**: 전체 플로우 시연 가능한 프론트엔드 완성

### 통합 테스트
- [ ] 회원가입 → 로그인 → 상품 검색 → 주문 전체 플로우 테스트
- [ ] 에러 케이스 처리 (재고 부족, 인증 만료 등)

### UI/UX 개선
- [ ] 로딩 상태, 에러 상태 처리
- [ ] 반응형 디자인
- [ ] Toast 알림

---

## ⏳ Phase 5: 고급 기술 통합 (학습 & 적용)

> **목표**: Redis, Elasticsearch, Kafka 학습 후 점진적 적용
> **접근 방식**: 기존 기능에 고급 기술 적용하여 전후 비교

### 5-A: Redis 적용

#### 학습 목표
- Redis 기본 개념 (String, Hash, Set, List, Sorted Set)
- Spring Data Redis 사용법
- 캐싱 전략 (Cache-Aside, Write-Through 등)
- 분산 락 개념

#### 적용 항목
- [ ] Refresh Token Redis 저장 (현재 메모리/DB → Redis)
  - 적용 전: 서버 재시작 시 토큰 무효화
  - 적용 후: Redis에 저장하여 영속성 확보
- [ ] 상품 상세 정보 캐싱
  - 적용 전: 매번 DB 조회
  - 적용 후: Redis 캐싱으로 응답 속도 개선 (측정 비교)
- [ ] 분산 락 (선착순 한정판 재고)
  - 적용 전: 낙관적 락으로 동시성 처리
  - 적용 후: Redis 분산 락으로 경쟁 상태 방지

### 5-B: Elasticsearch 적용

#### 학습 목표
- Elasticsearch 기본 개념 (인덱스, 문서, 매핑)
- Nori 한글 형태소 분석기
- 검색 쿼리 (match, bool, filter)
- Aggregation

#### 적용 항목
- [ ] 상품 검색 기능 개선
  - 적용 전: MySQL LIKE 검색
  - 적용 후: Elasticsearch 전문 검색 (검색 품질 비교)
- [ ] 자동완성 기능
- [ ] 패싯 검색 (브랜드별, 카테고리별 상품 수)

### 5-C: Kafka 적용

#### 학습 목표
- Kafka 기본 개념 (토픽, 파티션, 컨슈머 그룹)
- Spring Kafka 사용법
- 이벤트 기반 아키텍처
- Saga 패턴

#### 적용 항목
- [ ] Spring Event → Kafka 전환
  - 적용 전: 동기 처리
  - 적용 후: 비동기 이벤트 처리
- [ ] Saga 패턴 (주문-결제-재고)
  - 분산 트랜잭션 처리
  - 보상 트랜잭션
- [ ] CDC (Change Data Capture)
  - MySQL → Elasticsearch 자동 동기화
- [ ] 실시간 알림 (SSE)
  - 재입고 알림

---

## 📝 기타 작업 (선택)

### 문서화
- [ ] Swagger API 문서 상세화
- [ ] 기술 블로그 정리
- [ ] ADR 추가 작성

### 모니터링
- [ ] Actuator + Prometheus 연동
- [ ] Grafana 대시보드

### CI/CD
- [ ] GitHub Actions 워크플로우
- [ ] Docker 이미지 빌드 자동화

### 테스트 고도화
- [ ] JMeter 부하 테스트
- [ ] 테스트 커버리지 리포트

---

## 📌 참고 문서

- **프로젝트 스펙**: [docs/design/LookMarket_Project_Specification.md](../design/LookMarket_Project_Specification.md)
- **개발 일지**: [docs/project/DEVELOPMENT_LOG.md](./DEVELOPMENT_LOG.md)
- **개발 가이드**: [CLAUDE.md](../../CLAUDE.md)
- **아키텍처 규칙**: [docs/architecture/ENFORCEMENT_RULES.md](../architecture/ENFORCEMENT_RULES.md)
