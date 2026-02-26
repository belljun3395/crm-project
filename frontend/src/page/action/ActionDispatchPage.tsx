import React, { useState } from 'react';
import { Button, Input, Textarea } from 'common/component';
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

    const payload: ActionDispatchRequest = {
      channel: form.channel,
      destination: form.destination.trim(),
      subject: form.subject.trim() || undefined,
      body: form.body,
      variables,
      campaignId: form.campaignId ? Number(form.campaignId) : undefined,
      journeyExecutionId: form.journeyExecutionId ? Number(form.journeyExecutionId) : undefined
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
        <p className="text-sm text-slate-300">`/actions/dispatch`, `/actions/dispatch/histories` API 연동</p>
      </div>

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
                      {new Date(history.createdAt).toLocaleString()}
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
