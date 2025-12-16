# PayPoint - 통합 포인트 & 간편결제 플랫폼

> 백엔드 개발자 포트폴리오 프로젝트 (경력직 대상)
> **타겟 기업:** 토스, 카카오페이 등 핀테크 기업
> **기술 스택:** Java Spring Boot + MySQL + Redis + Kafka

---

## 📋 목차

1. [프로젝트 개요](#프로젝트-개요)
2. [백엔드 개발자 어필 포인트](#백엔드-개발자-어필-포인트)
3. [기술 스택 상세](#기술-스택-상세)
4. [시스템 아키텍처](#시스템-아키텍처)
5. [데이터베이스 설계](#데이터베이스-설계)
6. [핵심 비즈니스 로직](#핵심-비즈니스-로직)
7. [API 설계](#api-설계)
8. [기술적 챌린지와 해결 방안](#기술적-챌린지와-해결-방안)
9. [구현 로드맵](#구현-로드맵)
10. [성능 최적화 전략](#성능-최적화-전략)
11. [테스트 전략](#테스트-전략)

---

## 프로젝트 개요

### 비즈니스 목표
여러 파트너 서비스(쇼핑몰, OTT, 배달앱 등)에서 공통으로 사용할 수 있는 통합 포인트를 관리하고, 이를 활용한 간편결제 서비스를 제공하는 핀테크 플랫폼

### 핵심 가치
- **사용자**: 하나의 포인트로 다양한 서비스 이용, 간편한 결제 경험
- **파트너**: 통합된 결제 시스템, 고객 리텐션 향상
- **플랫폼**: 결제 수수료 수익, 데이터 기반 비즈니스 확장

### 주요 기능
1. 간편결제 (카드 등록, PIN 인증, 결제 실행)
2. 포인트 관리 (적립, 사용, 만료, 선물)
3. 거래 내역 조회 및 분석
4. 프로모션 및 이벤트 관리
5. 파트너 가맹점 정산

---

## 백엔드 개발자 어필 포인트

### 🎯 왜 이 프로젝트가 경력직 포트폴리오에 적합한가?

#### 1. 복잡한 도메인 모델링 능력 입증
- **금융 도메인의 복잡한 비즈니스 규칙**을 코드로 표현
- DDD(Domain-Driven Design) 적용으로 도메인 중심 설계 역량 증명
- 집계(Aggregate), 엔티티, 값 객체를 명확히 구분한 설계

```java
// 예시: Point Aggregate Root
public class Point {
    private PointId id;
    private UserId userId;
    private Money balance;
    private PointPolicy policy;

    // 비즈니스 로직: 포인트 차감 시 잔액 검증
    public PointTransaction deduct(Money amount, String reason) {
        if (!this.canDeduct(amount)) {
            throw new InsufficientPointException();
        }
        // 도메인 이벤트 발행
        this.registerEvent(new PointDeductedEvent(...));
        return PointTransaction.createDeduction(this.id, amount, reason);
    }
}
```

#### 2. 트랜잭션 일관성 보장 능력
- **분산 트랜잭션 처리** (Saga Pattern)
- 보상 트랜잭션(Compensating Transaction) 구현
- 이벤트 소싱으로 감사 추적(Audit Trail) 보장

**시나리오:** 결제 성공 → 포인트 적립 실패 시
```
1. PaymentService: 결제 승인 → SUCCESS
2. PointService: 포인트 적립 시도 → FAILURE
3. PaymentService: 보상 트랜잭션 → 결제 취소 or 재시도 큐 등록
```

#### 3. 동시성 제어 전문성
- **낙관적 락(Optimistic Lock)**: 포인트 잔액 업데이트
- **비관적 락(Pessimistic Lock)**: 선착순 이벤트 포인트 지급
- **분산 락(Distributed Lock)**: Redis 기반 중복 결제 방지

```java
@Transactional
public PaymentResult processPayment(PaymentRequest request) {
    // 분산 락으로 동일 사용자 동시 결제 방지
    String lockKey = "payment:lock:" + request.getUserId();
    return redisLockService.executeWithLock(lockKey, () -> {
        // 낙관적 락으로 포인트 차감
        Point point = pointRepository.findByIdWithOptimisticLock(request.getUserId());
        point.deduct(request.getPointAmount());

        // 결제 처리
        return paymentGateway.pay(request);
    });
}
```

#### 4. 대용량 데이터 처리 경험
- **Spring Batch**: 수백만 건 포인트 만료 배치 처리
- **파티셔닝**: 대량 데이터를 청크 단위로 분산 처리
- **성능 튜닝**: Bulk Insert/Update, 인덱스 최적화

#### 5. 이벤트 기반 아키텍처(EDA) 구현
- **Kafka 활용**: 결제-포인트 간 느슨한 결합
- **비동기 처리**: 포인트 적립은 비동기로 처리하여 결제 응답 시간 단축
- **이벤트 재처리**: 멱등성 보장으로 중복 처리 방지

#### 6. RESTful API 설계 역량
- REST 성숙도 모델 Level 2-3 준수
- 명확한 HTTP 상태 코드 사용
- API 버저닝 전략 (URI vs Header)
- 페이징, 필터링, 정렬 표준화

#### 7. 보안 및 인증/인가 구현
- JWT 기반 인증
- Spring Security를 활용한 역할 기반 접근 제어(RBAC)
- 민감 정보 암호화 (카드 번호, PIN)
- API Rate Limiting (DDoS 방어)

#### 8. 성능 최적화 및 모니터링
- Redis 캐싱 전략 (Look-Aside, Write-Through)
- N+1 쿼리 문제 해결 (Fetch Join, Batch Size)
- DB 인덱싱 전략 (복합 인덱스, 커버링 인덱스)
- APM 도구 연동 준비 (Pinpoint, Scouter)

---

## 기술 스택 상세

### Backend Framework
```yaml
Language: Java 17 (LTS)
Framework: Spring Boot 3.2.x
Build Tool: Gradle 8.x

Spring Modules:
  - Spring Web (REST API)
  - Spring Data JPA (ORM)
  - Spring Security (인증/인가)
  - Spring Batch (배치 처리)
  - Spring Cloud Stream (Kafka 연동)
  - Spring Cache (추상화)
```

### Database
```yaml
Primary DB: MySQL 8.0
  - InnoDB 스토리지 엔진
  - 트랜잭션 ACID 보장
  - Row-Level Locking

Cache: Redis 7.x
  - 포인트 잔액 캐싱
  - 세션 관리
  - 분산 락
  - Rate Limiting

Query Builder: QueryDSL 5.x
  - 타입 세이프 쿼리
  - 동적 쿼리 생성
  - 복잡한 조회 쿼리 최적화
```

### Message Queue
```yaml
Kafka 3.x:
  - Event-Driven Architecture
  - 결제 이벤트 발행/구독
  - 포인트 적립 비동기 처리
  - Dead Letter Queue (실패 처리)
```

### Persistence
```yaml
ORM: Hibernate 6.x (JPA 구현체)
Connection Pool: HikariCP
Migration: Flyway
```

### Testing
```yaml
Unit Test: JUnit 5, Mockito, AssertJ
Integration Test: @SpringBootTest, Testcontainers
API Test: RestAssured
Performance Test: JMeter, Gatling
```

### DevOps (Optional)
```yaml
Containerization: Docker
CI/CD: GitHub Actions
Monitoring: Prometheus + Grafana (준비)
Logging: Logback + ELK Stack (준비)
```

---

## 시스템 아키텍처

### 전체 아키텍처 (Hexagonal Architecture)

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   REST API   │  │  Exception   │  │   DTO        │      │
│  │  Controller  │  │   Handler    │  │  Converter   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Service    │  │    Facade    │  │  UseCase     │      │
│  │  (비즈니스)   │  │  (오케스트레이션)│  │  (시나리오)  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Aggregate   │  │    Entity    │  │ Value Object │      │
│  │    Root      │  │              │  │              │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│  ┌──────────────┐  ┌──────────────┐                        │
│  │ Domain Event │  │ Domain Service│                       │
│  └──────────────┘  └──────────────┘                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  Infrastructure Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ JPA Repository│ │  Kafka       │  │   Redis      │      │
│  │              │  │  Producer    │  │   Client     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│  ┌──────────────┐  ┌──────────────┐                        │
│  │   PG Gateway │  │  External API│                        │
│  │   Adapter    │  │   Client     │                        │
│  └──────────────┘  └──────────────┘                        │
└─────────────────────────────────────────────────────────────┘
```

### 모듈 구조 (멀티 모듈)

```
paypoint/
├── paypoint-api/              # REST API (Presentation)
├── paypoint-application/      # Application Service
├── paypoint-domain/           # Domain Model (핵심 비즈니스 로직)
│   ├── payment/
│   ├── point/
│   ├── user/
│   └── transaction/
├── paypoint-infrastructure/   # 외부 연동
│   ├── persistence/           # JPA, QueryDSL
│   ├── messaging/             # Kafka
│   └── cache/                 # Redis
├── paypoint-batch/            # Spring Batch
└── paypoint-common/           # 공통 유틸리티
```

### 이벤트 기반 플로우

```
[사용자 결제 요청]
       │
       ▼
[PaymentController] POST /api/v1/payments
       │
       ▼
[PaymentService]
  ├─ 1. 결제 수단 검증
  ├─ 2. 포인트 차감 (동기)
  ├─ 3. PG사 결제 요청
  └─ 4. 결제 이벤트 발행 ──────┐
       │                        │
       ▼                        ▼
  [결제 성공 응답]         [Kafka Topic: payment-completed]
                                │
                                ▼
                         [PointEventListener]
                                │
                                ▼
                         [포인트 적립 (비동기)]
                                │
                                ▼
                         [Kafka Topic: point-earned]
                                │
                                ▼
                         [NotificationService] (Push 알림)
```

---

## 데이터베이스 설계

### ERD 핵심 엔티티

```sql
-- 사용자 (Users)
CREATE TABLE users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    pin VARCHAR(255) NOT NULL,  -- 간편결제 PIN (암호화)
    user_grade VARCHAR(20) DEFAULT 'BASIC',  -- BASIC, SILVER, GOLD, VIP
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 포인트 계좌 (Point Account)
CREATE TABLE point_accounts (
    point_account_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    balance DECIMAL(15,2) DEFAULT 0.00,  -- 현재 잔액
    total_earned DECIMAL(15,2) DEFAULT 0.00,  -- 누적 적립
    total_used DECIMAL(15,2) DEFAULT 0.00,    -- 누적 사용
    version INT DEFAULT 0,  -- 낙관적 락 버전
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    UNIQUE KEY uk_user_id (user_id),
    INDEX idx_balance (balance)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 포인트 거래 내역 (Point Transactions)
CREATE TABLE point_transactions (
    transaction_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    point_account_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,  -- EARN, USE, EXPIRE, GIFT, CANCEL
    amount DECIMAL(15,2) NOT NULL,
    balance_after DECIMAL(15,2) NOT NULL,  -- 거래 후 잔액
    reason VARCHAR(100),
    reference_type VARCHAR(50),  -- PAYMENT, EVENT, ADMIN, etc.
    reference_id VARCHAR(100),
    expiry_date DATE,  -- 포인트 만료일
    status VARCHAR(20) DEFAULT 'COMPLETED',  -- PENDING, COMPLETED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (point_account_id) REFERENCES point_accounts(point_account_id),
    INDEX idx_account_type_date (point_account_id, transaction_type, created_at),
    INDEX idx_expiry_date (expiry_date),
    INDEX idx_reference (reference_type, reference_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 결제 수단 (Payment Methods)
CREATE TABLE payment_methods (
    payment_method_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    method_type VARCHAR(20) NOT NULL,  -- CARD, ACCOUNT
    card_number VARCHAR(255) NOT NULL,  -- 암호화된 카드번호
    card_company VARCHAR(50),
    is_primary BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    INDEX idx_user_primary (user_id, is_primary)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 결제 (Payments)
CREATE TABLE payments (
    payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    payment_method_id BIGINT,
    merchant_id BIGINT NOT NULL,
    order_id VARCHAR(100) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    point_amount DECIMAL(15,2) DEFAULT 0.00,  -- 포인트 사용액
    actual_amount DECIMAL(15,2) NOT NULL,     -- 실결제액
    payment_status VARCHAR(20) NOT NULL,  -- PENDING, APPROVED, FAILED, CANCELLED
    pg_transaction_id VARCHAR(100),  -- PG사 거래 ID
    approved_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (payment_method_id) REFERENCES payment_methods(payment_method_id),
    UNIQUE KEY uk_order_id (order_id),
    INDEX idx_user_status_date (user_id, payment_status, created_at),
    INDEX idx_status_date (payment_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 프로모션 (Promotions)
CREATE TABLE promotions (
    promotion_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    promotion_type VARCHAR(20) NOT NULL,  -- POINT_EARN_RATE, BONUS_POINT, DISCOUNT
    earn_rate DECIMAL(5,2),  -- 적립률 (예: 5.00 = 5%)
    bonus_point DECIMAL(15,2),  -- 보너스 포인트
    target_grade VARCHAR(20),  -- 대상 등급 (NULL이면 전체)
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_dates_status (start_date, end_date, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 파트너 가맹점 (Merchants)
CREATE TABLE merchants (
    merchant_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_name VARCHAR(100) NOT NULL,
    business_number VARCHAR(20) UNIQUE NOT NULL,
    category VARCHAR(50),
    default_earn_rate DECIMAL(5,2) DEFAULT 1.00,  -- 기본 적립률
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 인덱싱 전략

#### 1. 복합 인덱스 (Composite Index)
```sql
-- 사용자별 거래 내역 조회 최적화
CREATE INDEX idx_point_transactions_user_date
ON point_transactions(point_account_id, created_at DESC);

-- 결제 내역 조회 최적화
CREATE INDEX idx_payments_user_status_date
ON payments(user_id, payment_status, created_at DESC);
```

#### 2. 커버링 인덱스 (Covering Index)
```sql
-- 포인트 잔액 조회 시 테이블 접근 없이 인덱스만으로 처리
CREATE INDEX idx_point_balance_covering
ON point_accounts(user_id, balance, updated_at);
```

#### 3. 부분 인덱스 (Partial Index) - MySQL 8.0+
```sql
-- 활성 결제 수단만 인덱싱
CREATE INDEX idx_payment_methods_active
ON payment_methods(user_id, is_primary)
WHERE status = 'ACTIVE';
```

---

## 핵심 비즈니스 로직

### 1. 결제 프로세스 (Payment Flow)

```java
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PointService pointService;
    private final PaymentGateway paymentGateway;
    private final PaymentEventPublisher eventPublisher;
    private final RedisLockService lockService;

    /**
     * 간편결제 실행
     *
     * 비즈니스 규칙:
     * 1. 동일 사용자의 동시 결제는 분산 락으로 방지
     * 2. 포인트 차감은 결제 승인 전에 수행 (원자성)
     * 3. PG 결제 실패 시 포인트 복원 (보상 트랜잭션)
     * 4. 결제 완료 이벤트 발행 (비동기 포인트 적립)
     */
    public PaymentResult processPayment(PaymentRequest request) {
        String lockKey = "payment:lock:" + request.getUserId();

        return lockService.executeWithLock(lockKey, 3000, () -> {
            // 1. 결제 수단 검증
            PaymentMethod paymentMethod = validatePaymentMethod(request);

            // 2. 포인트 사용 (동기 차감)
            if (request.getPointAmount() > 0) {
                pointService.deductPoint(
                    request.getUserId(),
                    request.getPointAmount(),
                    "PAYMENT:" + request.getOrderId()
                );
            }

            try {
                // 3. PG사 결제 요청
                PGResponse pgResponse = paymentGateway.pay(
                    paymentMethod,
                    request.getActualAmount()
                );

                // 4. 결제 엔티티 생성 및 저장
                Payment payment = Payment.create(request, pgResponse);
                payment.approve(pgResponse.getTransactionId());
                paymentRepository.save(payment);

                // 5. 결제 완료 이벤트 발행 (포인트 적립은 비동기)
                eventPublisher.publish(new PaymentCompletedEvent(payment));

                return PaymentResult.success(payment);

            } catch (PaymentGatewayException e) {
                // 6. PG 결제 실패 시 포인트 복원 (보상 트랜잭션)
                if (request.getPointAmount() > 0) {
                    pointService.restorePoint(
                        request.getUserId(),
                        request.getPointAmount(),
                        "PAYMENT_FAILED:" + request.getOrderId()
                    );
                }
                throw new PaymentProcessException("결제 처리 실패", e);
            }
        });
    }

    /**
     * 결제 취소 (전체/부분)
     *
     * 비즈니스 규칙:
     * 1. 부분 취소 가능 (남은 금액만큼 취소)
     * 2. 포인트 복원은 사용한 포인트 비율만큼
     * 3. 취소된 금액에 대한 적립 포인트도 회수
     */
    public CancelResult cancelPayment(Long paymentId, CancelRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // 1. 취소 가능 검증
        payment.validateCancellable(request.getCancelAmount());

        // 2. PG사 취소 요청
        PGCancelResponse pgResponse = paymentGateway.cancel(
            payment.getPgTransactionId(),
            request.getCancelAmount()
        );

        // 3. 결제 상태 업데이트
        if (request.isFullCancel()) {
            payment.cancel();
        } else {
            payment.partialCancel(request.getCancelAmount());
        }

        // 4. 포인트 복원 (비율 계산)
        if (payment.getPointAmount() > 0) {
            BigDecimal restoreRatio = request.getCancelAmount()
                .divide(payment.getTotalAmount(), 4, RoundingMode.HALF_UP);
            BigDecimal restorePoint = payment.getPointAmount()
                .multiply(restoreRatio)
                .setScale(0, RoundingMode.DOWN);

            pointService.restorePoint(
                payment.getUserId(),
                restorePoint,
                "CANCEL:" + paymentId
            );
        }

        // 5. 취소 이벤트 발행 (적립 포인트 회수)
        eventPublisher.publish(new PaymentCancelledEvent(payment, request));

        return CancelResult.success(payment);
    }
}
```

### 2. 포인트 관리 (Point Management)

```java
@Service
@RequiredArgsConstructor
public class PointService {

    private final PointAccountRepository accountRepository;
    private final PointTransactionRepository transactionRepository;
    private final PointPolicyService policyService;
    private final PointCache pointCache;

    /**
     * 포인트 적립
     *
     * 비즈니스 규칙:
     * 1. 등급별 적립률 적용 (BASIC: 1%, SILVER: 2%, GOLD: 3%, VIP: 5%)
     * 2. 진행 중인 프로모션 적립률 추가 적용
     * 3. 적립된 포인트는 365일 후 만료
     * 4. 소수점 이하 버림
     */
    @Transactional
    public PointTransaction earnPoint(Long userId, BigDecimal paymentAmount, String referenceId) {
        // 1. 포인트 계좌 조회 (낙관적 락)
        PointAccount account = accountRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new PointAccountNotFoundException(userId));

        // 2. 적립 정책 조회 (등급별 + 프로모션)
        PointEarnPolicy policy = policyService.getEarnPolicy(
            account.getUserGrade(),
            LocalDate.now()
        );

        // 3. 적립 포인트 계산
        BigDecimal earnAmount = policy.calculate(paymentAmount);

        // 4. 포인트 적립
        account.earn(earnAmount);

        // 5. 거래 내역 생성
        PointTransaction transaction = PointTransaction.createEarn(
            account.getId(),
            earnAmount,
            "PAYMENT:" + referenceId,
            LocalDate.now().plusDays(365)  // 만료일
        );
        transactionRepository.save(transaction);

        // 6. 캐시 무효화
        pointCache.evict(userId);

        return transaction;
    }

    /**
     * 포인트 차감
     *
     * 비즈니스 규칙:
     * 1. 잔액 부족 시 예외 발생
     * 2. 만료일이 빠른 포인트부터 차감 (FIFO)
     * 3. 낙관적 락으로 동시성 제어
     */
    @Transactional
    public PointTransaction deductPoint(Long userId, BigDecimal amount, String referenceId) {
        // 1. 포인트 계좌 조회 (낙관적 락)
        PointAccount account = accountRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new PointAccountNotFoundException(userId));

        // 2. 잔액 검증
        if (!account.canDeduct(amount)) {
            throw new InsufficientPointException(
                String.format("포인트 잔액 부족: 요청=%s, 잔액=%s", amount, account.getBalance())
            );
        }

        // 3. 포인트 차감
        account.deduct(amount);

        // 4. 거래 내역 생성
        PointTransaction transaction = PointTransaction.createUse(
            account.getId(),
            amount,
            referenceId
        );
        transactionRepository.save(transaction);

        // 5. 캐시 무효화
        pointCache.evict(userId);

        return transaction;
    }

    /**
     * 포인트 조회 (캐시 적용)
     */
    @Transactional(readOnly = true)
    public PointBalance getBalance(Long userId) {
        // 1. 캐시 조회
        return pointCache.get(userId, () -> {
            // 2. 캐시 미스 시 DB 조회
            PointAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new PointAccountNotFoundException(userId));

            // 3. 만료 예정 포인트 계산
            BigDecimal expiringPoint = transactionRepository
                .sumExpiringPoint(account.getId(), LocalDate.now().plusDays(30));

            return PointBalance.of(account, expiringPoint);
        });
    }
}
```

### 3. 포인트 만료 배치 (Spring Batch)

```java
@Configuration
@RequiredArgsConstructor
public class PointExpiryBatchConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final PointTransactionRepository transactionRepository;
    private final PointAccountRepository accountRepository;

    /**
     * 포인트 만료 배치 Job
     *
     * 실행 조건: 매일 새벽 2시
     * 처리 방식: 청크 지향 처리 (Chunk Size: 1000)
     * 예상 처리량: 100만 건 / 10분
     */
    @Bean
    public Job pointExpiryJob() {
        return jobBuilderFactory.get("pointExpiryJob")
            .start(expirePointStep())
            .next(updateAccountBalanceStep())
            .build();
    }

    /**
     * Step 1: 만료 대상 포인트 조회 및 만료 처리
     */
    @Bean
    public Step expirePointStep() {
        return stepBuilderFactory.get("expirePointStep")
            .<PointTransaction, PointTransaction>chunk(1000)
            .reader(expiringPointReader())
            .processor(pointExpiryProcessor())
            .writer(pointExpiryWriter())
            .taskExecutor(taskExecutor())  // 멀티 스레드 처리
            .throttleLimit(10)  // 최대 10 스레드
            .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<PointTransaction> expiringPointReader() {
        return new JpaPagingItemReaderBuilder<PointTransaction>()
            .name("expiringPointReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString(
                "SELECT pt FROM PointTransaction pt " +
                "WHERE pt.transactionType = 'EARN' " +
                "AND pt.status = 'COMPLETED' " +
                "AND pt.expiryDate = :targetDate " +
                "ORDER BY pt.id"
            )
            .parameterValues(Map.of("targetDate", LocalDate.now()))
            .pageSize(1000)
            .build();
    }

    @Bean
    public ItemProcessor<PointTransaction, PointTransaction> pointExpiryProcessor() {
        return transaction -> {
            // 만료 처리
            transaction.expire();
            return transaction;
        };
    }

    @Bean
    public JpaItemWriter<PointTransaction> pointExpiryWriter() {
        JpaItemWriter<PointTransaction> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }

    /**
     * Step 2: 계좌 잔액 업데이트 (만료된 포인트 차감)
     */
    @Bean
    public Step updateAccountBalanceStep() {
        return stepBuilderFactory.get("updateAccountBalanceStep")
            .tasklet((contribution, chunkContext) -> {
                // Bulk Update로 성능 최적화
                int updatedCount = accountRepository.deductExpiredPoints(LocalDate.now());
                log.info("포인트 만료 처리 완료: {} 계좌", updatedCount);
                return RepeatStatus.FINISHED;
            })
            .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("point-expiry-");
        executor.initialize();
        return executor;
    }
}
```

---

## API 설계

### API 명세 원칙
1. **RESTful URI 설계**: 리소스 중심, 행위는 HTTP 메서드로 표현
2. **일관된 응답 형식**: 성공/실패 모두 동일한 래퍼 사용
3. **명확한 HTTP 상태 코드**: 2xx(성공), 4xx(클라이언트 오류), 5xx(서버 오류)
4. **페이징 표준화**: `page`, `size`, `sort` 파라미터 통일
5. **에러 코드 체계**: 도메인별 에러 코드 정의

### 공통 응답 형식

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2024-01-15T10:30:00"
}
```

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INSUFFICIENT_POINT",
    "message": "포인트 잔액이 부족합니다.",
    "details": {
      "requested": 10000,
      "balance": 5000
    }
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

### 1. 인증/인가 API

#### 회원가입
```http
POST /api/v1/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecureP@ssw0rd",
  "name": "홍길동",
  "phone": "01012345678",
  "pin": "123456"
}

Response 201 Created
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "name": "홍길동"
  }
}
```

#### 로그인 (JWT 발급)
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecureP@ssw0rd"
}

Response 200 OK
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

### 2. 결제 API

#### 결제 수단 등록
```http
POST /api/v1/payment-methods
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "methodType": "CARD",
  "cardNumber": "1234-5678-9012-3456",
  "cardCompany": "신한카드",
  "isPrimary": true
}

Response 201 Created
{
  "success": true,
  "data": {
    "paymentMethodId": 10,
    "maskedCardNumber": "1234-****-****-3456",
    "isPrimary": true
  }
}
```

#### 간편결제 실행
```http
POST /api/v1/payments
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "orderId": "ORDER-20240115-001",
  "merchantId": 5,
  "paymentMethodId": 10,
  "totalAmount": 50000,
  "pointAmount": 5000,
  "pin": "123456"
}

Response 200 OK
{
  "success": true,
  "data": {
    "paymentId": 1001,
    "orderId": "ORDER-20240115-001",
    "totalAmount": 50000,
    "pointAmount": 5000,
    "actualAmount": 45000,
    "paymentStatus": "APPROVED",
    "approvedAt": "2024-01-15T10:30:00",
    "earnedPoint": 450  // 결제 금액의 1% 적립 예정
  }
}
```

#### 결제 취소
```http
POST /api/v1/payments/{paymentId}/cancel
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "cancelAmount": 50000,
  "reason": "단순 변심"
}

Response 200 OK
{
  "success": true,
  "data": {
    "paymentId": 1001,
    "cancelledAmount": 50000,
    "restoredPoint": 5000,
    "cancelledAt": "2024-01-15T11:00:00"
  }
}
```

#### 결제 내역 조회
```http
GET /api/v1/payments?page=0&size=20&startDate=2024-01-01&endDate=2024-01-31
Authorization: Bearer {accessToken}

Response 200 OK
{
  "success": true,
  "data": {
    "content": [
      {
        "paymentId": 1001,
        "merchantName": "스타벅스",
        "totalAmount": 50000,
        "pointAmount": 5000,
        "actualAmount": 45000,
        "paymentStatus": "APPROVED",
        "approvedAt": "2024-01-15T10:30:00"
      }
    ],
    "pageable": {
      "page": 0,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8
    }
  }
}
```

### 3. 포인트 API

#### 포인트 잔액 조회
```http
GET /api/v1/points/balance
Authorization: Bearer {accessToken}

Response 200 OK
{
  "success": true,
  "data": {
    "balance": 125000,
    "totalEarned": 500000,
    "totalUsed": 375000,
    "expiringPoint": 5000,  // 30일 내 만료 예정
    "expiryDate": "2024-02-15"
  }
}
```

#### 포인트 거래 내역
```http
GET /api/v1/points/transactions?page=0&size=20&type=EARN
Authorization: Bearer {accessToken}

Response 200 OK
{
  "success": true,
  "data": {
    "content": [
      {
        "transactionId": 5001,
        "transactionType": "EARN",
        "amount": 450,
        "balanceAfter": 125000,
        "reason": "결제 적립",
        "expiryDate": "2025-01-15",
        "createdAt": "2024-01-15T10:30:00"
      }
    ],
    "pageable": {
      "page": 0,
      "size": 20,
      "totalElements": 320,
      "totalPages": 16
    }
  }
}
```

#### 포인트 선물하기
```http
POST /api/v1/points/gift
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "recipientPhone": "01098765432",
  "amount": 10000,
  "message": "생일 축하해!"
}

Response 200 OK
{
  "success": true,
  "data": {
    "giftId": 200,
    "amount": 10000,
    "recipientName": "김철수",
    "status": "SENT",
    "sentAt": "2024-01-15T10:30:00"
  }
}
```

### 4. 프로모션 API (관리자)

#### 프로모션 생성
```http
POST /api/v1/admin/promotions
Authorization: Bearer {adminToken}
Content-Type: application/json

{
  "name": "신규 회원 가입 이벤트",
  "promotionType": "BONUS_POINT",
  "bonusPoint": 10000,
  "targetGrade": null,
  "startDate": "2024-01-15",
  "endDate": "2024-02-15"
}

Response 201 Created
{
  "success": true,
  "data": {
    "promotionId": 50,
    "name": "신규 회원 가입 이벤트",
    "status": "ACTIVE"
  }
}
```

---

## 기술적 챌린지와 해결 방안

### Challenge 1: 동시성 제어 - 포인트 잔액 정합성

**문제 상황**
```
사용자 A의 포인트 잔액: 10,000원

시간    Thread 1 (결제1)          Thread 2 (결제2)
T1      잔액 조회: 10,000원
T2                                잔액 조회: 10,000원
T3      5,000원 차감
T4                                7,000원 차감
T5      잔액 저장: 5,000원
T6                                잔액 저장: 3,000원 (❌ 잘못된 잔액!)

실제 잔액: 3,000원 (올바른 값: -2,000원 또는 둘 중 하나 실패)
```

**해결 방안 1: 낙관적 락 (Optimistic Lock)**

```java
@Entity
@Table(name = "point_accounts")
public class PointAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal balance;

    @Version  // JPA 낙관적 락
    private Integer version;

    public void deduct(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientPointException();
        }
        this.balance = this.balance.subtract(amount);
    }
}

// Repository
public interface PointAccountRepository extends JpaRepository<PointAccount, Long> {

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT pa FROM PointAccount pa WHERE pa.userId = :userId")
    Optional<PointAccount> findByUserIdWithLock(@Param("userId") Long userId);
}

// Service
@Transactional
public void deductPoint(Long userId, BigDecimal amount) {
    try {
        PointAccount account = accountRepository.findByUserIdWithLock(userId)
            .orElseThrow();
        account.deduct(amount);
        // version이 변경되었으면 OptimisticLockException 발생
    } catch (OptimisticLockException e) {
        // 재시도 로직
        throw new ConcurrentPointUpdateException("동시 포인트 사용 감지. 다시 시도해주세요.");
    }
}
```

**언제 사용?**
- 충돌이 드문 경우 (대부분의 포인트 사용)
- 재시도가 허용되는 경우

**해결 방안 2: 비관적 락 (Pessimistic Lock)**

```java
public interface PointAccountRepository extends JpaRepository<PointAccount, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pa FROM PointAccount pa WHERE pa.userId = :userId")
    Optional<PointAccount> findByUserIdWithPessimisticLock(@Param("userId") Long userId);
}
```

**언제 사용?**
- 충돌이 빈번한 경우 (선착순 이벤트)
- 반드시 순서 보장이 필요한 경우

**해결 방안 3: 분산 락 (Distributed Lock with Redis)**

```java
@Component
@RequiredArgsConstructor
public class RedisLockService {

    private final RedisTemplate<String, String> redisTemplate;

    public <T> T executeWithLock(String lockKey, long timeoutMs, Supplier<T> supplier) {
        String lockValue = UUID.randomUUID().toString();
        boolean acquired = false;

        try {
            // 락 획득 (NX: 없을 때만 설정, PX: 만료 시간)
            acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, timeoutMs, TimeUnit.MILLISECONDS);

            if (!acquired) {
                throw new LockAcquisitionException("락 획득 실패");
            }

            return supplier.get();

        } finally {
            if (acquired) {
                // Lua 스크립트로 원자적 락 해제 (자신의 락만 해제)
                String script =
                    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "  return redis.call('del', KEYS[1]) " +
                    "else " +
                    "  return 0 " +
                    "end";
                redisTemplate.execute(
                    new DefaultRedisScript<>(script, Long.class),
                    Collections.singletonList(lockKey),
                    lockValue
                );
            }
        }
    }
}
```

**언제 사용?**
- 마이크로서비스 환경 (여러 인스턴스)
- 애플리케이션 레벨 락이 필요한 경우

---

### Challenge 2: 분산 트랜잭션 - 결제 성공, 포인트 적립 실패

**문제 상황**
```
[PaymentService] 결제 승인 → SUCCESS
[PointService] 포인트 적립 → FAILURE (DB 장애, 네트워크 타임아웃 등)

결과: 결제는 완료되었지만 포인트는 적립되지 않음 (데이터 불일치)
```

**해결 방안 1: Saga 패턴 (Choreography)**

```java
// Step 1: 결제 서비스 - 결제 승인 후 이벤트 발행
@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public PaymentResult processPayment(PaymentRequest request) {
        // 1. 결제 승인
        Payment payment = Payment.create(request);
        payment.approve();
        paymentRepository.save(payment);

        // 2. 결제 완료 이벤트 발행 (Kafka)
        PaymentCompletedEvent event = new PaymentCompletedEvent(
            payment.getId(),
            payment.getUserId(),
            payment.getTotalAmount(),
            payment.getApprovedAt()
        );
        kafkaTemplate.send("payment-completed", event);

        return PaymentResult.success(payment);
    }
}

// Step 2: 포인트 서비스 - 이벤트 수신 후 포인트 적립
@Service
@RequiredArgsConstructor
public class PointEventListener {

    private final PointService pointService;
    private final KafkaTemplate<String, PointEvent> kafkaTemplate;

    @KafkaListener(topics = "payment-completed")
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            // 포인트 적립
            pointService.earnPoint(
                event.getUserId(),
                event.getPaymentAmount(),
                "PAYMENT:" + event.getPaymentId()
            );

            // 성공 이벤트 발행
            kafkaTemplate.send("point-earned", new PointEarnedEvent(event));

        } catch (Exception e) {
            // 실패 이벤트 발행 (보상 트랜잭션 트리거)
            kafkaTemplate.send("point-earn-failed", new PointEarnFailedEvent(event, e));
            throw e;  // Dead Letter Queue로 이동
        }
    }

    // Step 3: 보상 트랜잭션 - 포인트 적립 실패 시 알림
    @KafkaListener(topics = "point-earn-failed")
    public void handlePointEarnFailed(PointEarnFailedEvent event) {
        // 관리자 알림 전송
        notificationService.notifyAdmin(
            "포인트 적립 실패",
            String.format("결제 ID: %d, 사용자 ID: %d",
                event.getPaymentId(), event.getUserId())
        );

        // 재시도 큐에 등록 (비동기 재처리)
        retryQueueService.enqueue(event);
    }
}
```

**장점:**
- 서비스 간 느슨한 결합
- 높은 가용성 (한 서비스 장애가 다른 서비스에 영향 없음)

**단점:**
- 복잡한 이벤트 플로우
- 일시적 데이터 불일치 (최종 일관성)

**해결 방안 2: Outbox 패턴**

```java
@Entity
@Table(name = "event_outbox")
public class EventOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private EventStatus status;  // PENDING, PUBLISHED, FAILED

    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    private Integer retryCount;
}

// 결제 승인과 이벤트 저장을 동일 트랜잭션으로 처리
@Service
@Transactional
public class PaymentService {

    public PaymentResult processPayment(PaymentRequest request) {
        // 1. 결제 승인
        Payment payment = createAndApprovePayment(request);
        paymentRepository.save(payment);

        // 2. 이벤트를 Outbox 테이블에 저장 (동일 트랜잭션)
        EventOutbox outbox = EventOutbox.create(
            "PAYMENT_COMPLETED",
            objectMapper.writeValueAsString(new PaymentCompletedEvent(payment)),
            EventStatus.PENDING
        );
        outboxRepository.save(outbox);

        // 3. 트랜잭션 커밋 → 결제와 이벤트가 원자적으로 저장
        return PaymentResult.success(payment);
    }
}

// 별도 스케줄러가 Outbox 이벤트를 Kafka로 발행
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    @Scheduled(fixedDelay = 1000)  // 1초마다 실행
    @Transactional
    public void publishPendingEvents() {
        List<EventOutbox> pendingEvents = outboxRepository
            .findByStatusOrderByCreatedAtAsc(EventStatus.PENDING, PageRequest.of(0, 100));

        for (EventOutbox event : pendingEvents) {
            try {
                // Kafka로 발행
                kafkaTemplate.send(event.getEventType(), event.getPayload());

                // 발행 성공 표시
                event.markAsPublished();

            } catch (Exception e) {
                // 재시도 카운트 증가
                event.incrementRetryCount();

                if (event.getRetryCount() > 5) {
                    event.markAsFailed();
                    // 알림 전송
                }
            }
        }
    }
}
```

**장점:**
- 이벤트 발행 보장 (At-least-once delivery)
- 감사 추적 용이

**단점:**
- 추가 테이블 및 스케줄러 필요

---

### Challenge 3: 대용량 배치 처리 - 1000만 건 포인트 만료

**문제 상황**
- 매일 자정에 만료되는 포인트 처리 필요
- 예상 처리량: 1000만 건
- 요구 처리 시간: 1시간 이내

**해결 방안 1: 청크 지향 처리 + 멀티 스레드**

```java
@Configuration
public class PointExpiryBatchConfig {

    private static final int CHUNK_SIZE = 1000;
    private static final int THREAD_COUNT = 10;

    @Bean
    public Step expirePointStep() {
        return stepBuilderFactory.get("expirePointStep")
            .<PointTransaction, PointTransaction>chunk(CHUNK_SIZE)
            .reader(expiringPointReader())
            .processor(pointExpiryProcessor())
            .writer(pointExpiryWriter())
            .taskExecutor(taskExecutor())
            .throttleLimit(THREAD_COUNT)
            .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(THREAD_COUNT);
        executor.setMaxPoolSize(THREAD_COUNT * 2);
        executor.setQueueCapacity(CHUNK_SIZE * 2);
        executor.initialize();
        return executor;
    }
}
```

**성능 개선:**
- 단일 스레드: 1000만 건 / 60분 = 약 2,777 TPS
- 10 스레드: 약 27,770 TPS (이론적)
- 실제: DB I/O 병목 고려 → 약 15,000 TPS

**해결 방안 2: 파티셔닝 (Partitioning)**

```java
@Bean
public Step masterStep() {
    return stepBuilderFactory.get("masterStep")
        .partitioner("slaveStep", partitioner())
        .step(slaveStep())
        .gridSize(10)  // 10개 파티션
        .taskExecutor(taskExecutor())
        .build();
}

@Bean
public Partitioner partitioner() {
    return gridSize -> {
        Map<String, ExecutionContext> partitions = new HashMap<>();

        // user_id 범위로 파티셔닝
        long minUserId = 1;
        long maxUserId = 10_000_000;
        long rangeSize = (maxUserId - minUserId) / gridSize;

        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();
            context.putLong("minUserId", minUserId + (i * rangeSize));
            context.putLong("maxUserId", minUserId + ((i + 1) * rangeSize));
            partitions.put("partition" + i, context);
        }

        return partitions;
    };
}
```

**해결 방안 3: Bulk Update (최종 최적화)**

```java
@Repository
public interface PointAccountRepository extends JpaRepository<PointAccount, Long> {

    @Modifying
    @Query(value =
        "UPDATE point_accounts pa " +
        "SET pa.balance = pa.balance - (" +
        "  SELECT COALESCE(SUM(pt.amount), 0) " +
        "  FROM point_transactions pt " +
        "  WHERE pt.point_account_id = pa.point_account_id " +
        "  AND pt.expiry_date = :expiryDate " +
        "  AND pt.status = 'COMPLETED'" +
        ") " +
        "WHERE pa.point_account_id IN (" +
        "  SELECT DISTINCT pt2.point_account_id " +
        "  FROM point_transactions pt2 " +
        "  WHERE pt2.expiry_date = :expiryDate" +
        ")",
        nativeQuery = true
    )
    int bulkDeductExpiredPoints(@Param("expiryDate") LocalDate expiryDate);
}
```

**최종 성능:**
- 1000만 건 처리: 약 10-15분
- Bulk Update 활용으로 DB 왕복 횟수 최소화

---

### Challenge 4: 캐싱 전략 - 포인트 조회 성능 vs 정합성

**문제 상황**
- 포인트 잔액 조회는 매우 빈번 (모든 결제 전 조회)
- DB 부하 증가 → 응답 시간 저하
- 캐시 사용 시 실시간 정합성 문제

**해결 방안: Look-Aside 패턴 + TTL + 이벤트 기반 캐시 무효화**

```java
@Service
@RequiredArgsConstructor
public class PointCacheService {

    private final RedisTemplate<String, PointBalance> redisTemplate;
    private final PointAccountRepository accountRepository;

    private static final String CACHE_KEY_PREFIX = "point:balance:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    /**
     * Look-Aside 패턴
     */
    public PointBalance getBalance(Long userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;

        // 1. 캐시 조회
        PointBalance cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 2. 캐시 미스 → DB 조회
        PointBalance balance = loadFromDatabase(userId);

        // 3. 캐시 저장 (TTL 5분)
        redisTemplate.opsForValue().set(cacheKey, balance, CACHE_TTL);

        return balance;
    }

    /**
     * 이벤트 기반 캐시 무효화
     */
    @EventListener
    public void handlePointChanged(PointChangedEvent event) {
        String cacheKey = CACHE_KEY_PREFIX + event.getUserId();
        redisTemplate.delete(cacheKey);
    }
}

// 포인트 변경 시 이벤트 발행
@Service
public class PointService {

    @Transactional
    public void deductPoint(Long userId, BigDecimal amount) {
        // 포인트 차감 로직
        // ...

        // 이벤트 발행 (캐시 무효화 트리거)
        eventPublisher.publishEvent(new PointChangedEvent(userId));
    }
}
```

**캐싱 전략 비교:**

| 전략 | 정합성 | 성능 | 복잡도 | 사용 케이스 |
|-----|--------|------|--------|------------|
| **Cache-Aside** | 최종 일관성 | 높음 | 중 | 조회 빈번, 쓰기 드문 경우 |
| **Write-Through** | 강한 일관성 | 중 | 중 | 정합성 중요 |
| **Write-Behind** | 최종 일관성 | 매우 높음 | 높음 | 쓰기 집약적 |

**PayPoint 선택: Cache-Aside + 이벤트 무효화**
- 이유: 포인트 조회 >> 변경, 5분 TTL로 정합성 허용 범위

---

## 구현 로드맵

### Phase 1: MVP (4주) - 핵심 기능 구현

**Week 1: 기반 구축**
- [ ] 프로젝트 초기 설정 (멀티 모듈, Gradle)
- [ ] MySQL, Redis, Kafka 환경 구성 (Docker Compose)
- [ ] 도메인 모델 설계 (DDD)
- [ ] 기본 인증/인가 (JWT, Spring Security)

**Week 2: 결제 기능**
- [ ] 결제 수단 관리 API
- [ ] 간편결제 실행 (PG 모킹)
- [ ] 결제 취소/환불
- [ ] 결제 내역 조회

**Week 3: 포인트 기능**
- [ ] 포인트 계좌 생성
- [ ] 포인트 적립/사용/조회
- [ ] 포인트 거래 내역
- [ ] 낙관적 락 적용

**Week 4: 통합 및 테스트**
- [ ] 결제-포인트 통합 플로우
- [ ] 단위/통합 테스트 작성
- [ ] API 문서 자동화 (Swagger/Spring REST Docs)
- [ ] 배포 환경 구성 (Docker)

### Phase 2: 비즈니스 로직 강화 (2주)

**Week 5: 고급 기능**
- [ ] 등급별 포인트 적립률
- [ ] 프로모션 엔진 (이벤트 포인트)
- [ ] 부분 취소/환불 로직
- [ ] 포인트 선물하기

**Week 6: 관리 기능**
- [ ] 관리자 대시보드 API
- [ ] 가맹점 관리
- [ ] 정산 기능
- [ ] 통계 API

### Phase 3: 확장성 & 최적화 (2주)

**Week 7: 이벤트 기반 아키텍처**
- [ ] Kafka 프로듀서/컨슈머 구현
- [ ] 비동기 포인트 적립
- [ ] Saga 패턴 적용 (분산 트랜잭션)
- [ ] Dead Letter Queue 처리

**Week 8: 성능 최적화**
- [ ] Redis 캐싱 전략
- [ ] 포인트 만료 배치 (Spring Batch)
- [ ] N+1 쿼리 해결
- [ ] DB 인덱싱 최적화
- [ ] JMeter 성능 테스트

---

## 성능 최적화 전략

### 1. 데이터베이스 최적화

#### 인덱스 설계
```sql
-- 복합 인덱스: 포인트 거래 내역 조회
CREATE INDEX idx_point_tx_account_date
ON point_transactions(point_account_id, created_at DESC)
INCLUDE (transaction_type, amount);

-- 커버링 인덱스: 결제 내역 조회
CREATE INDEX idx_payments_user_covering
ON payments(user_id, payment_status, created_at DESC)
INCLUDE (total_amount, actual_amount);

-- 부분 인덱스: 활성 결제 수단
CREATE INDEX idx_payment_methods_active
ON payment_methods(user_id)
WHERE status = 'ACTIVE';
```

#### 쿼리 최적화
```java
// N+1 문제 해결: Fetch Join
@Query("SELECT p FROM Payment p " +
       "JOIN FETCH p.paymentMethod " +
       "JOIN FETCH p.user " +
       "WHERE p.userId = :userId")
List<Payment> findByUserIdWithDetails(@Param("userId") Long userId);

// Batch Size 설정으로 N+1 완화
@BatchSize(size = 100)
@OneToMany(mappedBy = "pointAccount")
private List<PointTransaction> transactions;
```

### 2. 캐싱 전략

```java
// 계층별 캐싱
@Cacheable(value = "pointBalance", key = "#userId")
public PointBalance getBalance(Long userId) {
    // DB 조회는 캐시 미스 시에만 실행
}

// 캐시 무효화
@CacheEvict(value = "pointBalance", key = "#userId")
public void deductPoint(Long userId, BigDecimal amount) {
    // 포인트 차감 후 캐시 무효화
}

// 다중 캐시 키
@Cacheable(value = "paymentHistory",
    key = "#userId + '_' + #startDate + '_' + #endDate")
public List<Payment> getPaymentHistory(...) {
    // ...
}
```

### 3. 비동기 처리

```java
@Async("pointTaskExecutor")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public CompletableFuture<PointTransaction> earnPointAsync(
    Long userId,
    BigDecimal amount
) {
    PointTransaction transaction = earnPoint(userId, amount);
    return CompletableFuture.completedFuture(transaction);
}

// TaskExecutor 설정
@Bean(name = "pointTaskExecutor")
public TaskExecutor pointTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(20);
    executor.setMaxPoolSize(50);
    executor.setQueueCapacity(200);
    executor.setThreadNamePrefix("point-async-");
    executor.setRejectedExecutionHandler(new CallerRunsPolicy());
    executor.initialize();
    return executor;
}
```

### 4. 성능 목표 및 측정

| 항목 | 목표 | 측정 방법 |
|-----|------|----------|
| **API 응답 시간** | P95 < 200ms | JMeter, APM |
| **TPS (Throughput)** | > 1,000 TPS | 부하 테스트 |
| **결제 동시 처리** | 100 건/초 | Gatling |
| **포인트 조회** | P99 < 100ms | Redis 캐싱 |
| **배치 처리** | 1000만 건 / 15분 | Spring Batch |

---

## 테스트 전략

### 1. 단위 테스트 (Unit Test)

```java
@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private PointAccountRepository accountRepository;

    @Mock
    private PointTransactionRepository transactionRepository;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("포인트 차감 - 잔액 부족 시 예외 발생")
    void deductPoint_InsufficientBalance_ThrowsException() {
        // Given
        Long userId = 1L;
        BigDecimal requestAmount = new BigDecimal("10000");

        PointAccount account = PointAccount.builder()
            .userId(userId)
            .balance(new BigDecimal("5000"))
            .build();

        when(accountRepository.findByUserIdWithLock(userId))
            .thenReturn(Optional.of(account));

        // When & Then
        assertThatThrownBy(() -> pointService.deductPoint(userId, requestAmount, "TEST"))
            .isInstanceOf(InsufficientPointException.class)
            .hasMessageContaining("포인트 잔액 부족");

        verify(transactionRepository, never()).save(any());
    }
}
```

### 2. 통합 테스트 (Integration Test)

```java
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("paypoint_test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PointService pointService;

    @Test
    @DisplayName("결제 성공 시 포인트 차감 및 적립")
    @Transactional
    void processPayment_Success_DeductsAndEarnsPoint() {
        // Given
        Long userId = 1L;
        BigDecimal totalAmount = new BigDecimal("50000");
        BigDecimal pointAmount = new BigDecimal("5000");

        PaymentRequest request = PaymentRequest.builder()
            .userId(userId)
            .totalAmount(totalAmount)
            .pointAmount(pointAmount)
            .build();

        // When
        PaymentResult result = paymentService.processPayment(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPayment().getPaymentStatus())
            .isEqualTo(PaymentStatus.APPROVED);

        // 포인트 차감 확인
        PointBalance balance = pointService.getBalance(userId);
        assertThat(balance.getBalance()).isLessThan(initialBalance);

        // 비동기 적립은 이벤트 발행 확인으로 대체
        verify(eventPublisher).publish(any(PaymentCompletedEvent.class));
    }
}
```

### 3. API 테스트 (REST Assured)

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentApiTest {

    @LocalServerPort
    private int port;

    private String accessToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        accessToken = loginAndGetToken("user@example.com", "password");
    }

    @Test
    @Order(1)
    @DisplayName("간편결제 API - 성공")
    void processPayment_Success() {
        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "orderId": "ORDER-001",
                  "merchantId": 1,
                  "paymentMethodId": 1,
                  "totalAmount": 50000,
                  "pointAmount": 5000,
                  "pin": "123456"
                }
                """)
        .when()
            .post("/api/v1/payments")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.paymentStatus", equalTo("APPROVED"))
            .body("data.actualAmount", equalTo(45000));
    }
}
```

### 4. 성능 테스트 (JMeter)

```xml
<!-- JMeter Test Plan: 결제 부하 테스트 -->
<jmeterTestPlan>
  <ThreadGroup>
    <stringProp name="ThreadGroup.num_threads">100</stringProp>
    <stringProp name="ThreadGroup.ramp_time">10</stringProp>
    <stringProp name="ThreadGroup.duration">60</stringProp>
  </ThreadGroup>

  <HTTPSamplerProxy>
    <stringProp name="HTTPSampler.domain">localhost</stringProp>
    <stringProp name="HTTPSampler.port">8080</stringProp>
    <stringProp name="HTTPSampler.path">/api/v1/payments</stringProp>
    <stringProp name="HTTPSampler.method">POST</stringProp>
  </HTTPSamplerProxy>
</jmeterTestPlan>
```

**목표:**
- 동시 사용자 100명
- 60초 동안 지속
- P95 응답 시간 < 200ms
- 에러율 < 0.1%

---

## 추가 고려 사항

### 1. 보안

- **민감 정보 암호화**: 카드 번호, PIN (AES-256)
- **SQL Injection 방어**: PreparedStatement, JPA
- **XSS 방어**: Input Validation, Output Encoding
- **CSRF 방어**: SameSite Cookie, CSRF Token
- **API Rate Limiting**: Redis 기반 (100 req/min per user)

### 2. 모니터링

```yaml
# Actuator 설정
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    tags:
      application: paypoint
```

**주요 모니터링 지표:**
- API 응답 시간 (P50, P95, P99)
- TPS (Transactions Per Second)
- 에러율
- DB Connection Pool 사용률
- Redis Hit Rate
- Kafka Consumer Lag

### 3. 문서화

- **API 문서**: Swagger UI 또는 Spring REST Docs
- **아키텍처 다이어그램**: C4 Model
- **ERD**: dbdiagram.io
- **README**: 프로젝트 설명, 실행 방법, 기술 스택

---

## 다음 단계

이 문서를 바탕으로 다음과 같이 진행할 수 있습니다:

1. **아키텍처 설계서 작성**
   - 상세 시스템 아키텍처 다이어그램
   - 도메인 모델 클래스 다이어그램
   - 시퀀스 다이어그램 (핵심 플로우)

2. **ERD 상세 설계**
   - ERD 도구로 시각화 (draw.io, dbdiagram.io)
   - 테이블 명세서 작성
   - 마이그레이션 스크립트 (Flyway)

3. **API 명세서 작성**
   - OpenAPI 3.0 스펙 작성
   - 샘플 요청/응답 정의
   - 에러 코드 체계 정리

4. **프로젝트 초기 설정**
   - 멀티 모듈 구조 생성
   - Docker Compose 환경 구성
   - CI/CD 파이프라인 설계 (GitHub Actions)

어떤 부분을 먼저 진행하고 싶으신가요?