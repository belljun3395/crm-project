import React from 'react';
import { GuidePanel } from 'common/component';

interface FeatureSection {
  id: string;
  title: string;
  summary: string;
  features: string[];
  usage: string[];
  apis: string[];
  references: string[];
}

const commonRules: string[] = [
  '쓰기 API는 Idempotency-Key 규약을 사용해 중복 요청을 방지합니다.',
  '대부분 화면은 진입 시 자동 조회되며, 잘못된 입력은 프론트에서 먼저 검증합니다.',
  '시간은 브라우저 로컬 시간대로 렌더링되며 datetime-local 입력은 초 단위가 보정될 수 있습니다.'
];

const featureSections: FeatureSection[] = [
  {
    id: 'overview',
    title: 'Overview',
    summary: '전체 운영 현황을 KPI 카드와 최근 이력으로 확인합니다.',
    features: [
      'Total Users, Templates, Schedules, Histories, Webhooks, Segments, Journeys, Actions, Audit Logs 지표 제공',
      'Recent Users / Recent Action Dispatches 표 제공'
    ],
    usage: [
      '상단 카드로 이상 징후를 먼저 확인합니다.',
      '아래 최근 이력 표에서 상세 확인 후 해당 메뉴로 이동합니다.'
    ],
    apis: [
      'GET /users',
      'GET /users/count',
      'GET /emails/templates',
      'GET /emails/histories',
      'GET /emails/schedules/notifications/email',
      'GET /webhooks',
      'GET /segments',
      'GET /journeys',
      'GET /journeys/executions',
      'GET /actions/dispatch/histories',
      'GET /audit-logs'
    ],
    references: ['PR #227']
  },
  {
    id: 'campaign-dashboard',
    title: 'Campaign Dashboard',
    summary: '캠페인 집계와 실시간 이벤트 스트림(SSE)을 운영 화면에서 확인합니다.',
    features: [
      'Time Window(MINUTE/HOUR/DAY/WEEK/MONTH), 기간 필터, Summary 조회',
      'SSE 연결/해제/재연결 상태 표시',
      'Stream Length 및 최근 라이브 이벤트 표시'
    ],
    usage: [
      'Campaign ID 입력 후 Refresh로 집계/스트림 상태를 동기화합니다.',
      'Connect로 실시간 이벤트를 확인하고 필요 시 Clear/Disconnect를 사용합니다.'
    ],
    apis: [
      'GET /campaigns/{campaignId}/dashboard',
      'GET /campaigns/{campaignId}/dashboard/summary',
      'GET /campaigns/{campaignId}/dashboard/stream/status',
      'GET /campaigns/{campaignId}/dashboard/stream (SSE)'
    ],
    references: ['Issue #197', 'Issue #192', 'PR #214', 'PR #216', 'PR #217']
  },
  {
    id: 'webhooks',
    title: 'Webhooks',
    summary: '외부 연동 엔드포인트를 CRUD하고 전달 성공/실패를 추적합니다.',
    features: [
      'Webhook 생성/수정/삭제/목록 조회',
      'Delivery Logs 및 Dead Letters(DLQ) 조회',
      'URL 형식 및 이벤트 타입 입력 검증'
    ],
    usage: [
      'New Webhook에서 수신 URL과 이벤트 타입을 등록합니다.',
      'Logs 버튼으로 전달 로그와 DLQ를 확인합니다.'
    ],
    apis: [
      'POST /webhooks',
      'PUT /webhooks/{id}',
      'DELETE /webhooks/{id}',
      'GET /webhooks',
      'GET /webhooks/{id}/deliveries',
      'GET /webhooks/{id}/dead-letters'
    ],
    references: ['Issue #189', 'Issue #197', 'PR #214', 'PR #219', 'PR #220']
  },
  {
    id: 'users-events',
    title: 'Users & Events',
    summary: '고객 등록과 이벤트 적재/검색을 통해 타겟팅 기초 데이터를 관리합니다.',
    features: [
      'Users: externalId + userAttributes 등록/검색',
      'Events: eventName + where DSL 기반 서버 검색',
      'Event 생성 시 campaignName/externalId/properties 입력 가능'
    ],
    usage: [
      'Users에서 고객을 등록하고 검색으로 데이터 품질을 확인합니다.',
      'Events에서 where DSL로 검색 결과를 검증한 뒤 이벤트를 적재합니다.'
    ],
    apis: [
      'GET /users',
      'POST /users',
      'GET /users/count',
      'GET /events?eventName&where',
      'POST /events'
    ],
    references: ['Issue #194', 'PR #218']
  },
  {
    id: 'messaging',
    title: 'Templates / Histories / Schedules',
    summary: '메일 템플릿 관리, 발송 이력 조회, 예약 발송 스케줄을 운영합니다.',
    features: [
      'Templates: 템플릿 생성/삭제 및 변수 정의',
      'Email Histories: user/status/page/size 필터 조회',
      'Schedules: 템플릿 + 사용자 목록 + 만료시각 기반 예약/취소'
    ],
    usage: [
      'Templates에서 문안을 등록합니다.',
      'Schedules에서 대상 사용자와 만료시각을 설정해 예약합니다.',
      'Histories에서 발송 결과와 메시지 ID를 추적합니다.'
    ],
    apis: [
      'GET /emails/templates',
      'POST /emails/templates',
      'DELETE /emails/templates/{templateId}',
      'GET /emails/histories',
      'GET /emails/schedules/notifications/email',
      'POST /emails/schedules/notifications/email',
      'DELETE /emails/schedules/notifications/email/{scheduleId}'
    ],
    references: ['Issue #199', 'PR #202']
  },
  {
    id: 'segments-journeys-actions-audit',
    title: 'Segments / Journeys / Actions / Audit',
    summary: '타겟팅 그룹, 자동화 여정, 멀티채널 전송, 감사 추적을 통합 운영합니다.',
    features: [
      'Segments: 조건 JSON 기반 CRUD',
      'Journeys: Trigger + Steps JSON 기반 자동화 흐름 생성 및 실행 이력 조회',
      'Actions: EMAIL/SLACK/DISCORD 즉시 전송',
      'Audit Logs: 운영 작업 조회 필터(limit/action/resourceType/actorId)'
    ],
    usage: [
      'Segments 생성 후 Journeys trigger와 연계합니다.',
      'Actions로 즉시 발송 테스트 후 히스토리를 확인합니다.',
      'Audit Logs에서 변경 작업 추적을 점검합니다.'
    ],
    apis: [
      'GET /segments',
      'POST /segments',
      'PUT /segments/{id}',
      'DELETE /segments/{id}',
      'GET /journeys',
      'POST /journeys',
      'GET /journeys/executions',
      'GET /journeys/executions/{executionId}/histories',
      'POST /actions/dispatch',
      'GET /actions/dispatch/histories',
      'GET /audit-logs'
    ],
    references: ['Issue #188', 'Issue #187', 'Issue #185', 'Issue #190', 'PR #222', 'PR #224', 'PR #225', 'PR #221']
  }
];

const operationFlow: string[] = [
  '1) Users에서 고객 데이터 등록/검증',
  '2) Events에서 행동 데이터 적재 + 검색 DSL 확인',
  '3) Segments 생성 후 타겟 그룹 확정',
  '4) Templates 작성 후 Schedules 또는 Actions로 발송',
  '5) Journeys 실행 이력과 Campaign Dashboard 지표 확인',
  '6) Webhooks 전달 로그/DLQ + Audit Logs로 최종 운영 추적'
];

export const FeatureGuidePage: React.FC = () => {
  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-cyan-500/30 bg-cyan-500/10 p-5">
        <p className="text-xs uppercase tracking-[0.14em] text-cyan-200">Static Documentation</p>
        <h2 className="mt-1 text-2xl font-semibold text-white">CRM 기능 가이드</h2>
        <p className="mt-2 text-sm text-slate-200">
          이 페이지는 이슈/PR과 현재 구현 코드를 기준으로 정리한 운영 콘솔 기능 문서입니다. 상세 문서는
          <code className="ml-1 rounded bg-slate-900/70 px-2 py-0.5 text-xs">docs/CONSOLE_FEATURE_GUIDE.md</code>를 참고하세요.
        </p>
      </header>

      <GuidePanel
        description="운영자가 기능을 빠르게 이해하고 실수 없이 사용할 수 있도록 핵심 규칙을 먼저 정리했습니다."
        items={commonRules}
        note="근거 이슈: #185~#199 / #214~#225 / #227 / #228"
      />

      <section className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-5 backdrop-blur">
        <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-300">권장 운영 순서</h3>
        <div className="mt-3 grid gap-2">
          {operationFlow.map((step) => (
            <p key={step} className="rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-200">
              {step}
            </p>
          ))}
        </div>
      </section>

      <section className="space-y-4">
        {featureSections.map((section) => (
          <article key={section.id} className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-5 backdrop-blur">
            <div className="mb-4">
              <h3 className="text-xl font-semibold text-white">{section.title}</h3>
              <p className="mt-1 text-sm text-slate-300">{section.summary}</p>
            </div>

            <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
              <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-4">
                <p className="text-xs uppercase tracking-wide text-cyan-300">주요 기능</p>
                <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-slate-200">
                  {section.features.map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              </div>

              <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-4">
                <p className="text-xs uppercase tracking-wide text-emerald-300">사용 방법</p>
                <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-slate-200">
                  {section.usage.map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              </div>
            </div>

            <div className="mt-4 rounded-xl border border-slate-800 bg-slate-950/60 p-4">
              <p className="text-xs uppercase tracking-wide text-amber-300">연결 API</p>
              <div className="mt-2 flex flex-wrap gap-2">
                {section.apis.map((api) => (
                  <code key={api} className="rounded bg-slate-900 px-2 py-1 text-xs text-slate-200">
                    {api}
                  </code>
                ))}
              </div>
            </div>

            <div className="mt-4 flex flex-wrap gap-2">
              {section.references.map((ref) => (
                <span
                  key={ref}
                  className="rounded-full border border-slate-700 bg-slate-900/70 px-3 py-1 text-xs text-slate-300"
                >
                  {ref}
                </span>
              ))}
            </div>
          </article>
        ))}
      </section>
    </div>
  );
};
