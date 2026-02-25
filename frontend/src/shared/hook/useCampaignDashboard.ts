import { useCallback, useEffect, useRef, useState } from 'react';
import { campaignAPI } from 'shared/api';
import type {
  CampaignEventData,
  GetCampaignDashboardUseCaseOut,
  StreamConnectionStatus,
  StreamStatusResponse,
  TimeWindowUnit
} from 'shared/type';

const MAX_LIVE_EVENTS = 50;

interface DashboardQueryParams {
  startTime?: string;
  endTime?: string;
  timeWindowUnit?: TimeWindowUnit;
}

export const useCampaignDashboard = () => {
  const [dashboard, setDashboard] = useState<GetCampaignDashboardUseCaseOut | null>(null);
  const [streamStatus, setStreamStatus] = useState<StreamStatusResponse | null>(null);
  const [liveEvents, setLiveEvents] = useState<CampaignEventData[]>([]);

  const [loadingDashboard, setLoadingDashboard] = useState(false);
  const [loadingStreamStatus, setLoadingStreamStatus] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [connectionStatus, setConnectionStatus] = useState<StreamConnectionStatus>('idle');
  const [streamMessage, setStreamMessage] = useState<string>('');

  const eventSourceRef = useRef<EventSource | null>(null);
  const lastEventIdRef = useRef<string | null>(null);

  const fetchDashboard = useCallback(
    async (campaignId: number, params?: DashboardQueryParams): Promise<boolean> => {
      setLoadingDashboard(true);
      setError(null);
      try {
        const data = await campaignAPI.getDashboard(campaignId, params);
        if (!data) {
          setError('Failed to load campaign dashboard data');
          return false;
        }
        setDashboard(data);
        return true;
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Failed to load campaign dashboard data';
        setError(message);
        return false;
      } finally {
        setLoadingDashboard(false);
      }
    },
    []
  );

  const fetchStreamStatus = useCallback(async (campaignId: number): Promise<boolean> => {
    setLoadingStreamStatus(true);
    setError(null);
    try {
      const data = await campaignAPI.getStreamStatus(campaignId);
      if (!data) {
        setError('Failed to load stream status');
        return false;
      }
      setStreamStatus(data);
      return true;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load stream status';
      setError(message);
      return false;
    } finally {
      setLoadingStreamStatus(false);
    }
  }, []);

  const disconnectStream = useCallback(() => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }
    setConnectionStatus('closed');
    setStreamMessage('Stream disconnected');
  }, []);

  const connectStream = useCallback(
    (campaignId: number, durationSeconds: number = 3600) => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }

      setConnectionStatus('connecting');
      setStreamMessage('Connecting to stream...');

      const streamParams: {
        durationSeconds?: number;
        lastEventId?: string;
      } = {
        durationSeconds
      };
      if (lastEventIdRef.current) {
        streamParams.lastEventId = lastEventIdRef.current;
      }

      const streamUrl = campaignAPI.getStreamUrl(campaignId, {
        ...streamParams
      });

      const eventSource = new EventSource(streamUrl);
      eventSourceRef.current = eventSource;

      eventSource.onopen = () => {
        setConnectionStatus('connected');
        setStreamMessage('Connected');
      };

      eventSource.addEventListener('campaign-event', (event) => {
        try {
          const message = event as MessageEvent<string>;
          const parsedEvent = JSON.parse(message.data) as CampaignEventData;
          if (message.lastEventId) {
            lastEventIdRef.current = message.lastEventId;
          }
          setLiveEvents((prev) => [parsedEvent, ...prev].slice(0, MAX_LIVE_EVENTS));
        } catch (err) {
          console.error('Failed to parse campaign stream event:', err);
        }
      });

      eventSource.addEventListener('stream-end', () => {
        setConnectionStatus('closed');
        setStreamMessage('Stream ended by server');
        eventSource.close();
      });

      eventSource.onerror = () => {
        if (eventSource.readyState === EventSource.CLOSED) {
          setConnectionStatus('error');
          setStreamMessage('Stream closed due to connection error');
        } else {
          setConnectionStatus('reconnecting');
          setStreamMessage('Connection lost. Reconnecting...');
        }
      };
    },
    []
  );

  const clearLiveEvents = useCallback(() => {
    setLiveEvents([]);
    lastEventIdRef.current = null;
  }, []);

  useEffect(() => {
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
    };
  }, []);

  return {
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
  };
};
