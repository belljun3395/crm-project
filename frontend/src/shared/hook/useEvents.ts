import { useState, useCallback, useRef, useEffect } from 'react';
import { eventAPI } from 'shared/api';
import type { Event, CreateEventRequest, CreateCampaignRequest } from 'shared/type';

export const useEvents = () => {
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  // 이벤트 검색
  const searchEvents = useCallback(async (eventName: string, where: string) => {
    // Cancel previous request if still pending
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    
    const abortController = new AbortController();
    abortControllerRef.current = abortController;
    
    setLoading(true);
    setError(null);
    try {
      const data = await eventAPI.searchEvents(eventName, where);
      if (!abortController.signal.aborted) {
        setEvents(data);
        return data;
      }
      return [];
    } catch (err) {
      if (!abortController.signal.aborted) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to search events';
        setError(errorMessage);
        console.error('Failed to search events:', err);
      }
      return [];
    } finally {
      if (!abortController.signal.aborted) {
        setLoading(false);
      }
    }
  }, []);

  // 이벤트 생성
  const createEvent = useCallback(async (eventData: CreateEventRequest): Promise<boolean> => {
    setLoading(true);
    setError(null);
    try {
      const result = await eventAPI.postEvent(eventData);
      if (result) {
        // 성공 시 이벤트 목록 갱신을 위해 다시 검색할 수 있음
        return true;
      }
      return false;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to create event';
      setError(errorMessage);
      console.error('Failed to create event:', err);
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  // 캠페인 생성
  const createCampaign = useCallback(async (campaignData: CreateCampaignRequest): Promise<boolean> => {
    setLoading(true);
    setError(null);
    try {
      const result = await eventAPI.postCampaign(campaignData);
      if (result) {
        return true;
      }
      return false;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to create campaign';
      setError(errorMessage);
      console.error('Failed to create campaign:', err);
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, []);

  return {
    events,
    loading,
    error,
    searchEvents,
    createEvent,
    createCampaign,
  };
};