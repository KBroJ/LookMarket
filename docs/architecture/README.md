# 🏛️ Architecture - 아키텍처 원칙 및 규칙

> 이 폴더는 LookMarket 프로젝트의 아키텍처 원칙과 강제 규칙을 정의합니다.
> 팀원과 기여자가 일관된 아키텍처 패턴을 유지할 수 있도록 명확한 가이드라인을 제공합니다.

---

## 📁 폴더 구조

```
architecture/
├── ENFORCEMENT_RULES.md     # 아키텍처 강제 규칙 (상세 예시)
└── decisions/               # ADR (Architecture Decision Records)
    └── (향후 설계 결정 기록)
```

---

## 📄 문서 목록

### ENFORCEMENT_RULES.md

**목적**: 아키텍처 패턴 준수를 위한 강제 규칙

**주요 내용:**
- 레이어 간 의존성 방향
- Import 금지 목록
- DDD 패턴 강제 규칙
- Repository 패턴
- 트랜잭션 경계
- DTO 변환 규칙
- 예외 처리 규칙

**언제 사용하나?**
- 새 기능 구현 전 규칙 확인
- 코드 리뷰 시 체크리스트
- AI 도구(Claude Code) 코드 생성 시 참고

**예시:**
```java
// ❌ 금지: Domain에 JPA 애노테이션
@Entity
public class User { }

// ✅ 허용: 순수 Java
public class User { }
```

---

### decisions/ - ADR (Architecture Decision Records)

**목적**: 중요한 설계 결정 사항 기록

**ADR이란?**
- Architecture Decision Record의 약자
- 중요한 기술적 결정과 그 이유를 문서화
- 미래의 개발자(또는 자신)에게 "왜 이렇게 했는지" 설명

**ADR 작성 시점:**
- 새로운 라이브러리 도입 결정
- 아키텍처 패턴 변경
- 기술 스택 선택
- 중요한 설계 트레이드오프

**파일명 형식:**
```
ADR-001-왜-멀티모듈을-선택했는가.md
ADR-002-QueryDSL-도입-결정.md
ADR-003-Kafka-vs-RabbitMQ-선택.md
```

**ADR 템플릿:**
```markdown
# ADR-XXX: [제목]

**날짜**: YYYY-MM-DD
**상태**: 제안됨 / 승인됨 / 폐기됨

## 상황 (Context)
어떤 문제를 해결하려고 하는가?

## 고려한 옵션들 (Options)
1. 옵션 A
2. 옵션 B
3. 옵션 C

## 결정 (Decision)
최종적으로 무엇을 선택했는가?

## 이유 (Rationale)
왜 이것을 선택했는가?

## 결과 (Consequences)
- 긍정적 영향:
- 부정적 영향:
- 리스크:
```

---

## 🎯 architecture/ vs learning/ 차이점

| 구분 | architecture/ | learning/ |
|------|--------------|----------|
| **목적** | 프로젝트 원칙 정의 | 개인 학습 및 이해 |
| **독자** | 팀원, 기여자 | 본인, 학습자 |
| **내용** | 강제 규칙, ADR | 비교 분석, Q&A |
| **변경** | 신중하게 (팀 합의) | 자유롭게 |
| **예시** | "Domain에 JPA 사용 금지" | "Loopers vs LookMarket 비교" |

**간단히:**
- `architecture/` = "우리 프로젝트는 이렇게 해야 한다" (규칙)
- `learning/` = "이런 방식들이 있고, 차이는 이렇다" (학습)

---

## 📖 사용 가이드

### 1. 새 기능 구현 전

1. `ENFORCEMENT_RULES.md`에서 관련 규칙 확인
2. 해당 레이어의 의존성 방향 검토
3. Import 금지 목록 확인

```java
// 예: Domain Layer에서 User 작성 시
// ENFORCEMENT_RULES.md의 "Domain Model 규칙" 섹션 참고
```

### 2. 코드 리뷰 시

**체크리스트:**
- [ ] 레이어 간 의존성 방향 준수
- [ ] Domain에 프레임워크 의존성 없음
- [ ] Repository 인터페이스가 Domain에 위치
- [ ] @Transactional이 Application 레이어에만 존재
- [ ] DTO 변환이 적절한 레이어에서 수행

### 3. 중요한 설계 결정 시

**ADR 작성 프로세스:**
1. `decisions/` 폴더에 새 ADR 파일 생성
2. 템플릿에 따라 작성
3. 팀원 리뷰 (개인 프로젝트는 생략)
4. 승인 후 상태 업데이트
5. 해당 결정 사항 구현

---

## 🔍 자주 묻는 질문

### Q1: ENFORCEMENT_RULES.md는 언제 수정하나요?

**답변:** 다음 경우에만 수정합니다.
- 새로운 강제 규칙 추가
- 기존 규칙의 예외 사항 발견
- 규칙 위반 패턴 발견 및 명확화 필요

**주의:** 규칙 변경은 신중하게! 기존 코드에 영향을 줄 수 있습니다.

---

### Q2: ADR은 꼭 작성해야 하나요?

**답변:** 중요한 결정은 꼭 작성하세요.

**작성 권장:**
- ✅ 새 라이브러리 도입 (QueryDSL, Kafka 등)
- ✅ 아키텍처 패턴 변경 (멀티모듈 도입 등)
- ✅ 기술 스택 선택 (JPA vs MyBatis)

**작성 불필요:**
- ❌ 작은 코드 스타일 변경
- ❌ 변수명 변경
- ❌ 자명한 결정

**기준:** "6개월 후의 나에게 설명이 필요한가?"

---

### Q3: architecture/ vs learning/ 중 어디에 작성할까?

**architecture/ 작성 기준:**
- 프로젝트 전체에 적용되는 규칙
- 모든 개발자가 따라야 하는 원칙
- "반드시 이렇게 해야 한다"

**learning/ 작성 기준:**
- 개인 학습 및 이해를 위한 자료
- 여러 방식의 비교 분석
- "이런 방식들이 있다"

**예시:**
- `architecture/ENFORCEMENT_RULES.md`: "Domain에 @Entity 사용 금지"
- `learning/Loopers-vs-LookMarket-비교분석.md`: "Loopers는 @Entity를 사용하고, LookMarket은 사용하지 않는다. 각각의 장단점은..."

---

## 📚 관련 문서

- [CLAUDE.md](../../CLAUDE.md) - 프로젝트 전체 가이드
- [learning/](../learning/) - 학습 자료 및 비교 분석
- [design/](../design/) - 시스템 설계 사양

---

## 🤝 기여 가이드

### 새 규칙 추가 시

1. `ENFORCEMENT_RULES.md`에 규칙 추가
2. 예시 코드 포함 (❌ 잘못된 예, ✅ 올바른 예)
3. 이유 설명 추가

### ADR 작성 시

1. `decisions/` 폴더에 새 파일 생성
2. 템플릿에 따라 작성
3. 이 README.md 업데이트 (필요 시)

---

**마지막 업데이트**: 2025-12-16
**관리자**: LookMarket 개발팀
