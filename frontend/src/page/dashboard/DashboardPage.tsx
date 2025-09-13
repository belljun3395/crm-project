import React from 'react';
import { useUsers, useTemplates, useEmailSchedules } from 'shared/hook';

export const DashboardPage: React.FC = () => {
  const { users, userCount } = useUsers();
  const { templates } = useTemplates();
  const { schedules } = useEmailSchedules();

  return (
    <div className="space-y-8">
      <h1 className="text-4xl font-bold tracking-tight">Dashboard</h1>
      
      {/* 통계 카드 */}
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
        <div className="rounded-xl border border-gray-800 bg-gray-900 p-6">
          <h3 className="text-lg font-medium text-gray-300">Total Users</h3>
          <p className="mt-2 text-4xl font-bold text-white">{userCount.toLocaleString()}</p>
        </div>
        <div className="rounded-xl border border-gray-800 bg-gray-900 p-6">
          <h3 className="text-lg font-medium text-gray-300">Email Templates</h3>
          <p className="mt-2 text-4xl font-bold text-white">{templates.length}</p>
        </div>
        <div className="rounded-xl border border-gray-800 bg-gray-900 p-6">
          <h3 className="text-lg font-medium text-gray-300">Scheduled Emails</h3>
          <p className="mt-2 text-4xl font-bold text-white">{schedules.length}</p>
        </div>
      </div>

      {/* 최근 사용자 목록 */}
      <div className="space-y-6">
        <h2 className="text-2xl font-bold tracking-tight">Recent Users</h2>
        <div className="overflow-hidden rounded-xl border border-gray-800 bg-gray-900">
          <table className="min-w-full divide-y divide-gray-800">
            <thead className="bg-gray-800/50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-400">
                  External ID
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-400">
                  Attributes
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-400">
                  Created
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-800">
              {users.slice(0, 5).map((user) => (
                <tr key={user.id}>
                  <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-white">
                    {user.externalId}
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-400 max-w-xs truncate">
                    {user.userAttributes}
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-400">
                    {user.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'N/A'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {users.length === 0 && (
            <div className="p-8 text-center text-gray-400">
              No users found
            </div>
          )}
        </div>
      </div>
    </div>
  );
};