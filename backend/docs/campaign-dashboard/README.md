# Campaign Dashboard Backend Guide

## 목적

백엔드 관점에서 Campaign Dashboard의 현재 구현, 운영 포인트, 제약사항을 빠르게 확인하기 위한 문서입니다.

## 기능 범위

- 캠페인 이벤트 실시간 스트림 발행 (Redis Stream)
- 캠페인 대시보드 조회 API
- SSE 스트리밍 API
- 캠페인 요약/스트림 상태 API

## 데이터 경로

1. `POST /api/v1/events`
2. `PostEventUseCase`가 이벤트/캠페인 매핑 저장
3. `CampaignDashboardService.publishCampaignEvent()` 호출
4. Redis Stream 발행 + 메트릭 upsert

스트림 키

- `campaign:dashboard:stream:{campaignId}`

## 메트릭 저장

저장 테이블

- `campaign_dashboard_metrics` (MySQL)

현재 자동 집계

- `EVENT_COUNT`의 `HOUR`, `DAY`

스키마상 지원하지만 미완성인 항목

- `UNIQUE_USER_COUNT`, `TOTAL_USER_COUNT`
- `MINUTE`, `WEEK`, `MONTH`의 자동 집계 정책

## API 요약

- `GET /api/v1/campaigns/{campaignId}/dashboard`
- `GET /api/v1/campaigns/{campaignId}/dashboard/stream`
- `GET /api/v1/campaigns/{campaignId}/dashboard/summary`
- `GET /api/v1/campaigns/{campaignId}/dashboard/stream/status`

SSE 참고

- `lastEventId`(query) 또는 `Last-Event-ID`(header)
- 기본 타임아웃: `durationSeconds=3600`

## 운영 포인트

- Stream trim: 길이가 100 배수일 때 최대 10,000까지 정리
- 모니터링: `/stream/status`에서 stream length 확인
- 장애 시: SSE 에러 이벤트 후 `stream-end`로 종료

## 관련 이슈

- `#191` 플랫폼 안정화/성능 개선
- `#192` 분석 엔진 확장
- `#197` 운영 콘솔 UI 커버리지 확장
