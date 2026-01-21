# Campaign Dashboard - 문서 인덱스

## 📚 전체 문서 목록

### 🎯 시작하기
| 문서 | 크기 | 설명 | 추천 대상 |
|------|------|------|----------|
| **[QUICK_START.md](QUICK_START.md)** | 4.6KB | 5분 안에 시작하기 | 모든 사용자 ⭐ |
| **[README.md](README.md)** | 9.9KB | 전체 개요 및 아키텍처 | 개발자, PM |

### 📖 상세 문서
| 문서 | 크기 | 설명 | 추천 대상 |
|------|------|------|----------|
| **[API.md](API.md)** | 9.1KB | API 명세서 | 프론트엔드, QA |
| **[IMPLEMENTATION.md](IMPLEMENTATION.md)** | 12KB | 구현 상세 및 설계 결정 | 백엔드 개발자 |
| **[GLOSSARY.md](GLOSSARY.md)** | 6.5KB | 용어 사전 | 모든 사용자 |

### 🔍 심화 주제
| 문서 | 크기 | 설명 | 추천 대상 |
|------|------|------|----------|
| **[CONSUMER_GROUP.md](CONSUMER_GROUP.md)** | 12KB | Consumer Group 개념 및 설계 | 아키텍트, 시니어 개발자 |

---

## 🎓 학습 경로

### 1️⃣ 초급 (처음 사용하는 경우)
```
QUICK_START.md → README.md (개요 부분) → API.md
```
**소요 시간**: 20분

**학습 내용**:
- 기본 사용법
- API 엔드포인트
- 간단한 테스트

---

### 2️⃣ 중급 (기능을 이해하고 싶은 경우)
```
README.md → GLOSSARY.md → IMPLEMENTATION.md
```
**소요 시간**: 40분

**학습 내용**:
- 아키텍처 이해
- 주요 개념 (metricValue, timeWindowUnit 등)
- 설계 결정 배경

---

### 3️⃣ 고급 (확장/개선하려는 경우)
```
IMPLEMENTATION.md → CONSUMER_GROUP.md → 코드 분석
```
**소요 시간**: 60분

**학습 내용**:
- 구현 패턴
- Consumer Group 필요성 판단
- 향후 개선 방향

---

## 🔍 목적별 문서 찾기

### "API를 사용하고 싶어요"
→ [QUICK_START.md](QUICK_START.md) ⭐

### "어떤 엔드포인트가 있나요?"
→ [API.md](API.md)

### "metricValue가 뭐죠?"
→ [GLOSSARY.md](GLOSSARY.md#metricvalue)

### "아키텍처가 궁금해요"
→ [README.md](README.md#아키텍처)

### "왜 이렇게 설계했나요?"
→ [IMPLEMENTATION.md](IMPLEMENTATION.md#설계-결정-사항)

### "Consumer Group이 뭐죠? 왜 없나요?"
→ [CONSUMER_GROUP.md](CONSUMER_GROUP.md) ⭐

### "실시간 스트리밍을 사용하고 싶어요"
→ [QUICK_START.md](QUICK_START.md#4단계-실시간-스트리밍-체험)

### "향후 개선 계획이 있나요?"
→ [README.md](README.md#향후-개선-사항)

---

## 📊 역할별 추천 문서

### 👨‍💻 백엔드 개발자
**필수**:
- [README.md](README.md) - 전체 구조 파악
- [IMPLEMENTATION.md](IMPLEMENTATION.md) - 구현 상세
- [CONSUMER_GROUP.md](CONSUMER_GROUP.md) - 설계 의도

**선택**:
- [GLOSSARY.md](GLOSSARY.md) - 용어 참고

**순서**: README → IMPLEMENTATION → CONSUMER_GROUP

---

### 💻 프론트엔드 개발자
**필수**:
- [QUICK_START.md](QUICK_START.md) - 빠른 시작
- [API.md](API.md) - API 명세
- [GLOSSARY.md](GLOSSARY.md) - 용어 이해

**선택**:
- [README.md](README.md#api-엔드포인트) - 전체 기능

**순서**: QUICK_START → API → GLOSSARY

---

### 🧪 QA / 테스터
**필수**:
- [QUICK_START.md](QUICK_START.md) - 테스트 시나리오
- [API.md](API.md) - API 스펙

**선택**:
- [README.md](README.md#사용-예시) - 사용 예시

**순서**: QUICK_START → API

---

### 📋 PM / 기획자
**필수**:
- [README.md](README.md#개요) - 전체 기능
- [QUICK_START.md](QUICK_START.md) - 데모

**선택**:
- [API.md](API.md) - 기능 상세

**순서**: README (개요) → QUICK_START

---

### 🏗️ 아키텍트
**필수**:
- [README.md](README.md) - 전체 구조
- [IMPLEMENTATION.md](IMPLEMENTATION.md) - 설계 결정
- [CONSUMER_GROUP.md](CONSUMER_GROUP.md) - 아키텍처 의사결정

**선택**:
- [GLOSSARY.md](GLOSSARY.md) - 용어 정리

**순서**: README → IMPLEMENTATION → CONSUMER_GROUP

---

## 🔗 외부 링크

### Redis 공식 문서
- [Redis Streams](https://redis.io/docs/data-types/streams/)
- [Redis Consumer Groups](https://redis.io/docs/data-types/streams/#consumer-groups)

### Spring 공식 문서
- [Spring WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Spring Data Redis Reactive](https://docs.spring.io/spring-data/redis/docs/current/reference/html/#redis:reactive)

### SSE 표준
- [Server-Sent Events (MDN)](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)
- [EventSource API](https://developer.mozilla.org/en-US/docs/Web/API/EventSource)

---

## 📝 문서 업데이트 내역

### 2025-11-16 (v1.0.0)
- ✅ 초기 문서 작성
- ✅ 6개 문서 완성
- ✅ Consumer Group 설명 추가
- ✅ metricValue 용어 상세화

---

## 💬 피드백

문서 개선 제안이나 질문이 있으시면:
- GitHub Issues
- Tech Lead에게 문의

---

## 🎯 Quick Links

| 질문 | 답변 |
|------|------|
| 어떻게 시작하나요? | [QUICK_START.md](QUICK_START.md) |
| API 명세는? | [API.md](API.md) |
| Consumer Group이 뭔가요? | [CONSUMER_GROUP.md](CONSUMER_GROUP.md) |
| metricValue가 뭔가요? | [GLOSSARY.md](GLOSSARY.md#metricvalue) |
| 왜 이렇게 만들었나요? | [IMPLEMENTATION.md](IMPLEMENTATION.md#설계-결정-사항) |
