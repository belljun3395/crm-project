import React, { useMemo, useState } from 'react';
import { Button, GuidePanel, Input, Modal, Textarea } from 'common/component';
import { useJourneys, useSegments } from 'shared/hook';
import { COLLECTED_USER_FIELDS, JOURNEY_AUTO_VARIABLES } from 'shared/variableGuide';
import type { CreateJourneyRequest, Journey, JourneyStepRequest } from 'shared/type';

interface JourneyFormState {
  name: string;
  triggerType: string;
  triggerEventName: string;
  triggerSegmentId: string;
  triggerSegmentEvent: string;
  triggerSegmentWatchFields: string;
  triggerSegmentCountThreshold: string;
  active: boolean;
  stepsJson: string;
}

const initialJourneyForm: JourneyFormState = {
  name: '',
  triggerType: 'EVENT',
  triggerEventName: '',
  triggerSegmentId: '',
  triggerSegmentEvent: 'ENTER',
  triggerSegmentWatchFields: '',
  triggerSegmentCountThreshold: '',
  active: true,
  stepsJson: JSON.stringify(
    [
      {
        stepOrder: 1,
        stepType: 'ACTION',
        channel: 'EMAIL',
        destination: '{{user.email}}',
        subject: 'Welcome',
        body: 'welcome-message',
        retryCount: 1
      }
    ],
    null,
    2
  )
};

const triggerGuides: Record<
  string,
  {
    description: string;
    triggerExample: string;
    stepsExample: string;
  }
> = {
  EVENT: {
    description: '특정 이벤트 이름이 저장될 때 여정이 자동 실행됩니다.',
    triggerExample: `{
  "triggerType": "EVENT",
  "triggerEventName": "USER_SIGNUP"
}`,
    stepsExample: `[
  {
    "stepOrder": 1,
    "stepType": "ACTION",
    "channel": "EMAIL",
    "destination": "{{user.email}}",
    "subject": "Welcome",
    "body": "welcome {{user.name}}",
    "retryCount": 0
  }
]`
  },
  SEGMENT: {
    description:
      '세그먼트 기반 트리거 입력입니다. ENTER/EXIT/UPDATE/COUNT_REACHED/COUNT_DROPPED를 지원합니다.',
    triggerExample: `{
  "triggerType": "SEGMENT",
  "triggerSegmentId": 2,
  "triggerSegmentEvent": "ENTER"
}`,
    stepsExample: `[
  {
    "stepOrder": 1,
    "stepType": "ACTION",
    "channel": "EMAIL",
    "destination": "{{user.email}}",
    "subject": "Segment trigger",
    "body": "segment run",
    "retryCount": 0
  }
]`
  },
  CONDITION: {
    description:
      '조건 기반 트리거 입력입니다. 조건식이 만족되면 이벤트 유입 시 여정이 자동 실행됩니다.',
    triggerExample: `{
  "triggerType": "CONDITION",
  "triggerEventName": "event.plan==\\"PRO\\""
}`,
    stepsExample: `[
  {
    "stepOrder": 1,
    "stepType": "BRANCH",
    "conditionExpression": "event.plan==\\"PRO\\""
  },
  {
    "stepOrder": 2,
    "stepType": "ACTION",
    "channel": "EMAIL",
    "destination": "{{user.email}}",
    "body": "branch matched",
    "retryCount": 0
  }
]`
  }
};

const parseSteps = (raw: string): JourneyStepRequest[] | null => {
  try {
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed) || parsed.length === 0) {
      return null;
    }

    const normalized = parsed.map((step) => {
      if (typeof step !== 'object' || step === null) {
        throw new Error('Invalid step: step must be an object');
      }

      const candidate = step as JourneyStepRequest;
      if (
        !Number.isFinite(candidate.stepOrder) ||
        candidate.stepOrder <= 0 ||
        typeof candidate.stepType !== 'string' ||
        candidate.stepType.trim().length === 0
      ) {
        throw new Error('Invalid step: missing or invalid stepOrder/stepType');
      }

      return {
        ...candidate,
        stepType: candidate.stepType.trim()
      };
    });

    return normalized;
  } catch {
    return null;
  }
};

const formatDateTime = (value?: string): string => {
  if (!value) {
    return '-';
  }

  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? '-' : parsed.toLocaleString();
};

export const JourneyManagementPage: React.FC = () => {
  const {
    journeys,
    executions,
    executionHistories,
    loading,
    saving,
    error,
    createJourney,
    pauseJourney,
    resumeJourney,
    fetchExecutionHistories
  } = useJourneys();
  const { segments } = useSegments();

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [form, setForm] = useState<JourneyFormState>(initialJourneyForm);
  const [selectedExecutionId, setSelectedExecutionId] = useState<number | null>(null);
  const [selectedJourney, setSelectedJourney] = useState<Journey | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const histories = useMemo(() => {
    if (!selectedExecutionId) {
      return [];
    }
    return executionHistories[selectedExecutionId] ?? [];
  }, [executionHistories, selectedExecutionId]);
  const activeSegments = useMemo(() => {
    return segments.filter((segment) => segment.active);
  }, [segments]);
  const selectedTriggerGuide = triggerGuides[form.triggerType] ?? triggerGuides.EVENT;

  const handleCreate = async () => {
    if (!form.name.trim()) {
      setFormError('여정 이름은 필수입니다.');
      return;
    }

    const parsedSteps = parseSteps(form.stepsJson);
    if (!parsedSteps) {
      setFormError('steps JSON 형식을 확인해주세요.');
      return;
    }

    const triggerEventName = form.triggerEventName.trim();
    const parsedTriggerSegmentId =
      form.triggerType === 'SEGMENT' && form.triggerSegmentId ? Number(form.triggerSegmentId) : undefined;
    const triggerSegmentEvent = form.triggerType === 'SEGMENT' ? form.triggerSegmentEvent.trim() : undefined;
    const triggerSegmentWatchFields = form.triggerSegmentWatchFields
      .split(',')
      .map((field) => field.trim())
      .filter((field) => field.length > 0);
    const triggerSegmentCountThreshold =
      form.triggerSegmentCountThreshold.trim().length > 0
        ? Number(form.triggerSegmentCountThreshold.trim())
        : undefined;

    if ((form.triggerType === 'EVENT' || form.triggerType === 'CONDITION') && !triggerEventName) {
      setFormError(`${form.triggerType} 트리거는 Trigger Condition/Event Expression이 필수입니다.`);
      return;
    }

    if (form.triggerType === 'SEGMENT') {
      if (!form.triggerSegmentId) {
        setFormError('SEGMENT 트리거는 Trigger Segment가 필수입니다.');
        return;
      }
      if (
        parsedTriggerSegmentId === undefined ||
        !Number.isFinite(parsedTriggerSegmentId) ||
        parsedTriggerSegmentId <= 0
      ) {
        setFormError('Trigger Segment는 유효한 양수여야 합니다.');
        return;
      }
      if (!triggerSegmentEvent) {
        setFormError('SEGMENT 트리거는 Segment Trigger Event가 필수입니다.');
        return;
      }
      if (triggerSegmentEvent === 'UPDATE' && triggerSegmentWatchFields.length === 0) {
        setFormError('SEGMENT UPDATE 트리거는 Watch Fields가 1개 이상 필요합니다.');
        return;
      }
      if (triggerSegmentEvent === 'COUNT_REACHED' || triggerSegmentEvent === 'COUNT_DROPPED') {
        if (
          triggerSegmentCountThreshold === undefined ||
          !Number.isFinite(triggerSegmentCountThreshold) ||
          triggerSegmentCountThreshold <= 0
        ) {
          setFormError('COUNT 트리거는 양수 임계값(Count Threshold)이 필요합니다.');
          return;
        }
      }
    }

    setFormError(null);

    const payload: CreateJourneyRequest = {
      name: form.name.trim(),
      triggerType: form.triggerType,
      triggerEventName: (form.triggerType === 'EVENT' || form.triggerType === 'CONDITION')
        ? triggerEventName
        : undefined,
      triggerSegmentId: form.triggerType === 'SEGMENT' ? parsedTriggerSegmentId : undefined,
      triggerSegmentEvent: form.triggerType === 'SEGMENT' ? triggerSegmentEvent : undefined,
      triggerSegmentWatchFields:
        form.triggerType === 'SEGMENT' && triggerSegmentEvent === 'UPDATE' ? triggerSegmentWatchFields : undefined,
      triggerSegmentCountThreshold:
        form.triggerType === 'SEGMENT' &&
        (triggerSegmentEvent === 'COUNT_REACHED' || triggerSegmentEvent === 'COUNT_DROPPED')
          ? triggerSegmentCountThreshold
          : undefined,
      active: form.active,
      steps: parsedSteps
    };

    const success = await createJourney(payload);
    if (!success) {
      return;
    }

    setForm(initialJourneyForm);
    setFormError(null);
    setIsCreateOpen(false);
  };

  const handleLoadHistory = async (executionId: number) => {
    setSelectedExecutionId(executionId);
    await fetchExecutionHistories(executionId);
  };

  const openJourneyDetail = (journey: Journey) => {
    setSelectedJourney(journey);
    setIsDetailOpen(true);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-white">Journeys</h2>
          <p className="text-sm text-slate-300">자동화 여정과 실행 상태를 관리합니다.</p>
        </div>
        <Button onClick={() => setIsCreateOpen(true)}>새 여정</Button>
      </div>

      {error && (
        <div className="rounded-xl border border-rose-700/60 bg-rose-900/20 p-3 text-sm text-rose-100">{error}</div>
      )}

      <GuidePanel
        description="고객 행동에 따라 자동 메시지 흐름(여정)을 만드는 화면입니다."
        items={[
          '새 여정에서 Trigger를 정하고 Steps JSON으로 실행 단계를 정의합니다.',
          '생성 후 Execution List에서 실제 실행 상태를 확인합니다.',
          'Journey 행 클릭으로 상세 설정을 확인하고, History 버튼으로 단계 이력을 확인합니다.'
        ]}
        note="Trigger와 Steps 조건이 맞아야 실행이 시작됩니다."
      />

      <section className="grid grid-cols-1 gap-5 xl:grid-cols-2">
        <div className="overflow-hidden rounded-2xl border border-slate-800/80 bg-slate-900/60 backdrop-blur">
          <div className="border-b border-slate-800 px-4 py-3">
            <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-300">Journey List</h3>
          </div>
          <div className="max-h-[420px] overflow-auto">
            <table className="min-w-full divide-y divide-slate-800">
              <thead className="bg-slate-800/60">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">ID</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Name</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Trigger</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Status</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Version</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Steps</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {loading ? (
                  <tr>
                    <td colSpan={7} className="px-4 py-8 text-center text-sm text-slate-300">
                      Loading journeys...
                    </td>
                  </tr>
                ) : journeys.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-4 py-8 text-center text-sm text-slate-300">
                      No journeys
                    </td>
                  </tr>
                ) : (
                  journeys.map((journey) => (
                    <tr
                      key={journey.id}
                      className="cursor-pointer hover:bg-slate-800/30"
                      onClick={() => openJourneyDetail(journey)}
                    >
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">{journey.id}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm font-semibold text-white">{journey.name}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">
                        {journey.triggerType === 'SEGMENT'
                          ? `${journey.triggerType}:${journey.triggerSegmentEvent ?? '-'}`
                          : journey.triggerType}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{journey.lifecycleStatus}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{journey.version}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{journey.steps.length}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm">
                        {journey.lifecycleStatus === 'ACTIVE' ? (
                          <Button
                            size="sm"
                            variant="secondary"
                            loading={saving}
                            onClick={(event) => {
                              event.stopPropagation();
                              void pauseJourney(journey.id);
                            }}
                          >
                            Pause
                          </Button>
                        ) : (
                          <Button
                            size="sm"
                            variant="secondary"
                            loading={saving}
                            onClick={(event) => {
                              event.stopPropagation();
                              void resumeJourney(journey.id);
                            }}
                            disabled={journey.lifecycleStatus === 'ARCHIVED'}
                          >
                            Resume
                          </Button>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        <div className="overflow-hidden rounded-2xl border border-slate-800/80 bg-slate-900/60 backdrop-blur">
          <div className="border-b border-slate-800 px-4 py-3">
            <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-300">Execution List</h3>
          </div>
          <div className="max-h-[420px] overflow-auto">
            <table className="min-w-full divide-y divide-slate-800">
              <thead className="bg-slate-800/60">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">ID</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Journey</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Status</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {loading ? (
                  <tr>
                    <td colSpan={4} className="px-4 py-8 text-center text-sm text-slate-300">
                      Loading executions...
                    </td>
                  </tr>
                ) : executions.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="px-4 py-8 text-center text-sm text-slate-300">
                      No executions
                    </td>
                  </tr>
                ) : (
                  executions.map((execution) => (
                    <tr key={execution.id} className="hover:bg-slate-800/30">
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">{execution.id}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{execution.journeyId}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-white">{execution.status}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm">
                        <Button size="sm" variant="secondary" onClick={() => handleLoadHistory(execution.id)}>
                          History
                        </Button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <section className="overflow-hidden rounded-2xl border border-slate-800/80 bg-slate-900/60 backdrop-blur">
        <div className="border-b border-slate-800 px-4 py-3">
          <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-300">
            Execution Histories {selectedExecutionId ? `#${selectedExecutionId}` : ''}
          </h3>
        </div>
        <div className="max-h-[300px] overflow-auto">
          <table className="min-w-full divide-y divide-slate-800">
            <thead className="bg-slate-800/60">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">ID</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Step</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Status</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Attempt</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Message</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">
                  Idempotency Key
                </th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Created</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {histories.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-sm text-slate-300">
                    선택된 실행 이력이 없습니다.
                  </td>
                </tr>
              ) : (
                histories.map((history) => (
                  <tr key={history.id} className="hover:bg-slate-800/30">
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">{history.id}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{history.journeyStepId}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-white">{history.status}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{history.attempt}</td>
                    <td className="max-w-[360px] px-4 py-3 text-sm text-slate-300">{history.message || '-'}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{history.idempotencyKey || '-'}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">
                      {formatDateTime(history.createdAt)}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Create Journey" size="xl">
        <div className="space-y-4">
          {(formError || error) && (
            <div className="rounded-xl border border-rose-700/60 bg-rose-900/20 p-3 text-sm text-rose-100">
              {formError || error}
            </div>
          )}

          <Input
            label="Journey Name"
            value={form.name}
            onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
            placeholder="Welcome Journey"
            required
          />

          <div>
            <label className="mb-2 block text-sm font-medium text-slate-200">Trigger Type</label>
            <select
              value={form.triggerType}
              onChange={(e) => {
                const nextType = e.target.value;
                setForm((prev) => ({
                  ...prev,
                  triggerType: nextType,
                  triggerEventName: (nextType === 'EVENT' || nextType === 'CONDITION') ? prev.triggerEventName : '',
                  triggerSegmentId: nextType === 'SEGMENT' ? prev.triggerSegmentId : '',
                  triggerSegmentEvent: nextType === 'SEGMENT' ? prev.triggerSegmentEvent : 'ENTER',
                  triggerSegmentWatchFields: nextType === 'SEGMENT' ? prev.triggerSegmentWatchFields : '',
                  triggerSegmentCountThreshold: nextType === 'SEGMENT' ? prev.triggerSegmentCountThreshold : ''
                }));
              }}
              className="w-full rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-white focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500"
            >
              <option value="EVENT">EVENT</option>
              <option value="SEGMENT">SEGMENT</option>
              <option value="CONDITION">CONDITION</option>
            </select>
          </div>

          {(form.triggerType === 'EVENT' || form.triggerType === 'CONDITION') && (
            <Input
              label={form.triggerType === 'EVENT' ? 'Trigger Event Name' : 'Trigger Condition Expression'}
              value={form.triggerEventName}
              onChange={(e) => setForm((prev) => ({ ...prev, triggerEventName: e.target.value }))}
              placeholder={form.triggerType === 'EVENT' ? 'USER_SIGNUP' : 'event.plan=="PRO"'}
              required
            />
          )}

          {form.triggerType === 'SEGMENT' && (
            <div className="space-y-3">
              <div>
                <label className="mb-2 block text-sm font-medium text-slate-200">Trigger Segment</label>
                <select
                  value={form.triggerSegmentId}
                  onChange={(e) => setForm((prev) => ({ ...prev, triggerSegmentId: e.target.value }))}
                  className="w-full rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-white focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500"
                >
                  <option value="">세그먼트 선택</option>
                  {activeSegments.map((segment) => (
                    <option key={segment.id} value={segment.id}>
                      #{segment.id} - {segment.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="mb-2 block text-sm font-medium text-slate-200">Segment Trigger Event</label>
                <select
                  value={form.triggerSegmentEvent}
                  onChange={(e) => setForm((prev) => ({ ...prev, triggerSegmentEvent: e.target.value }))}
                  className="w-full rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-white focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500"
                >
                  <option value="ENTER">ENTER</option>
                  <option value="EXIT">EXIT</option>
                  <option value="UPDATE">UPDATE</option>
                  <option value="COUNT_REACHED">COUNT_REACHED</option>
                  <option value="COUNT_DROPPED">COUNT_DROPPED</option>
                </select>
              </div>

              {form.triggerSegmentEvent === 'UPDATE' && (
                <Input
                  label="Watch Fields (comma-separated)"
                  value={form.triggerSegmentWatchFields}
                  onChange={(e) => setForm((prev) => ({ ...prev, triggerSegmentWatchFields: e.target.value }))}
                  placeholder="user.email, user.name, user.plan"
                />
              )}

              {(form.triggerSegmentEvent === 'COUNT_REACHED' || form.triggerSegmentEvent === 'COUNT_DROPPED') && (
                <Input
                  label="Count Threshold"
                  type="number"
                  value={form.triggerSegmentCountThreshold}
                  onChange={(e) => setForm((prev) => ({ ...prev, triggerSegmentCountThreshold: e.target.value }))}
                  placeholder="100"
                />
              )}
            </div>
          )}

          {form.triggerType === 'CONDITION' && (
            <div className="rounded-xl border border-amber-700/50 bg-amber-900/20 p-3 text-sm text-amber-100">
              CONDITION 트리거는 Trigger Condition Expression을 기준으로 이벤트 유입 시 자동 실행됩니다.
            </div>
          )}

          <div className="rounded-xl border border-slate-700/70 bg-slate-900/60 p-3">
            <p className="text-sm font-semibold text-slate-100">타입별 입력 가이드</p>
            <p className="mt-1 text-sm text-slate-300">{selectedTriggerGuide.description}</p>
            <div className="mt-3 grid grid-cols-1 gap-3">
              <div>
                <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-slate-400">Trigger Example</p>
                <pre className="max-h-44 overflow-y-auto overflow-x-hidden whitespace-pre-wrap break-words rounded-lg border border-slate-700 bg-slate-950/60 p-3 font-mono text-xs leading-5 text-slate-200">
                  {selectedTriggerGuide.triggerExample}
                </pre>
              </div>
              <div>
                <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-slate-400">Steps Example</p>
                <pre className="max-h-56 overflow-y-auto overflow-x-hidden whitespace-pre-wrap break-words rounded-lg border border-slate-700 bg-slate-950/60 p-3 font-mono text-xs leading-5 text-slate-200">
                  {selectedTriggerGuide.stepsExample}
                </pre>
              </div>
            </div>

            <div className="mt-3 grid grid-cols-1 gap-3 lg:grid-cols-2">
              <div className="rounded-lg border border-slate-700/80 bg-slate-950/50 p-3">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-300">Common Collected Fields</p>
                <ul className="mt-2 space-y-1 text-xs text-slate-300">
                  {COLLECTED_USER_FIELDS.map((field) => (
                    <li key={field.key}>
                      <span className="font-mono text-slate-200">{field.key}</span>
                      {' '}({field.required ? '필수' : '선택'}) - {field.description}
                    </li>
                  ))}
                </ul>
              </div>
              <div className="rounded-lg border border-slate-700/80 bg-slate-950/50 p-3">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-300">Journey Auto Variables</p>
                <ul className="mt-2 space-y-1 text-xs text-slate-300">
                  {JOURNEY_AUTO_VARIABLES.map((item) => (
                    <li key={item.key}>
                      <span className="font-mono text-slate-200">{item.key}</span> - {item.description}
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </div>

          <Textarea
            label="Steps JSON"
            value={form.stepsJson}
            onChange={(e) => setForm((prev) => ({ ...prev, stepsJson: e.target.value }))}
            rows={10}
            required
          />

          <label className="flex items-center gap-2 text-sm text-slate-300">
            <input
              type="checkbox"
              checked={form.active}
              onChange={(e) => setForm((prev) => ({ ...prev, active: e.target.checked }))}
              className="h-4 w-4 rounded border-slate-600 bg-slate-800"
            />
            Active
          </label>

          <div className="grid grid-cols-2 gap-3 pt-2">
            <Button onClick={handleCreate} loading={saving}>
              생성
            </Button>
            <Button variant="secondary" onClick={() => setIsCreateOpen(false)}>
              취소
            </Button>
          </div>
        </div>
      </Modal>

      <Modal
        isOpen={isDetailOpen}
        onClose={() => setIsDetailOpen(false)}
        title={selectedJourney ? `Journey #${selectedJourney.id}` : 'Journey Detail'}
        size="xl"
      >
        {selectedJourney ? (
          <div className="space-y-4">
            <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
              <div className="rounded-lg border border-slate-700/80 bg-slate-900/60 p-3">
                <p className="text-xs uppercase tracking-wide text-slate-400">Name</p>
                <p className="mt-1 text-sm text-white">{selectedJourney.name}</p>
              </div>
              <div className="rounded-lg border border-slate-700/80 bg-slate-900/60 p-3">
                <p className="text-xs uppercase tracking-wide text-slate-400">Trigger Type</p>
                <p className="mt-1 text-sm text-white">{selectedJourney.triggerType}</p>
              </div>
              <div className="rounded-lg border border-slate-700/80 bg-slate-900/60 p-3">
                <p className="text-xs uppercase tracking-wide text-slate-400">Trigger Event Name</p>
                <p className="mt-1 text-sm text-white">{selectedJourney.triggerEventName || '-'}</p>
              </div>
              <div className="rounded-lg border border-slate-700/80 bg-slate-900/60 p-3">
                <p className="text-xs uppercase tracking-wide text-slate-400">Trigger Segment ID</p>
                <p className="mt-1 text-sm text-white">{selectedJourney.triggerSegmentId ?? '-'}</p>
              </div>
              <div className="rounded-lg border border-slate-700/80 bg-slate-900/60 p-3">
                <p className="text-xs uppercase tracking-wide text-slate-400">Segment Trigger Event</p>
                <p className="mt-1 text-sm text-white">{selectedJourney.triggerSegmentEvent ?? '-'}</p>
              </div>
              <div className="rounded-lg border border-slate-700/80 bg-slate-900/60 p-3 md:col-span-2">
                <p className="text-xs uppercase tracking-wide text-slate-400">Segment Watch Fields</p>
                <p className="mt-1 text-sm text-white">
                  {selectedJourney.triggerSegmentWatchFields?.length
                    ? selectedJourney.triggerSegmentWatchFields.join(', ')
                    : '-'}
                </p>
              </div>
              <div className="rounded-lg border border-slate-700/80 bg-slate-900/60 p-3">
                <p className="text-xs uppercase tracking-wide text-slate-400">Segment Count Threshold</p>
                <p className="mt-1 text-sm text-white">{selectedJourney.triggerSegmentCountThreshold ?? '-'}</p>
              </div>
              <div className="rounded-lg border border-slate-700/80 bg-slate-900/60 p-3">
                <p className="text-xs uppercase tracking-wide text-slate-400">Active</p>
                <p className="mt-1 text-sm text-white">{selectedJourney.active ? 'true' : 'false'}</p>
              </div>
              <div className="rounded-lg border border-slate-700/80 bg-slate-900/60 p-3">
                <p className="text-xs uppercase tracking-wide text-slate-400">Lifecycle Status</p>
                <p className="mt-1 text-sm text-white">{selectedJourney.lifecycleStatus}</p>
              </div>
              <div className="rounded-lg border border-slate-700/80 bg-slate-900/60 p-3">
                <p className="text-xs uppercase tracking-wide text-slate-400">Version</p>
                <p className="mt-1 text-sm text-white">{selectedJourney.version}</p>
              </div>
              <div className="rounded-lg border border-slate-700/80 bg-slate-900/60 p-3">
                <p className="text-xs uppercase tracking-wide text-slate-400">Created At</p>
                <p className="mt-1 text-sm text-white">{formatDateTime(selectedJourney.createdAt)}</p>
              </div>
            </div>

            <div className="rounded-xl border border-slate-700/80 bg-slate-900/60">
              <div className="border-b border-slate-700 px-4 py-3">
                <p className="text-sm font-semibold text-slate-100">Steps</p>
              </div>
              <div className="max-h-[280px] overflow-auto px-4 py-3">
                <table className="min-w-full divide-y divide-slate-800">
                  <thead className="bg-slate-800/60">
                    <tr>
                      <th className="px-3 py-2 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">
                        Order
                      </th>
                      <th className="px-3 py-2 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">
                        Type
                      </th>
                      <th className="px-3 py-2 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">
                        Channel
                      </th>
                      <th className="px-3 py-2 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">
                        Destination
                      </th>
                      <th className="px-3 py-2 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">
                        Retry
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {selectedJourney.steps.map((step) => (
                      <tr key={step.id}>
                        <td className="whitespace-nowrap px-3 py-2 text-sm text-slate-300">{step.stepOrder}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-sm text-slate-300">{step.stepType}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-sm text-slate-300">{step.channel || '-'}</td>
                        <td className="px-3 py-2 text-sm text-slate-300">{step.destination || '-'}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-sm text-slate-300">{step.retryCount ?? 0}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        ) : null}
      </Modal>
    </div>
  );
};

export default JourneyManagementPage;
