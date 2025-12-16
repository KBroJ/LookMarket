# 📚 LookMarket 프로젝트 문서

> LookMarket 프로젝트의 모든 문서를 체계적으로 관리합니다.

## 📁 문서 구조

```
docs/
├── 📋 project/              # 프로젝트 관리 및 진행 상황
├── 📐 design/               # 설계 및 사양 문서
├── 🏛️ architecture/          # 아키텍처 원칙 및 규칙
├── 📚 learning/             # 학습 자료 및 비교 분석
├── 📖 guides/               # 실용 가이드 (사용법, 튜토리얼)
├── 🛠️ setup/                # 환경 설정 및 도구 사용법
└── 📦 archive/              # 보관/참고용 문서
```

---

## 📋 project/ - 프로젝트 관리

**목적**: 진행 상황 추적 및 보고서

| 문서 | 설명 |
|------|------|
| `DEVELOPMENT_LOG.md` | 개발 일지 (일별 작업, 기술 결정, 학습 내용) |
| `Phase0-환경검증-완료보고서.md` | Phase 0 환경 검증 보고서 |

**언제 사용하나?**
- 매 작업 세션 후 개발 일지 업데이트
- Phase 완료 시 보고서 작성
- 포트폴리오 설명 시 참고

---

## 📐 design/ - 설계 및 사양

**목적**: 시스템 설계 및 사양 문서

| 문서 | 설명 |
|------|------|
| `LookMarket_Project_Specification.md` | 프로젝트 전체 사양서 (4,362 라인) |

**언제 사용하나?**
- 새로운 기능 구현 전 사양 확인
- 아키텍처 패턴 준수 여부 검토
- API 설계 시 명세 참고

**주요 내용**:
- Hexagonal Architecture
- 데이터베이스 설계 (ERD)
- Kafka 이벤트 아키텍처
- API 설계
- 6주 구현 로드맵

---

## 🏛️ architecture/ - 아키텍처 원칙 및 규칙

**목적**: 프로젝트의 아키텍처 원칙과 강제 규칙 정의

| 문서 | 설명 |
|------|------|
| `ENFORCEMENT_RULES.md` | 아키텍처 강제 규칙 (상세 예시) |
| `decisions/` | ADR (Architecture Decision Records) |

**언제 사용하나?**
- 새 기능 구현 전 규칙 확인
- 코드 리뷰 시 체크리스트
- 중요한 설계 결정 시 ADR 작성

**주요 내용**:
- 레이어 간 의존성 방향
- Import 금지 목록
- DDD 패턴 강제 규칙
- Repository 패턴
- 트랜잭션 경계

**특징:**
- "반드시 이렇게 해야 한다" (규칙)
- 팀 전체가 따라야 하는 원칙

---

## 📚 learning/ - 학습 자료 및 비교 분석

**목적**: 개인 학습, 개념 이해, 다양한 접근법 비교

| 문서 | 설명 |
|------|------|
| `Hexagonal-Architecture-Domain-구현-방식-비교.md` | Hexagonal Architecture 두 가지 방식 비교 (Domain Lombok 사용 여부) |
| `멀티모듈-vs-폴더구분-비교분석.md` | 멀티 모듈 vs 폴더 구분 비교 (Why 멀티 모듈인가?) |
| `qna/` | 질의응답 모음 |

**언제 사용하나?**
- 아키텍처 패턴 이해가 필요할 때
- "왜 이렇게 설계했는가?" 질문 준비
- 다른 방식과의 차이점 학습
- 면접 대비 (설계 결정 이유)

**주요 내용**:
- Loopers (실용적 접근) vs LookMarket (순수 접근)
- Domain에서 Lombok을 사용하지 않는 이유
- 멀티 모듈 vs 폴더 구분의 의존성 제어 차이
- 면접 대비 답변 가이드

**특징:**
- "이런 방식들이 있다" (비교)
- 객관적 장단점 분석
- "왜(Why)"와 "무엇(What)"에 집중

---

## 📖 guides/ - 실용 가이드

**목적**: 기술 사용법 및 튜토리얼

| 문서 | 설명 |
|------|------|
| `멀티모듈-설정-가이드.md` | 멀티 모듈 Gradle 설정 및 실행 방법 (How) |
| `Docker-Compose-설정-가이드.md` | docker-compose.yml 상세 설명 |

**언제 사용하나?**
- 새 모듈 추가 시 Gradle 설정 방법
- 빌드/실행 명령어 찾기
- Docker Compose 설정 변경 시
- 트러블슈팅 (컴파일 오류, 의존성 문제)

**학습 순서**:
1. 멀티모듈-설정-가이드 → Gradle 설정 방법 이해
2. Docker-Compose-설정-가이드 → 인프라 환경 이해

---

## 🛠️ setup/ - 환경 설정

**목적**: 개발 환경 설정 및 도구 사용법

| 문서 | 설명 |
|------|------|
| `window 환경에서 클로드 코드 및 관련 설치.txt` | Windows 환경 설정 가이드 |
| `클로드 코드 단축키 및 사용법.txt` | Claude Code 효율적 사용법 |

**언제 사용하나?**
- 최초 개발 환경 구성
- 새로운 PC에서 환경 설정
- 단축키 빠른 참고

---

## 📦 archive/ - 보관 문서

**목적**: 참고용 또는 구버전 문서

| 문서 | 설명 |
|------|------|
| `PayPoint_Project_Specification.md` | 이전 프로젝트 사양서 (참고용) |

**언제 사용하나?**
- 이전 프로젝트 참고
- 폐기된 설계 안 검토

**주의**: 이 폴더의 문서는 최신 상태가 아닐 수 있음

---

## 🎯 폴더 간 차이점 정리

### architecture/ vs learning/

| 구분 | architecture/ | learning/ |
|------|--------------|----------|
| **목적** | 프로젝트 원칙 정의 | 개인 학습 및 이해 |
| **톤** | "우리는 이렇게 해야 한다" | "이런 방식들이 있다" |
| **내용** | 강제 규칙, ADR | 비교 분석, Q&A |
| **변경** | 신중하게 (팀 합의) | 자유롭게 |
| **예시** | "Domain에 JPA 사용 금지" | "Loopers vs LookMarket 비교" |

### learning/ vs guides/

| 구분 | learning/ | guides/ |
|------|----------|---------|
| **목적** | 개념 이해, 비교 | 사용법 안내 |
| **스타일** | 설명 및 분석 | 단계별 튜토리얼 |
| **초점** | "왜?", "차이는?" | "어떻게?" |
| **예시** | "멀티모듈 vs 폴더 구분 차이" | "멀티모듈 설정하는 방법" |

### design/ vs architecture/

| 구분 | design/ | architecture/ |
|------|---------|--------------|
| **목적** | 시스템 설계 사양 | 아키텍처 원칙 |
| **내용** | 스펙, ERD, API 명세 | 강제 규칙, ADR |
| **예시** | "User 테이블 스키마" | "Domain 독립성 원칙" |

---

## 🎯 빠른 참고

### 처음 시작하는 경우
1. `setup/window 환경에서 클로드 코드 및 관련 설치.txt` - 환경 설정
2. `guides/멀티모듈-아키텍처-가이드.md` - 프로젝트 구조 이해
3. `guides/Docker-Compose-설정-가이드.md` - 인프라 이해
4. `design/LookMarket_Project_Specification.md` - 전체 사양 파악

### 개발 진행 중
- `project/DEVELOPMENT_LOG.md` - 진행 상황 확인 및 업데이트
- `architecture/ENFORCEMENT_RULES.md` - 규칙 준수 확인
- `design/LookMarket_Project_Specification.md` - 구현 방향 확인
- `guides/` - 기술 사용법 참고

### 학습 및 이해
- `learning/Hexagonal-Architecture-Domain-구현-방식-비교.md` - 아키텍처 비교
- `learning/qna/` - 궁금한 점 Q&A
- `guides/` - 실용 가이드

### 문제 발생 시
- `guides/Docker-Compose-설정-가이드.md` - 인프라 문제 해결
- `project/Phase0-환경검증-완료보고서.md` - 환경 검증 참고
- `project/DEVELOPMENT_LOG.md` - 이전 문제 해결 사례

---

## 📝 문서 작성 규칙

### 1. 일관성 유지
- 동일한 형식으로 작성
- Markdown 스타일 가이드 준수
- 이모지 사용 일관성

### 2. 구체성
- 추상적 설명보다 구체적 예시
- 코드 예제 포함
- 스크린샷 활용 (필요 시)

### 3. 맥락 포함
- 왜 그 결정을 했는지
- 어떤 문제를 해결하는지
- 다른 방법과의 비교

### 4. 학습 중심
- 초보자도 이해할 수 있게
- 전문 용어 설명 추가
- 단계별 설명

### 5. 최신 상태 유지
- 변경 사항 즉시 반영
- 마지막 업데이트 날짜 기록
- 폐기된 문서는 archive로 이동

---

## 🔍 문서 찾기

### 주제별 문서
- **멀티 모듈 설정**: `guides/멀티모듈-설정-가이드.md` (Gradle 설정 및 실행)
- **멀티 모듈 비교**: `learning/멀티모듈-vs-폴더구분-비교분석.md` (왜 멀티 모듈인가?)
- **Docker**: `guides/Docker-Compose-설정-가이드.md`
- **아키텍처 규칙**: `architecture/ENFORCEMENT_RULES.md`
- **아키텍처 비교**: `learning/Hexagonal-Architecture-Domain-구현-방식-비교.md`
- **환경 설정**: `setup/` 폴더
- **전체 설계**: `design/LookMarket_Project_Specification.md`
- **진행 상황**: `project/DEVELOPMENT_LOG.md`

### 문서 유형별
- **원칙/규칙**: `architecture/` 폴더
- **학습 자료**: `learning/` 폴더
- **사용법**: `guides/` 폴더
- **설계 문서**: `design/` 폴더
- **진행 보고**: `project/` 폴더
- **설정 가이드**: `setup/` 폴더

---

## 📊 문서 통계

| 폴더 | 문서 수 | 용도 |
|------|---------|------|
| project/ | 2 | 진행 관리 |
| design/ | 1 | 설계 사양 |
| architecture/ | 1 + ADR | 원칙/규칙 |
| learning/ | 1 + Q&A | 학습 자료 |
| guides/ | 2 | 사용법 |
| setup/ | 2 | 환경 설정 |
| archive/ | 1 | 보관 |
| **총계** | **10+** | |

---

## 🤝 기여 가이드

### 새 문서 추가 시

**1단계: 적절한 폴더 선택**
- 프로젝트 원칙? → `architecture/`
- 학습/비교? → `learning/`
- 사용법? → `guides/`
- 설계 사양? → `design/`
- 진행 보고? → `project/`

**2단계: 문서 작성**
- 해당 폴더의 README 참고
- 템플릿 사용 (있는 경우)

**3단계: README 업데이트**
- 해당 폴더 README.md 업데이트
- 루트 README.md (이 파일) 업데이트

### 문서 이동 시
1. 파일 이동
2. 이전 폴더 README.md 업데이트
3. 새 폴더 README.md 업데이트
4. 관련 링크 수정

### 문서 삭제 시
- 삭제하지 말고 `archive/`로 이동
- 삭제 이유를 archive/README.md에 기록

---

## 📞 관련 링크

- [프로젝트 루트 README](../README.md)
- [CLAUDE.md](../CLAUDE.md) - 프로젝트 전체 가이드
- [GitHub Repository](https://github.com/KBroJ/LookMarket)

---

**마지막 업데이트**: 2025-12-16
**관리자**: LookMarket 개발팀
**버전**: 2.0 (폴더 구조 개선)
