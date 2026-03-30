# API Current State

## 문서 목적

이 문서는 CRM API의 **현재 지원 범위**와 **사용 판단 기준**만 간결히 제공합니다.

---

## 1) 지원 도메인 상태

| Domain | Status | 요약 |
|---|---|---|
| User | Implemented | 등록/목록/카운트 |
| Email | Implemented | 템플릿/발송/스케줄/히스토리 |
| Event | Implemented | 생성/조회/검색 (Event U/D 미지원) |
| Campaign Dashboard | Implemented | Campaign CRUD + dashboard/stream/analytics |
| Segment | Implemented | CRUD + 매칭 사용자 조회 |
| Journey | Implemented | CRUD + lifecycle + executions/histories |
| Webhook | Implemented | CRUD + deliveries/dead-letters/retry |
| Action | Implemented | dispatch + histories |
| Audit | Implemented | 감사 로그 조회 |

---

## 2) 공통 사용 규칙

- Base path: `/api/v1/**`
- 쓰기 경로 일부는 `Idempotency-Key`를 요구
- OpenAPI 계약상 `Authorization` 헤더 사용
- 응답은 `ApiResponse` 래퍼 패턴 중심

---

## 3) 언제 어떤 API를 선택하나

### A. 이벤트 기반 캠페인 분석

1. 캠페인 생성: `POST /api/v1/events/campaign` 또는 `POST /api/v1/campaigns`
2. 이벤트 적재: `POST /api/v1/events`
3. 집계 조회: `GET /api/v1/campaigns/{campaignId}/dashboard`
4. 실시간 모니터링: `GET /api/v1/campaigns/{campaignId}/dashboard/stream`

### B. 세그먼트 타기팅 + 여정 자동화

1. 세그먼트 생성/검증: `POST /api/v1/segments` → `GET /api/v1/segments/{id}/users`
2. 여정 생성: `POST /api/v1/journeys`
3. 실행 추적: `GET /api/v1/journeys/executions` / histories

### C. 외부 시스템 연동 운영

1. webhook 등록/수정: `POST|PUT /api/v1/webhooks...`
2. 실패 재처리: dead-letter 조회 + retry API
3. 감사 추적: `GET /api/v1/audit-logs`

---

## 4) Event CUD 범위 (명시)

- Event 리소스는 현재 **Create + Read** 중심입니다.
- 미지원:
  - `PUT /api/v1/events/{id}`
  - `DELETE /api/v1/events/{id}`
- Campaign 수정/삭제는 Campaign API에서 지원합니다.

---

## 5) 상세 엔드포인트 맵

도메인별 전체 엔드포인트는 아래 문서를 사용하세요.

- [API 도메인별 기능/엔드포인트](./API_DOMAIN_CAPABILITIES.md)
