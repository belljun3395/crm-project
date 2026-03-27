import React, { useState } from 'react';
import { Button, GuidePanel, Input, Modal } from 'common/component';
import { useToggle } from 'common/hook';
import { useEvents } from 'shared/hook';
import type { EventFormData } from 'shared/type';

type Operator = '=' | '!=' | '>' | '>=' | '<' | '<=' | 'like' | 'between';
type JoinOp = 'and' | 'or';

interface WhereCondition {
  key: string;
  operator: Operator;
  value: string;
  value2: string; // BETWEEN 전용
  joinOperation: JoinOp; // 마지막 조건 제외한 연결 연산자
}

const OPERATORS: { label: string; value: Operator }[] = [
  { label: '=', value: '=' },
  { label: '≠', value: '!=' },
  { label: '>', value: '>' },
  { label: '≥', value: '>=' },
  { label: '<', value: '<' },
  { label: '≤', value: '<=' },
  { label: 'LIKE', value: 'like' },
  { label: 'BETWEEN', value: 'between' },
];

function buildWhereString(conditions: WhereCondition[]): string {
  const filled = conditions.filter(c => c.key.trim() && c.value.trim());
  if (filled.length === 0) return '';
  return filled
    .map((c, i) => {
      const join = i < filled.length - 1 ? c.joinOperation : 'end';
      if (c.operator === 'between') {
        return `${c.key}&${c.value}&${c.key}&${c.value2}&between&${join}`;
      }
      return `${c.key}&${c.value}&${c.operator}&${join}`;
    })
    .join(',');
}

function emptyCondition(): WhereCondition {
  return { key: '', operator: '=', value: '', value2: '', joinOperation: 'and' };
}

export const EventPage: React.FC = () => {
  const { events, loading, error, createEvent, browseAllEvents, searchEvents } = useEvents();
  const { value: isModalOpen, setTrue: openModal, setFalse: closeModal } = useToggle();
  const [eventNameQuery, setEventNameQuery] = useState('');
  const [conditions, setConditions] = useState<WhereCondition[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [hasSearched, setHasSearched] = useState(false);
  const [formData, setFormData] = useState<EventFormData>({
    name: '',
    campaignName: '',
    externalId: '',
    properties: [{ key: '', value: '' }]
  });
  const [selectedEvent, setSelectedEvent] = useState<(typeof events)[number] | null>(null);

  const whereString = buildWhereString(conditions);

  const filteredEvents = events.filter(event =>
    event.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (event.externalId && event.externalId.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  const handleSearch = async () => {
    setHasSearched(true);
    await searchEvents(eventNameQuery, whereString);
  };

  const handleLoadAll = async () => {
    setHasSearched(true);
    await browseAllEvents();
  };

  const handleSubmit = async () => {
    if (!formData.name || !formData.externalId) return;
    const success = await createEvent(formData);
    if (success) {
      setFormData({ name: '', campaignName: '', externalId: '', properties: [{ key: '', value: '' }] });
      closeModal();
      if (eventNameQuery.trim()) {
        setHasSearched(true);
        await searchEvents(eventNameQuery, whereString);
      }
    }
  };

  const addCondition = () => {
    setConditions(prev => [...prev, emptyCondition()]);
  };

  const removeCondition = (index: number) => {
    setConditions(prev => prev.filter((_, i) => i !== index));
  };

  const updateCondition = (index: number, patch: Partial<WhereCondition>) => {
    setConditions(prev =>
      prev.map((c, i) => (i === index ? { ...c, ...patch } : c))
    );
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
        description="Event Name만 입력해도 조회할 수 있습니다. Where 조건은 선택 사항입니다."
        items={[
          'Event Name 입력 후 조건 없이 Search → 이름만으로 전체 조회',
          '+ Add Condition으로 조건 추가, 조건이 여러 개면 AND/OR로 연결',
          'BETWEEN 선택 시 값 범위 입력 필드(~)가 추가됩니다',
          'Load All로 이름 무관하게 전체 이벤트 조회',
          'Campaign 생성/관리는 Campaigns 화면에서 진행합니다.',
        ]}
        note="연산자: =, ≠, >, ≥, <, ≤, LIKE, BETWEEN"
      />

      {/* 서버 검색 */}
      <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-4 space-y-3">
        {/* Event Name + 액션 버튼 */}
        <div className="flex gap-3 items-end">
          <div className="flex-1">
            <Input
              label="Event Name"
              value={eventNameQuery}
              onChange={(e) => setEventNameQuery(e.target.value)}
              placeholder="view_product"
              required
            />
          </div>
          <Button onClick={handleSearch} loading={loading}>
            Search
          </Button>
          <Button variant="secondary" onClick={handleLoadAll} loading={loading}>
            Load All
          </Button>
        </div>

        {/* Where 조건 빌더 */}
        {conditions.length > 0 && (
          <div className="space-y-2">
            <p className="text-xs font-medium text-slate-400 uppercase tracking-wide">Where 조건</p>
            {conditions.map((cond, i) => (
              <div key={i} className="flex items-center gap-2 flex-wrap">
                {/* AND/OR 연결 (첫 번째 제외) */}
                {i > 0 && conditions[i - 1] && (
                  <select
                    value={conditions[i - 1]!.joinOperation}
                    onChange={(e) => updateCondition(i - 1, { joinOperation: e.target.value as JoinOp })}
                    className="rounded border border-slate-600 bg-slate-800 px-2 py-1.5 text-xs text-indigo-300 font-semibold"
                  >
                    <option value="and">AND</option>
                    <option value="or">OR</option>
                  </select>
                )}

                {/* Key */}
                <input
                  value={cond.key}
                  onChange={(e) => updateCondition(i, { key: e.target.value })}
                  placeholder="key"
                  className="w-28 rounded border border-slate-600 bg-slate-800 px-2 py-1.5 text-sm text-slate-200 placeholder:text-slate-500"
                />

                {/* Operator */}
                <select
                  value={cond.operator}
                  onChange={(e) => updateCondition(i, { operator: e.target.value as Operator, value2: '' })}
                  className="rounded border border-slate-600 bg-slate-800 px-2 py-1.5 text-sm text-slate-200"
                >
                  {OPERATORS.map(op => (
                    <option key={op.value} value={op.value}>{op.label}</option>
                  ))}
                </select>

                {/* Value */}
                <input
                  value={cond.value}
                  onChange={(e) => updateCondition(i, { value: e.target.value })}
                  placeholder={cond.operator === 'between' ? 'from' : 'value'}
                  className="w-28 rounded border border-slate-600 bg-slate-800 px-2 py-1.5 text-sm text-slate-200 placeholder:text-slate-500"
                />

                {/* Value2 (BETWEEN 전용) */}
                {cond.operator === 'between' && (
                  <>
                    <span className="text-slate-500 text-sm">~</span>
                    <input
                      value={cond.value2}
                      onChange={(e) => updateCondition(i, { value2: e.target.value })}
                      placeholder="to"
                      className="w-28 rounded border border-slate-600 bg-slate-800 px-2 py-1.5 text-sm text-slate-200 placeholder:text-slate-500"
                    />
                  </>
                )}

                {/* 삭제 */}
                <button
                  onClick={() => removeCondition(i)}
                  className="text-slate-500 hover:text-red-400 text-sm px-1"
                  title="조건 삭제"
                >
                  ✕
                </button>
              </div>
            ))}
          </div>
        )}

        {/* 조건 추가 버튼 */}
        <button
          onClick={addCondition}
          className="text-xs text-slate-400 hover:text-slate-200 border border-dashed border-slate-600 hover:border-slate-400 rounded px-3 py-1.5 transition-colors"
        >
          + Add Condition
        </button>

        {/* 생성된 DSL 미리보기 */}
        {whereString && (
          <p className="text-xs text-slate-500 font-mono break-all">
            <span className="text-slate-600">where: </span>
            <span className="text-blue-400">{whereString}</span>
          </p>
        )}

        <p className="text-xs text-slate-600">
          검색 후 아래 결과를 로컬 필터로 추가 좁힐 수 있습니다.
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
                  Event Name을 입력하고 Search를 눌러주세요.
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
