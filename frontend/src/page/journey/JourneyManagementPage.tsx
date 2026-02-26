import React, { useMemo, useState } from 'react';
import { Button, GuidePanel, Input, Modal, Textarea } from 'common/component';
import { useJourneys } from 'shared/hook';
import type { CreateJourneyRequest, JourneyStepRequest } from 'shared/type';

interface JourneyFormState {
  name: string;
  triggerType: string;
  triggerEventName: string;
  triggerSegmentId: string;
  active: boolean;
  stepsJson: string;
}

const initialJourneyForm: JourneyFormState = {
  name: '',
  triggerType: 'EVENT',
  triggerEventName: '',
  triggerSegmentId: '',
  active: true,
  stepsJson: JSON.stringify(
    [
      {
        stepOrder: 1,
        stepType: 'ACTION',
        channel: 'EMAIL',
        destination: 'user@example.com',
        subject: 'Welcome',
        body: 'welcome-message',
        retryCount: 1
      }
    ],
    null,
    2
  )
};

const parseSteps = (raw: string): JourneyStepRequest[] | null => {
  try {
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed) || parsed.length === 0) {
      return null;
    }

    const normalized = parsed.map((step) => {
      const candidate = step as JourneyStepRequest;
      if (typeof candidate.stepOrder !== 'number' || !candidate.stepType) {
        throw new Error('Invalid step');
      }
      return candidate;
    });

    return normalized;
  } catch {
    return null;
  }
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
    fetchExecutionHistories
  } = useJourneys();

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [form, setForm] = useState<JourneyFormState>(initialJourneyForm);
  const [selectedExecutionId, setSelectedExecutionId] = useState<number | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const histories = useMemo(() => {
    if (!selectedExecutionId) {
      return [];
    }
    return executionHistories[selectedExecutionId] ?? [];
  }, [executionHistories, selectedExecutionId]);

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

    const parsedTriggerSegmentId = form.triggerSegmentId ? Number(form.triggerSegmentId) : undefined;
    if (
      parsedTriggerSegmentId !== undefined &&
      (!Number.isFinite(parsedTriggerSegmentId) || parsedTriggerSegmentId <= 0)
    ) {
      setFormError('Trigger Segment ID는 유효한 양수여야 합니다.');
      return;
    }

    setFormError(null);

    const payload: CreateJourneyRequest = {
      name: form.name.trim(),
      triggerType: form.triggerType,
      triggerEventName: form.triggerEventName.trim() || undefined,
      triggerSegmentId: parsedTriggerSegmentId,
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
          'History 버튼으로 각 단계 성공/실패 이력을 상세 확인할 수 있습니다.'
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
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Steps</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {loading ? (
                  <tr>
                    <td colSpan={4} className="px-4 py-8 text-center text-sm text-slate-300">
                      Loading journeys...
                    </td>
                  </tr>
                ) : journeys.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="px-4 py-8 text-center text-sm text-slate-300">
                      No journeys
                    </td>
                  </tr>
                ) : (
                  journeys.map((journey) => (
                    <tr key={journey.id} className="hover:bg-slate-800/30">
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">{journey.id}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm font-semibold text-white">{journey.name}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{journey.triggerType}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{journey.steps.length}</td>
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
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Created</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {histories.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-sm text-slate-300">
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
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">
                      {new Date(history.createdAt).toLocaleString()}
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
              onChange={(e) => setForm((prev) => ({ ...prev, triggerType: e.target.value }))}
              className="w-full rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-white focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500"
            >
              <option value="EVENT">EVENT</option>
              <option value="SEGMENT">SEGMENT</option>
              <option value="CONDITION">CONDITION</option>
            </select>
          </div>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <Input
              label="Trigger Event Name"
              value={form.triggerEventName}
              onChange={(e) => setForm((prev) => ({ ...prev, triggerEventName: e.target.value }))}
              placeholder="USER_SIGNUP"
            />
            <Input
              label="Trigger Segment ID"
              value={form.triggerSegmentId}
              onChange={(e) => setForm((prev) => ({ ...prev, triggerSegmentId: e.target.value }))}
              placeholder="1"
              type="number"
            />
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
    </div>
  );
};
