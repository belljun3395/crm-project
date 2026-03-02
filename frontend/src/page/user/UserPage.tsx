import React, { useState } from 'react';
import { Button, GuidePanel, Input, Modal, Textarea } from 'common/component';
import { useToggle } from 'common/hook';
import { useUsers } from 'shared/hook';
import { COLLECTED_USER_FIELDS } from 'shared/variableGuide';
import type { UserFormData } from 'shared/type';

const formatUserAttributes = (raw?: string): string => {
  if (!raw) {
    return '';
  }

  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
};

export const UserPage: React.FC = () => {
  const { users, loading, enrolling, error, enrollUser } = useUsers();
  const { value: isModalOpen, setTrue: openModal, setFalse: closeModal } = useToggle();
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedUser, setSelectedUser] = useState<(typeof users)[number] | null>(null);
  const [formData, setFormData] = useState<UserFormData>({
    externalId: '',
    userAttributes: ''
  });

  // 검색 필터링 (NPE 방지)
  const filteredUsers = users.filter(user => {
    const searchLower = searchTerm.toLowerCase();
    const externalId = user.externalId?.toLowerCase() || '';
    const userAttributes = user.userAttributes?.toLowerCase() || '';
    return externalId.includes(searchLower) || userAttributes.includes(searchLower);
  });

  // 폼 제출
  const handleSubmit = async () => {
    if (!formData.externalId || !formData.userAttributes) return;
    
    const success = await enrollUser(formData);
    if (success) {
      setFormData({ externalId: '', userAttributes: '' });
      closeModal();
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">Users</h1>
        <Button onClick={openModal}>
          <span className="text-lg mr-2">+</span>
          Enroll User
        </Button>
      </div>

      {/* Error Message */}
      {error && (
        <div className="rounded-lg bg-red-900/50 border border-red-700 p-4">
          <div className="flex">
            <div className="flex-shrink-0">
              <span className="text-red-400">⚠️</span>
            </div>
            <div className="ml-3">
              <p className="text-sm text-red-300">{error}</p>
            </div>
          </div>
        </div>
      )}

      <GuidePanel
        description="고객 식별값과 고객 속성 정보를 등록하고 조회하는 화면입니다."
        items={[
          'Enroll User 버튼으로 새 사용자를 등록합니다.',
          'External ID는 고객을 구분하는 고유 값으로 사용됩니다.',
          'userAttributes.email은 필수 키입니다.',
          '검색창에서 외부 ID나 속성 텍스트로 빠르게 찾을 수 있습니다.'
        ]}
        note="공통으로 externalId, userAttributes.email을 사용합니다. name은 개인화에 권장됩니다."
      />

      {/* 검색 */}
      <div className="relative">
        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">🔍</span>
        <Input
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="Search users by external ID or attributes..."
          className="pl-12"
        />
      </div>

      {/* 사용자 테이블 */}
      <div className="overflow-hidden rounded-xl border border-gray-800 bg-gray-900">
        <table className="min-w-full divide-y divide-gray-800">
          <thead className="bg-gray-800/50">
            <tr>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">ID</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">External ID</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">Attributes</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">Created</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-800">
            {loading ? (
              <tr>
                <td colSpan={4} className="px-6 py-8 text-center text-gray-400">
                  Loading users...
                </td>
              </tr>
            ) : filteredUsers.length > 0 ? (
              filteredUsers.map((user) => (
                <tr
                  key={user.id}
                  className="cursor-pointer hover:bg-gray-800/50"
                  onClick={() => setSelectedUser(user)}
                >
                  <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-400">{user.id}</td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-white">
                    {user.externalId}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-400 max-w-xs truncate">
                    {user.userAttributes}
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-400">
                    {user.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'N/A'}
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={4} className="px-6 py-8 text-center text-gray-400">
                  No users found
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <Modal
        isOpen={Boolean(selectedUser)}
        onClose={() => setSelectedUser(null)}
        title={selectedUser ? `User #${selectedUser.id}` : 'User Detail'}
        size="lg"
      >
        {selectedUser && (
          <div className="space-y-4">
            <div className="grid grid-cols-1 gap-3 text-sm text-slate-200 md:grid-cols-2">
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">ID</p>
                <p>{selectedUser.id}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">External ID</p>
                <p>{selectedUser.externalId}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Created</p>
                <p>{selectedUser.createdAt ? new Date(selectedUser.createdAt).toLocaleString() : '-'}</p>
              </div>
            </div>

            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">User Attributes</p>
              <pre className="mt-2 max-h-[300px] overflow-auto rounded-lg border border-slate-700 bg-slate-950/80 p-3 text-xs text-slate-200">
                {formatUserAttributes(selectedUser.userAttributes)}
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

      {/* 사용자 등록 모달 */}
      <Modal isOpen={isModalOpen} onClose={closeModal} title="Enroll User">
        <div className="space-y-4">
          <div className="rounded-lg border border-slate-700/70 bg-slate-900/60 p-3">
            <p className="text-sm font-semibold text-slate-100">공통 수집 필드</p>
            <ul className="mt-2 space-y-1 text-xs text-slate-300">
              {COLLECTED_USER_FIELDS.map((field) => (
                <li key={field.key}>
                  <span className="font-mono text-slate-200">{field.key}</span>
                  {' '}({field.required ? '필수' : '선택'}) - {field.description}
                </li>
              ))}
            </ul>
          </div>

          <Input
            label="External ID"
            value={formData.externalId}
            onChange={(e) => setFormData({...formData, externalId: e.target.value})}
            placeholder="Enter external ID"
            required
          />
          <Textarea
            label="User Attributes"
            value={formData.userAttributes}
            onChange={(e) => setFormData({...formData, userAttributes: e.target.value})}
            placeholder='{"email":"user@example.com","name":"홍길동","tier":"BETA"}'
            rows={5}
            required
          />
          <div className="flex gap-3 pt-4">
            <Button onClick={handleSubmit} loading={enrolling} className="flex-1">
              Enroll
            </Button>
            <Button onClick={closeModal} variant="secondary" className="flex-1">
              Cancel
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
