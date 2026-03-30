# Architecture Current State

## 문서 목적

이 문서는 현재 CRM 백엔드/프론트엔드 통합 구조를 유지보수 관점으로 요약합니다.

---

## 1) 시스템 개요

- Backend: Spring Boot 3 + Kotlin + WebFlux + R2DBC(MySQL)
- Frontend: React 운영 콘솔
- 데이터/인프라: MySQL, Redis Stream, Kafka, SMTP/SES

---

## 2) 레이어 책임

### Controller
- HTTP I/O와 DTO 매핑
- UseCase 호출 중심

### Application / UseCase
- 비즈니스 시나리오 단위 오케스트레이션

### Domain / Repository
- 엔티티/VO, 저장/조회 contract

### Stream/Queue Integration
- Redis Stream, queue publish, 외부 발송 채널 연동

---

## 3) 도메인 구현 맵

| Domain | Controller Entry | 핵심 특성 |
|---|---|---|
| User | `user/controller/UserController.kt` | 사용자 등록/조회 |
| Email | `email/controller/EmailController.kt` | 템플릿/발송/스케줄 |
| Event/Campaign | `event/controller/*` | 이벤트 적재 + 캠페인 대시보드 |
| Segment | `segment/controller/SegmentController.kt` | 세그먼트 CRUD + 대상 매칭 |
| Journey | `journey/controller/JourneyController.kt` | 여정 정의/라이프사이클 |
| Webhook | `webhook/controller/WebhookController.kt` | 외부 전송/재처리 + 감사 |
| Action | `action/controller/ActionController.kt` | 액션 디스패치 |
| Audit | `audit/controller/AuditLogController.kt` | 감사 로그 조회 |

---

## 4) Event 구현 스코프 연결

- Event 지원 범위의 공식 기준은 `API_CURRENT_STATE.md`와 `API_DOMAIN_CAPABILITIES.md`를 따릅니다.
- Event 자체는 Create/Read 중심이고, Campaign Update/Delete는 Campaign Controller에서 수행합니다.

---

## 5) 보안/런타임 가정

- API 계약상 `Authorization`, `Idempotency-Key` 정책 사용
- 일부 경로는 비동기 처리(큐/스트림) 포함
- 운영 시 최종 일관성(eventual consistency) 특성을 고려해야 함
