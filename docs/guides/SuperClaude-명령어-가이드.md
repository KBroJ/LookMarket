# SuperClaude 명령어 가이드

이 문서는 SuperClaude(`/sc:`) 명령어들의 용도, 사용 상황, 예시를 정리합니다.

---

## 목차

1. [명령어 빠른 참조표](#명령어-빠른-참조표)
2. [상황별 명령어 선택 가이드](#상황별-명령어-선택-가이드)
3. [자주 사용하는 명령어 TOP 10](#자주-사용하는-명령어-top-10)
4. [명령어 상세 설명](#명령어-상세-설명)
5. [플래그(Flags) 가이드](#플래그flags-가이드)

---

## 명령어 빠른 참조표

| 명령어 | 용도 | 사용 빈도 |
|--------|------|:--------:|
| `/sc:implement` | 기능 구현 | ⭐⭐⭐ |
| `/sc:explain` | 코드/개념 설명 | ⭐⭐⭐ |
| `/sc:analyze` | 코드 분석 (품질, 보안, 성능) | ⭐⭐⭐ |
| `/sc:test` | 테스트 실행 및 커버리지 분석 | ⭐⭐⭐ |
| `/sc:git` | Git 작업 (커밋, 브랜치 등) | ⭐⭐⭐ |
| `/sc:troubleshoot` | 문제 진단 및 해결 | ⭐⭐ |
| `/sc:build` | 프로젝트 빌드 | ⭐⭐ |
| `/sc:brainstorm` | 요구사항 탐색 및 아이디어 정리 | ⭐⭐ |
| `/sc:design` | 시스템/API 설계 | ⭐⭐ |
| `/sc:document` | 문서화 | ⭐⭐ |
| `/sc:improve` | 코드 품질 개선 | ⭐ |
| `/sc:cleanup` | 코드 정리 (dead code 제거) | ⭐ |
| `/sc:workflow` | PRD → 구현 워크플로우 생성 | ⭐ |
| `/sc:reflect` | 작업 회고 및 검증 | ⭐ |
| `/sc:research` | 웹 리서치 | ⭐ |

---

## 상황별 명령어 선택 가이드

### 개발 단계별

```
기획/설계 단계
├── /sc:brainstorm  ← 아이디어가 모호할 때
├── /sc:design      ← 아키텍처/API 설계
└── /sc:workflow    ← PRD를 구현 계획으로 변환

구현 단계
├── /sc:implement   ← 기능 구현 (가장 많이 사용)
├── /sc:build       ← 빌드 및 패키징
└── /sc:test        ← 테스트 실행

유지보수 단계
├── /sc:analyze     ← 코드 분석
├── /sc:improve     ← 리팩토링
├── /sc:cleanup     ← Dead code 제거
└── /sc:troubleshoot ← 버그 수정

문서화/학습
├── /sc:explain     ← 코드/개념 설명
├── /sc:document    ← 문서 생성
└── /sc:reflect     ← 작업 회고
```

### 질문 유형별

| 질문/상황 | 추천 명령어 |
|----------|------------|
| "이 코드가 뭘 하는 거야?" | `/sc:explain` |
| "로그인 기능 만들어줘" | `/sc:implement` |
| "이 코드 품질 좀 봐줘" | `/sc:analyze` |
| "빌드가 안 돼" | `/sc:troubleshoot --type build` |
| "테스트 돌려줘" | `/sc:test` |
| "커밋 메시지 만들어줘" | `/sc:git commit --smart-commit` |
| "이 기능 어떻게 설계하면 좋을까?" | `/sc:design` |
| "뭘 만들지 같이 고민해줘" | `/sc:brainstorm` |
| "불필요한 코드 정리해줘" | `/sc:cleanup` |
| "성능 개선해줘" | `/sc:improve --type performance` |

---

## 자주 사용하는 명령어 TOP 10

### 1. `/sc:implement` - 기능 구현 ⭐⭐⭐

**언제 사용**: 새로운 기능을 구현할 때

```bash
# React 컴포넌트 구현
/sc:implement user profile component --type component --framework react

# API 서비스 구현 (테스트 포함)
/sc:implement user authentication API --type api --safe --with-tests

# 전체 기능 구현
/sc:implement payment processing system --type feature --with-tests
```

**특징**:
- 여러 페르소나 활성화 (architect, frontend, backend, security)
- 프레임워크별 베스트 프랙티스 적용
- `--with-tests` 옵션으로 테스트 코드 함께 생성

---

### 2. `/sc:explain` - 코드/개념 설명 ⭐⭐⭐

**언제 사용**: 코드나 개념을 이해하고 싶을 때

```bash
# 기본 코드 설명
/sc:explain authentication.js --level basic

# 프레임워크 개념 설명
/sc:explain react-hooks --level intermediate --context react

# 시스템 아키텍처 설명
/sc:explain microservices-system --level advanced

# 보안 개념 설명
/sc:explain jwt-authentication --context security
```

**수준 옵션**:
- `--level basic`: 초보자용
- `--level intermediate`: 중급자용
- `--level advanced`: 고급자용

---

### 3. `/sc:analyze` - 코드 분석 ⭐⭐⭐

**언제 사용**: 코드 품질, 보안, 성능을 점검하고 싶을 때

```bash
# 전체 프로젝트 분석
/sc:analyze

# 보안 집중 분석
/sc:analyze src/auth --focus security --depth deep

# 성능 분석
/sc:analyze --focus performance --format report

# 빠른 품질 체크
/sc:analyze src/components --focus quality --depth quick
```

**분석 유형 (`--focus`)**:
- `quality`: 코드 품질, 코드 스멜
- `security`: 보안 취약점
- `performance`: 성능 병목
- `architecture`: 아키텍처 검토

---

### 4. `/sc:test` - 테스트 실행 ⭐⭐⭐

**언제 사용**: 테스트를 실행하고 커버리지를 확인할 때

```bash
# 전체 테스트 실행
/sc:test

# 특정 디렉토리 단위 테스트 + 커버리지
/sc:test src/components --type unit --coverage

# E2E 테스트 (Playwright 사용)
/sc:test --type e2e

# 개발 중 watch 모드
/sc:test --watch --fix
```

**테스트 유형 (`--type`)**:
- `unit`: 단위 테스트
- `integration`: 통합 테스트
- `e2e`: End-to-End 테스트
- `all`: 모든 테스트

---

### 5. `/sc:git` - Git 작업 ⭐⭐⭐

**언제 사용**: Git 관련 작업을 할 때

```bash
# 상태 분석 및 다음 단계 추천
/sc:git status

# 변경 내용 분석 후 자동 커밋 메시지 생성
/sc:git commit --smart-commit

# 인터랙티브 머지
/sc:git merge feature-branch --interactive
```

**주요 기능**:
- Conventional Commits 형식 자동 생성
- 변경 내용 분석 기반 커밋 메시지
- 충돌 해결 가이드

---

### 6. `/sc:troubleshoot` - 문제 해결 ⭐⭐

**언제 사용**: 버그, 빌드 실패, 성능 문제를 해결할 때

```bash
# 버그 조사
/sc:troubleshoot "Null pointer exception in user service" --type bug --trace

# 빌드 오류 해결
/sc:troubleshoot "TypeScript compilation errors" --type build --fix

# 성능 문제 진단
/sc:troubleshoot "API response times degraded" --type performance

# 배포 문제 해결
/sc:troubleshoot "Service not starting in production" --type deployment
```

**문제 유형 (`--type`)**:
- `bug`: 코드 버그
- `build`: 빌드 실패
- `performance`: 성능 저하
- `deployment`: 배포 문제

---

### 7. `/sc:build` - 프로젝트 빌드 ⭐⭐

**언제 사용**: 프로젝트를 빌드하거나 패키징할 때

```bash
# 기본 빌드
/sc:build

# 프로덕션 빌드 (최적화 포함)
/sc:build --type prod --clean --optimize

# 특정 모듈 빌드 (상세 로그)
/sc:build frontend --verbose

# 개발 빌드 + 검증
/sc:build --type dev --validate
```

---

### 8. `/sc:brainstorm` - 아이디어 탐색 ⭐⭐

**언제 사용**: 아이디어가 모호하거나 요구사항을 정리하고 싶을 때

```bash
# 체계적인 아이디어 탐색
/sc:brainstorm "AI-powered project management tool" --strategy systematic --depth deep

# 애자일 방식 탐색
/sc:brainstorm "real-time collaboration features" --strategy agile --parallel

# 엔터프라이즈 솔루션 검증
/sc:brainstorm "enterprise data analytics platform" --strategy enterprise
```

**전략 옵션 (`--strategy`)**:
- `systematic`: 체계적 단계별 탐색
- `agile`: 빠른 반복 탐색
- `enterprise`: 엔터프라이즈 수준 검증

---

### 9. `/sc:design` - 시스템 설계 ⭐⭐

**언제 사용**: 아키텍처, API, 데이터베이스를 설계할 때

```bash
# 시스템 아키텍처 설계
/sc:design user-management-system --type architecture --format diagram

# API 스펙 설계
/sc:design payment-api --type api --format spec

# 컴포넌트 인터페이스 설계
/sc:design notification-service --type component --format code

# 데이터베이스 스키마 설계
/sc:design e-commerce-db --type database --format diagram
```

**설계 유형 (`--type`)**:
- `architecture`: 시스템 아키텍처
- `api`: API 스펙
- `component`: 컴포넌트 인터페이스
- `database`: 데이터베이스 스키마

---

### 10. `/sc:document` - 문서화 ⭐⭐

**언제 사용**: 코드나 API에 대한 문서를 생성할 때

```bash
# 인라인 코드 문서 (JSDoc 등)
/sc:document src/auth/login.js --type inline

# API 레퍼런스 문서
/sc:document src/api --type api --style detailed

# 사용자 가이드
/sc:document payment-module --type guide --style brief

# 컴포넌트 문서
/sc:document components/ --type external
```

---

## 명령어 상세 설명

### 개선/정리 명령어

#### `/sc:improve` - 코드 개선

**언제 사용**: 리팩토링, 성능 개선, 보안 강화가 필요할 때

```bash
# 코드 품질 개선
/sc:improve src/ --type quality --safe

# 성능 최적화
/sc:improve api-endpoints --type performance --interactive

# 유지보수성 개선
/sc:improve legacy-modules --type maintainability --preview

# 보안 강화
/sc:improve auth-service --type security --validate
```

#### `/sc:cleanup` - 코드 정리

**언제 사용**: Dead code 제거, import 정리, 프로젝트 구조 개선이 필요할 때

```bash
# 안전한 코드 정리
/sc:cleanup src/ --type code --safe

# import 정리 (미리보기)
/sc:cleanup --type imports --preview

# 전체 정리 (인터랙티브)
/sc:cleanup --type all --interactive

# 적극적 정리
/sc:cleanup components/ --aggressive
```

### 워크플로우/반영 명령어

#### `/sc:workflow` - 워크플로우 생성

**언제 사용**: PRD나 기능 요구사항을 구현 계획으로 변환할 때

```bash
# PRD를 워크플로우로 변환
/sc:workflow docs/PRD/feature-spec.md --strategy systematic --depth deep

# 기능 구현 워크플로우
/sc:workflow "user authentication system" --strategy agile --parallel
```

#### `/sc:reflect` - 작업 회고

**언제 사용**: 작업 완료 후 검증하거나 세션을 회고할 때

```bash
# 작업 준수 확인
/sc:reflect --type task --analyze

# 세션 진행 분석
/sc:reflect --type session --validate

# 완료 여부 검증
/sc:reflect --type completion
```

### 유틸리티 명령어

#### `/sc:research` - 웹 리서치

**언제 사용**: 기술, 라이브러리, 시장에 대해 조사할 때

```bash
/sc:research "best practices for microservices authentication 2024"
```

#### `/sc:help` - 도움말

**언제 사용**: 사용 가능한 명령어 목록을 볼 때

```bash
/sc:help
```

---

## 플래그(Flags) 가이드

### 분석 깊이 플래그

| 플래그 | 설명 | 토큰 사용량 |
|--------|------|------------|
| `--think` | 표준 분석 | ~4K 토큰 |
| `--think-hard` | 심층 분석 | ~10K 토큰 |
| `--ultrathink` | 최대 깊이 분석 | ~32K 토큰 |

```bash
# 예시: 복잡한 아키텍처 분석
/sc:analyze --think-hard --focus architecture
```

### MCP 서버 플래그

| 플래그 | 용도 |
|--------|------|
| `--c7` / `--context7` | 라이브러리/프레임워크 문서 조회 |
| `--seq` / `--sequential` | 복잡한 다단계 추론 |
| `--serena` | 심볼 기반 코드 분석 |
| `--play` / `--playwright` | 브라우저 테스트 |
| `--all-mcp` | 모든 MCP 서버 활성화 |
| `--no-mcp` | MCP 서버 비활성화 |

```bash
# 예시: Context7로 React 공식 문서 참조
/sc:explain react-hooks --context7
```

### 실행 제어 플래그

| 플래그 | 설명 |
|--------|------|
| `--safe` | 안전 모드 (보수적 실행) |
| `--validate` | 실행 전 검증 |
| `--interactive` | 대화형 모드 |
| `--preview` | 미리보기 (실행 안 함) |
| `--fix` | 자동 수정 적용 |

```bash
# 예시: 안전하게 코드 정리 (미리보기)
/sc:cleanup src/ --safe --preview
```

### 출력 최적화 플래그

| 플래그 | 설명 |
|--------|------|
| `--uc` / `--ultracompressed` | 토큰 30-50% 절약 |
| `--scope file\|module\|project` | 분석 범위 지정 |
| `--focus quality\|security\|performance` | 집중 영역 지정 |

---

## LookMarket 프로젝트 활용 예시

### Phase 1: User 도메인 구현 시

```bash
# 1. 설계 검토
/sc:explain User 도메인 구현 --level advanced

# 2. 기능 구현
/sc:implement User registration API --type api --with-tests

# 3. 코드 분석
/sc:analyze lookmarket-domain/src --focus quality

# 4. 테스트 실행
/sc:test lookmarket-domain --type unit --coverage

# 5. 커밋
/sc:git commit --smart-commit
```

### 버그 수정 시

```bash
# 1. 문제 진단
/sc:troubleshoot "UserService에서 NPE 발생" --type bug --trace

# 2. 수정 후 테스트
/sc:test --type unit

# 3. 코드 개선
/sc:improve src/user --type quality --safe
```

### 새 기능 기획 시

```bash
# 1. 아이디어 탐색
/sc:brainstorm "Product 검색 기능 with Elasticsearch" --strategy systematic

# 2. 설계
/sc:design product-search-api --type api --format spec

# 3. 워크플로우 생성
/sc:workflow "Product search implementation" --strategy agile
```

---

## 참고

- SuperClaude 명령어 파일 위치: `~/.claude/commands/sc/`
- 전체 명령어 목록: `/sc:help`
- MCP 서버 상태 확인: `claude mcp list`
