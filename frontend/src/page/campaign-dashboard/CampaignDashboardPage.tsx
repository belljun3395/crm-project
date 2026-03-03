import React, { useEffect, useMemo, useState } from 'react';
import { Button, GuidePanel, Input } from 'common/component';
import { useCampaignDashboard, useCampaigns } from 'shared/hook';
import type { TimeWindowUnit } from 'shared/type';

const toLocalDateTime = (value: string): string | undefined => {
  if (!value) {
    return undefined;
  }
  return value.length === 16 ? `${value}:00` : value;
};

const parseCsvValues = (value: string): string[] => {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
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
    summary,
    streamStatus,
    funnelAnalytics,
    segmentComparison,
    liveEvents,
    loadingDashboard,
    loadingStreamStatus,
    loadingFunnelAnalytics,
    loadingSegmentComparison,
    error,
    connectionStatus,
    streamMessage,
    fetchDashboard,
    fetchSummary,
    fetchStreamStatus,
    fetchFunnelAnalytics,
    fetchSegmentComparison,
    connectStream,
    disconnectStream,
    clearLiveEvents
  } = useCampaignDashboard();
  const { campaigns, fetchCampaignDetail, detailLoadingId } = useCampaigns();

  const [campaignIdInput, setCampaignIdInput] = useState('');
  const [timeWindowUnit, setTimeWindowUnit] = useState<TimeWindowUnit>('HOUR');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [durationSeconds, setDurationSeconds] = useState('3600');
  const [funnelStepsInput, setFunnelStepsInput] = useState('signup,open,click');
  const [segmentIdsInput, setSegmentIdsInput] = useState('');
  const [comparisonEventName, setComparisonEventName] = useState('');

  const campaignId = Number(campaignIdInput);
  const isCampaignIdValid = Number.isInteger(campaignId) && campaignId > 0;

  useEffect(() => {
    if (!campaignIdInput && campaigns.length > 0) {
      setCampaignIdInput(String(campaigns[0].id));
    }
  }, [campaignIdInput, campaigns]);

  useEffect(() => {
    if (!isCampaignIdValid) {
      return;
    }
    void fetchCampaignDetail(campaignId).then((detail) => {
      if (!detail) {
        return;
      }
      setSegmentIdsInput(detail.segmentIds.join(','));
    });
  }, [campaignId, isCampaignIdValid, fetchCampaignDetail]);

  const parsedFunnelSteps = useMemo(() => parseCsvValues(funnelStepsInput), [funnelStepsInput]);
  const parsedSegmentIds = useMemo(
    () =>
      parseCsvValues(segmentIdsInput)
        .map((value) => Number(value))
        .filter((id): id is number => Number.isInteger(id) && id > 0),
    [segmentIdsInput]
  );

  const sortedMetrics = useMemo(() => {
    if (!dashboard?.metrics) {
      return [];
    }
    return [...dashboard.metrics].sort((a, b) => b.timeWindowStart.localeCompare(a.timeWindowStart));
  }, [dashboard]);
  const summaryData = summary ?? dashboard?.summary;

  const getRangeParams = () => {
    const normalizedStartTime = toLocalDateTime(startTime);
    const normalizedEndTime = toLocalDateTime(endTime);
    return {
      startTime: normalizedStartTime,
      endTime: normalizedEndTime
    };
  };

  const handleRefresh = async () => {
    if (!isCampaignIdValid) {
      return;
    }

    const rangeParams = getRangeParams();

    const jobs: Array<Promise<boolean>> = [
      fetchDashboard(campaignId, {
        ...rangeParams,
        timeWindowUnit
      }),
      fetchSummary(campaignId),
      fetchStreamStatus(campaignId)
    ];

    if (parsedFunnelSteps.length >= 2) {
      jobs.push(
        fetchFunnelAnalytics(campaignId, {
          steps: parsedFunnelSteps,
          ...rangeParams
        })
      );
    }

    if (parsedSegmentIds.length > 0) {
      jobs.push(
        fetchSegmentComparison(campaignId, {
          segmentIds: parsedSegmentIds,
          eventName: comparisonEventName.trim() || undefined,
          ...rangeParams
        })
      );
    }

    await Promise.all(jobs);
  };

  const handleFetchFunnelAnalytics = async () => {
    if (!isCampaignIdValid || parsedFunnelSteps.length < 2) {
      return;
    }
    await fetchFunnelAnalytics(campaignId, {
      steps: parsedFunnelSteps,
      ...getRangeParams()
    });
  };

  const handleFetchSegmentComparison = async () => {
    if (!isCampaignIdValid || parsedSegmentIds.length === 0) {
      return;
    }
    await fetchSegmentComparison(campaignId, {
      segmentIds: parsedSegmentIds,
      eventName: comparisonEventName.trim() || undefined,
      ...getRangeParams()
    });
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
        description="특정 캠페인의 성과를 기간별로 보고, 실시간 이벤트 흐름과 퍼널/세그먼트 전환율까지 함께 분석하는 화면입니다."
        items={[
          'Campaign 목록에서 대상을 선택한 뒤 Refresh를 눌러 최신 집계를 조회합니다.',
          'Funnel Steps에 이벤트 이름을 순서대로 입력하고 Load Funnel을 누르면 단계별 전환율이 계산됩니다.',
          'Segment IDs와 Event Name을 지정하면 세그먼트별 전환율을 비교할 수 있습니다.'
        ]}
        note="실시간 연결이 길어지면 Disconnect로 종료하고 다시 연결할 수 있습니다."
      />

      <div className="rounded-xl border border-gray-800 bg-gray-900 p-4">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-5">
          <div>
            <label className="mb-2 block text-sm font-medium text-gray-300">Campaign</label>
            <select
              value={campaignIdInput}
              onChange={(e) => setCampaignIdInput(e.target.value)}
              className="w-full rounded-lg border border-gray-700 bg-gray-800 px-4 py-2 text-white focus:border-[#22c55e] focus:outline-none focus:ring-2 focus:ring-[#22c55e]"
            >
              {campaigns.length === 0 ? (
                <option value="">No campaigns</option>
              ) : (
                campaigns.map((campaign) => (
                  <option key={campaign.id} value={campaign.id}>
                    #{campaign.id} - {campaign.name}
                  </option>
                ))
              )}
            </select>
          </div>
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
          <p className="mt-2 text-2xl font-bold text-white">{summaryData?.totalEvents ?? '-'}</p>
        </div>
        <div className="rounded-xl border border-gray-800 bg-gray-900 p-4">
          <p className="text-sm text-gray-400">Last 24 Hours</p>
          <p className="mt-2 text-2xl font-bold text-white">{summaryData?.eventsLast24Hours ?? '-'}</p>
        </div>
        <div className="rounded-xl border border-gray-800 bg-gray-900 p-4">
          <p className="text-sm text-gray-400">Last 7 Days</p>
          <p className="mt-2 text-2xl font-bold text-white">{summaryData?.eventsLast7Days ?? '-'}</p>
        </div>
        <div className="rounded-xl border border-gray-800 bg-gray-900 p-4">
          <p className="text-sm text-gray-400">Stream Length</p>
          <p className="mt-2 text-2xl font-bold text-white">{streamStatus?.streamLength ?? '-'}</p>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
        <section className="rounded-xl border border-gray-800 bg-gray-900 p-4">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-xl font-semibold text-white">Funnel Conversion</h2>
            <Button
              onClick={handleFetchFunnelAnalytics}
              loading={loadingFunnelAnalytics}
              disabled={!isCampaignIdValid || parsedFunnelSteps.length < 2}
            >
              Load Funnel
            </Button>
          </div>

          <Input
            label="Funnel Steps (comma separated)"
            value={funnelStepsInput}
            onChange={(e) => setFunnelStepsInput(e.target.value)}
            placeholder="signup,open,click"
          />

          <p className="mt-2 text-xs text-gray-400">
            Step은 2개 이상 필요합니다. 현재 {parsedFunnelSteps.length}개 입력됨
          </p>

          <div className="mt-4 max-h-[320px] overflow-auto rounded-lg border border-gray-800">
            <table className="min-w-full divide-y divide-gray-800">
              <thead className="bg-gray-800/60">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-300">Step</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-300">Events</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-300">Qualified Users</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-300">Conversion (%)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-800">
                {loadingFunnelAnalytics ? (
                  <tr>
                    <td colSpan={4} className="px-4 py-6 text-center text-sm text-gray-400">Loading funnel analytics...</td>
                  </tr>
                ) : !funnelAnalytics || funnelAnalytics.stepMetrics.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="px-4 py-6 text-center text-sm text-gray-400">No funnel data</td>
                  </tr>
                ) : (
                  funnelAnalytics.stepMetrics.map((stepMetric) => (
                    <tr key={stepMetric.step} className="hover:bg-gray-800/40">
                      <td className="px-4 py-3 text-sm text-white">{stepMetric.step}</td>
                      <td className="px-4 py-3 text-sm text-gray-300">{stepMetric.eventCount}</td>
                      <td className="px-4 py-3 text-sm text-gray-300">{stepMetric.qualifiedUserCount}</td>
                      <td className="px-4 py-3 text-sm text-gray-300">{stepMetric.conversionFromPrevious.toFixed(2)}%</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="rounded-xl border border-gray-800 bg-gray-900 p-4">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-xl font-semibold text-white">Segment Comparison</h2>
            <Button
              onClick={handleFetchSegmentComparison}
              loading={loadingSegmentComparison}
              disabled={!isCampaignIdValid || parsedSegmentIds.length === 0}
            >
              Compare
            </Button>
          </div>

          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <Input
              label="Segment IDs (comma separated)"
              value={segmentIdsInput}
              onChange={(e) => setSegmentIdsInput(e.target.value)}
              placeholder="1,2,3"
            />
            <Input
              label="Event Name (optional)"
              value={comparisonEventName}
              onChange={(e) => setComparisonEventName(e.target.value)}
              placeholder="purchase"
            />
          </div>

          {detailLoadingId === campaignId && (
            <p className="mt-2 text-xs text-gray-400">Loading default segment IDs from campaign...</p>
          )}

          <p className="mt-2 text-xs text-gray-400">
            현재 {parsedSegmentIds.length}개 세그먼트 선택됨
          </p>

          {segmentComparison?.eventName && (
            <p className="mt-2 text-xs text-gray-400">
              Filter Event: <span className="text-gray-200">{segmentComparison.eventName}</span>
            </p>
          )}

          <div className="mt-4 max-h-[320px] overflow-auto rounded-lg border border-gray-800">
            <table className="min-w-full divide-y divide-gray-800">
              <thead className="bg-gray-800/60">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-300">Segment</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-300">Target Users</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-300">Event Users</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-300">Event Count</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-300">Conversion (%)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-800">
                {loadingSegmentComparison ? (
                  <tr>
                    <td colSpan={5} className="px-4 py-6 text-center text-sm text-gray-400">Loading segment comparison...</td>
                  </tr>
                ) : !segmentComparison || segmentComparison.segmentMetrics.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-4 py-6 text-center text-sm text-gray-400">No segment comparison data</td>
                  </tr>
                ) : (
                  segmentComparison.segmentMetrics.map((metric) => (
                    <tr key={metric.segmentId} className="hover:bg-gray-800/40">
                      <td className="px-4 py-3 text-sm text-white">
                        {metric.segmentName ? `${metric.segmentName} (#${metric.segmentId})` : `#${metric.segmentId}`}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-300">{metric.targetUserCount}</td>
                      <td className="px-4 py-3 text-sm text-gray-300">{metric.eventUserCount}</td>
                      <td className="px-4 py-3 text-sm text-gray-300">{metric.eventCount}</td>
                      <td className="px-4 py-3 text-sm text-gray-300">{metric.conversionRate.toFixed(2)}%</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
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
