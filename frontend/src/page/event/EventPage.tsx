import React, { useState } from 'react';
import { Button, GuidePanel, Input, Modal } from 'common/component';
import { useToggle } from 'common/hook';
import { useEvents } from 'shared/hook';
import type { EventFormData } from 'shared/type';

export const EventPage: React.FC = () => {
  const { events, loading, error, createEvent, browseAllEvents, searchEvents } = useEvents();
  const { value: isModalOpen, setTrue: openModal, setFalse: closeModal } = useToggle();
  const [eventNameQuery, setEventNameQuery] = useState('');
  const [whereQuery, setWhereQuery] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [hasSearched, setHasSearched] = useState(false);
  const [formData, setFormData] = useState<EventFormData>({
    name: '',
    campaignName: '',
    externalId: '',
    properties: [{ key: '', value: '' }]
  });
  const [selectedEvent, setSelectedEvent] = useState<(typeof events)[number] | null>(null);

  // 검색 필터링
  const filteredEvents = events.filter(event =>
    event.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (event.externalId && event.externalId.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  const handleSearch = async () => {
    setHasSearched(true);
    await searchEvents(eventNameQuery, whereQuery);
  };

  const handleLoadAll = async () => {
    setHasSearched(true);
    await browseAllEvents();
  };

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
      // 검색 조건이 유효할 때만 서버 재조회
      if (eventNameQuery.trim() && whereQuery.trim()) {
        setHasSearched(true);
        await searchEvents(eventNameQuery, whereQuery);
      }
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

      <GuidePanel
        title="이벤트 검색 가이드"
        description="조건 검색 또는 전체 조회로 이벤트를 불러올 수 있습니다."
        items={[
          '단일 조건: category&electronics&=&end',
          '다중 조건: category&electronics&=&and,brand&samsung&=&end',
          '범위 조건: amount&100&amount&200&between&end',
          'Load All로 전체 이벤트를 조건 없이 조회할 수 있습니다.',
          'Campaign 생성/관리는 Campaigns 화면에서 진행합니다.'
        ]}
        note="연산자: =, !=, >, >=, <, <=, like, between / 연결: and, or, end"
      />

      {/* 서버 검색 */}
      <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-4">
        <div className="grid gap-3 md:grid-cols-[1fr,2fr,auto,auto]">
          <Input
            label="Event Name"
            value={eventNameQuery}
            onChange={(e) => setEventNameQuery(e.target.value)}
            placeholder="view_product"
            required
          />
          <Input
            label="Where"
            value={whereQuery}
            onChange={(e) => setWhereQuery(e.target.value)}
            placeholder="category&electronics&=&end"
            required
          />
          <Button onClick={handleSearch} loading={loading} className="md:self-end">
            Search
          </Button>
          <Button variant="secondary" onClick={handleLoadAll} loading={loading} className="md:self-end">
            Load All
          </Button>
        </div>
        <p className="mt-2 text-xs text-slate-400">
          where 예시: <code>category&electronics&=&end</code>
        </p>
        <p className="mt-1 text-xs text-slate-500">
          검색 후 아래 결과를 필요하면 로컬 필터로 추가 좁힐 수 있습니다.
        </p>
      </div>

      {/* 로컬 필터 */}
      <div className="relative">
        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">🔍</span>
        <Input
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="Filter loaded events..."
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
                <tr
                  key={event.id}
                  className="cursor-pointer hover:bg-gray-800/50"
                  onClick={() => setSelectedEvent(event)}
                >
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
            ) : hasSearched ? (
              <tr>
                <td colSpan={4} className="px-6 py-8 text-center text-gray-400">
                  No events found
                </td>
              </tr>
            ) : (
              <tr>
                <td colSpan={4} className="px-6 py-8 text-center text-slate-400">
                  Event Name과 Where를 입력하고 Search를 눌러주세요.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <Modal
        isOpen={Boolean(selectedEvent)}
        onClose={() => setSelectedEvent(null)}
        title={selectedEvent ? `Event #${selectedEvent.id}` : 'Event Detail'}
        size="lg"
      >
        {selectedEvent && (
          <div className="space-y-4">
            <div className="grid grid-cols-1 gap-3 text-sm text-slate-200 md:grid-cols-2">
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Name</p>
                <p>{selectedEvent.name}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">External ID</p>
                <p>{selectedEvent.externalId || '-'}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Campaign</p>
                <p>{selectedEvent.campaignName || '-'}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Created</p>
                <p>{new Date(selectedEvent.createdAt).toLocaleString()}</p>
              </div>
            </div>

            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Properties</p>
              <pre className="mt-2 max-h-[260px] overflow-auto rounded-lg border border-slate-700 bg-slate-950/80 p-3 text-xs text-slate-200">
                {JSON.stringify(selectedEvent.properties, null, 2)}
              </pre>
            </div>

            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Raw JSON</p>
              <pre className="mt-2 max-h-[240px] overflow-auto rounded-lg border border-slate-700 bg-slate-950/80 p-3 text-xs text-slate-200">
                {JSON.stringify(selectedEvent, null, 2)}
              </pre>
            </div>
          </div>
        )}
      </Modal>

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
