# [ISSUE-001] 백엔드 기능 명세 관리 자동화

- 우선순위: High
- 대상: 백엔드
- 목표: 기능 명세(문서/계약/테스트)를 코드 변경과 함께 자동으로 유지

## 배경

- 기능 추가/변경 시 명세가 따로 관리되면서 동기화가 느슨해지는 문제가 있음
- UseCase/Domain은 핵심 계약이므로 사용자 관점의 계약은 코드 주석으로 고정하고, 동작 검증은 테스트로 보장하고자 함

## 제안

- UseCase/Domain에 사용자 계약 중심 KDoc을 의무화
- 테스트 명명/구조를 계약 ID 기준으로 정리
- OpenAPI 스펙을 생성-비교하여 변경 감지 실패 처리

## 작업 항목

- [x] 계약 KDoc 템플릿 정의 (`docs/spec-governance/CONTRACT-GUIDE.md`)
- [ ] UseCase/Domain 대상 KDoc 적용 범위 합의 (모든 신규 기능 기준)
- [ ] 테스트명 계약 매핑 규칙 반영 (예: `UC-USER-001`, `DM-USER-001`)
- [x] OpenAPI 자동 생성 스크립트 도입 (`scripts/refresh-openapi-docs.sh`)
- [x] CI에서 OpenAPI Drift Check Job 추가 (`.github/workflows/validation.yml`)
- [x] 검수 규칙 정의: 문서 미반영 동작 변경은 PR에서 Fail 처리

## 진행 현황

- #200 구현 및 머지 완료: CI 계약 가드 + OpenAPI 드리프트 검증 동작 확인
- #201 진행 중: 핵심 UseCase 4종 KDoc 계약 ID/테스트 계약 ID 1차 반영 PR 진행

## 완료 조건 (DoD)

- 신규/변경된 UseCase가 계약 KDoc 없이 머지되지 않음
- 핵심 흐름 테스트에서 계약 ID가 추적 가능
- OpenAPI 변경 시 `docs/openapi.json` 업데이트 누락 시 CI 경고 또는 실패 발생

## 리스크

- OpenAPI 생성 시 외부 인프라 의존성 이슈
- 계약 ID 관리 부재로 중복/누락 가능성
- 기존 테스트 네이밍 체계 변경 비용
