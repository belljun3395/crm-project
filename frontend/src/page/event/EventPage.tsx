import React, { useEffect, useState } from 'react';
import { Button, Input, Modal } from 'common/component';
import { useToggle } from 'common/hook';
import { useEvents } from 'shared/hook';
import type { EventFormData } from 'shared/type';

export const EventPage: React.FC = () => {
  const { events, loading, error, createEvent, searchEvents } = useEvents();
  const { value: isModalOpen, setTrue: openModal, setFalse: closeModal } = useToggle();
  const [searchTerm, setSearchTerm] = useState('');
  const [formData, setFormData] = useState<EventFormData>({
    name: '',
    campaignName: '',
    externalId: '',
    properties: [{ key: '', value: '' }]
  });

  // 초기 데이터 로드
  useEffect(() => {
    // 전체 이벤트 조회 (빈 문자열로 검색)
    searchEvents('', '');
  }, [searchEvents]);

  // 검색 필터링
  const filteredEvents = events.filter(event => 
    event.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (event.externalId && event.externalId.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  // 폼 제출
  const handleSubmit = async () => {
    if (!formData.name || !formData.externalId) return;

    const success = await createEvent(formData);
    if (success) {
      setFormData({
        name: '',
        campaignName: '',
        externalId: '',
        properties: [{ key: '', value: '' }]
      });
      closeModal();
      // 이벤트 생성 후 목록 새로고침
      searchEvents('', '');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">Events</h1>
        <Button onClick={openModal}>
          <span className="text-lg mr-2">+</span>
          New Event
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

      {/* 검색 */}
      <div className="relative">
        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">🔍</span>
        <Input
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="Search events..."
          className="pl-12"
        />
      </div>

      {/* 이벤트 테이블 */}
      <div className="overflow-hidden rounded-xl border border-gray-800 bg-gray-900">
        <table className="min-w-full divide-y divide-gray-800">
          <thead className="bg-gray-800/50">
            <tr>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">Name</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">External ID</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">Properties</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">Created</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-800">
            {loading ? (
              <tr>
                <td colSpan={4} className="px-6 py-8 text-center text-gray-400">
                  Loading events...
                </td>
              </tr>
            ) : filteredEvents.length > 0 ? (
              filteredEvents.map((event) => (
                <tr key={event.id} className="hover:bg-gray-800/50">
                  <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-white">
                    {event.name}
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-400">
                    {event.externalId || 'N/A'}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-400 max-w-xs truncate">
                    {event.properties.map(p => `${p.key}=${p.value}`).join(', ')}
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-400">
                    {new Date(event.createdAt).toLocaleDateString()}
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={4} className="px-6 py-8 text-center text-gray-400">
                  No events found
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* 이벤트 생성 모달 */}
      <Modal isOpen={isModalOpen} onClose={closeModal} title="Create Event">
        <div className="space-y-4">
          <Input
            label="Event Name"
            value={formData.name}
            onChange={(e) => setFormData({...formData, name: e.target.value})}
            placeholder="Enter event name"
            required
          />
          <Input
            label="Campaign Name"
            value={formData.campaignName}
            onChange={(e) => setFormData({...formData, campaignName: e.target.value})}
            placeholder="Enter campaign name (optional)"
          />
          <Input
            label="External ID"
            value={formData.externalId}
            onChange={(e) => setFormData({...formData, externalId: e.target.value})}
            placeholder="Enter external ID"
            required
          />
          <div className="grid grid-cols-2 gap-2">
            <Input
              label="Property Key"
              value={formData.properties[0]?.key || ''}
              onChange={(e) => setFormData({
                ...formData, 
                properties: [{...(formData.properties[0] || { key: '', value: '' }), key: e.target.value}]
              })}
              placeholder="Property key"
            />
            <Input
              label="Property Value"
              value={formData.properties[0]?.value || ''}
              onChange={(e) => setFormData({
                ...formData, 
                properties: [{...(formData.properties[0] || { key: '', value: '' }), value: e.target.value}]
              })}
              placeholder="Property value"
            />
          </div>
          <div className="flex gap-3 pt-4">
            <Button onClick={handleSubmit} loading={loading} className="flex-1">
              Create
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