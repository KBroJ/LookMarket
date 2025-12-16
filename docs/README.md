# 📚 LookMarket 프로젝트 문서

> LookMarket 프로젝트의 모든 문서를 체계적으로 관리합니다.

## 📁 문서 구조

```
docs/
├── 📋 project/              # 프로젝트 관리 및 진행 상황
├── 📐 design/               # 설계 및 아키텍처 문서
├── 📚 guides/               # 학습 및 설명 가이드
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

## 📐 design/ - 설계 및 아키텍처

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

## 📚 guides/ - 학습 및 설명 가이드

**목적**: 기술 개념 설명 및 학습 자료

| 문서 | 설명 |
|------|------|
| `멀티모듈-아키텍처-가이드.md` | 멀티 모듈 vs 폴더 구분, 의존성 제어 |
| `Docker-Compose-설정-가이드.md` | docker-compose.yml 상세 설명 |

**언제 사용하나?**
- 프로젝트 구조가 처음인 경우
- Docker Compose 설정 변경 시
- 팀원 교육 자료로 활용

**학습 순서**:
1. 멀티모듈-아키텍처-가이드 → 프로젝트 구조 이해
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

## 🎯 빠른 참고

### 처음 시작하는 경우
1. `setup/window 환경에서 클로드 코드 및 관련 설치.txt` - 환경 설정
2. `guides/멀티모듈-아키텍처-가이드.md` - 프로젝트 구조 이해
3. `guides/Docker-Compose-설정-가이드.md` - 인프라 이해
4. `design/LookMarket_Project_Specification.md` - 전체 사양 파악

### 개발 진행 중
- `project/DEVELOPMENT_LOG.md` - 진행 상황 확인 및 업데이트
- `design/LookMarket_Project_Specification.md` - 구현 방향 확인
- `guides/` - 기술 개념 학습

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
- **멀티 모듈**: `guides/멀티모듈-아키텍처-가이드.md`
- **Docker**: `guides/Docker-Compose-설정-가이드.md`
- **환경 설정**: `setup/` 폴더
- **전체 설계**: `design/LookMarket_Project_Specification.md`
- **진행 상황**: `project/DEVELOPMENT_LOG.md`

### 문서 유형별
- **학습 자료**: `guides/` 폴더
- **설계 문서**: `design/` 폴더
- **진행 보고**: `project/` 폴더
- **설정 가이드**: `setup/` 폴더

---

## 📊 문서 통계

| 폴더 | 문서 수 | 용도 |
|------|---------|------|
| project/ | 2 | 진행 관리 |
| design/ | 1 | 설계 사양 |
| guides/ | 2 | 학습 자료 |
| setup/ | 2 | 환경 설정 |
| archive/ | 1 | 보관 |
| **총계** | **8** | |

---

## 🤝 기여 가이드

### 새 문서 추가 시
1. 적절한 폴더 선택
2. 문서 작성
3. 해당 폴더 README.md 업데이트
4. 루트 README.md (이 파일) 업데이트

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
**버전**: 1.0
