import React, { useState } from 'react';
import { Button, Input } from 'common/component';
import { useAuditLogs } from 'shared/hook';

export const AuditLogPage: React.FC = () => {
  const { logs, loading, error, fetchLogs } = useAuditLogs();

  const [limit, setLimit] = useState('50');
  const [action, setAction] = useState('');
  const [resourceType, setResourceType] = useState('');
  const [actorId, setActorId] = useState('');

  const handleSearch = async () => {
    await fetchLogs({
      limit: limit ? Number(limit) : undefined,
      action: action.trim() || undefined,
      resourceType: resourceType.trim() || undefined,
      actorId: actorId.trim() || undefined
    });
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-semibold text-white">Audit Logs</h2>
        <p className="text-sm text-slate-300">`/audit-logs` API 연동</p>
      </div>

      <section className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-5 backdrop-blur">
        <h3 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-300">Filter</h3>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
          <Input
            label="Limit"
            type="number"
            value={limit}
            onChange={(e) => setLimit(e.target.value)}
            placeholder="50"
          />
          <Input
            label="Action"
            value={action}
            onChange={(e) => setAction(e.target.value)}
            placeholder="CREATE_USER"
          />
          <Input
            label="Resource Type"
            value={resourceType}
            onChange={(e) => setResourceType(e.target.value)}
            placeholder="USER"
          />
          <Input
            label="Actor ID"
            value={actorId}
            onChange={(e) => setActorId(e.target.value)}
            placeholder="admin@acme.com"
          />
        </div>

        <div className="mt-4">
          <Button onClick={handleSearch}>조회</Button>
        </div>
      </section>

      {error && (
        <div className="rounded-xl border border-rose-700/60 bg-rose-900/20 p-3 text-sm text-rose-100">{error}</div>
      )}

      <section className="overflow-hidden rounded-2xl border border-slate-800/80 bg-slate-900/60 backdrop-blur">
        <div className="border-b border-slate-800 px-4 py-3">
          <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-300">Logs</h3>
        </div>
        <div className="max-h-[500px] overflow-auto">
          <table className="min-w-full divide-y divide-slate-800">
            <thead className="bg-slate-800/60">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">ID</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Action</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Resource</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Actor</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Status</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Created</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {loading ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-sm text-slate-300">
                    Loading logs...
                  </td>
                </tr>
              ) : logs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-sm text-slate-300">
                    No logs
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-800/30">
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">{log.id}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-white">{log.action}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{log.resourceType}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{log.actorId ?? '-'}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{log.statusCode ?? '-'}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">
                      {log.createdAt ? new Date(log.createdAt).toLocaleString() : '-'}
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
