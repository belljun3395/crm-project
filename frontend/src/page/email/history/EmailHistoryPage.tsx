import React, { useMemo, useState } from 'react';
import { Button, GuidePanel, Input, Modal } from 'common/component';
import { useEmailHistories, useUsers } from 'shared/hook';

const formatDateTime = (value?: string): string => {
  if (!value) {
    return '-';
  }

  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? '-' : parsed.toLocaleString();
};

const extractUserEmail = (raw?: string): string => {
  if (!raw) {
    return '';
  }

  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    const email = parsed.email;
    return typeof email === 'string' ? email : '';
  } catch {
    return '';
  }
};

export const EmailHistoryPage: React.FC = () => {
  const { histories, totalCount, page, size, loading, error, fetchHistories } = useEmailHistories();
  const { users } = useUsers();

  const [userId, setUserId] = useState('');
  const [userQuery, setUserQuery] = useState('');
  const [sendStatus, setSendStatus] = useState('');
  const [pageInput, setPageInput] = useState('0');
  const [sizeInput, setSizeInput] = useState('20');
  const [selectedHistory, setSelectedHistory] = useState<(typeof histories)[number] | null>(null);

  const filteredUsers = useMemo(() => {
    const query = userQuery.trim().toLowerCase();
    if (!query) {
      return users.slice(0, 200);
    }

    return users
      .filter((user) => {
        const externalId = user.externalId?.toLowerCase() ?? '';
        const email = extractUserEmail(user.userAttributes).toLowerCase();
        return externalId.includes(query) || email.includes(query);
      })
      .slice(0, 200);
  }, [users, userQuery]);

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
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-5">
          <Input
            label="User Search"
            value={userQuery}
            onChange={(e) => setUserQuery(e.target.value)}
            placeholder="externalId 또는 email 검색"
          />

          <div>
            <label className="mb-2 block text-sm font-medium text-slate-200">User (optional)</label>
            <select
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              className="w-full rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-white focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500"
            >
              <option value="">전체 사용자</option>
              {filteredUsers.map((user) => {
                const email = extractUserEmail(user.userAttributes);
                return (
                  <option key={user.id} value={user.id}>
                    #{user.id} - {user.externalId}{email ? ` (${email})` : ''}
                  </option>
                );
              })}
            </select>
            <p className="mt-1 text-xs text-slate-500">검색 결과 상위 200명만 표시됩니다.</p>
          </div>

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
                <tr
                  key={history.id}
                  className="cursor-pointer hover:bg-slate-800/30"
                  onClick={() => setSelectedHistory(history)}
                >
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

      <Modal
        isOpen={Boolean(selectedHistory)}
        onClose={() => setSelectedHistory(null)}
        title={selectedHistory ? `Email History #${selectedHistory.id}` : 'Email History'}
        size="lg"
      >
        {selectedHistory && (
          <div className="space-y-4">
            <div className="grid grid-cols-1 gap-3 text-sm text-slate-200 md:grid-cols-2">
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">User Email</p>
                <p>{selectedHistory.userEmail}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Status</p>
                <p>{selectedHistory.sendStatus}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Message ID</p>
                <p>{selectedHistory.emailMessageId}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Created</p>
                <p>{formatDateTime(selectedHistory.createdAt)}</p>
              </div>
            </div>

            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Raw JSON</p>
              <pre className="mt-2 max-h-[360px] overflow-auto rounded-lg border border-slate-700 bg-slate-950/80 p-3 text-xs text-slate-200">
                {JSON.stringify(selectedHistory, null, 2)}
              </pre>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
