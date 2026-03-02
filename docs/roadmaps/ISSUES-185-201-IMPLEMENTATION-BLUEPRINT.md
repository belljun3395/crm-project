# Issues #185-#201 Detailed Implementation Blueprint

## 목적
- #185~#201을 바로 개발 가능한 수준으로 쪼개서 실행 순서, 파일 단위 작업, PR 단위를 명확히 정의한다.
- 병렬 worktree 개발 + 단일 merge lane 운영을 유지하면서 충돌과 재작업을 최소화한다.

## 고정 제약
- 머지 방식: squash only
- Co-author: 미사용
- 머지 조건: required checks 전체 통과
- 커버리지 수치: 블로커 아님

## 즉시 시작 규칙
1. Ready PR은 항상 1개만 유지한다.
2. 나머지 이슈는 Draft PR로 병렬 진행한다.
3. 공통 모델/스키마 변경은 먼저 작은 선행 PR로 분리한다.
4. 각 PR은 단일 이슈/단일 목적만 포함한다.

## 현재 진행 현황
- [x] #200 완료 (PR #206 머지)
- [ ] #201 진행 중 (PR #207, CI 대기)
- [ ] 다음 merge lane 후보: #194

## 단계/의존 관계

### Stage A: 거버넌스/가드
- #200 -> #201

### Stage B: 기반 안정화(보안/무결성/멱등/권한)
- #194 -> #188
- #195 -> #196
- #196 -> #189
- #190 -> #198

### Stage C: 코어 기능 확장
- #188 -> #186
- #185 -> #187
- #186 -> #187
- #199 (템플릿 변수 리팩터링)은 #185/#186/#187 선행 권장

### Stage D: 안정성/분석/UI
- #191, #192
- #189 -> #197
- #192 -> #197

### Stage E: 마감 문서화
- #193 (최종 동기화)

## 공통 PR 템플릿
- 브랜치: `feature/issue-<번호>-<slug>`
- Worktree: `../crm-project-wt-issue<번호>`
- PR 단위: 기본 2~3개
  - PR-1: 계약/인터페이스/스키마(필요 시)
  - PR-2: 핵심 동작
  - PR-3: UI/문서/정리

---

## Issue-by-Issue 상세 구현 계획

## #200 기능 명세 자동화 도입
목표: UseCase/Domain 계약 누락을 CI에서 잡고 OpenAPI 드리프트를 자동 검증한다.

작업 파일:
- `.github/workflows/validation.yml`
- `scripts/refresh-openapi-docs.sh`
- `scripts/check-backend-spec-governance.sh`
- `docs/spec-governance/CONTRACT-GUIDE.md`
- `docs/issues/ISSUE-001-backend-spec-governance.md`

구현 단계:
1. CI job에서 계약 가드와 OpenAPI 체크 순서를 고정한다.
2. OpenAPI 생성 스크립트 입력/출력 경로를 명시한다.
3. 계약 가드 스크립트에서 변경 파일 탐지 기준(`*UseCase.kt`, `domain/*.kt`)을 고정한다.
4. 실패 메시지를 리뷰 가능한 형태로 정리한다.

완료 기준:
- PR에서 계약 누락/스펙 드리프트가 재현 가능하게 검출된다.

## #201 UseCase/Domain 계약 KDoc 및 테스트 계약 ID 반영
목표: 핵심 UseCase/Domain과 테스트 이름을 계약 ID로 연결한다.

작업 파일(대표):
- `backend/src/main/kotlin/com/manage/crm/user/application/EnrollUserUseCase.kt`
- `backend/src/main/kotlin/com/manage/crm/event/application/PostEventUseCase.kt`
- `backend/src/main/kotlin/com/manage/crm/email/application/PostTemplateUseCase.kt`
- `backend/src/main/kotlin/com/manage/crm/webhook/application/PostWebhookUseCase.kt`
- 대응 테스트 파일(`backend/src/test/kotlin/com/manage/crm/**/*UseCaseTest.kt`)

구현 단계:
1. UseCase/Domain KDoc에 계약 ID, 입력/성공/실패/부작용을 기입한다.
2. 연계 테스트 제목/설명에 동일 계약 ID를 반영한다.
3. `scripts/check-backend-spec-governance.sh` 기준으로 누락 여부를 검증한다.

완료 기준:
- KDoc과 테스트에서 계약 ID 추적이 가능하다.

## #194 검색 보안 하드닝
목표: 동적 문자열 SQL 경로를 줄이고 필터 규약을 안전하게 만든다.

작업 파일:
- `backend/src/main/kotlin/com/manage/crm/event/domain/repository/EventRepositoryCustomImpl.kt`
- `backend/src/main/kotlin/com/manage/crm/user/domain/repository/UserRepositoryCustomImpl.kt`
- `backend/src/main/kotlin/com/manage/crm/event/application/SearchEventsUseCase.kt`
- `backend/src/main/kotlin/com/manage/crm/event/controller/EventController.kt`
- `backend/src/test/kotlin/com/manage/crm/event/domain/repository/EventRepositoryCustomTest.kt`

구현 단계:
1. `EventRepositoryCustomImpl`의 문자열 결합 where clause 생성부를 파라미터 기반 구조로 치환한다.
2. BETWEEN/연산자/타입 검증 규칙을 분리해 DSL 파서 검증 경로를 만든다.
3. `EventController`의 `where` 파라미터 파싱 실패 시 표준 에러를 반환한다.
4. SQL injection 시나리오 테스트를 추가한다.

완료 기준:
- quote/comment/union 계열 입력이 비정상 쿼리 실행으로 이어지지 않는다.

## #195 DB 스키마 무결성 강화
목표: NOT NULL/UNIQUE/인덱스를 DB 레벨에서 강제한다.

작업 파일:
- `backend/src/main/resources/db/migration/entity/*`
- `backend/src/main/kotlin/com/manage/crm/event/domain/Campaign.kt`
- `backend/src/main/kotlin/com/manage/crm/email/domain/repository/*`

구현 단계:
1. 신규 migration으로 제약 조건을 additive 방식으로 추가한다.
2. 기존 데이터 정리 SQL/검증 쿼리를 먼저 적용한다.
3. 서비스 코드에서 제약 위반 예외를 표준 에러로 변환한다.

완료 기준:
- 필수값 누락/중복 저장이 DB 레벨에서 차단된다.

## #196 전역 Idempotency-Key 표준화
목표: write API 전체의 중복 요청을 안정적으로 차단한다.

작업 파일:
- `backend/src/main/kotlin/com/manage/crm/email/controller/EmailController.kt`
- `backend/src/main/kotlin/com/manage/crm/event/controller/EventController.kt`
- `backend/src/main/kotlin/com/manage/crm/user/controller/UserController.kt`
- `backend/src/main/kotlin/com/manage/crm/webhook/controller/WebhookController.kt`
- (신규) `backend/src/main/kotlin/com/manage/crm/support/idempotency/*`

구현 단계:
1. `Idempotency-Key` 추출/검증 필터(또는 interceptor)를 도입한다.
2. 요청 본문 해시 + 키 기반 저장소(TTL) 정책을 구현한다.
3. 동일 키/동일 본문은 응답 재사용, 동일 키/상이 본문은 충돌 에러를 반환한다.
4. 주요 write endpoint에 정책을 연결한다.

완료 기준:
- 재시도 시 중복 데이터가 생성되지 않는다.

## #190 거버넌스/RBAC/컴플라이언스
목표: 권한 기반 접근 제어와 감사 추적 기반을 만든다.

작업 파일:
- `backend/src/main/kotlin/com/manage/crm/config/SpringDocConfig.kt`
- (신규) `backend/src/main/kotlin/com/manage/crm/config/security/SecurityConfig.kt`
- 각 controller(`UserController`, `EventController`, `EmailController`, `WebhookController`)
- (신규) `backend/src/main/kotlin/com/manage/crm/support/audit/*`

구현 단계:
1. 역할 모델과 권한 매핑을 정의한다.
2. 변경 API에 권한 검증을 적용한다.
3. 주요 액션에 감사 로그 적재 포인트를 추가한다.

완료 기준:
- 무권한 토큰으로 변경 API 접근이 차단된다.

## #199 템플릿 변수 체계 리팩터링
목표: `user_xxx/campaign_xxx` 문자열 체계를 `source + resolver`로 전환한다.

작업 파일:
- `backend/src/main/kotlin/com/manage/crm/email/domain/vo/Variable.kt`
- `backend/src/main/kotlin/com/manage/crm/email/domain/vo/Variables.kt`
- `backend/src/main/kotlin/com/manage/crm/email/domain/support/VariablesSupport.kt`
- `backend/src/main/kotlin/com/manage/crm/email/application/PostTemplateUseCase.kt`
- `backend/src/main/kotlin/com/manage/crm/email/application/SendNotificationEmailUseCase.kt`
- 테스트:
  - `backend/src/test/kotlin/com/manage/crm/email/application/PostTemplateUseCaseTest.kt`
  - `backend/src/test/kotlin/com/manage/crm/email/application/SendNotificationEmailUseCaseTest.kt`

구현 단계:
1. `VariableSource`(USER/CAMPAIGN) 도입 및 파서/포매터 분리.
2. 레거시 포맷(`user_xxx`) 입력은 표준 포맷(`user.xxx`)으로 정규화.
3. `VariableResolver` 인터페이스와 source별 resolver를 도입.
4. `PostTemplateUseCase`/`SendNotificationEmailUseCase`를 resolver 기반으로 전환.

완료 기준:
- 레거시 포맷 하위호환을 유지하면서 resolver 확장이 가능하다.

## #188 세그먼트/오디언스 CRUD
목표: 세그먼트 도메인과 CRUD/Preview API를 추가한다.

작업 파일:
- (신규) `backend/src/main/kotlin/com/manage/crm/segment/*`
- (신규) migration 파일(`backend/src/main/resources/db/migration/entity/*`)
- `backend/src/main/kotlin/com/manage/crm/event/domain/repository/EventRepositoryCustomImpl.kt`

구현 단계:
1. 세그먼트 엔티티/조건 DSL/리포지토리를 추가.
2. CRUD API와 preview API를 구현.
3. 조건식 검증(타입/연산자/범위) 로직을 분리.

완료 기준:
- 조건식으로 일관된 타겟 집합을 재현 가능하게 조회한다.

## #186 캠페인 + 세그먼트 타기팅 통합
목표: 캠페인/이벤트/메일 발송 경로에 `segmentId`를 통합한다.

작업 파일:
- `backend/src/main/kotlin/com/manage/crm/event/domain/Campaign.kt`
- `backend/src/main/kotlin/com/manage/crm/event/application/PostCampaignUseCase.kt`
- `backend/src/main/kotlin/com/manage/crm/event/application/PostEventUseCase.kt`
- `backend/src/main/kotlin/com/manage/crm/email/application/SendNotificationEmailUseCase.kt`

구현 단계:
1. 캠페인 도메인과 입력 DTO에 `segmentId`를 추가.
2. `segmentId`/`userIds` 우선순위 정책을 고정한다.
3. 세그먼트 기반 결과를 기존 직접 대상 결과와 분리 기록한다.

완료 기준:
- `segmentId`만으로 발송/실행이 가능하고 기존 경로 호환성이 유지된다.

## #185 멀티채널 액션 프로바이더
목표: Email 외 채널 확장을 위한 공통 action provider를 도입한다.

작업 파일:
- `backend/src/main/kotlin/com/manage/crm/email/application/service/MailServiceImpl.kt`
- `backend/src/main/kotlin/com/manage/crm/email/support/EmailEventPublisher.kt`
- `backend/src/main/kotlin/com/manage/crm/config/AwsClientConfig.kt`
- (신규) `backend/src/main/kotlin/com/manage/crm/action/provider/*`

구현 단계:
1. `ActionProvider` 인터페이스 정의.
2. Email provider를 인터페이스 구현으로 이전.
3. Slack/Discord provider MVP 추가(실패 응답 모델 통일).
4. 실행 결과를 캠페인/여정 로그와 연결.

완료 기준:
- 동일 액션이 Email/Slack/Discord 경로에서 공통 계약으로 실행된다.

## #187 여정 자동화 엔진
목표: Trigger -> Action 실행 가능한 journey 엔진을 추가한다.

작업 파일:
- (신규) `backend/src/main/kotlin/com/manage/crm/journey/*`
- `backend/src/main/kotlin/com/manage/crm/email/event/schedule/ScheduledEventListener.kt`
- `backend/src/main/kotlin/com/manage/crm/event/application/PostEventUseCase.kt`

구현 단계:
1. Journey/JourneyStep/JourneyExecution 모델 생성.
2. 이벤트 트리거와 조건 분기, delay/wait 노드 실행기 구현.
3. 중복 실행 방지 키(eventId/userId/journeyStep) 적용.

완료 기준:
- 이벤트 발생 시 최소 1개 액션이 실행되고 실행 상태를 조회 가능하다.

## #189 웹훅 신뢰성 및 전달 파이프라인
목표: 웹훅 전달 실패 복구/보안/과부하 보호를 추가한다.

작업 파일:
- `backend/src/main/kotlin/com/manage/crm/webhook/domain/Webhook.kt`
- `backend/src/main/kotlin/com/manage/crm/webhook/domain/WebhookEvents.kt`
- `backend/src/main/kotlin/com/manage/crm/webhook/domain/repository/WebhookRepositoryCustomImpl.kt`
- `backend/src/main/kotlin/com/manage/crm/webhook/infrastructure/WebClientWebhookClient.kt`
- (신규) `backend/src/main/kotlin/com/manage/crm/webhook/service/WebhookDeliveryService.kt`

구현 단계:
1. 비동기 dispatch + retry 워커 경로를 분리.
2. exponential backoff/최대 횟수/DLQ 정책을 구현.
3. HMAC + timestamp/nonce 검증을 도입.
4. 전달 히스토리/실패 로그 조회 API를 추가.

완료 기준:
- 영구 실패는 DLQ/실패 로그에 남고 재처리 경로가 존재한다.

## #191 플랫폼 안정화 및 성능 개선
목표: 캐시/파이프라인/동시성 병목을 줄인다.

작업 파일:
- `backend/src/main/kotlin/com/manage/crm/event/domain/cache/CampaignCacheManager.kt`
- `backend/src/main/kotlin/com/manage/crm/email/application/service/ScheduleTaskServiceImpl.kt`
- `backend/src/main/kotlin/com/manage/crm/infrastructure/scheduler/consumer/ScheduledTaskConsumer.kt`
- `backend/src/main/kotlin/com/manage/crm/event/service/CampaignDashboardService.kt`

구현 단계:
1. N+1 조회 경로를 배치 조회로 변경.
2. `runBlocking` 사용 경로를 점진 제거.
3. 고빈도 쓰기 경로의 배치/upsert 전략을 실험 후 적용.

완료 기준:
- 병목 지표(응답시간/락대기/재시도)가 기존 대비 개선된다.

## #192 분석 엔진 확장
목표: metric/window 계산 범위를 확장하고 API 응답을 보강한다.

작업 파일:
- `backend/src/main/kotlin/com/manage/crm/event/domain/CampaignDashboardMetrics.kt`
- `backend/src/main/kotlin/com/manage/crm/event/service/CampaignDashboardService.kt`
- `backend/src/main/kotlin/com/manage/crm/event/application/GetCampaignDashboardUseCase.kt`
- `backend/src/main/kotlin/com/manage/crm/event/controller/CampaignDashboardController.kt`
- `frontend/src/shared/api/crm/campaign/index.ts`

구현 단계:
1. `UNIQUE_USER_COUNT`, `TOTAL_USER_COUNT` 집계를 추가.
2. `MINUTE/WEEK/MONTH` 윈도우 계산/조회를 활성화.
3. summary API 구조를 확장하고 대시보드 소비 경로를 맞춘다.

완료 기준:
- 지정 metric/window 조합이 전부 조회 가능하다.

## #197 운영 콘솔 UI 커버리지 확장
목표: Campaign Dashboard/Webhook 관리 화면을 완성한다.

작업 파일:
- `frontend/src/page/dashboard/DashboardPage.tsx`
- `frontend/src/shared/api/crm/campaign/index.ts`
- `frontend/src/shared/api/crm/webhook/index.ts`
- (신규) `frontend/src/page/webhook/WebhookManagementPage.tsx`

구현 단계:
1. 대시보드 요약/윈도우 필터 UI를 구현.
2. SSE 연결/재연결/오류 상태 UI를 추가.
3. webhook CRUD 화면과 검증 UX를 추가.

완료 기준:
- 운영 콘솔에서 대시보드 조회와 webhook CRUD를 수행할 수 있다.

## #198 개인정보 보존/삭제 정책
목표: retention/masking/right-to-delete 정책을 제품 레벨로 반영한다.

작업 파일:
- `backend/src/main/kotlin/com/manage/crm/user/domain/User.kt`
- `backend/src/main/kotlin/com/manage/crm/user/domain/repository/UserRepository.kt`
- `backend/src/main/kotlin/com/manage/crm/email/domain/repository/EmailSendHistoryRepository.kt`
- (신규) `backend/src/main/kotlin/com/manage/crm/privacy/*`

구현 단계:
1. 테이블별 보존 기간과 삭제 정책(하드/소프트/익명화)을 명시.
2. 조회/로그 노출 값에 마스킹 적용.
3. 사용자 단위 삭제 요청 처리 플로우와 처리 이력 API 구현.

완료 기준:
- 삭제 요청 결과와 재처리 기준이 추적 가능하다.

## #193 문서 및 아키텍처 정합성
목표: 구현 결과를 문서에 동기화해 설계 추적성을 확보한다.

작업 파일:
- `docs/Domain-and-UseCase-Flows.md`
- `docs/CAMPAIGN_DASHBOARD_IMPLEMENTATION.md`
- `docs/issues/ISSUE-002-open-issues-execution-plan-185-201.md`
- `docs/roadmaps/WORKTREE-MERGE-LANE-PLAYBOOK.md`

구현 단계:
1. 실제 반영된 도메인/플로우/API를 문서에 갱신.
2. PR/이슈 링크를 문서 내 추적 항목에 반영.
3. 온보딩 관점에서 단계별 운영 가이드를 정리.

완료 기준:
- 코드/문서 간 불일치가 없는 1차 동기점을 달성한다.

---

## 개발 시작 순서(실행 큐)
1. #200
2. #201
3. #194
4. #195
5. #196
6. #190
7. #199
8. #188
9. #186
10. #185
11. #187
12. #189
13. #191
14. #192
15. #197
16. #198
17. #193

## 각 이슈 공통 완료 체크
- [ ] PR 범위가 단일 목적을 유지함
- [ ] required checks green
- [ ] 봇 결과 확인 후 리뷰 요청
- [ ] squash 메시지에서 `Co-authored-by:` 없음
- [ ] 머지 후 worktree/브랜치 정리 완료
