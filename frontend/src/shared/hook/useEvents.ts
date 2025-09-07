import { useState, useCallback } from 'react';
import { eventAPI } from 'shared/api';
import type { Event, CreateEventRequest, CreateCampaignRequest } from 'shared/type';

export const useEvents = () => {
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 이벤트 검색
  const searchEvents = useCallback(async (eventName: string, where: string) => {
    setLoading(true);
    setError(null);
    try {
      const data = await eventAPI.searchEvents(eventName, where);
      setEvents(data);
      return data;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to search events');
      return [];
    } finally {
      setLoading(false);
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
      setError(err instanceof Error ? err.message : 'Failed to create event');
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
      setError(err instanceof Error ? err.message : 'Failed to create campaign');
      return false;
    } finally {
      setLoading(false);
    }
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