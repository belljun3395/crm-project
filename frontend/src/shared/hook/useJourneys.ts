import { useCallback, useEffect, useMemo, useState } from 'react';
import { journeyAPI } from 'shared/api';
import type {
  CreateJourneyRequest,
  Journey,
  JourneyExecution,
  JourneyExecutionHistory
} from 'shared/type';

export const useJourneys = () => {
  const [journeys, setJourneys] = useState<Journey[]>([]);
  const [executions, setExecutions] = useState<JourneyExecution[]>([]);
  const [executionHistories, setExecutionHistories] = useState<Record<number, JourneyExecutionHistory[]>>({});
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchJourneys = useCallback(async (): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const data = await journeyAPI.getJourneys();
      setJourneys(data);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to fetch journeys';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchExecutions = useCallback(async (params?: {
    journeyId?: number;
    eventId?: number;
    userId?: number;
  }): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const data = await journeyAPI.getExecutions(params);
      setExecutions(data);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to fetch journey executions';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchExecutionHistories = useCallback(async (executionId: number): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const data = await journeyAPI.getExecutionHistories(executionId);
      setExecutionHistories((prev) => ({ ...prev, [executionId]: data }));
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to fetch execution histories';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  const createJourney = useCallback(async (payload: CreateJourneyRequest): Promise<boolean> => {
    setSaving(true);
    setError(null);
    try {
      const created = await journeyAPI.createJourney(payload);
      if (!created) {
        setError('Failed to create journey');
        return false;
      }
      await fetchJourneys();
      return true;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to create journey';
      setError(message);
      return false;
    } finally {
      setSaving(false);
    }
  }, [fetchJourneys]);

  const sortedJourneys = useMemo(() => {
    return [...journeys].sort((a, b) => b.id - a.id);
  }, [journeys]);

  const sortedExecutions = useMemo(() => {
    return [...executions].sort((a, b) => b.id - a.id);
  }, [executions]);

  useEffect(() => {
    fetchJourneys();
    fetchExecutions();
  }, [fetchJourneys, fetchExecutions]);

  return {
    journeys: sortedJourneys,
    executions: sortedExecutions,
    executionHistories,
    loading,
    saving,
    error,
    fetchJourneys,
    fetchExecutions,
    fetchExecutionHistories,
    createJourney
  };
};
