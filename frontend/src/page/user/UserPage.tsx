import React, { useState } from 'react';
import { Button, Input, Modal } from 'common/component';
import { useToggle } from 'common/hook';
import { useUsers } from 'shared/hook';
import type { UserFormData } from 'shared/type';

export const UserPage: React.FC = () => {
  const { users, loading, enrollUser } = useUsers();
  const { value: isModalOpen, setTrue: openModal, setFalse: closeModal } = useToggle();
  const [searchTerm, setSearchTerm] = useState('');
  const [formData, setFormData] = useState<UserFormData>({
    externalId: '',
    userAttributes: ''
  });

  // 검색 필터링
  const filteredUsers = users.filter(user => 
    user.externalId.toLowerCase().includes(searchTerm.toLowerCase()) ||
    user.userAttributes.toLowerCase().includes(searchTerm.toLowerCase())
  );

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
                <tr key={user.id} className="hover:bg-gray-800/50">
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

      {/* 사용자 등록 모달 */}
      <Modal isOpen={isModalOpen} onClose={closeModal} title="Enroll User">
        <div className="space-y-4">
          <Input
            label="External ID"
            value={formData.externalId}
            onChange={(e) => setFormData({...formData, externalId: e.target.value})}
            placeholder="Enter external ID"
            required
          />
          <Input
            label="User Attributes"
            value={formData.userAttributes}
            onChange={(e) => setFormData({...formData, userAttributes: e.target.value})}
            placeholder="Enter user attributes (JSON format)"
            required
          />
          <div className="flex gap-3 pt-4">
            <Button onClick={handleSubmit} loading={loading} className="flex-1">
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