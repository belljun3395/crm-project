import React, { useMemo, useState } from 'react';
import { GuidePanel, Modal } from 'common/component';
import {
  useUsers,
  useCampaigns,
  useTemplates,
  useEmailHistories,
  useEmailSchedules,
  useWebhooks,
  useSegments,
  useJourneys,
  useActions,
  useAuditLogs
} from 'shared/hook';

const formatNumber = (value: number): string => new Intl.NumberFormat('ko-KR').format(value);
const formatDateTime = (value?: string): string => {
  if (!value) {
    return '-';
  }

  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? '-' : parsed.toLocaleString();
};

export const DashboardPage: React.FC = () => {
  const { users, userCount } = useUsers();
  const { campaigns } = useCampaigns();
  const { templates } = useTemplates();
  const { totalCount: emailHistoriesTotalCount } = useEmailHistories();
  const { schedules } = useEmailSchedules();
  const { webhooks } = useWebhooks();
  const { segments } = useSegments();
  const { journeys, executions } = useJourneys();
  const { histories } = useActions();
  const { logs } = useAuditLogs();
  const [selectedUser, setSelectedUser] = useState<(typeof users)[number] | null>(null);
  const [selectedHistory, setSelectedHistory] = useState<(typeof histories)[number] | null>(null);

  const cards = useMemo(
    () => [
      { label: 'Total Users', value: formatNumber(userCount), accent: 'from-cyan-400/20 to-cyan-500/0' },
      { label: 'Campaigns', value: formatNumber(campaigns.length), accent: 'from-lime-400/20 to-lime-500/0' },
      { label: 'Email Templates', value: formatNumber(templates.length), accent: 'from-emerald-400/20 to-emerald-500/0' },
      { label: 'Scheduled Emails', value: formatNumber(schedules.length), accent: 'from-amber-400/20 to-amber-500/0' },
      { label: 'Email Histories', value: formatNumber(emailHistoriesTotalCount), accent: 'from-orange-400/20 to-orange-500/0' },
      { label: 'Webhooks', value: formatNumber(webhooks.length), accent: 'from-fuchsia-400/20 to-fuchsia-500/0' },
      { label: 'Segments', value: formatNumber(segments.length), accent: 'from-sky-400/20 to-sky-500/0' },
      { label: 'Journeys', value: formatNumber(journeys.length), accent: 'from-indigo-400/20 to-indigo-500/0' },
      { label: 'Journey Executions', value: formatNumber(executions.length), accent: 'from-teal-400/20 to-teal-500/0' },
      { label: 'Action Histories', value: formatNumber(histories.length), accent: 'from-rose-400/20 to-rose-500/0' },
      { label: 'Audit Logs', value: formatNumber(logs.length), accent: 'from-violet-400/20 to-violet-500/0' }
    ],
    [
      userCount,
      campaigns.length,
      templates.length,
      schedules.length,
      emailHistoriesTotalCount,
      webhooks.length,
      segments.length,
      journeys.length,
      executions.length,
      histories.length,
      logs.length
    ]
  );

  return (
    <div className="space-y-7">
      <div>
        <h2 className="text-2xl font-semibold text-white">Overview</h2>
        <p className="text-sm text-slate-300">운영 API 상태를 한눈에 보는 대시보드</p>
      </div>

      <GuidePanel
        description="서비스 전체 상태를 빠르게 파악하는 화면입니다."
        items={[
          '상단 카드 숫자로 전체 규모를 확인합니다.',
          '아래 표에서 최근 사용자와 최근 발송 이력을 확인합니다.',
          '문제가 보이면 좌측 메뉴에서 해당 상세 화면으로 이동해 원인을 확인합니다.'
        ]}
        note="이 화면은 요약 화면이며, 상세 수정은 각 기능 화면에서 진행합니다."
      />

      <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {cards.map((card) => (
          <article
            key={card.label}
            className={`relative overflow-hidden rounded-2xl border border-slate-800/80 bg-slate-900/60 p-5 backdrop-blur`}
          >
            <div className={`pointer-events-none absolute inset-0 bg-gradient-to-r ${card.accent}`} />
            <p className="relative text-xs uppercase tracking-wide text-slate-300">{card.label}</p>
            <p className="relative mt-2 text-3xl font-semibold text-white">{card.value}</p>
          </article>
        ))}
      </section>

      <section className="grid grid-cols-1 gap-5 xl:grid-cols-2">
        <div className="overflow-hidden rounded-2xl border border-slate-800/80 bg-slate-900/60 backdrop-blur">
          <div className="border-b border-slate-800 px-4 py-3">
            <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-300">Recent Users</h3>
          </div>
          <table className="min-w-full divide-y divide-slate-800">
            <thead className="bg-slate-800/60">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">External ID</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Attributes</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Created</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {users.slice(0, 8).map((user) => (
                <tr
                  key={user.id}
                  className="cursor-pointer hover:bg-slate-800/30"
                  onClick={() => setSelectedUser(user)}
                >
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-white">{user.externalId}</td>
                  <td className="max-w-xs truncate px-4 py-3 text-sm text-slate-300">{user.userAttributes}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">
                    {formatDateTime(user.createdAt)}
                  </td>
                </tr>
              ))}
              {users.length === 0 && (
                <tr>
                  <td colSpan={3} className="px-4 py-8 text-center text-sm text-slate-300">
                    No users found
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="overflow-hidden rounded-2xl border border-slate-800/80 bg-slate-900/60 backdrop-blur">
          <div className="border-b border-slate-800 px-4 py-3">
            <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-300">Recent Action Dispatches</h3>
          </div>
          <table className="min-w-full divide-y divide-slate-800">
            <thead className="bg-slate-800/60">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">ID</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Channel</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Status</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Created</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {histories.slice(0, 8).map((history) => (
                <tr
                  key={history.id}
                  className="cursor-pointer hover:bg-slate-800/30"
                  onClick={() => setSelectedHistory(history)}
                >
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">{history.id}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-white">{history.channel}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{history.status}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">
                    {formatDateTime(history.createdAt)}
                  </td>
                </tr>
              ))}
              {histories.length === 0 && (
                <tr>
                  <td colSpan={4} className="px-4 py-8 text-center text-sm text-slate-300">
                    No action histories
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>

      <Modal
        isOpen={Boolean(selectedUser)}
        onClose={() => setSelectedUser(null)}
        title={selectedUser ? `Recent User #${selectedUser.id}` : 'Recent User'}
        size="lg"
      >
        {selectedUser && (
          <div className="space-y-4">
            <div className="grid grid-cols-1 gap-3 text-sm text-slate-200 md:grid-cols-2">
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">External ID</p>
                <p>{selectedUser.externalId}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Created</p>
                <p>{formatDateTime(selectedUser.createdAt)}</p>
              </div>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Attributes</p>
              <pre className="mt-2 max-h-[300px] overflow-auto rounded-lg border border-slate-700 bg-slate-950/80 p-3 text-xs text-slate-200">
                {selectedUser.userAttributes}
              </pre>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Raw JSON</p>
              <pre className="mt-2 max-h-[220px] overflow-auto rounded-lg border border-slate-700 bg-slate-950/80 p-3 text-xs text-slate-200">
                {JSON.stringify(selectedUser, null, 2)}
              </pre>
            </div>
          </div>
        )}
      </Modal>

      <Modal
        isOpen={Boolean(selectedHistory)}
        onClose={() => setSelectedHistory(null)}
        title={selectedHistory ? `Recent Dispatch #${selectedHistory.id}` : 'Recent Dispatch'}
        size="lg"
      >
        {selectedHistory && (
          <div className="space-y-4">
            <div className="grid grid-cols-1 gap-3 text-sm text-slate-200 md:grid-cols-2">
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Channel</p>
                <p>{selectedHistory.channel}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Status</p>
                <p>{selectedHistory.status}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Created</p>
                <p>{formatDateTime(selectedHistory.createdAt)}</p>
              </div>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Raw JSON</p>
              <pre className="mt-2 max-h-[260px] overflow-auto rounded-lg border border-slate-700 bg-slate-950/80 p-3 text-xs text-slate-200">
                {JSON.stringify(selectedHistory, null, 2)}
              </pre>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
