# ADR-002: RESTful API 설계 원칙

## 상태
**승인됨** (2025-12-17)

## 상황 (Context)

LookMarket 프로젝트에서 API 설계 시 일관된 규칙이 필요합니다. 초기 구현에서 일부 API가 RESTful 원칙을 위반하고 있었습니다:

- `POST /users/{id}/activate` - 동사 사용
- `POST /users/{id}/suspend` - 동사 사용
- `PATCH /users/{id}/email` - 과도한 세분화

또한 API 버저닝, 응답 형식, 에러 처리 등에 대한 통일된 기준이 필요했습니다.

## 결정 (Decision)

### 1. API 버저닝 전략

**URL Path 버저닝** 채택: `/api/v1/...`

```
/api/v1/users
/api/v1/products
/api/v1/orders
```

**이유:**
- 명확하고 직관적
- 캐싱 용이
- 테스트 및 문서화 편리
- 업계 표준 (GitHub, Stripe, Twitter 등)

### 2. RESTful URL 설계 원칙

| 원칙 | 규칙 | 좋은 예 | 나쁜 예 |
|------|------|---------|---------|
| 리소스 중심 | URL은 명사(리소스) | `/users` | `/getUsers` |
| HTTP 메서드 | 행위는 메서드로 | `DELETE /users/1` | `POST /deleteUser` |
| 복수형 | 컬렉션은 복수 | `/users` | `/user` |
| 계층 구조 | 소속 관계 표현 | `/users/1/orders` | `/userOrders` |
| 소문자 | URL은 소문자 | `/users` | `/Users` |
| 케밥 케이스 | 단어 연결 시 | `/order-items` | `/orderItems` |

### 3. HTTP 메서드 사용 규칙

| 메서드 | 용도 | 멱등성 | 예시 |
|--------|------|--------|------|
| GET | 조회 | O | `GET /users/1` |
| POST | 생성 | X | `POST /users` |
| PUT | 전체 수정 | O | `PUT /users/1` |
| PATCH | 부분 수정 | O | `PATCH /users/1` |
| DELETE | 삭제 | O | `DELETE /users/1` |

### 4. 표준 API 구조

#### 인증 API
```
POST   /api/v1/auth/login           # 로그인 (토큰 발급)
POST   /api/v1/auth/refresh         # 토큰 갱신
POST   /api/v1/auth/logout          # 로그아웃 (토큰 폐기)
```

#### 사용자 API
```
POST   /api/v1/users                # 회원가입
GET    /api/v1/users/{id}           # 프로필 조회
PATCH  /api/v1/users/{id}           # 프로필 수정
DELETE /api/v1/users/{id}           # 회원 탈퇴

# 현재 로그인 사용자
GET    /api/v1/users/me             # 내 정보 조회
PATCH  /api/v1/users/me             # 내 정보 수정
PUT    /api/v1/users/me/password    # 비밀번호 변경
```

#### 상태 변경 (Sub-resource 패턴)
```
# 동사 대신 상태를 리소스로 취급
PATCH  /api/v1/users/{id}/status
Body: { "status": "ACTIVE" | "SUSPENDED" | "INACTIVE" }
```

### 5. 응답 형식

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

### 6. HTTP 상태 코드

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

## 결과 (Consequences)

### 장점
- API 일관성 확보
- 클라이언트 개발자가 예측 가능
- HTTP 표준 준수로 캐싱, 프록시 등 인프라 활용 용이
- 문서화 자동화 (Swagger) 용이

### 단점
- 기존 코드 리팩토링 필요
- 일부 복잡한 비즈니스 로직은 RESTful로 표현하기 어려울 수 있음

### 예외 허용
- 복잡한 검색: `POST /api/v1/products/search` (body에 조건)
- 벌크 작업: `POST /api/v1/products/bulk-delete`

## 참고 자료
- [Microsoft REST API Guidelines](https://github.com/microsoft/api-guidelines)
- [Google API Design Guide](https://cloud.google.com/apis/design)
- [REST API Tutorial](https://restfulapi.net/)
