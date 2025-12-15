# StyleHub - 패션 통합 커머스 플랫폼

> 백엔드 개발자 포트폴리오 프로젝트 (경력직 대상)
> **타겟 기업:** 무신사, 29CM, 카카오스타일, 토스 등 커머스/핀테크 기업
> **기술 스택:** Java 21 + Spring Boot + MySQL + Redis + Elasticsearch + Kafka + React

---

## 📋 목차

1. [프로젝트 개요](#프로젝트-개요)
2. [백엔드 개발자 어필 포인트](#백엔드-개발자-어필-포인트)
3. [기술 스택 상세](#기술-스택-상세)
4. [시스템 아키텍처](#시스템-아키텍처)
5. [데이터베이스 설계](#데이터베이스-설계)
6. [핵심 비즈니스 로직](#핵심-비즈니스-로직)
7. [Kafka 이벤트 아키텍처](#kafka-이벤트-아키텍처)
8. [API 설계](#api-설계)
9. [프론트엔드 구조](#프론트엔드-구조)
10. [기술적 챌린지와 해결 방안](#기술적-챌린지와-해결-방안)
11. [구현 로드맵](#구현-로드맵)
12. [성능 최적화 전략](#성능-최적화-전략)
13. [테스트 전략](#테스트-전략)

---

## 프로젝트 개요

### 비즈니스 목표
무신사, 올리브영 스타일의 멀티 브랜드 패션/뷰티 통합 플랫폼으로, 실시간 재고 관리와 이벤트 기반 아키텍처를 통한 확장 가능한 B2C 커머스 시스템

### 핵심 가치
- **사용자**: 빠른 상품 검색, 실시간 재고 알림, 안정적인 주문/결제
- **관리자**: 실시간 판매 통계, 자동화된 정산, 효율적인 재고 관리
- **기술적**: 이벤트 기반 확장성, 마이크로서비스 전환 가능 구조

### 주요 기능
1. **상품 관리 & 검색** (Elasticsearch 기반 고성능 검색)
2. **실시간 재고 관리** (동시성 제어, 재입고 알림)
3. **주문/결제 시스템** (분산 트랜잭션, Saga Pattern)
4. **이벤트 기반 아키텍처** (Kafka 4가지 패턴)
5. **실시간 통계 & 분석** (Kafka Streams)
6. **프론트엔드 시연** (React + TypeScript)

---

## 백엔드 개발자 어필 포인트

### 🎯 왜 이 프로젝트가 경력직 포트폴리오에 적합한가?

#### 1. Java 21 최신 기술 활용

```java
// Virtual Threads (Project Loom) - 대량 동시 요청 처리
@Configuration
public class VirtualThreadConfig {

    @Bean
    public TaskExecutor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ExecutorService executor =
        Executors.newVirtualThreadPerTaskExecutor();

    // Virtual Thread로 수천 개 주문 동시 처리
    public CompletableFuture<Order> createOrderAsync(OrderRequest request) {
        return CompletableFuture.supplyAsync(
            () -> createOrder(request),
            executor
        );
    }
}

// Record Pattern Matching - 간결한 DTO
public record ProductSearchRequest(
    String keyword,
    Long categoryId,
    List<Long> brandIds,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    SortType sort,
    int page,
    int size
) {
    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }

    public boolean hasPriceRange() {
        return minPrice != null || maxPrice != null;
    }
}

// Sequenced Collections - 더 직관적인 컬렉션 API
List<Product> products = getProducts();
Product firstProduct = products.getFirst();  // Java 21
Product lastProduct = products.getLast();
products.reversed().forEach(System.out::println);
```

#### 2. 동시성 제어 전문성 (커머스 핵심 역량)

**시나리오**: 한정판 스니커즈 100개, 동시 주문 1000건 → 100건만 성공

```java
@Service
@Transactional
@RequiredArgsConstructor
public class InventoryService {

    private final ProductOptionRepository optionRepository;
    private final RedisLockService redisLockService;

    /**
     * 낙관적 락: 일반 재고 차감
     * - @Version으로 동시성 제어
     * - 충돌 시 OptimisticLockException 발생 → 재시도
     */
    public void deductStock(Long optionId, int quantity) {
        ProductOption option = optionRepository
            .findByIdWithOptimisticLock(optionId)
            .orElseThrow(() -> new ProductOptionNotFoundException(optionId));

        if (!option.canDeduct(quantity)) {
            throw new OutOfStockException(
                String.format("재고 부족: 요청=%d, 재고=%d",
                    quantity, option.getStockQuantity())
            );
        }

        option.deduct(quantity);
    }

    /**
     * 분산 락: 선착순 한정판 상품
     * - Redis SETNX로 락 획득
     * - Lua 스크립트로 원자적 락 해제
     */
    public void deductLimitedStock(Long optionId, int quantity) {
        String lockKey = "stock:lock:" + optionId;

        redisLockService.executeWithLock(lockKey, 3000, () -> {
            ProductOption option = optionRepository.findById(optionId)
                .orElseThrow();

            if (!option.canDeduct(quantity)) {
                throw new OutOfStockException("한정판 상품이 품절되었습니다.");
            }

            option.deduct(quantity);
            optionRepository.save(option);

            return null;
        });
    }
}

@Component
@RequiredArgsConstructor
public class RedisLockService {

    private final RedisTemplate<String, String> redisTemplate;

    public <T> T executeWithLock(String lockKey, long timeoutMs, Supplier<T> supplier) {
        String lockValue = UUID.randomUUID().toString();
        boolean acquired = false;

        try {
            // 락 획득 시도
            acquired = Boolean.TRUE.equals(
                redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, timeoutMs, TimeUnit.MILLISECONDS)
            );

            if (!acquired) {
                throw new LockAcquisitionException("락 획득 실패. 다시 시도해주세요.");
            }

            return supplier.get();

        } finally {
            if (acquired) {
                // Lua 스크립트로 안전하게 락 해제 (자신의 락만 해제)
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

#### 3. 고도화된 Kafka 이벤트 아키텍처

**4가지 핵심 패턴 구현**:

```java
// Pattern 1: Saga Pattern (분산 트랜잭션)
@Service
public class OrderSagaOrchestrator {

    @Transactional
    public Order startOrderSaga(OrderRequest request) {
        // Step 1: 주문 생성 + 재고 차감
        Order order = orderService.createOrderWithStockDeduction(request);

        // Step 2: 결제 이벤트 발행 (비동기)
        kafkaTemplate.send("order-events", new OrderCreatedEvent(order));

        return order;
    }

    @KafkaListener(topics = "order-events", groupId = "payment-processor")
    public void handleOrderCreated(OrderCreatedEvent event) {
        try {
            Payment payment = paymentService.processPayment(event);
            kafkaTemplate.send("payment-events",
                new PaymentCompletedEvent(event.getOrderId(), payment));
        } catch (PaymentException e) {
            // 보상 트랜잭션: 재고 복원
            kafkaTemplate.send("payment-events",
                new PaymentFailedEvent(event.getOrderId(), e));
        }
    }
}

// Pattern 2: CDC (Change Data Capture) - Elasticsearch 자동 동기화
@KafkaListener(topics = "mysql.stylehub.products", groupId = "elasticsearch-sync")
public void handleProductChange(DebeziumChangeEvent event) {
    switch (event.getOperation()) {
        case CREATE, UPDATE -> {
            Long productId = event.getAfter().getLong("product_id");
            Product product = productRepository.findById(productId).orElseThrow();

            // Elasticsearch 인덱스 자동 업데이트
            ProductDocument document = ProductDocument.from(product);
            elasticsearchClient.index(i -> i
                .index("products")
                .id(String.valueOf(productId))
                .document(document)
            );
        }
        case DELETE -> {
            elasticsearchClient.delete(d -> d
                .index("products")
                .id(event.getBefore().getString("product_id"))
            );
        }
    }
}

// Pattern 3: Event-Driven Notification (실시간 알림)
@Service
public class StockNotificationService {

    private final Sinks.Many<StockNotification> notificationSink =
        Sinks.many().multicast().onBackpressureBuffer();

    @KafkaListener(topics = "inventory-events")
    public void handleStockRestored(StockRestoredEvent event) {
        List<StockNotificationRequest> subscribers =
            findActiveSubscribers(event.getProductId());

        subscribers.forEach(subscriber -> {
            StockNotification notification = createNotification(subscriber, event);
            // SSE로 실시간 전송
            notificationSink.tryEmitNext(notification);
        });
    }
}

// Pattern 4: Kafka Streams (실시간 통계)
@Configuration
public class OrderAnalyticsStreamConfig {

    @Bean
    public KStream<String, OrderCompletedEvent> orderAnalyticsStream(
        StreamsBuilder builder
    ) {
        KStream<String, OrderCompletedEvent> orderStream =
            builder.stream("order-events");

        // 1분 윈도우로 주문 건수 집계
        orderStream
            .groupBy((key, value) -> value.getOrderedMinute())
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
            .count(Materialized.as("order-count-by-minute"))
            .toStream()
            .to("analytics-order-count");

        // 상품별 판매량 집계
        orderStream
            .flatMapValues(order -> order.getItems())
            .groupBy((key, item) -> item.getProductId().toString())
            .aggregate(
                () -> new ProductSalesStats(),
                (productId, item, stats) -> {
                    stats.incrementQuantity(item.getQuantity());
                    stats.addRevenue(item.getTotalPrice());
                    return stats;
                },
                Materialized.as("product-sales-stats")
            );

        return orderStream;
    }
}
```

#### 4. Elasticsearch 고성능 검색 엔진

```java
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 복합 상품 검색
     * - 키워드 검색 (Nori 형태소 분석)
     * - 복합 필터링 (가격, 브랜드, 카테고리, 평점)
     * - 다중 정렬 (인기도, 가격, 최신순, 리뷰순)
     * - Aggregation (필터 개수 집계)
     */
    public SearchResult search(ProductSearchRequest request) {
        // 1. 캐시 확인
        String cacheKey = generateCacheKey(request);
        SearchResult cached = getFromCache(cacheKey);
        if (cached != null) return cached;

        // 2. Elasticsearch 쿼리 빌드
        BoolQueryBuilder query = QueryBuilders.boolQuery();

        // 키워드 검색 (must)
        if (request.hasKeyword()) {
            query.must(
                QueryBuilders.multiMatchQuery(request.getKeyword())
                    .field("name", 3.0f)      // 상품명 가중치 3배
                    .field("brand_name", 2.0f) // 브랜드명 가중치 2배
                    .field("description")
                    .analyzer("nori")          // 한글 형태소 분석
                    .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
            );
        }

        // 필터링 (filter - 점수 영향 없음)
        if (request.hasCategoryId()) {
            query.filter(QueryBuilders.termQuery("category_id", request.getCategoryId()));
        }

        if (request.hasBrandIds()) {
            query.filter(QueryBuilders.termsQuery("brand_id", request.getBrandIds()));
        }

        if (request.hasPriceRange()) {
            query.filter(
                QueryBuilders.rangeQuery("base_price")
                    .gte(request.getMinPrice())
                    .lte(request.getMaxPrice())
            );
        }

        // 정렬
        List<SortBuilder<?>> sorts = buildSorts(request.getSort());

        // Aggregation (필터 카운트)
        AggregationBuilder brandAgg = AggregationBuilders
            .terms("brands").field("brand_id").size(100);

        AggregationBuilder priceRangeAgg = AggregationBuilders
            .range("price_ranges")
            .field("base_price")
            .addRange(0, 50000)
            .addRange(50000, 100000)
            .addRange(100000, 200000)
            .addRange(200000, Double.MAX_VALUE);

        // 검색 실행
        SearchRequest searchRequest = new SearchRequest("products")
            .source(new SearchSourceBuilder()
                .query(query)
                .from(request.getOffset())
                .size(request.getSize())
                .sorts(sorts)
                .aggregation(brandAgg)
                .aggregation(priceRangeAgg)
            );

        SearchResponse response = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchResult result = convertToSearchResult(response);

        // 캐시 저장 (5분)
        saveToCache(cacheKey, result, Duration.ofMinutes(5));

        return result;
    }

    /**
     * 자동완성
     */
    public List<String> autocomplete(String keyword) {
        CompletionSuggestionBuilder suggestionBuilder =
            SuggestBuilders.completionSuggestion("name_suggest")
                .prefix(keyword)
                .size(10);

        SuggestBuilder suggestBuilder = new SuggestBuilder()
            .addSuggestion("product-suggest", suggestionBuilder);

        SearchRequest searchRequest = new SearchRequest("products")
            .source(new SearchSourceBuilder().suggest(suggestBuilder));

        SearchResponse response = elasticsearchClient.search(searchRequest);
        return extractSuggestions(response);
    }
}
```

#### 5. 복잡한 도메인 모델링 (DDD)

```java
// Aggregate Root: Order
public class Order {
    private OrderId id;
    private UserId userId;
    private List<OrderItem> items;
    private Money totalAmount;
    private Money discountAmount;
    private Money finalAmount;
    private OrderStatus status;
    private LocalDateTime orderedAt;

    /**
     * 비즈니스 규칙: 주문 생성
     *
     * 1. 재고 검증 (동시성 제어)
     * 2. 금액 계산 (할인 적용)
     * 3. 도메인 이벤트 발행
     */
    public static Order create(List<OrderItem> items, InventoryChecker inventoryChecker) {
        // 1. 재고 확인
        for (OrderItem item : items) {
            if (!inventoryChecker.isAvailable(item.getOptionId(), item.getQuantity())) {
                throw new OutOfStockException(
                    String.format("%s 상품의 재고가 부족합니다.", item.getProductName())
                );
            }
        }

        // 2. 금액 계산
        Money totalAmount = items.stream()
            .map(OrderItem::getTotalPrice)
            .reduce(Money.ZERO, Money::add);

        Money discountAmount = Money.ZERO; // 할인 로직
        Money finalAmount = totalAmount.subtract(discountAmount);

        // 3. 주문 생성
        Order order = new Order(items, totalAmount, discountAmount, finalAmount);

        // 4. 도메인 이벤트 발행
        order.registerEvent(new OrderCreatedEvent(order));

        return order;
    }

    /**
     * 비즈니스 규칙: 주문 취소
     */
    public void cancel(CancelReason reason) {
        // 취소 가능 상태 검증
        if (!this.status.isCancellable()) {
            throw new OrderNotCancellableException(
                String.format("주문 상태가 %s이므로 취소할 수 없습니다.", this.status)
            );
        }

        // 배송 중인 경우 추가 검증
        if (this.status == OrderStatus.SHIPPING) {
            throw new OrderNotCancellableException("배송 중인 주문은 취소할 수 없습니다.");
        }

        // 상태 변경
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();

        // 도메인 이벤트 발행 (재고 복원 트리거)
        this.registerEvent(new OrderCancelledEvent(this.id, this.items, reason));
    }

    /**
     * 비즈니스 규칙: 결제 완료
     */
    public void markAsPaid(PaymentId paymentId) {
        if (this.status != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException("대기 중인 주문만 결제할 수 있습니다.");
        }

        this.status = OrderStatus.PAID;
        this.paymentId = paymentId;
        this.paidAt = LocalDateTime.now();

        this.registerEvent(new OrderPaidEvent(this.id, paymentId));
    }
}

// Value Object: Money
public record Money(BigDecimal amount) {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("금액은 null일 수 없습니다.");
        }
        if (amount.scale() > 2) {
            amount = amount.setScale(2, RoundingMode.HALF_UP);
        }
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)));
    }

    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }
}
```

#### 6. QueryDSL 고급 활용 (복잡한 동적 쿼리)

```java
@Repository
@RequiredArgsConstructor
public class ProductQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 복잡한 동적 검색 쿼리
     * - 다중 조건 필터링
     * - 동적 정렬
     * - N+1 문제 해결 (Fetch Join)
     */
    public Page<ProductDto> searchProducts(ProductSearchCondition condition, Pageable pageable) {

        List<ProductDto> content = queryFactory
            .select(Projections.constructor(ProductDto.class,
                product.id,
                product.name,
                product.basePrice,
                brand.name,
                category.name,
                product.viewCount,
                product.reviewCount,
                product.rating.avg(),
                product.status
            ))
            .from(product)
            .leftJoin(product.brand, brand)
            .leftJoin(product.category, category)
            .leftJoin(product.reviews, review)
            .where(
                categoryEq(condition.getCategoryId()),
                brandIn(condition.getBrandIds()),
                priceBetween(condition.getMinPrice(), condition.getMaxPrice()),
                ratingGoe(condition.getMinRating()),
                statusEq(ProductStatus.ON_SALE)
            )
            .groupBy(product.id, brand.name, category.name)
            .orderBy(getSortOrder(condition.getSort()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = queryFactory
            .select(product.count())
            .from(product)
            .where(
                categoryEq(condition.getCategoryId()),
                brandIn(condition.getBrandIds()),
                priceBetween(condition.getMinPrice(), condition.getMaxPrice()),
                ratingGoe(condition.getMinRating()),
                statusEq(ProductStatus.ON_SALE)
            )
            .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * N+1 문제 해결: Fetch Join
     */
    public List<Order> findOrdersWithDetails(Long userId) {
        return queryFactory
            .selectFrom(order)
            .join(order.items, orderItem).fetchJoin()
            .join(orderItem.productOption, productOption).fetchJoin()
            .join(productOption.product, product).fetchJoin()
            .join(product.brand, brand).fetchJoin()
            .where(order.userId.eq(userId))
            .orderBy(order.orderedAt.desc())
            .fetch();
    }

    // 동적 WHERE 조건
    private BooleanExpression categoryEq(Long categoryId) {
        return categoryId != null ? product.category.id.eq(categoryId) : null;
    }

    private BooleanExpression brandIn(List<Long> brandIds) {
        return brandIds != null && !brandIds.isEmpty()
            ? product.brand.id.in(brandIds)
            : null;
    }

    private BooleanExpression priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null) {
            return product.basePrice.between(minPrice, maxPrice);
        } else if (minPrice != null) {
            return product.basePrice.goe(minPrice);
        } else if (maxPrice != null) {
            return product.basePrice.loe(maxPrice);
        }
        return null;
    }

    private BooleanExpression ratingGoe(BigDecimal minRating) {
        return minRating != null ? product.rating.goe(minRating) : null;
    }

    private BooleanExpression statusEq(ProductStatus status) {
        return status != null ? product.status.eq(status) : null;
    }

    // 동적 정렬
    private OrderSpecifier<?>[] getSortOrder(SortType sortType) {
        return switch (sortType) {
            case POPULAR -> new OrderSpecifier[] {
                product.viewCount.desc(),
                product.reviewCount.desc()
            };
            case PRICE_ASC -> new OrderSpecifier[] {
                product.basePrice.asc()
            };
            case PRICE_DESC -> new OrderSpecifier[] {
                product.basePrice.desc()
            };
            case LATEST -> new OrderSpecifier[] {
                product.createdAt.desc()
            };
            case REVIEW -> new OrderSpecifier[] {
                product.reviewCount.desc(),
                product.rating.desc()
            };
            default -> new OrderSpecifier[] {
                product.id.desc()
            };
        };
    }
}
```

#### 7. 풀스택 구현 역량 (React + TypeScript)

```typescript
// hooks/useStockNotification.ts
// SSE로 실시간 재입고 알림 수신

import { useEffect, useState } from 'react';
import { toast } from 'sonner';

interface StockNotification {
  productId: number;
  productName: string;
  restockedQuantity: number;
  notifiedAt: string;
}

export function useStockNotification(userId: number) {
  const [notifications, setNotifications] = useState<StockNotification[]>([]);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    const eventSource = new EventSource(
      `${import.meta.env.VITE_API_URL}/api/v1/notifications/stock/subscribe`,
      { withCredentials: true }
    );

    eventSource.onopen = () => {
      setIsConnected(true);
      console.log('[SSE] Stock notification connected');
    };

    eventSource.addEventListener('stock-restocked', (event) => {
      const notification: StockNotification = JSON.parse(event.data);

      setNotifications(prev => [notification, ...prev]);

      // Toast 알림
      toast.success(
        `${notification.productName} 재입고!`,
        {
          description: `${notification.restockedQuantity}개 입고되었습니다.`,
          action: {
            label: '보러가기',
            onClick: () => window.location.href = `/products/${notification.productId}`
          }
        }
      );
    });

    eventSource.onerror = () => {
      setIsConnected(false);
      console.error('[SSE] Connection error');
    };

    return () => {
      eventSource.close();
    };
  }, [userId]);

  return { notifications, isConnected };
}
```

---

## 기술 스택 상세

### Backend Framework

```yaml
Language: Java 21 (LTS)
Framework: Spring Boot 3.3.x
Build Tool: Gradle 8.x

Java 21 주요 기능:
  - Virtual Threads (Project Loom) - 대량 동시 요청 블로킹 없이 처리
  - Record Pattern Matching - DTO 간결화
  - Sequenced Collections - 컬렉션 API 개선
  - String Templates (Preview) - 문자열 처리 개선

Spring Modules:
  - Spring Web (REST API)
  - Spring Data JPA (Hibernate 6.4+)
  - Spring Data Elasticsearch 5.x
  - Spring Data Redis
  - Spring Security 6.x
  - Spring Batch 5.x
  - Spring Cloud Stream (Kafka)
  - Spring WebFlux (SSE 실시간)
  - Spring Validation
```

### Database & Storage

```yaml
Primary DB: MySQL 8.0
  - InnoDB 스토리지 엔진
  - 트랜잭션 ACID 보장
  - Row-Level Locking
  - 주문, 상품, 재고 데이터

Cache: Redis 7.x
  - 상품 정보 캐싱 (Look-Aside Pattern)
  - 분산 락 (재고 차감 동시성 제어)
  - 실시간 재고 카운터
  - 세션 관리
  - Rate Limiting

Search Engine: Elasticsearch 8.x
  - 상품 전문 검색
  - Nori 형태소 분석기 (한글)
  - Aggregation (필터 카운트)
  - Completion Suggester (자동완성)

Query Builder: QueryDSL 5.x
  - 타입 세이프 쿼리
  - 복잡한 동적 검색 쿼리
  - N+1 문제 해결 (Fetch Join)
```

### Message Queue

```yaml
Apache Kafka 3.6.x:
  - Event-Driven Architecture 핵심
  - 주문/결제/재고 이벤트 발행/구독
  - Saga Pattern 분산 트랜잭션
  - Dead Letter Queue (실패 처리)

Kafka Connect:
  - Debezium MySQL CDC
  - MySQL → Elasticsearch 자동 동기화

Kafka Streams:
  - 실시간 주문 통계
  - 상품별 판매량 집계
  - Interactive Query
```

### Frontend

```yaml
Framework: React 18
Language: TypeScript 5.x
Build Tool: Vite 5.x

주요 라이브러리:
  - React Router 6 (SPA 라우팅)
  - TanStack Query v5 (서버 상태 관리)
  - Zustand (클라이언트 상태 관리)
  - Axios (HTTP 클라이언트)
  - Tailwind CSS (스타일링)
  - shadcn/ui (UI 컴포넌트)
  - Sonner (Toast 알림)

목적:
  - 백엔드 API 시연용 간단한 UI
  - SSE 실시간 알림 확인
  - 상품 검색, 주문 플로우 시연
```

### DevOps & Tools

```yaml
Containerization: Docker & Docker Compose
CI/CD: GitHub Actions (Optional)
Monitoring:
  - Spring Boot Actuator
  - Prometheus + Grafana (준비)
Logging: Logback + ELK Stack (준비)
API Documentation: Swagger (SpringDoc OpenAPI 3)
Testing:
  - JUnit 5
  - Mockito
  - Testcontainers
  - RestAssured
```

---

## 시스템 아키텍처

### 전체 아키텍처 (Hexagonal Architecture + Event-Driven)

```
┌──────────────────────────────────────────────────────────────────┐
│                    Frontend Layer (React)                        │
│                                                                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐    │
│  │ 상품검색  │  │   주문    │  │ 실시간   │  │   대시보드   │    │
│  │  페이지   │  │  페이지   │  │ 알림     │  │   (관리자)   │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────┘    │
│         │             │              │                │           │
│         ▼             ▼              ▼                ▼           │
│    REST API      REST API         SSE          WebSocket         │
└──────────────────────────────────────────────────────────────────┘
                              │
┌──────────────────────────────────────────────────────────────────┐
│                  Presentation Layer (Spring MVC)                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   REST API   │  │   Exception  │  │   Security   │          │
│  │  Controller  │  │    Handler   │  │    Filter    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└──────────────────────────────────────────────────────────────────┘
                              │
┌──────────────────────────────────────────────────────────────────┐
│                    Application Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Service    │  │    Facade    │  │   UseCase    │          │
│  │ (비즈니스)    │  │(오케스트레이션)│  │  (시나리오)  │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└──────────────────────────────────────────────────────────────────┘
                              │
┌──────────────────────────────────────────────────────────────────┐
│                      Domain Layer (DDD)                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  Aggregate   │  │    Entity    │  │ Value Object │          │
│  │    Root      │  │              │  │              │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│  ┌──────────────┐  ┌──────────────┐                            │
│  │Domain Service│  │ Domain Event │                            │
│  └──────────────┘  └──────────────┘                            │
└──────────────────────────────────────────────────────────────────┘
                              │
┌──────────────────────────────────────────────────────────────────┐
│                  Infrastructure Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │JPA Repository│  │    Kafka     │  │    Redis     │          │
│  │              │  │  Producer    │  │   Client     │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│  ┌──────────────┐  ┌──────────────┐                            │
│  │Elasticsearch │  │  External    │                            │
│  │   Client     │  │  API Client  │                            │
│  └──────────────┘  └──────────────┘                            │
└──────────────────────────────────────────────────────────────────┘
```

### Kafka 이벤트 플로우

```
┌──────────────────────────────────────────────────────────────────┐
│                         Kafka Cluster                            │
│                                                                   │
│  [Topics]                                                         │
│  ├─ product-events      (상품 생성/수정/삭제)                    │
│  ├─ order-events        (주문 생성/취소)                         │
│  ├─ payment-events      (결제 완료/실패)                         │
│  ├─ shipping-events     (배송 준비/완료)                         │
│  ├─ inventory-events    (재고 차감/복원)                         │
│  ├─ analytics-events    (통계 데이터)                            │
│  └─ mysql.products      (CDC - Debezium)                         │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
       │           │            │            │            │
       ▼           ▼            ▼            ▼            ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│   Saga   │ │  Stock   │ │Elasticsearch│ │Analytics│ │ Kafka  │
│Orchestra │ │  Alert   │ │   Sync    │ │ Streams │ │Connect │
│  -tor    │ │  Service │ │  Service  │ │         │ │(Debezium)│
└──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘
       │           │            │            │            │
       ▼           ▼            ▼            ▼            ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│  MySQL   │ │  Redis   │ │Elasticsearch│ │ MySQL  │ │  MySQL   │
│(주문/재고)│ │(캐시/락) │ │  (검색)   │ │(통계 DB)│ │(원본 DB) │
└──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘
```

### 모듈 구조 (멀티 모듈)

```
stylehub/
├── stylehub-api/              # REST API (Presentation Layer)
│   ├── src/main/java/
│   │   └── com/stylehub/api/
│   │       ├── controller/
│   │       ├── dto/
│   │       └── config/
│   └── build.gradle
│
├── stylehub-application/      # Application Service
│   ├── src/main/java/
│   │   └── com/stylehub/application/
│   │       ├── service/
│   │       ├── facade/
│   │       └── usecase/
│   └── build.gradle
│
├── stylehub-domain/           # Domain Model (핵심 비즈니스 로직)
│   ├── src/main/java/
│   │   └── com/stylehub/domain/
│   │       ├── product/
│   │       ├── order/
│   │       ├── inventory/
│   │       ├── user/
│   │       └── common/
│   └── build.gradle
│
├── stylehub-infrastructure/   # 외부 연동
│   ├── src/main/java/
│   │   └── com/stylehub/infrastructure/
│   │       ├── persistence/   # JPA, QueryDSL
│   │       ├── messaging/     # Kafka
│   │       ├── cache/         # Redis
│   │       └── search/        # Elasticsearch
│   └── build.gradle
│
├── stylehub-batch/            # Spring Batch
│   └── build.gradle
│
├── stylehub-common/           # 공통 유틸리티
│   └── build.gradle
│
├── stylehub-frontend/         # React Frontend
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── hooks/
│   │   ├── api/
│   │   └── stores/
│   └── package.json
│
├── docker/
│   └── docker-compose.yml
│
├── build.gradle
└── settings.gradle
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
    user_grade VARCHAR(20) DEFAULT 'BASIC',  -- BASIC, SILVER, GOLD, VIP
    status VARCHAR(20) DEFAULT 'ACTIVE',     -- ACTIVE, INACTIVE, SUSPENDED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 브랜드 (Brands)
CREATE TABLE brands (
    brand_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    logo_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 카테고리 (Categories)
CREATE TABLE categories (
    category_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT,
    name VARCHAR(100) NOT NULL,
    level INT NOT NULL DEFAULT 1,
    sort_order INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES categories(category_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_level (level),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 상품 (Products)
CREATE TABLE products (
    product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    brand_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    base_price DECIMAL(15,2) NOT NULL,
    discount_rate DECIMAL(5,2) DEFAULT 0.00,  -- 할인율 (%)
    status VARCHAR(20) DEFAULT 'ON_SALE',     -- ON_SALE, SOLD_OUT, DISCONTINUED
    view_count INT DEFAULT 0,
    review_count INT DEFAULT 0,
    rating DECIMAL(3,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (brand_id) REFERENCES brands(brand_id),
    FOREIGN KEY (category_id) REFERENCES categories(category_id),
    INDEX idx_brand_category (brand_id, category_id),
    INDEX idx_status_rating (status, rating DESC),
    INDEX idx_created_at (created_at DESC),
    FULLTEXT INDEX ft_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 상품 옵션 (Product Options)
CREATE TABLE product_options (
    option_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    option_name VARCHAR(100),              -- "블랙-L", "레드-M"
    additional_price DECIMAL(15,2) DEFAULT 0.00,
    stock_quantity INT NOT NULL DEFAULT 0,
    version INT DEFAULT 0,                 -- 낙관적 락
    status VARCHAR(20) DEFAULT 'AVAILABLE', -- AVAILABLE, SOLD_OUT
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    INDEX idx_product_status (product_id, status),
    INDEX idx_stock (stock_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 상품 이미지 (Product Images)
CREATE TABLE product_images (
    image_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    image_order INT DEFAULT 0,
    is_thumbnail BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    INDEX idx_product_order (product_id, image_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 주문 (Orders)
CREATE TABLE orders (
    order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    discount_amount DECIMAL(15,2) DEFAULT 0.00,
    final_amount DECIMAL(15,2) NOT NULL,
    order_status VARCHAR(20) NOT NULL,     -- PENDING, PAID, SHIPPING, COMPLETED, CANCELLED
    payment_method VARCHAR(20),            -- CARD, BANK_TRANSFER
    delivery_address TEXT NOT NULL,
    delivery_request VARCHAR(200),
    ordered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP NULL,
    shipped_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    cancel_reason VARCHAR(200),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    UNIQUE KEY uk_order_number (order_number),
    INDEX idx_user_status_date (user_id, order_status, ordered_at DESC),
    INDEX idx_status_date (order_status, ordered_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 주문 상품 (Order Items)
CREATE TABLE order_items (
    order_item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    option_id BIGINT,
    product_name VARCHAR(200) NOT NULL,
    option_name VARCHAR(100),
    brand_name VARCHAR(100),
    quantity INT NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    total_price DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    FOREIGN KEY (option_id) REFERENCES product_options(option_id),
    INDEX idx_order (order_id),
    INDEX idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 결제 (Payments)
CREATE TABLE payments (
    payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL UNIQUE,
    payment_amount DECIMAL(15,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,   -- PENDING, COMPLETED, FAILED, CANCELLED
    pg_transaction_id VARCHAR(100),        -- PG사 거래 ID
    pg_name VARCHAR(50),                   -- PG사 이름
    approved_at TIMESTAMP NULL,
    failed_reason VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    INDEX idx_status_date (payment_status, created_at DESC),
    INDEX idx_pg_transaction_id (pg_transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 배송 (Shippings)
CREATE TABLE shippings (
    shipping_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL UNIQUE,
    courier_name VARCHAR(50),
    tracking_number VARCHAR(100),
    shipping_status VARCHAR(20) DEFAULT 'PREPARING', -- PREPARING, SHIPPED, IN_TRANSIT, DELIVERED
    shipped_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    INDEX idx_status_date (shipping_status, created_at DESC),
    INDEX idx_tracking_number (tracking_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 재입고 알림 신청 (Stock Notifications)
CREATE TABLE stock_notifications (
    notification_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    option_id BIGINT,
    status VARCHAR(20) DEFAULT 'ACTIVE',   -- ACTIVE, NOTIFIED, CANCELLED
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notified_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    FOREIGN KEY (option_id) REFERENCES product_options(option_id),
    INDEX idx_product_option_status (product_id, option_id, status),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 리뷰 (Reviews)
CREATE TABLE reviews (
    review_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content TEXT,
    has_images BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'ACTIVE',    -- ACTIVE, DELETED, HIDDEN
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id),
    INDEX idx_product_status_date (product_id, status, created_at DESC),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 리뷰 이미지 (Review Images)
CREATE TABLE review_images (
    image_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    image_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (review_id) REFERENCES reviews(review_id),
    INDEX idx_review_order (review_id, image_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 장바구니 (Cart Items)
CREATE TABLE cart_items (
    cart_item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    FOREIGN KEY (option_id) REFERENCES product_options(option_id),
    UNIQUE KEY uk_user_option (user_id, option_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 인덱싱 전략

#### 1. 복합 인덱스 (Composite Index)

```sql
-- 사용자별 주문 내역 조회 최적화
CREATE INDEX idx_orders_user_status_date
ON orders(user_id, order_status, ordered_at DESC);

-- 상품 검색 최적화 (브랜드 + 카테고리)
CREATE INDEX idx_products_brand_category_status
ON products(brand_id, category_id, status, rating DESC);

-- 상품별 리뷰 조회 최적화
CREATE INDEX idx_reviews_product_status_date
ON reviews(product_id, status, created_at DESC);
```

#### 2. 커버링 인덱스 (Covering Index)

```sql
-- 상품 목록 조회 시 테이블 접근 없이 인덱스만으로 처리
CREATE INDEX idx_products_list_covering
ON products(category_id, status, base_price, rating, view_count)
INCLUDE (name, brand_id);

-- 주문 금액 집계 시 커버링
CREATE INDEX idx_order_items_amount_covering
ON order_items(order_id, total_price);
```

#### 3. 부분 인덱스 (Partial Index)

```sql
-- 활성 상품만 인덱싱
CREATE INDEX idx_products_active
ON products(category_id, rating DESC)
WHERE status = 'ON_SALE';

-- 활성 재입고 알림만 인덱싱
CREATE INDEX idx_stock_notifications_active
ON stock_notifications(product_id, option_id)
WHERE status = 'ACTIVE';
```

---

## 핵심 비즈니스 로직

### 1. 주문 생성 프로세스

```java
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final RedisLockService redisLockService;

    /**
     * 주문 생성
     *
     * 비즈니스 규칙:
     * 1. 동일 사용자의 동시 주문은 분산 락으로 방지
     * 2. 재고 차감은 주문 생성과 동일 트랜잭션
     * 3. 재고 부족 시 즉시 실패
     * 4. 주문 생성 이벤트 발행 (결제 처리 트리거)
     */
    public Order createOrder(OrderRequest request) {
        String lockKey = "order:lock:user:" + request.getUserId();

        return redisLockService.executeWithLock(lockKey, 3000, () -> {
            // 1. 주문 엔티티 생성 (도메인 모델)
            Order order = Order.create(
                request.getUserId(),
                request.getItems(),
                request.getDeliveryAddress(),
                request.getDeliveryRequest()
            );

            // 2. 재고 차감 (동일 트랜잭션)
            for (OrderItem item : order.getItems()) {
                try {
                    inventoryService.deductStock(
                        item.getOptionId(),
                        item.getQuantity()
                    );
                } catch (OutOfStockException e) {
                    throw new OrderCreationException(
                        String.format("%s 상품의 재고가 부족합니다.", item.getProductName())
                    );
                }
            }

            // 3. 주문 저장
            orderRepository.save(order);

            // 4. 주문 생성 이벤트 발행 (Kafka)
            OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getUserId(),
                order.getFinalAmount(),
                order.getItems()
            );
            kafkaTemplate.send("order-events", event);

            log.info("Order created: orderId={}, userId={}, amount={}",
                order.getId(), order.getUserId(), order.getFinalAmount());

            return order;
        });
    }

    /**
     * 주문 취소
     *
     * 비즈니스 규칙:
     * 1. 취소 가능 상태 검증 (PENDING, PAID만 가능)
     * 2. 주문 취소 이벤트 발행 (재고 복원 트리거)
     */
    @Transactional
    public void cancelOrder(Long orderId, CancelReason reason) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 도메인 모델에서 비즈니스 규칙 검증
        order.cancel(reason);

        // 주문 취소 이벤트 발행
        OrderCancelledEvent event = new OrderCancelledEvent(
            order.getId(),
            order.getItems(),
            reason
        );
        kafkaTemplate.send("order-events", event);

        log.info("Order cancelled: orderId={}, reason={}", orderId, reason);
    }
}
```

### 2. 재고 관리 (동시성 제어)

```java
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductOptionRepository optionRepository;
    private final RedisLockService redisLockService;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    /**
     * 재고 차감 (낙관적 락)
     *
     * 동시성 제어:
     * - @Version으로 낙관적 락 적용
     * - 충돌 시 OptimisticLockException 발생
     * - 호출측에서 재시도 로직 처리
     */
    @Transactional
    public void deductStock(Long optionId, int quantity) {
        ProductOption option = optionRepository
            .findByIdWithOptimisticLock(optionId)
            .orElseThrow(() -> new ProductOptionNotFoundException(optionId));

        // 도메인 모델에서 비즈니스 규칙 검증
        if (!option.canDeduct(quantity)) {
            throw new OutOfStockException(
                String.format("재고 부족: 요청=%d, 재고=%d",
                    quantity, option.getStockQuantity())
            );
        }

        // 재고 차감
        option.deduct(quantity);

        // 품절 처리
        if (option.getStockQuantity() == 0) {
            option.markAsSoldOut();
        }

        // 재고 변동 이벤트 발행
        StockDeductedEvent event = new StockDeductedEvent(
            option.getProductId(),
            optionId,
            quantity,
            option.getStockQuantity()
        );
        kafkaTemplate.send("inventory-events", event);

        log.debug("Stock deducted: optionId={}, quantity={}, remaining={}",
            optionId, quantity, option.getStockQuantity());
    }

    /**
     * 재고 복원 (주문 취소 시)
     *
     * 비즈니스 규칙:
     * - 재고 0 → 양수로 복원 시 재입고 알림 발행
     */
    @Transactional
    public void restoreStock(Long optionId, int quantity) {
        ProductOption option = optionRepository.findById(optionId)
            .orElseThrow(() -> new ProductOptionNotFoundException(optionId));

        int previousStock = option.getStockQuantity();

        // 재고 복원
        option.restore(quantity);

        // 재입고 알림 발행 (품절 → 재입고)
        if (previousStock == 0 && option.getStockQuantity() > 0) {
            option.markAsAvailable();

            StockRestoredEvent event = new StockRestoredEvent(
                option.getProductId(),
                optionId,
                option.getProduct().getName(),
                option.getOptionName(),
                quantity
            );
            kafkaTemplate.send("inventory-events", event);

            log.info("Stock restocked: optionId={}, quantity={}, total={}",
                optionId, quantity, option.getStockQuantity());
        }
    }

    /**
     * 선착순 한정판 재고 차감 (분산 락)
     *
     * 동시성 제어:
     * - Redis 분산 락으로 동시 접근 완전 차단
     * - 선착순 이벤트 등 경쟁이 치열한 경우 사용
     */
    @Transactional
    public void deductLimitedStock(Long optionId, int quantity) {
        String lockKey = "stock:limited:lock:" + optionId;

        redisLockService.executeWithLock(lockKey, 3000, () -> {
            ProductOption option = optionRepository.findById(optionId)
                .orElseThrow();

            if (!option.canDeduct(quantity)) {
                throw new OutOfStockException("한정판 상품이 품절되었습니다.");
            }

            option.deduct(quantity);
            optionRepository.save(option);

            if (option.getStockQuantity() == 0) {
                option.markAsSoldOut();
            }

            log.info("Limited stock deducted: optionId={}, remaining={}",
                optionId, option.getStockQuantity());

            return null;
        });
    }
}
```

### 3. 상품 검색 (Elasticsearch)

```java
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 복합 상품 검색
     *
     * 기능:
     * - 키워드 검색 (Nori 형태소 분석)
     * - 복합 필터링 (가격, 브랜드, 카테고리, 평점)
     * - 다중 정렬 (인기도, 가격, 최신순, 리뷰순)
     * - Aggregation (필터 개수)
     * - Redis 캐싱 (5분 TTL)
     */
    public SearchResult search(ProductSearchRequest request) {
        // 1. 캐시 확인
        String cacheKey = generateCacheKey(request);
        SearchResult cached = (SearchResult) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache hit: {}", cacheKey);
            return cached;
        }

        // 2. Elasticsearch 쿼리 빌드
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // 키워드 검색 (must)
        if (request.hasKeyword()) {
            boolQuery.must(
                QueryBuilders.multiMatchQuery(request.getKeyword())
                    .field("name", 3.0f)      // 상품명 가중치 3배
                    .field("brand_name", 2.0f) // 브랜드명 가중치 2배
                    .field("description")
                    .analyzer("nori")          // 한글 형태소 분석
                    .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
            );
        }

        // 필터링 (filter - 점수 영향 없음)
        if (request.getCategoryId() != null) {
            boolQuery.filter(
                QueryBuilders.termQuery("category_id", request.getCategoryId())
            );
        }

        if (request.getBrandIds() != null && !request.getBrandIds().isEmpty()) {
            boolQuery.filter(
                QueryBuilders.termsQuery("brand_id", request.getBrandIds())
            );
        }

        if (request.hasPrice Range()) {
            boolQuery.filter(
                QueryBuilders.rangeQuery("base_price")
                    .gte(request.getMinPrice())
                    .lte(request.getMaxPrice())
            );
        }

        if (request.getMinRating() != null) {
            boolQuery.filter(
                QueryBuilders.rangeQuery("rating")
                    .gte(request.getMinRating())
            );
        }

        // 판매 중인 상품만
        boolQuery.filter(QueryBuilders.termQuery("status", "ON_SALE"));

        // 3. 정렬
        List<SortBuilder<?>> sorts = buildSorts(request.getSort());

        // 4. Aggregation (필터 개수)
        AggregationBuilder brandAgg = AggregationBuilders
            .terms("brands")
            .field("brand_id")
            .size(100);

        AggregationBuilder priceRangeAgg = AggregationBuilders
            .range("price_ranges")
            .field("base_price")
            .addRange(0, 50000)
            .addRange(50000, 100000)
            .addRange(100000, 200000)
            .addRange(200000, Double.MAX_VALUE);

        // 5. 검색 실행
        SearchRequest searchRequest = new SearchRequest("products")
            .source(new SearchSourceBuilder()
                .query(boolQuery)
                .from(request.getPage() * request.getSize())
                .size(request.getSize())
                .sorts(sorts)
                .aggregation(brandAgg)
                .aggregation(priceRangeAgg)
            );

        try {
            SearchResponse response = elasticsearchClient.search(
                searchRequest,
                RequestOptions.DEFAULT
            );

            SearchResult result = convertToSearchResult(response);

            // 6. 캐시 저장 (5분)
            redisTemplate.opsForValue().set(
                cacheKey,
                result,
                Duration.ofMinutes(5)
            );

            log.info("Product search: keyword={}, total={}, took={}ms",
                request.getKeyword(),
                result.getTotalCount(),
                response.getTook().millis());

            return result;

        } catch (IOException e) {
            throw new SearchException("상품 검색 실패", e);
        }
    }

    /**
     * 자동완성
     */
    public List<String> autocomplete(String keyword) {
        CompletionSuggestionBuilder suggestionBuilder =
            SuggestBuilders.completionSuggestion("name_suggest")
                .prefix(keyword)
                .size(10)
                .skipDuplicates(true);

        SuggestBuilder suggestBuilder = new SuggestBuilder()
            .addSuggestion("product-suggest", suggestionBuilder);

        SearchRequest searchRequest = new SearchRequest("products")
            .source(new SearchSourceBuilder().suggest(suggestBuilder));

        try {
            SearchResponse response = elasticsearchClient.search(
                searchRequest,
                RequestOptions.DEFAULT
            );

            return extractSuggestions(response);

        } catch (IOException e) {
            log.error("Autocomplete failed: keyword={}", keyword, e);
            return Collections.emptyList();
        }
    }

    private List<SortBuilder<?>> buildSorts(SortType sortType) {
        return switch (sortType) {
            case POPULAR -> List.of(
                SortBuilders.fieldSort("view_count").order(SortOrder.DESC),
                SortBuilders.fieldSort("review_count").order(SortOrder.DESC),
                SortBuilders.scoreSort()
            );
            case PRICE_ASC -> List.of(
                SortBuilders.fieldSort("base_price").order(SortOrder.ASC)
            );
            case PRICE_DESC -> List.of(
                SortBuilders.fieldSort("base_price").order(SortOrder.DESC)
            );
            case LATEST -> List.of(
                SortBuilders.fieldSort("created_at").order(SortOrder.DESC)
            );
            case REVIEW -> List.of(
                SortBuilders.fieldSort("review_count").order(SortOrder.DESC),
                SortBuilders.fieldSort("rating").order(SortOrder.DESC)
            );
            default -> List.of(SortBuilders.scoreSort());
        };
    }
}
```

---

## Kafka 이벤트 아키텍처

### Pattern 1: Saga Pattern (분산 트랜잭션)

**목적**: 주문-결제-배송 분산 트랜잭션 처리 및 보상 트랜잭션

```java
/**
 * Saga Orchestrator
 *
 * Flow:
 * 1. 주문 생성 → order-events 발행
 * 2. 결제 처리 → payment-events 발행
 * 3. 배송 준비 → shipping-events 발행
 *
 * 실패 시 보상 트랜잭션:
 * - 결제 실패 → 재고 복원
 * - 배송 실패 → 결제 취소 + 재고 복원
 */
@Service
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final ShippingService shippingService;
    private final InventoryService inventoryService;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    /**
     * Saga 시작: 주문 생성
     */
    @Transactional
    public Order startOrderSaga(OrderRequest request) {
        // Step 1: 주문 생성 + 재고 차감 (동일 트랜잭션)
        Order order = orderService.createOrderWithStockDeduction(request);

        // Step 2: 결제 이벤트 발행 (비동기)
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getUserId(),
            order.getFinalAmount(),
            order.getItems()
        );
        kafkaTemplate.send("order-events", event);

        return order;
    }

    /**
     * Step 2: 결제 처리
     */
    @KafkaListener(topics = "order-events",
                   groupId = "payment-processor",
                   containerFactory = "virtualThreadKafkaListenerFactory")
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        try {
            // 결제 승인 (Virtual Thread에서 블로킹 없이 처리)
            Payment payment = paymentService.processPayment(
                event.getOrderId(),
                event.getFinalAmount(),
                event.getPaymentMethod()
            );

            // 결제 성공 이벤트
            PaymentCompletedEvent successEvent = new PaymentCompletedEvent(
                event.getOrderId(),
                payment.getId(),
                payment.getApprovedAt()
            );
            kafkaTemplate.send("payment-events", successEvent);

            log.info("Payment completed: orderId={}, paymentId={}",
                event.getOrderId(), payment.getId());

        } catch (PaymentException e) {
            // 결제 실패 → 보상 트랜잭션 이벤트
            PaymentFailedEvent failureEvent = new PaymentFailedEvent(
                event.getOrderId(),
                e.getMessage()
            );
            kafkaTemplate.send("payment-events", failureEvent);

            log.error("Payment failed: orderId={}, reason={}",
                event.getOrderId(), e.getMessage());
        }
    }

    /**
     * Step 3: 배송 준비
     */
    @KafkaListener(topics = "payment-events",
                   groupId = "shipping-processor",
                   filter = "paymentCompletedFilter")
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            // 배송 정보 생성
            Shipping shipping = shippingService.prepareShipping(
                event.getOrderId()
            );

            // 배송 준비 완료 이벤트
            ShippingPreparedEvent preparedEvent = new ShippingPreparedEvent(
                event.getOrderId(),
                shipping.getId(),
                shipping.getTrackingNumber()
            );
            kafkaTemplate.send("shipping-events", preparedEvent);

            log.info("Shipping prepared: orderId={}, shippingId={}",
                event.getOrderId(), shipping.getId());

        } catch (ShippingException e) {
            // 배송 실패 → 보상 트랜잭션 이벤트
            ShippingFailedEvent failureEvent = new ShippingFailedEvent(
                event.getOrderId(),
                e.getMessage()
            );
            kafkaTemplate.send("shipping-events", failureEvent);

            log.error("Shipping failed: orderId={}, reason={}",
                event.getOrderId(), e.getMessage());
        }
    }

    /**
     * 보상 트랜잭션: 결제 실패 → 재고 복원
     */
    @KafkaListener(topics = "payment-events",
                   groupId = "saga-compensator",
                   filter = "paymentFailedFilter")
    @Transactional
    public void compensateStockOnPaymentFailed(PaymentFailedEvent event) {
        // 주문 취소
        Order order = orderService.cancelOrder(
            event.getOrderId(),
            CancelReason.PAYMENT_FAILED
        );

        // 재고 복원
        for (OrderItem item : order.getItems()) {
            inventoryService.restoreStock(
                item.getOptionId(),
                item.getQuantity()
            );
        }

        log.warn("Saga compensation: orderId={}, reason=payment-failed",
            event.getOrderId());
    }

    /**
     * 보상 트랜잭션: 배송 실패 → 결제 취소 + 재고 복원
     */
    @KafkaListener(topics = "shipping-events",
                   groupId = "saga-compensator",
                   filter = "shippingFailedFilter")
    @Transactional
    public void compensateOnShippingFailed(ShippingFailedEvent event) {
        Order order = orderService.findById(event.getOrderId());

        // 결제 취소
        if (order.getPaymentId() != null) {
            paymentService.cancelPayment(order.getPaymentId());
        }

        // 재고 복원
        for (OrderItem item : order.getItems()) {
            inventoryService.restoreStock(
                item.getOptionId(),
                item.getQuantity()
            );
        }

        // 주문 취소
        orderService.cancelOrder(
            event.getOrderId(),
            CancelReason.SHIPPING_FAILED
        );

        log.warn("Saga full compensation: orderId={}, reason=shipping-failed",
            event.getOrderId());
    }
}
```

### Pattern 2: CDC (Change Data Capture) - Elasticsearch 자동 동기화

**목적**: MySQL 상품 데이터 변경 시 Elasticsearch 인덱스 자동 업데이트

```yaml
# docker-compose.yml

services:
  kafka-connect:
    image: debezium/connect:2.5
    container_name: kafka-connect
    ports:
      - "8083:8083"
    environment:
      - BOOTSTRAP_SERVERS=kafka:9092
      - GROUP_ID=1
      - CONFIG_STORAGE_TOPIC=connect_configs
      - OFFSET_STORAGE_TOPIC=connect_offsets
      - STATUS_STORAGE_TOPIC=connect_status
    depends_on:
      - kafka
      - mysql
```

```json
// Debezium MySQL Connector 설정
{
  "name": "mysql-connector",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "mysql",
    "database.port": "3306",
    "database.user": "debezium",
    "database.password": "dbz",
    "database.server.id": "184054",
    "database.server.name": "mysql",
    "database.include.list": "stylehub",
    "table.include.list": "stylehub.products,stylehub.brands,stylehub.categories",
    "database.history.kafka.bootstrap.servers": "kafka:9092",
    "database.history.kafka.topic": "schema-changes.stylehub",
    "include.schema.changes": "false",
    "topic.prefix": "mysql",
    "transforms": "route",
    "transforms.route.type": "org.apache.kafka.connect.transforms.RegexRouter",
    "transforms.route.regex": "([^.]+)\\.([^.]+)\\.([^.]+)",
    "transforms.route.replacement": "$3"
  }
}
```

```java
/**
 * CDC Consumer: MySQL 변경사항 → Elasticsearch 인덱싱
 */
@Service
@RequiredArgsConstructor
public class ProductIndexSyncService {

    private final ElasticsearchClient elasticsearchClient;
    private final ProductRepository productRepository;

    /**
     * CDC 이벤트 처리: products 테이블 변경
     */
    @KafkaListener(topics = "mysql.stylehub.products",
                   groupId = "elasticsearch-sync")
    public void handleProductChange(DebeziumChangeEvent event) {

        switch (event.getOperation()) {
            case CREATE, UPDATE -> {
                // MySQL에서 변경된 상품 조회
                Long productId = event.getAfter().getLong("product_id");

                Product product = productRepository
                    .findByIdWithDetails(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));

                // Elasticsearch 인덱스 업데이트
                ProductDocument document = ProductDocument.from(product);

                try {
                    elasticsearchClient.index(i -> i
                        .index("products")
                        .id(String.valueOf(productId))
                        .document(document)
                    );

                    log.info("Indexed product to Elasticsearch: productId={}", productId);

                } catch (IOException e) {
                    log.error("Failed to index product: productId={}", productId, e);
                    throw new IndexingException("Elasticsearch 인덱싱 실패", e);
                }
            }

            case DELETE -> {
                // 삭제된 상품 인덱스에서도 제거
                Long productId = event.getBefore().getLong("product_id");

                try {
                    elasticsearchClient.delete(d -> d
                        .index("products")
                        .id(String.valueOf(productId))
                    );

                    log.info("Deleted product from Elasticsearch: productId={}", productId);

                } catch (IOException e) {
                    log.error("Failed to delete product: productId={}", productId, e);
                }
            }
        }
    }

    /**
     * 전체 재인덱싱 (초기 설정 또는 동기화 복구)
     */
    @Transactional(readOnly = true)
    public void reindexAll() {
        log.info("Starting full reindex");

        List<Product> products = productRepository.findAllWithDetails();

        int successCount = 0;
        int failCount = 0;

        for (Product product : products) {
            try {
                ProductDocument document = ProductDocument.from(product);

                elasticsearchClient.index(i -> i
                    .index("products")
                    .id(String.valueOf(product.getId()))
                    .document(document)
                );

                successCount++;

            } catch (IOException e) {
                log.error("Failed to reindex product: productId={}",
                    product.getId(), e);
                failCount++;
            }
        }

        log.info("Reindex completed: success={}, fail={}, total={}",
            successCount, failCount, products.size());
    }
}
```

### Pattern 3: Event-Driven Notification (실시간 알림)

**목적**: 재고 복원 이벤트 → 구독자에게 SSE로 실시간 알림 전송

```java
/**
 * 실시간 재입고 알림 서비스
 */
@Service
@RequiredArgsConstructor
public class StockNotificationService {

    private final StockNotificationRepository notificationRepository;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    // SSE 알림 스트림 (Reactor Sink)
    private final Sinks.Many<StockNotification> notificationSink =
        Sinks.many().multicast().onBackpressureBuffer();

    /**
     * Kafka Consumer: 재고 복원 이벤트 수신
     */
    @KafkaListener(topics = "inventory-events",
                   groupId = "notification-service",
                   containerFactory = "virtualThreadKafkaListenerFactory")
    @Transactional
    public void handleStockRestored(StockRestoredEvent event) {
        // 1. 재입고 알림 신청자 조회
        List<StockNotificationRequest> subscribers =
            notificationRepository.findActiveSubscribers(
                event.getProductId(),
                event.getOptionId()
            );

        if (subscribers.isEmpty()) {
            log.debug("No subscribers for restocked product: productId={}, optionId={}",
                event.getProductId(), event.getOptionId());
            return;
        }

        // 2. 각 구독자에게 실시간 알림 전송
        for (StockNotificationRequest subscriber : subscribers) {
            StockNotification notification = StockNotification.builder()
                .userId(subscriber.getUserId())
                .productId(event.getProductId())
                .productName(event.getProductName())
                .optionName(event.getOptionName())
                .restockedQuantity(event.getQuantity())
                .notifiedAt(LocalDateTime.now())
                .build();

            // SSE로 실시간 전송
            notificationSink.tryEmitNext(notification);

            // 알림 발송 완료 표시
            subscriber.markAsNotified();
            notificationRepository.save(subscriber);
        }

        log.info("Sent {} stock notifications for product {}",
            subscribers.size(), event.getProductId());
    }

    /**
     * SSE Endpoint: 사용자별 알림 구독
     */
    public Flux<StockNotification> subscribeNotifications(Long userId) {
        return notificationSink.asFlux()
            .filter(notification -> notification.getUserId().equals(userId))
            .doOnSubscribe(sub ->
                log.info("User {} subscribed to stock notifications", userId))
            .doOnCancel(() ->
                log.info("User {} unsubscribed from stock notifications", userId));
    }

    /**
     * 재입고 알림 신청
     */
    @Transactional
    public StockNotificationRequest requestNotification(
        Long userId,
        Long productId,
        Long optionId
    ) {
        // 중복 신청 확인
        Optional<StockNotificationRequest> existing =
            notificationRepository.findActiveRequest(userId, productId, optionId);

        if (existing.isPresent()) {
            return existing.get();
        }

        // 신청 생성
        StockNotificationRequest request = StockNotificationRequest.builder()
            .userId(userId)
            .productId(productId)
            .optionId(optionId)
            .status(NotificationStatus.ACTIVE)
            .requestedAt(LocalDateTime.now())
            .build();

        notificationRepository.save(request);

        log.info("Stock notification requested: userId={}, productId={}, optionId={}",
            userId, productId, optionId);

        return request;
    }
}

/**
 * REST Controller: SSE 엔드포인트
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final StockNotificationService notificationService;

    /**
     * 재입고 알림 구독 (SSE)
     */
    @GetMapping(value = "/stock/subscribe",
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<StockNotification>> subscribeStockNotifications(
        @AuthenticationPrincipal UserPrincipal user
    ) {
        return notificationService
            .subscribeNotifications(user.getUserId())
            .map(notification -> ServerSentEvent.<StockNotification>builder()
                .id(String.valueOf(notification.getId()))
                .event("stock-restocked")
                .data(notification)
                .build()
            )
            .doOnNext(event ->
                log.debug("Sent SSE event to user {}: {}",
                    user.getUserId(), event.data()));
    }

    /**
     * 재입고 알림 신청
     */
    @PostMapping("/stock/request")
    public ResponseEntity<ApiResponse<NotificationResponse>> requestStockNotification(
        @AuthenticationPrincipal UserPrincipal user,
        @RequestBody @Valid StockNotificationRequestDto request
    ) {
        StockNotificationRequest result = notificationService.requestNotification(
            user.getUserId(),
            request.getProductId(),
            request.getOptionId()
        );

        NotificationResponse response = NotificationResponse.from(result);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

### Pattern 4: Kafka Streams (실시간 통계)

**목적**: 주문 이벤트 스트림 → 실시간 통계 집계 → Interactive Query

```java
/**
 * Kafka Streams 설정: 실시간 주문 통계
 */
@Configuration
@EnableKafkaStreams
public class OrderAnalyticsStreamConfig {

    /**
     * 주문 통계 스트림
     *
     * 집계 항목:
     * - 시간대별 주문 건수 (1분 윈도우)
     * - 상품별 판매량
     * - 카테고리별 매출
     */
    @Bean
    public KStream<String, OrderCompletedEvent> orderAnalyticsStream(
        StreamsBuilder builder
    ) {
        // 1. 주문 완료 이벤트 스트림
        KStream<String, OrderCompletedEvent> orderStream =
            builder.stream("order-events",
                Consumed.with(Serdes.String(), orderEventSerde())
            );

        // 2. 시간대별 주문 통계 (1분 윈도우)
        orderStream
            .groupBy((key, value) -> value.getOrderedAt().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            ))
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
            .count(Materialized.as("order-count-by-minute"))
            .toStream()
            .to("analytics-order-count", Produced.with(
                WindowedSerdes.timeWindowedSerdeFrom(String.class),
                Serdes.Long()
            ));

        // 3. 상품별 판매량 집계
        orderStream
            .flatMapValues(order -> order.getItems())
            .groupBy((key, item) -> item.getProductId().toString())
            .aggregate(
                () -> new ProductSalesStats(),
                (productId, item, stats) -> {
                    stats.incrementQuantity(item.getQuantity());
                    stats.addRevenue(item.getTotalPrice());
                    return stats;
                },
                Materialized.as("product-sales-stats")
            )
            .toStream()
            .to("analytics-product-sales");

        // 4. 카테고리별 매출 집계
        orderStream
            .flatMapValues(order -> order.getItems())
            .groupBy((key, item) -> item.getCategoryId().toString())
            .aggregate(
                () -> BigDecimal.ZERO,
                (categoryId, item, revenue) ->
                    revenue.add(item.getTotalPrice()),
                Materialized.as("category-revenue")
            )
            .toStream()
            .to("analytics-category-revenue");

        return orderStream;
    }
}

/**
 * Interactive Query Service: 실시간 통계 조회
 */
@Service
@RequiredArgsConstructor
public class AnalyticsQueryService {

    private final KafkaStreams kafkaStreams;

    /**
     * 현재 시간 기준 1분간 주문 건수
     */
    public Long getCurrentMinuteOrderCount() {
        ReadOnlyWindowStore<String, Long> store =
            kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(
                    "order-count-by-minute",
                    QueryableStoreTypes.windowStore()
                )
            );

        String currentMinute = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        WindowStoreIterator<Long> iterator = store.fetch(
            currentMinute,
            Instant.now().minusSeconds(60),
            Instant.now()
        );

        Long count = 0L;
        while (iterator.hasNext()) {
            count += iterator.next().value;
        }

        return count;
    }

    /**
     * 상품별 실시간 판매량 Top 10
     */
    public List<ProductSalesRanking> getTopSellingProducts() {
        ReadOnlyKeyValueStore<String, ProductSalesStats> store =
            kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(
                    "product-sales-stats",
                    QueryableStoreTypes.keyValueStore()
                )
            );

        List<ProductSalesRanking> rankings = new ArrayList<>();

        try (KeyValueIterator<String, ProductSalesStats> iterator = store.all()) {
            while (iterator.hasNext()) {
                KeyValue<String, ProductSalesStats> kv = iterator.next();
                rankings.add(new ProductSalesRanking(
                    Long.parseLong(kv.key),
                    kv.value.getTotalQuantity(),
                    kv.value.getTotalRevenue()
                ));
            }
        }

        return rankings.stream()
            .sorted(Comparator.comparing(
                ProductSalesRanking::getTotalQuantity).reversed()
            )
            .limit(10)
            .collect(Collectors.toList());
    }

    /**
     * 카테고리별 실시간 매출
     */
    public Map<Long, BigDecimal> getCategoryRevenue() {
        ReadOnlyKeyValueStore<String, BigDecimal> store =
            kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(
                    "category-revenue",
                    QueryableStoreTypes.keyValueStore()
                )
            );

        Map<Long, BigDecimal> result = new HashMap<>();

        try (KeyValueIterator<String, BigDecimal> iterator = store.all()) {
            while (iterator.hasNext()) {
                KeyValue<String, BigDecimal> kv = iterator.next();
                result.put(Long.parseLong(kv.key), kv.value);
            }
        }

        return result;
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
// 성공 응답
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2024-01-15T10:30:00"
}

// 실패 응답
{
  "success": false,
  "data": null,
  "error": {
    "code": "OUT_OF_STOCK",
    "message": "재고가 부족합니다.",
    "details": {
      "requested": 10,
      "available": 5
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
  "phone": "01012345678"
}

Response 201 Created
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "userGrade": "BASIC"
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

### 2. 상품 API

#### 상품 검색 (Elasticsearch)

```http
GET /api/v1/products/search?keyword=나이키&categoryId=1&minPrice=50000&maxPrice=200000&sort=POPULAR&page=0&size=20
Authorization: Bearer {accessToken}

Response 200 OK
{
  "success": true,
  "data": {
    "content": [
      {
        "productId": 101,
        "name": "나이키 에어맥스 90",
        "brandName": "나이키",
        "categoryName": "스니커즈",
        "basePrice": 159000,
        "discountRate": 10.0,
        "finalPrice": 143100,
        "rating": 4.5,
        "reviewCount": 328,
        "viewCount": 15420,
        "thumbnailUrl": "https://...",
        "status": "ON_SALE"
      }
    ],
    "pageable": {
      "page": 0,
      "size": 20,
      "totalElements": 156,
      "totalPages": 8
    },
    "aggregations": {
      "brands": [
        {"brandId": 1, "brandName": "나이키", "count": 89},
        {"brandId": 2, "brandName": "아디다스", "count": 67}
      ],
      "priceRanges": [
        {"range": "0-50000", "count": 12},
        {"range": "50000-100000", "count": 45},
        {"range": "100000-200000", "count": 89},
        {"range": "200000+", "count": 10}
      ]
    }
  }
}
```

#### 상품 자동완성

```http
GET /api/v1/products/autocomplete?keyword=나이

Response 200 OK
{
  "success": true,
  "data": {
    "suggestions": [
      "나이키 에어맥스",
      "나이키 조던",
      "나이키 덩크",
      "나이키 에어포스"
    ]
  }
}
```

#### 상품 상세 조회

```http
GET /api/v1/products/101
Authorization: Bearer {accessToken}

Response 200 OK
{
  "success": true,
  "data": {
    "productId": 101,
    "name": "나이키 에어맥스 90",
    "description": "클래식한 디자인의 에어맥스 90...",
    "brandName": "나이키",
    "categoryName": "스니커즈",
    "basePrice": 159000,
    "discountRate": 10.0,
    "finalPrice": 143100,
    "rating": 4.5,
    "reviewCount": 328,
    "viewCount": 15420,
    "status": "ON_SALE",
    "images": [
      {"imageUrl": "https://...", "order": 0, "isThumbnail": true},
      {"imageUrl": "https://...", "order": 1, "isThumbnail": false}
    ],
    "options": [
      {
        "optionId": 1001,
        "optionName": "블랙-270",
        "additionalPrice": 0,
        "stockQuantity": 15,
        "status": "AVAILABLE"
      },
      {
        "optionId": 1002,
        "optionName": "화이트-270",
        "additionalPrice": 0,
        "stockQuantity": 0,
        "status": "SOLD_OUT"
      }
    ]
  }
}
```

### 3. 주문 API

#### 주문 생성

```http
POST /api/v1/orders
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "items": [
    {
      "productId": 101,
      "optionId": 1001,
      "quantity": 2
    }
  ],
  "deliveryAddress": "서울시 강남구 테헤란로 123",
  "deliveryRequest": "문 앞에 놓아주세요",
  "paymentMethod": "CARD"
}

Response 201 Created
{
  "success": true,
  "data": {
    "orderId": 5001,
    "orderNumber": "ORD-20240115-5001",
    "totalAmount": 286200,
    "discountAmount": 0,
    "finalAmount": 286200,
    "orderStatus": "PENDING",
    "orderedAt": "2024-01-15T14:30:00",
    "items": [
      {
        "productName": "나이키 에어맥스 90",
        "optionName": "블랙-270",
        "quantity": 2,
        "unitPrice": 143100,
        "totalPrice": 286200
      }
    ]
  }
}
```

#### 주문 목록 조회

```http
GET /api/v1/orders?page=0&size=20&status=COMPLETED
Authorization: Bearer {accessToken}

Response 200 OK
{
  "success": true,
  "data": {
    "content": [
      {
        "orderId": 5001,
        "orderNumber": "ORD-20240115-5001",
        "finalAmount": 286200,
        "orderStatus": "COMPLETED",
        "orderedAt": "2024-01-15T14:30:00",
        "completedAt": "2024-01-18T10:15:00",
        "itemCount": 1,
        "thumbnailUrl": "https://..."
      }
    ],
    "pageable": {
      "page": 0,
      "size": 20,
      "totalElements": 45,
      "totalPages": 3
    }
  }
}
```

#### 주문 상세 조회

```http
GET /api/v1/orders/5001
Authorization: Bearer {accessToken}

Response 200 OK
{
  "success": true,
  "data": {
    "orderId": 5001,
    "orderNumber": "ORD-20240115-5001",
    "totalAmount": 286200,
    "discountAmount": 0,
    "finalAmount": 286200,
    "orderStatus": "COMPLETED",
    "paymentMethod": "CARD",
    "deliveryAddress": "서울시 강남구 테헤란로 123",
    "deliveryRequest": "문 앞에 놓아주세요",
    "orderedAt": "2024-01-15T14:30:00",
    "paidAt": "2024-01-15T14:31:00",
    "shippedAt": "2024-01-16T09:00:00",
    "completedAt": "2024-01-18T10:15:00",
    "items": [
      {
        "productId": 101,
        "productName": "나이키 에어맥스 90",
        "optionName": "블랙-270",
        "brandName": "나이키",
        "quantity": 2,
        "unitPrice": 143100,
        "totalPrice": 286200,
        "thumbnailUrl": "https://..."
      }
    ],
    "shipping": {
      "courierName": "CJ대한통운",
      "trackingNumber": "123456789012",
      "shippingStatus": "DELIVERED"
    }
  }
}
```

#### 주문 취소

```http
POST /api/v1/orders/5001/cancel
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "reason": "단순 변심"
}

Response 200 OK
{
  "success": true,
  "data": {
    "orderId": 5001,
    "orderStatus": "CANCELLED",
    "cancelledAt": "2024-01-15T15:00:00",
    "cancelReason": "단순 변심"
  }
}
```

### 4. 장바구니 API

#### 장바구니 조회

```http
GET /api/v1/cart
Authorization: Bearer {accessToken}

Response 200 OK
{
  "success": true,
  "data": {
    "items": [
      {
        "cartItemId": 301,
        "productId": 101,
        "productName": "나이키 에어맥스 90",
        "optionId": 1001,
        "optionName": "블랙-270",
        "brandName": "나이키",
        "quantity": 2,
        "unitPrice": 143100,
        "totalPrice": 286200,
        "thumbnailUrl": "https://...",
        "stockStatus": "AVAILABLE"
      }
    ],
    "totalAmount": 286200
  }
}
```

#### 장바구니 추가

```http
POST /api/v1/cart
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "productId": 101,
  "optionId": 1001,
  "quantity": 2
}

Response 201 Created
{
  "success": true,
  "data": {
    "cartItemId": 301,
    "productId": 101,
    "optionId": 1001,
    "quantity": 2
  }
}
```

### 5. 재입고 알림 API

#### 알림 신청

```http
POST /api/v1/notifications/stock/request
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "productId": 101,
  "optionId": 1002
}

Response 201 Created
{
  "success": true,
  "data": {
    "notificationId": 701,
    "productId": 101,
    "optionId": 1002,
    "status": "ACTIVE",
    "requestedAt": "2024-01-15T14:30:00"
  }
}
```

#### 알림 구독 (SSE)

```http
GET /api/v1/notifications/stock/subscribe
Authorization: Bearer {accessToken}
Accept: text/event-stream

# SSE Stream
event: stock-restocked
id: 1
data: {"productId":101,"productName":"나이키 에어맥스 90","optionName":"화이트-270","restockedQuantity":20,"notifiedAt":"2024-01-15T15:00:00"}

event: stock-restocked
id: 2
data: {"productId":105,"productName":"아디다스 울트라부스트","optionName":"블랙-280","restockedQuantity":15,"notifiedAt":"2024-01-15T15:05:00"}
```

### 6. 리뷰 API

#### 리뷰 작성

```http
POST /api/v1/reviews
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "productId": 101,
  "orderItemId": 6001,
  "rating": 5,
  "content": "사이즈도 딱 맞고 너무 예뻐요!",
  "imageUrls": [
    "https://...",
    "https://..."
  ]
}

Response 201 Created
{
  "success": true,
  "data": {
    "reviewId": 8001,
    "productId": 101,
    "rating": 5,
    "content": "사이즈도 딱 맞고 너무 예뻐요!",
    "hasImages": true,
    "createdAt": "2024-01-20T10:00:00"
  }
}
```

#### 리뷰 목록 조회

```http
GET /api/v1/products/101/reviews?page=0&size=20&sort=LATEST
Authorization: Bearer {accessToken}

Response 200 OK
{
  "success": true,
  "data": {
    "content": [
      {
        "reviewId": 8001,
        "userName": "홍길동",
        "userGrade": "VIP",
        "rating": 5,
        "content": "사이즈도 딱 맞고 너무 예뻐요!",
        "hasImages": true,
        "imageUrls": ["https://...", "https://..."],
        "createdAt": "2024-01-20T10:00:00"
      }
    ],
    "pageable": {
      "page": 0,
      "size": 20,
      "totalElements": 328,
      "totalPages": 17
    },
    "statistics": {
      "averageRating": 4.5,
      "totalCount": 328,
      "ratingDistribution": {
        "5": 180,
        "4": 98,
        "3": 35,
        "2": 10,
        "1": 5
      }
    }
  }
}
```

### 7. 관리자 API (Analytics)

#### 실시간 통계 대시보드

```http
GET /api/v1/admin/analytics/dashboard
Authorization: Bearer {adminToken}

Response 200 OK
{
  "success": true,
  "data": {
    "currentMinuteOrders": 45,
    "todayOrders": 3420,
    "todayRevenue": 125600000,
    "topSellingProducts": [
      {
        "productId": 101,
        "productName": "나이키 에어맥스 90",
        "totalQuantity": 245,
        "totalRevenue": 35059500
      }
    ],
    "categoryRevenue": [
      {
        "categoryId": 1,
        "categoryName": "스니커즈",
        "revenue": 75000000
      }
    ]
  }
}
```

---

## 프론트엔드 구조

### 프로젝트 구조

```
stylehub-frontend/
├── src/
│   ├── components/
│   │   ├── common/
│   │   │   ├── Header.tsx
│   │   │   ├── Footer.tsx
│   │   │   └── Loading.tsx
│   │   ├── products/
│   │   │   ├── ProductCard.tsx
│   │   │   ├── ProductDetail.tsx
│   │   │   ├── ProductSearch.tsx
│   │   │   ├── ProductFilter.tsx
│   │   │   └── ProductList.tsx
│   │   ├── orders/
│   │   │   ├── OrderForm.tsx
│   │   │   ├── OrderList.tsx
│   │   │   └── OrderDetail.tsx
│   │   ├── cart/
│   │   │   ├── CartList.tsx
│   │   │   └── CartItem.tsx
│   │   └── notifications/
│   │       ├── StockNotification.tsx
│   │       └── NotificationBadge.tsx
│   ├── pages/
│   │   ├── HomePage.tsx
│   │   ├── ProductListPage.tsx
│   │   ├── ProductDetailPage.tsx
│   │   ├── CartPage.tsx
│   │   ├── OrderPage.tsx
│   │   └── MyOrdersPage.tsx
│   ├── hooks/
│   │   ├── useProducts.ts
│   │   ├── useProductSearch.ts
│   │   ├── useOrders.ts
│   │   ├── useCart.ts
│   │   └── useStockNotification.ts
│   ├── api/
│   │   ├── client.ts
│   │   ├── products.ts
│   │   ├── orders.ts
│   │   └── auth.ts
│   ├── stores/
│   │   ├── cartStore.ts
│   │   └── authStore.ts
│   ├── types/
│   │   ├── product.ts
│   │   ├── order.ts
│   │   └── common.ts
│   └── utils/
│       ├── format.ts
│       └── validation.ts
├── public/
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

### 핵심 컴포넌트 코드

#### 1. 상품 검색 (useProductSearch Hook)

```typescript
// hooks/useProductSearch.ts
import { useQuery } from '@tanstack/react-query';
import { api } from '@/api/client';

interface ProductSearchParams {
  keyword?: string;
  categoryId?: number;
  brandIds?: number[];
  minPrice?: number;
  maxPrice?: number;
  sort?: 'POPULAR' | 'PRICE_ASC' | 'PRICE_DESC' | 'LATEST' | 'REVIEW';
  page?: number;
  size?: number;
}

export function useProductSearch(params: ProductSearchParams) {
  return useQuery({
    queryKey: ['products', 'search', params],
    queryFn: async () => {
      const response = await api.get('/api/v1/products/search', { params });
      return response.data.data;
    },
    staleTime: 5 * 60 * 1000, // 5분간 캐시
    keepPreviousData: true,
  });
}

// 사용 예시
function ProductListPage() {
  const [searchParams, setSearchParams] = useState({
    keyword: '',
    sort: 'POPULAR' as const,
    page: 0,
    size: 20,
  });

  const { data, isLoading, error } = useProductSearch(searchParams);

  if (isLoading) return <Loading />;
  if (error) return <Error message="상품을 불러올 수 없습니다" />;

  return (
    <div>
      <ProductFilter
        onFilterChange={(filters) => setSearchParams({ ...searchParams, ...filters })}
      />
      <ProductList products={data.content} />
      <Pagination
        page={searchParams.page}
        totalPages={data.pageable.totalPages}
        onPageChange={(page) => setSearchParams({ ...searchParams, page })}
      />
    </div>
  );
}
```

#### 2. 실시간 재입고 알림 (SSE)

```typescript
// hooks/useStockNotification.ts
import { useEffect, useState } from 'react';
import { toast } from 'sonner';

interface StockNotification {
  productId: number;
  productName: string;
  optionName: string;
  restockedQuantity: number;
  notifiedAt: string;
}

export function useStockNotification(userId: number) {
  const [notifications, setNotifications] = useState<StockNotification[]>([]);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    const eventSource = new EventSource(
      `${import.meta.env.VITE_API_URL}/api/v1/notifications/stock/subscribe`,
      { withCredentials: true }
    );

    eventSource.onopen = () => {
      setIsConnected(true);
      console.log('[SSE] Stock notification connected');
    };

    eventSource.addEventListener('stock-restocked', (event) => {
      const notification: StockNotification = JSON.parse(event.data);

      setNotifications((prev) => [notification, ...prev]);

      // Toast 알림
      toast.success(`${notification.productName} 재입고!`, {
        description: `${notification.optionName} - ${notification.restockedQuantity}개 입고`,
        action: {
          label: '보러가기',
          onClick: () => (window.location.href = `/products/${notification.productId}`),
        },
        duration: 5000,
      });
    });

    eventSource.onerror = () => {
      setIsConnected(false);
      console.error('[SSE] Connection error');
    };

    return () => {
      eventSource.close();
      console.log('[SSE] Connection closed');
    };
  }, [userId]);

  return { notifications, isConnected };
}

// 사용 예시
function Header() {
  const { user } = useAuth();
  const { notifications, isConnected } = useStockNotification(user?.userId);

  return (
    <header>
      <nav>
        {/* ... */}
        <NotificationBadge
          count={notifications.length}
          isConnected={isConnected}
          notifications={notifications}
        />
      </nav>
    </header>
  );
}
```

#### 3. 주문 생성

```typescript
// hooks/useOrders.ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/api/client';

interface CreateOrderRequest {
  items: Array<{
    productId: number;
    optionId: number;
    quantity: number;
  }>;
  deliveryAddress: string;
  deliveryRequest?: string;
  paymentMethod: string;
}

export function useCreateOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (request: CreateOrderRequest) => {
      const response = await api.post('/api/v1/orders', request);
      return response.data.data;
    },
    onSuccess: () => {
      // 주문 생성 성공 시 장바구니 캐시 무효화
      queryClient.invalidateQueries(['cart']);
      queryClient.invalidateQueries(['orders']);
    },
    onError: (error: any) => {
      const errorCode = error.response?.data?.error?.code;

      if (errorCode === 'OUT_OF_STOCK') {
        toast.error('재고가 부족합니다', {
          description: error.response.data.error.message,
        });
      } else {
        toast.error('주문 생성 실패', {
          description: '다시 시도해주세요',
        });
      }
    },
  });
}

// 사용 예시
function OrderPage() {
  const { mutate: createOrder, isLoading } = useCreateOrder();
  const navigate = useNavigate();

  const handleSubmit = (formData: CreateOrderRequest) => {
    createOrder(formData, {
      onSuccess: (order) => {
        toast.success('주문이 완료되었습니다');
        navigate(`/orders/${order.orderId}`);
      },
    });
  };

  return (
    <form onSubmit={handleSubmit}>
      {/* 주문 폼 */}
      <button type="submit" disabled={isLoading}>
        {isLoading ? '처리 중...' : '주문하기'}
      </button>
    </form>
  );
}
```

#### 4. 상품 상세 페이지

```typescript
// pages/ProductDetailPage.tsx
import { useParams } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import { api } from '@/api/client';
import { toast } from 'sonner';

function ProductDetailPage() {
  const { productId } = useParams<{ productId: string }>();

  // 상품 상세 조회
  const { data: product, isLoading } = useQuery({
    queryKey: ['products', productId],
    queryFn: async () => {
      const response = await api.get(`/api/v1/products/${productId}`);
      return response.data.data;
    },
  });

  // 재입고 알림 신청
  const requestNotificationMutation = useMutation({
    mutationFn: async (optionId: number) => {
      const response = await api.post('/api/v1/notifications/stock/request', {
        productId: Number(productId),
        optionId,
      });
      return response.data.data;
    },
    onSuccess: () => {
      toast.success('재입고 알림 신청 완료', {
        description: '재입고 시 알림을 받으실 수 있습니다',
      });
    },
  });

  // 장바구니 추가
  const addToCartMutation = useMutation({
    mutationFn: async (data: { productId: number; optionId: number; quantity: number }) => {
      const response = await api.post('/api/v1/cart', data);
      return response.data.data;
    },
    onSuccess: () => {
      toast.success('장바구니에 추가되었습니다');
    },
  });

  if (isLoading) return <Loading />;
  if (!product) return <NotFound />;

  return (
    <div className="container mx-auto p-4">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* 이미지 갤러리 */}
        <div>
          <img
            src={product.images[0].imageUrl}
            alt={product.name}
            className="w-full rounded-lg"
          />
        </div>

        {/* 상품 정보 */}
        <div>
          <h1 className="text-3xl font-bold">{product.name}</h1>
          <p className="text-gray-600">{product.brandName}</p>

          <div className="mt-4">
            <span className="text-2xl font-bold">
              {product.finalPrice.toLocaleString()}원
            </span>
            {product.discountRate > 0 && (
              <>
                <span className="ml-2 text-red-500">{product.discountRate}%</span>
                <span className="ml-2 text-gray-400 line-through">
                  {product.basePrice.toLocaleString()}원
                </span>
              </>
            )}
          </div>

          {/* 옵션 선택 */}
          <div className="mt-6">
            <label className="block text-sm font-medium mb-2">옵션 선택</label>
            {product.options.map((option) => (
              <div key={option.optionId} className="flex items-center justify-between p-3 border rounded mb-2">
                <span>{option.optionName}</span>
                {option.status === 'AVAILABLE' ? (
                  <button
                    onClick={() =>
                      addToCartMutation.mutate({
                        productId: product.productId,
                        optionId: option.optionId,
                        quantity: 1,
                      })
                    }
                    className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
                  >
                    장바구니
                  </button>
                ) : (
                  <button
                    onClick={() => requestNotificationMutation.mutate(option.optionId)}
                    className="px-4 py-2 bg-gray-300 text-gray-700 rounded hover:bg-gray-400"
                  >
                    재입고 알림
                  </button>
                )}
              </div>
            ))}
          </div>

          {/* 상품 설명 */}
          <div className="mt-8">
            <h2 className="text-xl font-bold mb-4">상품 설명</h2>
            <p className="text-gray-700 whitespace-pre-line">{product.description}</p>
          </div>
        </div>
      </div>

      {/* 리뷰 섹션 */}
      <div className="mt-12">
        <h2 className="text-2xl font-bold mb-6">리뷰 ({product.reviewCount})</h2>
        <ReviewList productId={product.productId} />
      </div>
    </div>
  );
}
```

### 상태 관리 (Zustand)

```typescript
// stores/cartStore.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface CartItem {
  cartItemId: number;
  productId: number;
  optionId: number;
  quantity: number;
}

interface CartStore {
  items: CartItem[];
  addItem: (item: CartItem) => void;
  removeItem: (cartItemId: number) => void;
  updateQuantity: (cartItemId: number, quantity: number) => void;
  clearCart: () => void;
  getTotalQuantity: () => number;
}

export const useCartStore = create<CartStore>()(
  persist(
    (set, get) => ({
      items: [],

      addItem: (item) =>
        set((state) => ({
          items: [...state.items, item],
        })),

      removeItem: (cartItemId) =>
        set((state) => ({
          items: state.items.filter((item) => item.cartItemId !== cartItemId),
        })),

      updateQuantity: (cartItemId, quantity) =>
        set((state) => ({
          items: state.items.map((item) =>
            item.cartItemId === cartItemId ? { ...item, quantity } : item
          ),
        })),

      clearCart: () => set({ items: [] }),

      getTotalQuantity: () => {
        const items = get().items;
        return items.reduce((total, item) => total + item.quantity, 0);
      },
    }),
    {
      name: 'cart-storage',
    }
  )
);
```

---

## 기술적 챌린지와 해결 방안

### Challenge 1: 동시성 제어 - 재고 정합성 보장

**문제 상황**
```
상품 재고: 100개
동시 주문: 150건

시간    Thread 1 (주문1)     Thread 2 (주문2)
T1      재고 조회: 100개
T2                           재고 조회: 100개
T3      5개 차감
T4                           7개 차감
T5      재고 저장: 95개
T6                           재고 저장: 93개 (❌ 잘못된 결과!)

실제 재고: 93개 (올바른 값: 88개)
```

**해결 방안 1: 낙관적 락 (Optimistic Lock)**

```java
@Entity
@Table(name = "product_options")
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long optionId;

    private Integer stockQuantity;

    @Version  // JPA 낙관적 락
    private Integer version;

    public void deduct(int quantity) {
        if (this.stockQuantity < quantity) {
            throw new OutOfStockException();
        }
        this.stockQuantity -= quantity;
    }
}

@Service
@Transactional
public class InventoryService {

    @Retry(maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void deductStock(Long optionId, int quantity) {
        try {
            ProductOption option = optionRepository
                .findByIdWithOptimisticLock(optionId)
                .orElseThrow();

            option.deduct(quantity);
            // version이 변경되었으면 OptimisticLockException 발생

        } catch (OptimisticLockException e) {
            // 재시도 로직 (Spring Retry)
            throw new ConcurrentStockUpdateException("재고 업데이트 충돌. 재시도 중...");
        }
    }
}
```

**장점**: 충돌이 드문 경우 성능 좋음, DB 락 없음
**단점**: 충돌 시 재시도 필요, 선착순 이벤트에는 부적합

**해결 방안 2: 분산 락 (Redis Distributed Lock)**

```java
@Component
@RequiredArgsConstructor
public class RedisLockService {

    private final RedisTemplate<String, String> redisTemplate;

    public <T> T executeWithLock(String lockKey, long timeoutMs, Supplier<T> supplier) {
        String lockValue = UUID.randomUUID().toString();
        boolean acquired = false;

        try {
            // 락 획득 (SETNX + 만료 시간)
            acquired = Boolean.TRUE.equals(
                redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, timeoutMs, TimeUnit.MILLISECONDS)
            );

            if (!acquired) {
                throw new LockAcquisitionException("락 획득 실패");
            }

            return supplier.get();

        } finally {
            if (acquired) {
                // Lua 스크립트로 안전하게 락 해제
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

**장점**: 완벽한 동시성 제어, 선착순 이벤트 적합
**단점**: Redis 의존성, 약간의 오버헤드

**선택 기준**:
- 일반 주문: 낙관적 락 (성능 우선)
- 한정판/선착순: 분산 락 (정확성 우선)

---

### Challenge 2: Elasticsearch 인덱스 동기화

**문제 상황**
- MySQL에 상품 데이터 저장
- Elasticsearch에 별도로 인덱싱 필요
- 데이터 불일치 발생 가능성

**해결 방안: CDC (Change Data Capture) + Debezium**

```yaml
# Debezium Connector 설정
{
  "name": "mysql-connector",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "mysql",
    "database.port": "3306",
    "table.include.list": "stylehub.products",
    "database.history.kafka.topic": "schema-changes.stylehub"
  }
}
```

**동작 방식**:
1. MySQL binlog 모니터링
2. 변경사항을 Kafka로 발행
3. Consumer가 Elasticsearch 업데이트

**장점**:
- 실시간 동기화
- 애플리케이션 코드 변경 불필요
- 이벤트 재처리 가능

**대안**: Dual Write (애플리케이션 레벨)
```java
@Transactional
public Product createProduct(ProductRequest request) {
    // 1. MySQL 저장
    Product product = productRepository.save(Product.from(request));

    // 2. Elasticsearch 인덱싱 이벤트 발행
    kafkaTemplate.send("product-events", new ProductCreatedEvent(product));

    return product;
}
```

---

### Challenge 3: N+1 쿼리 문제

**문제 상황**
```java
// 주문 목록 조회 시 N+1 문제 발생
List<Order> orders = orderRepository.findByUserId(userId);

for (Order order : orders) {
    // 각 주문마다 추가 쿼리 발생
    List<OrderItem> items = order.getItems();  // N개 쿼리
    for (OrderItem item : items) {
        Product product = item.getProduct();    // N*M개 쿼리
    }
}
```

**해결 방안: QueryDSL Fetch Join**

```java
@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<Order> findOrdersWithDetails(Long userId) {
        return queryFactory
            .selectFrom(order)
            .join(order.items, orderItem).fetchJoin()
            .join(orderItem.productOption, productOption).fetchJoin()
            .join(productOption.product, product).fetchJoin()
            .join(product.brand, brand).fetchJoin()
            .where(order.userId.eq(userId))
            .orderBy(order.orderedAt.desc())
            .fetch();
    }
}
```

**결과**:
- Before: 1 + N + N*M 쿼리 (예: 1 + 10 + 30 = 41개)
- After: 1개 쿼리 (Fetch Join)

**대안: BatchSize**
```java
@Entity
public class Order {

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "order")
    private List<OrderItem> items;
}
```

---

### Challenge 4: 대용량 배치 처리

**문제 상황**
- 매일 자정 포인트 만료 처리
- 예상 데이터: 500만 건
- 요구 시간: 30분 이내

**해결 방안: Spring Batch + 파티셔닝**

```java
@Configuration
public class ExpiryBatchConfig {

    private static final int CHUNK_SIZE = 1000;
    private static final int GRID_SIZE = 10;

    @Bean
    public Job expiryJob() {
        return jobBuilderFactory.get("expiryJob")
            .start(masterStep())
            .build();
    }

    @Bean
    public Step masterStep() {
        return stepBuilderFactory.get("masterStep")
            .partitioner("slaveStep", partitioner())
            .step(slaveStep())
            .gridSize(GRID_SIZE)
            .taskExecutor(virtualThreadExecutor())
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

    @Bean
    public Step slaveStep() {
        return stepBuilderFactory.get("slaveStep")
            .<Transaction, Transaction>chunk(CHUNK_SIZE)
            .reader(expiryReader(null, null))
            .processor(expiryProcessor())
            .writer(expiryWriter())
            .build();
    }

    @Bean
    public TaskExecutor virtualThreadExecutor() {
        // Java 21 Virtual Threads
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

**성능 개선**:
- 단일 스레드: 500만 건 / 60분
- 10개 파티션 + Virtual Threads: 500만 건 / 15분

---

## 구현 로드맵

### MVP 로드맵 (6주)

#### Week 1: 기반 구축 및 환경 설정

**Backend**
- [x] Gradle 멀티 모듈 프로젝트 생성
- [x] Docker Compose 환경 구성
  - MySQL 8.0
  - Redis 7.x
  - Elasticsearch 8.x
  - Kafka 3.6.x + Zookeeper
  - Kafka Connect (Debezium)
- [x] Java 21 설정 (Virtual Threads 활성화)
- [x] Spring Boot 3.3.x 프로젝트 설정
- [x] 도메인 모델 설계 (Product, Order, Inventory)
- [x] 기본 인증/인가 (JWT, Spring Security)
- [x] Flyway 마이그레이션 스크립트

**Frontend**
- [x] Vite + React + TypeScript 프로젝트 생성
- [x] TanStack Query, Zustand 설치
- [x] Tailwind CSS, shadcn/ui 설정
- [x] 기본 레이아웃 컴포넌트

#### Week 2: 상품 관리 & 검색

**Backend**
- [x] 상품 CRUD API
- [x] 카테고리/브랜드 관리
- [x] Elasticsearch 연동
  - Nori 형태소 분석기 설정
  - 상품 인덱스 매핑
  - 기본 검색 API
- [x] QueryDSL 동적 쿼리
- [x] Redis 캐싱 (상품 상세)

**Frontend**
- [x] 상품 검색 페이지
- [x] 상품 목록 컴포넌트
- [x] 상품 상세 페이지
- [x] 필터/정렬 UI

**테스트**
- [x] 단위 테스트 (Service 계층)
- [x] 통합 테스트 (Testcontainers)

#### Week 3: 재고 관리 & 주문

**Backend**
- [x] 재고 관리 서비스
  - 낙관적 락 구현
  - 분산 락 구현 (Redis)
  - 동시성 테스트
- [x] 주문 생성 API
  - 재고 차감 로직
  - 트랜잭션 처리
- [x] 장바구니 CRUD API

**Frontend**
- [x] 장바구니 페이지
- [x] 주문 페이지
- [x] 재고 부족 에러 처리

**테스트**
- [x] 동시성 테스트 (JMeter)
- [x] 재고 정합성 테스트

#### Week 4: Kafka 이벤트 아키텍처

**Backend**
- [x] Kafka Producer/Consumer 설정
- [x] Virtual Thread Kafka Listener
- [x] Saga Pattern 구현
  - 주문 생성 이벤트
  - 결제 이벤트
  - 보상 트랜잭션
- [x] Dead Letter Queue 처리

**Frontend**
- [x] 주문 상태 실시간 업데이트

**테스트**
- [x] Saga 시나리오 테스트
- [x] 실패 케이스 테스트

#### Week 5: 실시간 알림 & CDC

**Backend**
- [x] CDC 설정 (Debezium)
- [x] Elasticsearch 동기화 Consumer
- [x] 재입고 알림 서비스
  - SSE 엔드포인트
  - Kafka Consumer
- [x] 알림 신청 API

**Frontend**
- [x] SSE 연결 (useStockNotification)
- [x] Toast 알림 UI
- [x] 재입고 알림 신청 버튼

**테스트**
- [x] SSE 연결 테스트
- [x] CDC 동기화 테스트

#### Week 6: Kafka Streams & 최적화

**Backend**
- [x] Kafka Streams 구현
  - 실시간 주문 통계
  - 상품별 판매량 집계
  - Interactive Query API
- [x] Spring Batch 정산 배치
- [x] 성능 최적화
  - N+1 문제 해결
  - 인덱스 튜닝
  - 쿼리 최적화
- [x] Swagger API 문서

**Frontend**
- [x] 관리자 대시보드 (실시간 통계)

**테스트**
- [x] JMeter 부하 테스트
- [x] 성능 벤치마크

**문서화**
- [x] README.md
- [x] API 문서 (Swagger)
- [x] 아키텍처 다이어그램
- [x] ERD

---

## 성능 최적화 전략

### 1. 데이터베이스 최적화

#### 인덱스 설계

```sql
-- 복합 인덱스: 주문 내역 조회
CREATE INDEX idx_orders_user_status_date
ON orders(user_id, order_status, ordered_at DESC);

-- 커버링 인덱스: 상품 목록
CREATE INDEX idx_products_covering
ON products(category_id, status, base_price, rating)
INCLUDE (name, brand_id, view_count);

-- 부분 인덱스: 활성 상품만
CREATE INDEX idx_products_active
ON products(category_id, rating DESC)
WHERE status = 'ON_SALE';
```

#### 쿼리 최적화

```java
// Before: N+1 문제
List<Order> orders = orderRepository.findAll();
orders.forEach(order -> {
    order.getItems().forEach(item -> {
        item.getProduct().getName();  // 각각 쿼리
    });
});

// After: Fetch Join (1개 쿼리)
List<Order> orders = queryFactory
    .selectFrom(order)
    .join(order.items, orderItem).fetchJoin()
    .join(orderItem.product, product).fetchJoin()
    .fetch();
```

### 2. 캐싱 전략

#### Redis 캐싱

```java
// Look-Aside 패턴
@Cacheable(value = "products", key = "#productId")
public Product getProduct(Long productId) {
    return productRepository.findById(productId)
        .orElseThrow();
}

// 캐시 무효화
@CacheEvict(value = "products", key = "#product.id")
public Product updateProduct(Product product) {
    return productRepository.save(product);
}

// 다중 캐시
@Cacheable(value = "productSearch",
    key = "#params.keyword + '_' + #params.categoryId + '_' + #params.page")
public SearchResult search(ProductSearchParams params) {
    // Elasticsearch 검색
}
```

### 3. 비동기 처리 (Virtual Threads)

```java
@Service
public class OrderService {

    private final ExecutorService executor =
        Executors.newVirtualThreadPerTaskExecutor();

    public CompletableFuture<List<Order>> getOrdersAsync(Long userId) {
        return CompletableFuture.supplyAsync(
            () -> orderRepository.findByUserId(userId),
            executor
        );
    }

    // 병렬 처리
    public OrderSummary getOrderSummary(Long userId) {
        CompletableFuture<List<Order>> ordersFuture = getOrdersAsync(userId);
        CompletableFuture<Long> countFuture = CompletableFuture.supplyAsync(
            () -> orderRepository.countByUserId(userId),
            executor
        );
        CompletableFuture<BigDecimal> totalFuture = CompletableFuture.supplyAsync(
            () -> orderRepository.sumTotalAmountByUserId(userId),
            executor
        );

        return CompletableFuture.allOf(ordersFuture, countFuture, totalFuture)
            .thenApply(v -> new OrderSummary(
                ordersFuture.join(),
                countFuture.join(),
                totalFuture.join()
            ))
            .join();
    }
}
```

### 4. 성능 목표 및 측정

| 항목 | 목표 | 측정 방법 | 결과 |
|-----|------|----------|------|
| **API 응답 시간** | P95 < 200ms | JMeter, APM | - |
| **TPS** | > 1,000 TPS | Gatling | - |
| **동시 주문 처리** | 100 req/s | 부하 테스트 | - |
| **상품 검색** | P99 < 150ms | Elasticsearch | - |
| **재고 차감** | P95 < 100ms | Redis 분산 락 | - |
| **배치 처리** | 500만 건 / 30분 | Spring Batch | - |

---

## 테스트 전략

### 1. 단위 테스트 (Unit Test)

```java
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductOptionRepository optionRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    @DisplayName("재고 차감 - 정상")
    void deductStock_Success() {
        // Given
        Long optionId = 1L;
        int quantity = 5;

        ProductOption option = ProductOption.builder()
            .optionId(optionId)
            .stockQuantity(10)
            .version(0)
            .build();

        when(optionRepository.findByIdWithOptimisticLock(optionId))
            .thenReturn(Optional.of(option));

        // When
        inventoryService.deductStock(optionId, quantity);

        // Then
        assertThat(option.getStockQuantity()).isEqualTo(5);
        verify(optionRepository).findByIdWithOptimisticLock(optionId);
    }

    @Test
    @DisplayName("재고 차감 - 재고 부족 시 예외 발생")
    void deductStock_InsufficientStock_ThrowsException() {
        // Given
        Long optionId = 1L;
        int quantity = 15;

        ProductOption option = ProductOption.builder()
            .optionId(optionId)
            .stockQuantity(10)
            .build();

        when(optionRepository.findByIdWithOptimisticLock(optionId))
            .thenReturn(Optional.of(option));

        // When & Then
        assertThatThrownBy(() -> inventoryService.deductStock(optionId, quantity))
            .isInstanceOf(OutOfStockException.class)
            .hasMessageContaining("재고 부족");

        verify(optionRepository, never()).save(any());
    }
}
```

### 2. 통합 테스트 (Integration Test)

```java
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("stylehub_test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryService inventoryService;

    @Test
    @DisplayName("주문 생성 - 재고 차감 확인")
    @Transactional
    void createOrder_DeductsStock() {
        // Given
        OrderRequest request = OrderRequest.builder()
            .userId(1L)
            .items(List.of(
                new OrderItemRequest(101L, 1001L, 2)
            ))
            .deliveryAddress("서울시 강남구")
            .paymentMethod("CARD")
            .build();

        // When
        Order order = orderService.createOrder(request);

        // Then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getItems()).hasSize(1);

        // 재고 차감 확인
        ProductOption option = inventoryService.getOption(1001L);
        assertThat(option.getStockQuantity()).isLessThan(15);
    }
}
```

### 3. API 테스트 (RestAssured)

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ProductApiTest {

    @LocalServerPort
    private int port;

    private String accessToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        accessToken = getAccessToken();
    }

    @Test
    @DisplayName("상품 검색 API - 성공")
    void searchProducts_Success() {
        given()
            .header("Authorization", "Bearer " + accessToken)
            .queryParam("keyword", "나이키")
            .queryParam("page", 0)
            .queryParam("size", 20)
        .when()
            .get("/api/v1/products/search")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThan(0)))
            .body("data.pageable.page", equalTo(0));
    }

    @Test
    @DisplayName("주문 생성 API - 재고 부족 시 실패")
    void createOrder_OutOfStock_Fails() {
        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "items": [
                    {"productId": 101, "optionId": 1002, "quantity": 100}
                  ],
                  "deliveryAddress": "서울시 강남구",
                  "paymentMethod": "CARD"
                }
                """)
        .when()
            .post("/api/v1/orders")
        .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.code", equalTo("OUT_OF_STOCK"));
    }
}
```

### 4. 동시성 테스트 (JMeter)

```xml
<!-- JMeter Test Plan: 재고 동시성 테스트 -->
<jmeterTestPlan>
  <ThreadGroup>
    <stringProp name="ThreadGroup.num_threads">100</stringProp>
    <stringProp name="ThreadGroup.ramp_time">1</stringProp>
    <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
      <stringProp name="LoopController.loops">10</stringProp>
    </elementProp>
  </ThreadGroup>

  <HTTPSamplerProxy>
    <stringProp name="HTTPSampler.domain">localhost</stringProp>
    <stringProp name="HTTPSampler.port">8080</stringProp>
    <stringProp name="HTTPSampler.path">/api/v1/orders</stringProp>
    <stringProp name="HTTPSampler.method">POST</stringProp>
    <boolProp name="HTTPSampler.follow_redirects">true</boolProp>
    <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
  </HTTPSamplerProxy>

  <ResultCollector>
    <stringProp name="filename">./results.jtl</stringProp>
  </ResultCollector>
</jmeterTestPlan>
```

**시나리오**: 재고 100개, 동시 주문 100건
**기대 결과**: 정확히 100건만 성공, 나머지 실패

### 5. 성능 테스트 (Gatling)

```scala
class ProductSearchSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .authorizationHeader("Bearer ${accessToken}")

  val scn = scenario("Product Search")
    .exec(http("Search Products")
      .get("/api/v1/products/search")
      .queryParam("keyword", "나이키")
      .queryParam("page", "0")
      .queryParam("size", "20")
      .check(status.is(200))
      .check(jsonPath("$.success").is("true"))
    )

  setUp(
    scn.inject(
      rampUsers(1000) during (60 seconds)
    ).protocols(httpProtocol)
  ).assertions(
    global.responseTime.max.lt(500),
    global.responseTime.percentile(95).lt(200),
    global.successfulRequests.percent.gt(99)
  )
}
```

---

## 추가 고려 사항

### 1. 보안

- **민감 정보 암호화**: 비밀번호 (BCrypt), 카드 번호 (AES-256)
- **SQL Injection 방어**: PreparedStatement, JPA
- **XSS 방어**: Input Validation, Output Encoding
- **CSRF 방어**: SameSite Cookie, CSRF Token
- **API Rate Limiting**: Redis 기반 (100 req/min per user)
- **JWT 보안**: Access Token (1시간), Refresh Token (7일)

### 2. 모니터링

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    tags:
      application: stylehub
    export:
      prometheus:
        enabled: true
```

**주요 모니터링 지표**:
- API 응답 시간 (P50, P95, P99)
- TPS (Transactions Per Second)
- 에러율
- DB Connection Pool 사용률
- Redis Hit Rate
- Kafka Consumer Lag
- JVM Heap Memory

### 3. 로깅

```java
@Slf4j
@RestController
public class ProductController {

    @GetMapping("/api/v1/products/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProduct(
        @PathVariable Long productId
    ) {
        log.info("[API] GET /api/v1/products/{} - Start", productId);

        try {
            ProductDetailResponse response = productService.getProduct(productId);
            log.info("[API] GET /api/v1/products/{} - Success", productId);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (ProductNotFoundException e) {
            log.warn("[API] GET /api/v1/products/{} - Not Found", productId);
            throw e;

        } catch (Exception e) {
            log.error("[API] GET /api/v1/products/{} - Error: {}", productId, e.getMessage(), e);
            throw e;
        }
    }
}
```

---

## 다음 단계

### 1. 프로젝트 초기 설정

```bash
# 멀티 모듈 프로젝트 생성
mkdir stylehub && cd stylehub
gradle init --type java-application --dsl kotlin

# Docker Compose 환경 구성
cd docker
docker-compose up -d
```

### 2. 아키텍처 설계서 작성

- 상세 시스템 아키텍처 다이어그램 (C4 Model)
- 도메인 모델 클래스 다이어그램
- 시퀀스 다이어그램 (핵심 플로우)

### 3. ERD 시각화

- dbdiagram.io 또는 ERDCloud로 ERD 작성
- Flyway 마이그레이션 스크립트 생성

### 4. 개발 시작

- Week 1부터 순차적으로 구현
- 각 주차별 완성 후 Git 커밋
- README.md에 진행 상황 기록

---

## 기대 효과

### 백엔드 개발자로서의 역량 증명

1. **최신 기술 스택 활용** (Java 21, Virtual Threads)
2. **복잡한 도메인 모델링** (DDD)
3. **동시성 제어 전문성** (낙관적 락, 분산 락)
4. **이벤트 기반 아키텍처** (Kafka 4가지 패턴)
5. **고성능 검색 엔진** (Elasticsearch)
6. **분산 트랜잭션 처리** (Saga Pattern)
7. **실시간 스트림 처리** (Kafka Streams)
8. **대용량 배치 처리** (Spring Batch)
9. **성능 최적화** (N+1 해결, 캐싱, 인덱싱)
10. **풀스택 구현** (React + TypeScript)

### 면접 대비

- **"재고 동시성 문제를 어떻게 해결했나요?"**
  → 낙관적 락, 분산 락, 선택 기준 설명

- **"Kafka를 어떻게 활용했나요?"**
  → Saga Pattern, CDC, Event-Driven Notification, Kafka Streams 설명

- **"Elasticsearch는 어떻게 동기화했나요?"**
  → Debezium CDC, 이벤트 기반 동기화 설명

- **"Virtual Threads를 어디에 적용했나요?"**
  → Kafka Listener, 비동기 처리, 성능 개선 수치 제시

---

## 라이선스

이 프로젝트는 포트폴리오 목적으로 작성되었습니다.

## 문의

질문이나 제안사항이 있으시면 이슈를 생성해주세요.
