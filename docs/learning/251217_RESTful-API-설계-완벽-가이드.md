# RESTful API 설계 완벽 가이드

> **작성일시**: 2025-12-17 01:30
>
> LookMarket 프로젝트의 API 설계 원칙과 실제 적용 사례

## 목차
1. [REST란 무엇인가?](#rest란-무엇인가)
2. [API 버저닝 전략](#api-버저닝-전략)
3. [RESTful URL 설계](#restful-url-설계)
4. [HTTP 메서드 활용](#http-메서드-활용)
5. [JWT 토큰 갱신 전략](#jwt-토큰-갱신-전략)
6. [응답 형식 설계](#응답-형식-설계)
7. [LookMarket API 구조](#lookmarket-api-구조)

---

## REST란 무엇인가?

### 정의
**REST** (Representational State Transfer)는 웹 API 설계의 아키텍처 스타일입니다.

### 핵심 제약조건 6가지

| 제약조건 | 설명 |
|---------|------|
| **Client-Server** | 클라이언트와 서버 분리 |
| **Stateless** | 서버는 클라이언트 상태 저장 X |
| **Cacheable** | 응답은 캐싱 가능해야 함 |
| **Uniform Interface** | 일관된 인터페이스 |
| **Layered System** | 계층화된 시스템 |
| **Code on Demand** | (선택) 코드 전송 가능 |

### RESTful이란?
REST 원칙을 잘 지키는 API를 **RESTful API**라고 합니다.

---

## API 버저닝 전략

### 왜 버저닝이 필요한가?

```
시나리오: API 응답 형식 변경

v1 (기존):
{ "userName": "홍길동" }

v2 (신규):
{ "user": { "firstName": "길동", "lastName": "홍" } }

→ v2 출시 후에도 v1 사용 클라이언트는 계속 동작해야 함
```

### 버저닝 전략 비교

| 전략 | 예시 | 장점 | 단점 |
|------|------|------|------|
| **URL Path** | `/api/v1/users` | 명확함, 캐싱 용이 | URL 변경됨 |
| Header | `Accept: application/vnd.api.v1+json` | URL 깔끔 | 테스트 어려움 |
| Query Param | `/api/users?version=1` | 간단 | 캐싱 어려움 |

### LookMarket 선택: URL Path 버저닝

```java
@RestController
@RequestMapping("/api/v1/users")  // ← v1 버전
public class UserController {
    // ...
}
```

**선택 이유:**
- 업계 표준 (GitHub, Stripe, Twitter, Google 등)
- 직관적이고 명확함
- 브라우저에서 바로 테스트 가능
- API 문서화 용이

### 버전 관리 정책

```
v1 출시 → v2 개발 → v2 출시
                   → v1 Deprecated (6개월 공지)
                   → v1 제거
```

---

## RESTful URL 설계

### 핵심 원칙

| 원칙 | 규칙 | 좋은 예 | 나쁜 예 |
|------|------|---------|---------|
| **리소스 중심** | URL은 명사 | `/users` | `/getUsers` |
| **복수형** | 컬렉션은 복수 | `/users` | `/user` |
| **소문자** | URL은 소문자 | `/users` | `/Users` |
| **케밥 케이스** | 단어 연결 | `/order-items` | `/orderItems` |
| **계층 구조** | 소속 관계 | `/users/1/orders` | `/userOrders` |

### 안티패턴과 개선

#### 1. 동사 사용 금지

```
❌ 나쁜 예:
POST /api/v1/users/1/activate
POST /api/v1/users/1/deactivate
POST /api/v1/createUser
GET  /api/v1/getUser/1
POST /api/v1/deleteUser/1

✅ 좋은 예:
PATCH /api/v1/users/1/status    Body: {"status": "ACTIVE"}
POST  /api/v1/users
GET   /api/v1/users/1
DELETE /api/v1/users/1
```

#### 2. 적절한 계층 구조

```
❌ 나쁜 예:
GET /api/v1/getUserOrders?userId=1
GET /api/v1/ordersByUser/1

✅ 좋은 예:
GET /api/v1/users/1/orders          # 사용자의 주문 목록
GET /api/v1/users/1/orders/5        # 사용자의 특정 주문
GET /api/v1/orders/5                # 주문 직접 조회 (권한 검사)
```

#### 3. 필터링은 Query Parameter로

```
❌ 나쁜 예:
GET /api/v1/products/active
GET /api/v1/products/category/1/brand/2

✅ 좋은 예:
GET /api/v1/products?status=ACTIVE
GET /api/v1/products?categoryId=1&brandId=2&minPrice=10000
```

---

## HTTP 메서드 활용

### 메서드별 용도

| 메서드 | 용도 | 멱등성 | Request Body | 예시 |
|--------|------|:------:|:------------:|------|
| **GET** | 조회 | O | X | `GET /users/1` |
| **POST** | 생성 | X | O | `POST /users` |
| **PUT** | 전체 수정 | O | O | `PUT /users/1` |
| **PATCH** | 부분 수정 | O | O | `PATCH /users/1` |
| **DELETE** | 삭제 | O | X | `DELETE /users/1` |

### 멱등성(Idempotency)이란?

같은 요청을 여러 번 보내도 결과가 동일한 성질

```
GET /users/1     → 100번 호출해도 같은 결과 (멱등)
DELETE /users/1  → 1번째: 삭제됨, 2번째: 404 (결과적으로 멱등)
POST /users      → 매번 새 사용자 생성 (비멱등)
```

### PUT vs PATCH

```java
// PUT: 전체 리소스 교체
PUT /api/v1/users/1
{
    "email": "new@email.com",
    "name": "새이름",
    "phoneNumber": "010-1234-5678",
    "role": "CUSTOMER"
}
// → 모든 필드 필요, 없으면 null로 설정됨

// PATCH: 부분 수정
PATCH /api/v1/users/1
{
    "name": "새이름"
}
// → 이름만 변경, 나머지는 유지
```

---

## JWT 토큰 갱신 전략

### 왜 두 개의 토큰을 사용하나?

```
Access Token:  짧은 만료 (15분~1시간)
              - 매 API 요청에 사용
              - 탈취 시 피해 최소화

Refresh Token: 긴 만료 (7일~30일)
              - Access Token 갱신 전용
              - 안전하게 저장 (HttpOnly Cookie)
```

### 토큰 갱신 흐름

```
┌──────────┐                              ┌──────────┐
│  Client  │                              │  Server  │
└────┬─────┘                              └────┬─────┘
     │                                         │
     │  1. POST /auth/login                    │
     │  {email, password}                      │
     │────────────────────────────────────────>│
     │                                         │
     │  {accessToken, refreshToken}            │
     │<────────────────────────────────────────│
     │                                         │
     │  2. GET /users/me                       │
     │  Authorization: Bearer {accessToken}    │
     │────────────────────────────────────────>│
     │                                         │
     │  200 OK {user data}                     │
     │<────────────────────────────────────────│
     │                                         │
     │  ... 15분 후 (Access Token 만료) ...     │
     │                                         │
     │  3. GET /users/me                       │
     │  Authorization: Bearer {accessToken}    │
     │────────────────────────────────────────>│
     │                                         │
     │  401 Unauthorized (토큰 만료)            │
     │<────────────────────────────────────────│
     │                                         │
     │  4. POST /auth/refresh                  │
     │  {refreshToken}                         │
     │────────────────────────────────────────>│
     │                                         │
     │  {newAccessToken, newRefreshToken}      │
     │<────────────────────────────────────────│
     │                                         │
     │  5. GET /users/me (재시도)               │
     │  Authorization: Bearer {newAccessToken} │
     │────────────────────────────────────────>│
     │                                         │
     │  200 OK {user data}                     │
     │<────────────────────────────────────────│
```

### 프론트엔드 구현 예시 (Axios Interceptor)

```typescript
// api/axios.ts
import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1',
});

// 응답 인터셉터: 401 에러 시 토큰 갱신
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // 401 에러 + 재시도 아닌 경우
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // 토큰 갱신
        const refreshToken = localStorage.getItem('refreshToken');
        const { data } = await axios.post('/api/v1/auth/refresh', {
          refreshToken
        });

        // 새 토큰 저장
        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);

        // 원래 요청 재시도
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(originalRequest);

      } catch (refreshError) {
        // Refresh Token도 만료 → 로그인 페이지로
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
```

---

## 응답 형식 설계

### 성공 응답

```json
// 단일 리소스 조회
GET /api/v1/users/1
{
  "id": 1,
  "email": "user@example.com",
  "name": "홍길동",
  "role": "CUSTOMER",
  "status": "ACTIVE",
  "createdAt": "2025-12-17T10:00:00Z"
}

// 컬렉션 조회 (페이징)
GET /api/v1/products?page=0&size=20
{
  "content": [
    { "id": 1, "name": "상품1", ... },
    { "id": 2, "name": "상품2", ... }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
}

// 생성 성공
POST /api/v1/users → 201 Created
{
  "id": 1,
  "email": "new@example.com",
  ...
}
```

### 에러 응답

```json
// 400 Bad Request (Validation 실패)
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "입력값이 올바르지 않습니다.",
    "details": {
      "email": "올바른 이메일 형식이 아닙니다.",
      "password": "비밀번호는 8자 이상이어야 합니다."
    }
  },
  "timestamp": "2025-12-17T10:30:00Z",
  "path": "/api/v1/users"
}

// 404 Not Found
{
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "사용자를 찾을 수 없습니다.",
    "details": { "userId": 999 }
  },
  "timestamp": "2025-12-17T10:30:00Z",
  "path": "/api/v1/users/999"
}

// 409 Conflict (중복)
{
  "error": {
    "code": "EMAIL_ALREADY_EXISTS",
    "message": "이미 사용 중인 이메일입니다.",
    "details": { "email": "existing@example.com" }
  },
  "timestamp": "2025-12-17T10:30:00Z",
  "path": "/api/v1/users"
}
```

### HTTP 상태 코드 사용

| 코드 | 의미 | 사용 시점 |
|------|------|----------|
| 200 | OK | 조회, 수정 성공 |
| 201 | Created | 생성 성공 |
| 204 | No Content | 삭제 성공 (응답 본문 없음) |
| 400 | Bad Request | 잘못된 요청 (validation) |
| 401 | Unauthorized | 인증 필요 (토큰 없음/만료) |
| 403 | Forbidden | 권한 없음 (인증됐지만 권한 부족) |
| 404 | Not Found | 리소스 없음 |
| 409 | Conflict | 충돌 (중복 이메일 등) |
| 422 | Unprocessable Entity | 비즈니스 로직 오류 |
| 500 | Internal Server Error | 서버 오류 |

---

## LookMarket API 구조

### 전체 API 맵

```
/api/v1
├── /auth
│   ├── POST   /login              # 로그인
│   ├── POST   /refresh            # 토큰 갱신
│   └── POST   /logout             # 로그아웃
│
├── /users
│   ├── POST   /                   # 회원가입
│   ├── GET    /{id}               # 프로필 조회
│   ├── PATCH  /{id}               # 프로필 수정
│   ├── DELETE /{id}               # 회원 탈퇴
│   ├── PATCH  /{id}/status        # 상태 변경 (관리자)
│   ├── GET    /me                 # 내 정보 조회
│   ├── PATCH  /me                 # 내 정보 수정
│   └── PUT    /me/password        # 비밀번호 변경
│
├── /products
│   ├── GET    /                   # 상품 목록
│   ├── GET    /{id}               # 상품 상세
│   ├── POST   /                   # 상품 등록 (관리자)
│   ├── PATCH  /{id}               # 상품 수정 (관리자)
│   ├── DELETE /{id}               # 상품 삭제 (관리자)
│   ├── GET    /search             # 상품 검색 (Elasticsearch)
│   └── GET    /autocomplete       # 자동완성
│
├── /orders
│   ├── GET    /                   # 내 주문 목록
│   ├── GET    /{id}               # 주문 상세
│   ├── POST   /                   # 주문 생성
│   └── POST   /{id}/cancel        # 주문 취소 (예외: 동사 허용)
│
└── /admin
    ├── GET    /users              # 사용자 목록 (관리자)
    ├── GET    /orders             # 전체 주문 목록 (관리자)
    └── GET    /statistics         # 통계 (관리자)
```

### 예외적으로 동사 허용하는 경우

```
# 주문 취소 - 복잡한 비즈니스 로직 (보상 트랜잭션 등)
POST /api/v1/orders/{id}/cancel

# 복잡한 검색 - body에 조건이 많을 때
POST /api/v1/products/search
{
  "keyword": "나이키",
  "filters": {...},
  "sort": {...}
}

# 벌크 작업
POST /api/v1/products/bulk-delete
{ "ids": [1, 2, 3] }
```

---

## 체크리스트

API 설계 시 확인할 사항:

- [ ] URL에 동사가 없는가? (리소스 중심)
- [ ] 복수형을 사용했는가?
- [ ] 소문자, 케밥 케이스인가?
- [ ] 적절한 HTTP 메서드를 사용했는가?
- [ ] 적절한 HTTP 상태 코드를 반환하는가?
- [ ] 일관된 응답 형식인가?
- [ ] 에러 응답이 명확한가?
- [ ] 버저닝이 적용되어 있는가?

---

## 참고 자료

- [Microsoft REST API Guidelines](https://github.com/microsoft/api-guidelines)
- [Google API Design Guide](https://cloud.google.com/apis/design)
- [REST API Tutorial](https://restfulapi.net/)
- [HTTP Status Codes](https://httpstatuses.com/)
