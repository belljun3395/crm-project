import React, { useMemo, useState } from 'react';
import { Button, GuidePanel, Input } from 'common/component';
import { useCampaignDashboard } from 'shared/hook';
import type { TimeWindowUnit } from 'shared/type';

const toLocalDateTime = (value: string): string | undefined => {
  if (!value) {
    return undefined;
  }
  return value.length === 16 ? `${value}:00` : value;
};

const statusBadgeClass: Record<string, string> = {
  idle: 'bg-gray-700 text-gray-100',
  connecting: 'bg-yellow-700 text-yellow-100',
  connected: 'bg-green-700 text-green-100',
  reconnecting: 'bg-orange-700 text-orange-100',
  error: 'bg-red-700 text-red-100',
  closed: 'bg-gray-700 text-gray-100'
};

export const CampaignDashboardPage: React.FC = () => {
  const {
    dashboard,
    streamStatus,
    liveEvents,
    loadingDashboard,
    loadingStreamStatus,
    error,
    connectionStatus,
    streamMessage,
    fetchDashboard,
    fetchStreamStatus,
    connectStream,
    disconnectStream,
    clearLiveEvents
  } = useCampaignDashboard();

  const [campaignIdInput, setCampaignIdInput] = useState('1');
  const [timeWindowUnit, setTimeWindowUnit] = useState<TimeWindowUnit>('HOUR');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [durationSeconds, setDurationSeconds] = useState('3600');

  const campaignId = Number(campaignIdInput);
  const isCampaignIdValid = Number.isInteger(campaignId) && campaignId > 0;

  const sortedMetrics = useMemo(() => {
    if (!dashboard?.metrics) {
      return [];
    }
    return [...dashboard.metrics].sort((a, b) => b.timeWindowStart.localeCompare(a.timeWindowStart));
  }, [dashboard]);

  const handleRefresh = async () => {
    if (!isCampaignIdValid) {
      return;
    }

    const params: {
      startTime?: string;
      endTime?: string;
      timeWindowUnit?: TimeWindowUnit;
    } = {
      timeWindowUnit
    };

    const normalizedStartTime = toLocalDateTime(startTime);
    const normalizedEndTime = toLocalDateTime(endTime);
    if (normalizedStartTime) {
      params.startTime = normalizedStartTime;
    }
    if (normalizedEndTime) {
      params.endTime = normalizedEndTime;
    }

    await Promise.all([
      fetchDashboard(campaignId, params),
      fetchStreamStatus(campaignId)
    ]);
  };

  const handleConnectStream = () => {
    if (!isCampaignIdValid) {
      return;
    }
    const parsedDuration = Number(durationSeconds);
    connectStream(campaignId, Number.isFinite(parsedDuration) && parsedDuration > 0 ? parsedDuration : 3600);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">Campaign Dashboard</h1>
        <div className="flex gap-2">
          <Button onClick={handleRefresh} loading={loadingDashboard || loadingStreamStatus} disabled={!isCampaignIdValid}>
            Refresh
          </Button>
        </div>
      </div>

      {error && (
        <div className="rounded-lg border border-red-700 bg-red-900/40 p-3 text-sm text-red-200">
          {error}
        </div>
      )}

      <GuidePanel
        description="특정 캠페인의 성과를 기간별로 보고, 실시간 이벤트 흐름까지 확인하는 화면입니다."
        items={[
          '먼저 Campaign ID를 입력한 뒤 Refresh를 눌러 최신 집계를 조회합니다.',
          'Connect를 누르면 실시간 이벤트가 Live Stream에 쌓입니다.',
          'Start/End Time을 채우면 원하는 기간만 좁혀서 볼 수 있습니다.'
        ]}
        note="실시간 연결이 길어지면 Disconnect로 종료하고 다시 연결할 수 있습니다."
      />

      <div className="rounded-xl border border-gray-800 bg-gray-900 p-4">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-5">
          <Input
            label="Campaign ID"
            type="number"
            value={campaignIdInput}
            onChange={(e) => setCampaignIdInput(e.target.value)}
            placeholder="Campaign ID"
          />
          <div>
            <label className="mb-2 block text-sm font-medium text-gray-300">Time Window</label>
            <select
              value={timeWindowUnit}
              onChange={(e) => setTimeWindowUnit(e.target.value as TimeWindowUnit)}
              className="w-full rounded-lg border border-gray-700 bg-gray-800 px-4 py-2 text-white focus:border-[#22c55e] focus:outline-none focus:ring-2 focus:ring-[#22c55e]"
            >
              <option value="MINUTE">MINUTE</option>
              <option value="HOUR">HOUR</option>
              <option value="DAY">DAY</option>
              <option value="WEEK">WEEK</option>
              <option value="MONTH">MONTH</option>
            </select>
          </div>
          <Input
            label="Start Time"
            type="datetime-local"
            value={startTime}
            onChange={(e) => setStartTime(e.target.value)}
          />
          <Input
            label="End Time"
            type="datetime-local"
            value={endTime}
            onChange={(e) => setEndTime(e.target.value)}
          />
          <Input
            label="SSE Duration (sec)"
            type="number"
            value={durationSeconds}
            onChange={(e) => setDurationSeconds(e.target.value)}
            placeholder="3600"
          />
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-4">
        <div className="rounded-xl border border-gray-800 bg-gray-900 p-4">
          <p className="text-sm text-gray-400">Total Events</p>
          <p className="mt-2 text-2xl font-bold text-white">{dashboard?.summary.totalEvents ?? '-'}</p>
        </div>
        <div className="rounded-xl border border-gray-800 bg-gray-900 p-4">
          <p className="text-sm text-gray-400">Last 24 Hours</p>
          <p className="mt-2 text-2xl font-bold text-white">{dashboard?.summary.eventsLast24Hours ?? '-'}</p>
        </div>
        <div className="rounded-xl border border-gray-800 bg-gray-900 p-4">
          <p className="text-sm text-gray-400">Last 7 Days</p>
          <p className="mt-2 text-2xl font-bold text-white">{dashboard?.summary.eventsLast7Days ?? '-'}</p>
        </div>
        <div className="rounded-xl border border-gray-800 bg-gray-900 p-4">
          <p className="text-sm text-gray-400">Stream Length</p>
          <p className="mt-2 text-2xl font-bold text-white">{streamStatus?.streamLength ?? '-'}</p>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
        <section className="rounded-xl border border-gray-800 bg-gray-900 p-4">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-xl font-semibold text-white">Live Stream</h2>
            <span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusBadgeClass[connectionStatus]}`}>
              {connectionStatus}
            </span>
          </div>

          <p className="mb-4 text-sm text-gray-400">{streamMessage || 'Stream is idle'}</p>

          <div className="mb-4 flex flex-wrap gap-2">
            <Button onClick={handleConnectStream} disabled={!isCampaignIdValid || connectionStatus === 'connected'}>
              Connect
            </Button>
            <Button variant="secondary" onClick={disconnectStream}>
              Disconnect
            </Button>
            <Button variant="secondary" onClick={clearLiveEvents}>
              Clear Events
            </Button>
          </div>

          <div className="max-h-[360px] space-y-2 overflow-y-auto rounded-lg border border-gray-800 bg-gray-950 p-3">
            {liveEvents.length === 0 ? (
              <p className="text-sm text-gray-500">No live events yet</p>
            ) : (
              liveEvents.map((event, index) => (
                <div key={`${event.eventId}-${event.timestamp}-${index}`} className="rounded-md border border-gray-800 bg-gray-900 p-3">
                  <div className="text-sm text-white">
                    <span className="font-semibold">{event.eventName}</span>
                    <span className="ml-2 text-gray-400">eventId={event.eventId}</span>
                  </div>
                  <p className="mt-1 text-xs text-gray-400">userId={event.userId}</p>
                  <p className="text-xs text-gray-500">{new Date(event.timestamp).toLocaleString()}</p>
                </div>
              ))
            )}
          </div>
        </section>

        <section className="rounded-xl border border-gray-800 bg-gray-900 p-4">
          <h2 className="mb-4 text-xl font-semibold text-white">Metrics</h2>

          <div className="max-h-[460px] overflow-auto rounded-lg border border-gray-800">
            <table className="min-w-full divide-y divide-gray-800">
              <thead className="bg-gray-800/60">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-300">Type</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-300">Value</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-300">Window</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-300">Start</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-800">
                {loadingDashboard ? (
                  <tr>
                    <td colSpan={4} className="px-4 py-6 text-center text-sm text-gray-400">Loading metrics...</td>
                  </tr>
                ) : sortedMetrics.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="px-4 py-6 text-center text-sm text-gray-400">No metrics found</td>
                  </tr>
                ) : (
                  sortedMetrics.map((metric, index) => (
                    <tr key={`${metric.id ?? index}-${metric.timeWindowStart}`} className="hover:bg-gray-800/40">
                      <td className="px-4 py-3 text-sm text-white">{metric.metricType}</td>
                      <td className="px-4 py-3 text-sm text-gray-300">{metric.metricValue}</td>
                      <td className="px-4 py-3 text-sm text-gray-300">{metric.timeWindowUnit}</td>
                      <td className="px-4 py-3 text-sm text-gray-400">{new Date(metric.timeWindowStart).toLocaleString()}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  );
};
