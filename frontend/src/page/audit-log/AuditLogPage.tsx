import React, { useState } from 'react';
import { Button, GuidePanel, Input, Modal } from 'common/component';
import { useAuditLogs } from 'shared/hook';

const formatDateTime = (value?: string): string => {
  if (!value) {
    return '-';
  }

  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? '-' : parsed.toLocaleString();
};

export const AuditLogPage: React.FC = () => {
  const { logs, loading, error, fetchLogs } = useAuditLogs();

  const [limit, setLimit] = useState('50');
  const [action, setAction] = useState('');
  const [resourceType, setResourceType] = useState('');
  const [actorId, setActorId] = useState('');
  const [selectedLog, setSelectedLog] = useState<(typeof logs)[number] | null>(null);

  const handleSearch = async () => {
    const parsedLimit = Number(limit);

    await fetchLogs({
      limit: Number.isFinite(parsedLimit) && parsedLimit > 0 ? parsedLimit : undefined,
      action: action.trim() || undefined,
      resourceType: resourceType.trim() || undefined,
      actorId: actorId.trim() || undefined
    });
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-semibold text-white">Audit Logs</h2>
        <p className="text-sm text-slate-300">운영 작업 이력을 조회하고 점검합니다.</p>
      </div>

      <GuidePanel
        description="시스템에서 누가 어떤 작업을 했는지 확인하는 기록 화면입니다."
        items={[
          'Action, Resource Type, Actor ID로 필요한 기록만 좁혀 조회합니다.',
          'Limit 값을 조정하면 한 번에 가져오는 기록 수를 바꿀 수 있습니다.',
          '문제 발생 시 상태 코드와 시간 정보를 함께 확인해 원인을 추적합니다.'
        ]}
        note="운영 감사와 장애 분석 시 가장 먼저 확인하는 화면입니다."
      />

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
                  <tr
                    key={log.id}
                    className="cursor-pointer hover:bg-slate-800/30"
                    onClick={() => setSelectedLog(log)}
                  >
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">{log.id}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-white">{log.action}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{log.resourceType}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{log.actorId ?? '-'}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{log.statusCode ?? '-'}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">
                      {formatDateTime(log.createdAt)}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      <Modal
        isOpen={Boolean(selectedLog)}
        onClose={() => setSelectedLog(null)}
        title={selectedLog ? `Audit Log #${selectedLog.id}` : 'Audit Log'}
        size="lg"
      >
        {selectedLog && (
          <div className="space-y-4">
            <div className="grid grid-cols-1 gap-3 text-sm text-slate-200 md:grid-cols-2">
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Action</p>
                <p>{selectedLog.action}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Resource</p>
                <p>{selectedLog.resourceType}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Actor</p>
                <p>{selectedLog.actorId ?? '-'}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Status</p>
                <p>{selectedLog.statusCode ?? '-'}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Created</p>
                <p>{formatDateTime(selectedLog.createdAt)}</p>
              </div>
            </div>

            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Raw JSON</p>
              <pre className="mt-2 max-h-[360px] overflow-auto rounded-lg border border-slate-700 bg-slate-950/80 p-3 text-xs text-slate-200">
                {JSON.stringify(selectedLog, null, 2)}
              </pre>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
