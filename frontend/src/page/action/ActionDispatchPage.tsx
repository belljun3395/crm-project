import React, { useState } from 'react';
import { Button, GuidePanel, Input, Textarea } from 'common/component';
import { useActions } from 'shared/hook';
import type { ActionDispatchRequest } from 'shared/type';

interface DispatchFormState {
  channel: string;
  destination: string;
  subject: string;
  body: string;
  variablesJson: string;
  campaignId: string;
  journeyExecutionId: string;
}

const initialForm: DispatchFormState = {
  channel: 'EMAIL',
  destination: '',
  subject: '',
  body: '',
  variablesJson: '{}',
  campaignId: '',
  journeyExecutionId: ''
};

const parseVariables = (raw: string): Record<string, string> | null => {
  try {
    const parsed = JSON.parse(raw) as unknown;
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
      return null;
    }

    return Object.entries(parsed as Record<string, unknown>).reduce<Record<string, string>>((acc, [key, value]) => {
      acc[key] = String(value);
      return acc;
    }, {});
  } catch {
    return null;
  }
};

export const ActionDispatchPage: React.FC = () => {
  const { histories, loading, dispatching, error, dispatchAction } = useActions();
  const [form, setForm] = useState<DispatchFormState>(initialForm);
  const [formError, setFormError] = useState<string | null>(null);

  const handleSubmit = async () => {
    if (!form.destination.trim() || !form.body.trim()) {
      setFormError('destination, body는 필수입니다.');
      return;
    }

    const variables = parseVariables(form.variablesJson);
    if (!variables) {
      setFormError('variables JSON 형식을 확인해주세요.');
      return;
    }

    const parsedCampaignId = form.campaignId ? Number(form.campaignId) : undefined;
    if (parsedCampaignId !== undefined && (!Number.isFinite(parsedCampaignId) || parsedCampaignId <= 0)) {
      setFormError('Campaign ID는 유효한 양수여야 합니다.');
      return;
    }

    const parsedJourneyExecutionId = form.journeyExecutionId ? Number(form.journeyExecutionId) : undefined;
    if (
      parsedJourneyExecutionId !== undefined &&
      (!Number.isFinite(parsedJourneyExecutionId) || parsedJourneyExecutionId <= 0)
    ) {
      setFormError('Journey Execution ID는 유효한 양수여야 합니다.');
      return;
    }

    setFormError(null);

    const payload: ActionDispatchRequest = {
      channel: form.channel,
      destination: form.destination.trim(),
      subject: form.subject.trim() || undefined,
      body: form.body.trim(),
      variables,
      campaignId: parsedCampaignId,
      journeyExecutionId: parsedJourneyExecutionId
    };

    const success = await dispatchAction(payload);
    if (!success) {
      return;
    }

    setForm(initialForm);
    setFormError(null);
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-semibold text-white">Action Dispatch</h2>
        <p className="text-sm text-slate-300">즉시 메시지를 발송하고 결과를 확인합니다.</p>
      </div>

      <GuidePanel
        description="이메일, 슬랙, 디스코드로 즉시 메시지를 보내는 화면입니다."
        items={[
          '채널과 수신 대상을 입력한 뒤 메시지 본문을 작성합니다.',
          'Variables JSON에는 치환할 값을 key-value 형태로 입력합니다.',
          '전송 후 아래 이력에서 성공/실패 상태를 확인합니다.'
        ]}
        note="긴급 공지나 운영 알림처럼 즉시 전달이 필요한 상황에서 사용합니다."
      />

      <section className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-5 backdrop-blur">
        <h3 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-300">Dispatch Form</h3>

        {(formError || error) && (
          <div className="mb-4 rounded-xl border border-rose-700/60 bg-rose-900/20 p-3 text-sm text-rose-100">
            {formError || error}
          </div>
        )}

        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <div>
            <label className="mb-2 block text-sm font-medium text-slate-200">Channel</label>
            <select
              value={form.channel}
              onChange={(e) => setForm((prev) => ({ ...prev, channel: e.target.value }))}
              className="w-full rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-white focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500"
            >
              <option value="EMAIL">EMAIL</option>
              <option value="SLACK">SLACK</option>
              <option value="DISCORD">DISCORD</option>
            </select>
          </div>

          <Input
            label="Destination"
            value={form.destination}
            onChange={(e) => setForm((prev) => ({ ...prev, destination: e.target.value }))}
            placeholder="example@acme.com"
            required
          />

          <Input
            label="Subject"
            value={form.subject}
            onChange={(e) => setForm((prev) => ({ ...prev, subject: e.target.value }))}
            placeholder="알림 제목"
          />

          <Input
            label="Campaign ID"
            type="number"
            value={form.campaignId}
            onChange={(e) => setForm((prev) => ({ ...prev, campaignId: e.target.value }))}
            placeholder="1"
          />

          <Input
            label="Journey Execution ID"
            type="number"
            value={form.journeyExecutionId}
            onChange={(e) => setForm((prev) => ({ ...prev, journeyExecutionId: e.target.value }))}
            placeholder="101"
          />

          <Textarea
            label="Message Body"
            value={form.body}
            onChange={(e) => setForm((prev) => ({ ...prev, body: e.target.value }))}
            rows={4}
            required
          />

          <Textarea
            label="Variables JSON"
            value={form.variablesJson}
            onChange={(e) => setForm((prev) => ({ ...prev, variablesJson: e.target.value }))}
            rows={4}
          />
        </div>

        <div className="mt-4">
          <Button onClick={handleSubmit} loading={dispatching}>
            Dispatch
          </Button>
        </div>
      </section>

      <section className="overflow-hidden rounded-2xl border border-slate-800/80 bg-slate-900/60 backdrop-blur">
        <div className="border-b border-slate-800 px-4 py-3">
          <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-300">Dispatch Histories</h3>
        </div>
        <div className="max-h-[420px] overflow-auto">
          <table className="min-w-full divide-y divide-slate-800">
            <thead className="bg-slate-800/60">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">ID</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Channel</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Status</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Destination</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Created</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {loading ? (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-sm text-slate-300">
                    Loading histories...
                  </td>
                </tr>
              ) : histories.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-sm text-slate-300">
                    No histories
                  </td>
                </tr>
              ) : (
                histories.map((history) => (
                  <tr key={history.id} className="hover:bg-slate-800/30">
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">{history.id}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-white">{history.channel}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{history.status}</td>
                    <td className="px-4 py-3 text-sm text-slate-300">{history.destination}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">
                      {history.createdAt ? new Date(history.createdAt).toLocaleString() : '-'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
};
