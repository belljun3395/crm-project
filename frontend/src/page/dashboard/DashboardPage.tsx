import React from 'react';
import {
  useUsers,
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

export const DashboardPage: React.FC = () => {
  const { users, userCount } = useUsers();
  const { templates } = useTemplates();
  const { histories: emailHistories } = useEmailHistories();
  const { schedules } = useEmailSchedules();
  const { webhooks } = useWebhooks();
  const { segments } = useSegments();
  const { journeys, executions } = useJourneys();
  const { histories } = useActions();
  const { logs } = useAuditLogs();

  const cards = [
    { label: 'Total Users', value: formatNumber(userCount), accent: 'from-cyan-400/20 to-cyan-500/0' },
    { label: 'Email Templates', value: formatNumber(templates.length), accent: 'from-emerald-400/20 to-emerald-500/0' },
    { label: 'Scheduled Emails', value: formatNumber(schedules.length), accent: 'from-amber-400/20 to-amber-500/0' },
    { label: 'Email Histories', value: formatNumber(emailHistories.length), accent: 'from-orange-400/20 to-orange-500/0' },
    { label: 'Webhooks', value: formatNumber(webhooks.length), accent: 'from-fuchsia-400/20 to-fuchsia-500/0' },
    { label: 'Segments', value: formatNumber(segments.length), accent: 'from-sky-400/20 to-sky-500/0' },
    { label: 'Journeys', value: formatNumber(journeys.length), accent: 'from-indigo-400/20 to-indigo-500/0' },
    { label: 'Journey Executions', value: formatNumber(executions.length), accent: 'from-teal-400/20 to-teal-500/0' },
    { label: 'Action Histories', value: formatNumber(histories.length), accent: 'from-rose-400/20 to-rose-500/0' },
    { label: 'Audit Logs', value: formatNumber(logs.length), accent: 'from-violet-400/20 to-violet-500/0' }
  ];

  return (
    <div className="space-y-7">
      <div>
        <h2 className="text-2xl font-semibold text-white">Overview</h2>
        <p className="text-sm text-slate-300">운영 API 상태를 한눈에 보는 대시보드</p>
      </div>

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
                <tr key={user.id} className="hover:bg-slate-800/30">
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-white">{user.externalId}</td>
                  <td className="max-w-xs truncate px-4 py-3 text-sm text-slate-300">{user.userAttributes}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">
                    {user.createdAt ? new Date(user.createdAt).toLocaleString() : '-'}
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
                <tr key={history.id} className="hover:bg-slate-800/30">
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">{history.id}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-white">{history.channel}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{history.status}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">
                    {new Date(history.createdAt).toLocaleString()}
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
    </div>
  );
};
