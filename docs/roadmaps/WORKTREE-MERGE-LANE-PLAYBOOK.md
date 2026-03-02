# Worktree + Merge Lane 운영 플레이북

## 목적
- 이슈는 병렬 개발(worktree)로 빠르게 진행하고, 머지는 단일 lane으로 안정적으로 처리한다.
- 머지 방식은 squash만 사용하고, CI 통과를 필수로 유지한다.

## 고정 원칙
- 개발 병렬, 머지 직렬: 여러 worktree에서 동시에 작업하되 merge-ready PR은 1개만 유지한다.
- Merge 전략: `Squash and merge`만 사용한다.
- 작성자 정책: squash 커밋 메시지에 `Co-authored-by:` 라인을 남기지 않는다.
- CI 정책: 필수 체크는 전부 green이어야 머지한다.
- 커버리지 정책: 커버리지 수치 미달 자체는 블로커로 보지 않는다.

## 브랜치/워크트리 규칙
- 브랜치 네이밍: `feature/issue-<번호>-<slug>`
- worktree 경로: `../crm-project-wt-issue<번호>`
- 1 이슈 = 1 브랜치 = 1 worktree
- worktree 내부에서 브랜치 전환 금지

```bash
git fetch origin
git worktree add "../crm-project-wt-issue200" -b "feature/issue-200-spec-automation" origin/main
```

## PR 상태 운영 규칙
- `Draft`: 구현/실험/CI 확인 단계
- `Ready for review`: merge lane 진입 상태 (동시에 1개만 허용)
- `Merged`: squash 후 즉시 worktree/로컬 브랜치 정리

## 리뷰 봇/CI 대기 규칙
- 마지막 push 이후 bot settle window를 둔다(권장 10~20분).
- settle window 동안 리뷰 봇 코멘트와 required checks 상태를 확인한다.
- 봇 결과 반영이 끝난 뒤 사람 리뷰를 요청한다.

## 머지 직전 체크리스트
- [ ] 대상 PR만 merge-ready 상태인가
- [ ] 최신 `origin/main` 반영 후 충돌 없음
- [ ] required checks 전체 green
- [ ] 대화(conversation) 미해결 항목 없음
- [ ] squash commit message에 `Co-authored-by:` 없음

## 머지 후 정리 체크리스트
- [ ] 원격 head branch 삭제 확인(auto-delete 또는 수동)
- [ ] 로컬 worktree 제거
- [ ] 로컬 브랜치 삭제
- [ ] 다음 Draft PR 1건만 Ready로 승격

```bash
git worktree remove "../crm-project-wt-issue200"
git branch -d "feature/issue-200-spec-automation"
```

## 충돌/리스크 대응
- stale base 방지: merge 직전 `origin/main` 재반영을 필수화한다.
- 교차 오염 방지: 커밋 직전 `git status`로 이슈 범위 파일만 포함됐는지 확인한다.
- flaky CI 대응: 재실행 1회 후 반복 실패 시 원인 수정이 끝날 때까지 lane 진입 금지.

## 이 저장소 기준 참고
- PR CI 트리거 브랜치: `main`, `dev` (`.github/workflows/validation.yml`)
- 현재 워크플로우의 커버리지 최소 임계값: 0 (`.github/workflows/validation.yml`)
