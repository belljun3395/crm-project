import React, { useState } from 'react';
import { Button, GuidePanel, Input } from 'common/component';
import { useEmailHistories } from 'shared/hook';

const formatDateTime = (value?: string): string => {
  if (!value) {
    return '-';
  }

  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? '-' : parsed.toLocaleString();
};

export const EmailHistoryPage: React.FC = () => {
  const { histories, totalCount, page, size, loading, error, fetchHistories } = useEmailHistories();

  const [userId, setUserId] = useState('');
  const [sendStatus, setSendStatus] = useState('');
  const [pageInput, setPageInput] = useState('0');
  const [sizeInput, setSizeInput] = useState('20');

  const handleSearch = async () => {
    const parsedUserId = Number(userId);
    const parsedPage = Number(pageInput);
    const parsedSize = Number(sizeInput);

    await fetchHistories({
      userId: Number.isFinite(parsedUserId) && parsedUserId > 0 ? parsedUserId : undefined,
      sendStatus: sendStatus.trim() || undefined,
      page: Number.isFinite(parsedPage) && parsedPage >= 0 ? parsedPage : 0,
      size: Number.isFinite(parsedSize) && parsedSize > 0 ? parsedSize : 20
    });
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-semibold text-white">Email Histories</h2>
        <p className="text-sm text-slate-300">메일 발송 결과를 조회하고 상태를 점검합니다.</p>
      </div>

      <GuidePanel
        description="이미 발송된 이메일 결과를 조회하는 화면입니다."
        items={[
          '필요한 조건(User ID, 상태, 페이지)을 넣고 조회를 누릅니다.',
          '상태값으로 성공/실패 발송만 골라 볼 수 있습니다.',
          'total, page, size를 보고 현재 조회 범위를 확인합니다.'
        ]}
        note="이 화면은 조회 전용이며, 발송 재시도/수정은 관련 기능 화면에서 진행합니다."
      />

      <section className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-5 backdrop-blur">
        <h3 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-300">Filter</h3>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
          <Input
            label="User ID"
            type="number"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            placeholder="1001"
          />
          <Input
            label="Send Status"
            value={sendStatus}
            onChange={(e) => setSendStatus(e.target.value)}
            placeholder="SUCCESS"
          />
          <Input
            label="Page"
            type="number"
            value={pageInput}
            onChange={(e) => setPageInput(e.target.value)}
          />
          <Input
            label="Size"
            type="number"
            value={sizeInput}
            onChange={(e) => setSizeInput(e.target.value)}
          />
        </div>

        <div className="mt-4 flex flex-wrap items-center gap-3">
          <Button onClick={handleSearch}>조회</Button>
          <p className="text-xs text-slate-400">
            total={totalCount}, page={page}, size={size}
          </p>
        </div>
      </section>

      {error && (
        <div className="rounded-xl border border-rose-700/60 bg-rose-900/20 p-3 text-sm text-rose-100">{error}</div>
      )}

      <section className="overflow-hidden rounded-2xl border border-slate-800/80 bg-slate-900/60 backdrop-blur">
        <table className="min-w-full divide-y divide-slate-800">
          <thead className="bg-slate-800/60">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">ID</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">User</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Status</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Message ID</th>
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
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-white">{history.userEmail}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{history.sendStatus}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{history.emailMessageId}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">
                    {formatDateTime(history.createdAt)}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </section>
    </div>
  );
};
