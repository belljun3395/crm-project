# [ISSUE-002] 오픈 이슈(#185~#201) 실행 계획

- 우선순위: High
- 대상: Backend + Frontend + Docs
- 목표: 병렬 구현(worktree) + 직렬 머지(merge lane)로 #185~#201을 안정적으로 순차 해결

## 운영 전제
- 머지 방식: squash only
- Co-author: 사용하지 않음 (squash 메시지에 `Co-authored-by:` 미포함)
- CI: required checks 전부 통과가 머지 조건
- 커버리지: 수치 미달 자체는 블로커로 보지 않음

## 이슈 맵 (요약)

### 거버넌스/문서
- #200 기능 명세 자동화
- #201 KDoc/테스트 계약 ID 반영
- #193 문서 및 아키텍처 정합성

### 안정성/보안/데이터 기반
- #194 검색 보안 하드닝(SQL Injection 차단 + DSL)
- #195 DB 무결성 강화
- #196 전역 Idempotency-Key 표준화
- #190 거버넌스/RBAC/컴플라이언스
- #191 플랫폼 안정화/성능 개선

### 핵심 기능 확장
- #189 웹훅 신뢰성 및 전달 파이프라인
- #192 분석 엔진 확장
- #188 세그먼트/오디언스 CRUD
- #186 캠페인 + 세그먼트 타기팅 통합
- #185 멀티채널 액션 프로바이더
- #187 여정 자동화 엔진
- #198 개인정보 보존/삭제 정책
- #197 운영 콘솔 UI 커버리지 확장

## 권장 선행 관계
- #200 -> #201
- #194 -> #188
- #195 -> #196
- #196 -> #189
- #188 -> #186
- #185 -> #187
- #186 -> #187
- #189 -> #197
- #192 -> #197
- #190 -> #198

## 단계별 실행 계획

### Phase 0: 규칙/가드 선반영
- [x] #200 구현/검증 완료
- [ ] #201 구현/검증 완료 (PR #207 진행 중)
- [ ] `docs/issues/`에 적용 파일 추적 규칙 고정

머지 기준:
- 계약 가드/문서화 자동화가 실제 PR 플로우에서 동작해야 다음 phase 진입

### Phase 1: 기반 안정화(보안/무결성/멱등/권한)
- [ ] #194 검색 쿼리 보안 하드닝
- [ ] #195 DB 제약/인덱스 정비
- [ ] #196 전역 Idempotency-Key 표준화
- [ ] #190 RBAC/감사/컴플라이언스 베이스 구축

머지 기준:
- 쓰기 API 중복 처리, 검색 입력 검증, DB 제약 실패 케이스가 테스트로 재현 가능해야 함

### Phase 2: 운영 안정성/관측성 강화
- [ ] #189 웹훅 전달 신뢰성(재시도/백오프/DLQ)
- [ ] #191 플랫폼 병목/경합 개선
- [ ] #192 분석 엔진 확장(윈도우/메트릭 확장)

머지 기준:
- 이벤트 유입/웹훅 실패/재시도 경로의 오류 처리와 추적 경로가 일관적이어야 함

### Phase 3: 도메인 확장(세그먼트/타기팅/여정/멀티채널)
- [ ] #188 세그먼트/오디언스 CRUD
- [ ] #186 캠페인 + 세그먼트 연동
- [ ] #185 멀티채널 액션 프로바이더
- [ ] #187 여정 자동화 엔진

머지 기준:
- 세그먼트 기반 대상 선정과 액션 실행(중복 방지 포함)이 end-to-end로 검증되어야 함

### Phase 4: 운영 UI/개인정보/문서 마감
- [ ] #197 운영 콘솔 UI 확장(Campaign Dashboard/Webhook)
- [ ] #198 Retention/Masking/Right-to-delete
- [ ] #193 문서/아키텍처 정합성 최종 반영

머지 기준:
- 운영자가 UI/API 양쪽에서 동일 정책/상태를 확인 가능해야 함

## merge lane 운영 규칙 (이 계획에 적용)
- Ready PR은 1개만 유지한다.
- 나머지는 Draft로 병렬 개발하고, CI/봇 결과만 선확인한다.
- 현재 lane PR 머지 후 다음 PR을 Ready로 승격한다.

권장 라벨:
- `merge:ready` 현재 lane 대상
- `merge:next` 다음 승격 후보
- `merge:draft` 병렬 진행 중

## worktree 운영 템플릿

```bash
git fetch origin
git worktree add "../crm-project-wt-issue196" -b "feature/issue-196-idempotency-key" origin/main
```

```bash
git worktree remove "../crm-project-wt-issue196"
git branch -d "feature/issue-196-idempotency-key"
```

## PR 체크리스트 (공통)
- [ ] 이슈 범위 외 변경 없음
- [ ] required checks green
- [ ] 리뷰 봇 결과 수렴 후(권장 10~20분) 사람 리뷰 요청
- [ ] squash commit message 확인 (`Co-authored-by:` 없음)

## 리스크/완화
- 리스크: 여러 PR이 같은 모델/DTO를 동시 수정해 충돌 증가
  - 완화: 공통 계약 변경은 별도 선행 PR로 분리
- 리스크: stale base로 머지 직전 CI 재실패
  - 완화: 머지 직전 최신 main 반영 + 최종 SHA 기준 CI 재확인
- 리스크: 후기 phase(UI/개인정보)가 기반 기능 미완료 상태에서 재작업 발생
  - 완화: Phase 게이트 충족 전 후기 phase는 Draft 유지

## 완료 조건 (DoD)
- #185~#201이 계획된 phase 순서로 처리되고, 머지 후 main CI가 지속 green 상태를 유지
- 각 이슈 PR에 테스트/검증 근거와 정책(멱등/보안/권한/삭제)이 추적 가능
- 최종적으로 #193에 전체 구조/운영 문서 반영 완료

## 상세 구현 문서
- 상세 구현 블루프린트: `docs/roadmaps/ISSUES-185-201-IMPLEMENTATION-BLUEPRINT.md`

## 커뮤니케이션 로그
- 머지 지시 취소 반영 완료
- PR 코멘트로 진행 완료 사실 전달 완료 (`@belljun3395`)
- 코멘트 링크: `https://github.com/belljun3395/crm-project/pull/204#issuecomment-3918827378`
- #200 머지 완료: `https://github.com/belljun3395/crm-project/pull/206`
- #201 진행 PR 생성: `https://github.com/belljun3395/crm-project/pull/207`
