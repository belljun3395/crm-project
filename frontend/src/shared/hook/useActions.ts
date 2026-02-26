import { useCallback, useEffect, useMemo, useState } from 'react';
import { actionAPI } from 'shared/api';
import type {
  ActionDispatchHistory,
  ActionDispatchRequest
} from 'shared/type';

export const useActions = () => {
  const [histories, setHistories] = useState<ActionDispatchHistory[]>([]);
  const [loading, setLoading] = useState(false);
  const [dispatching, setDispatching] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchHistories = useCallback(async (params?: {
    campaignId?: number;
    journeyExecutionId?: number;
  }): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const data = await actionAPI.getDispatchHistories(params);
      setHistories(data);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to fetch action dispatch histories';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  const dispatchAction = useCallback(async (payload: ActionDispatchRequest): Promise<boolean> => {
    setDispatching(true);
    setError(null);
    try {
      const result = await actionAPI.dispatch(payload);
      if (!result) {
        setError('Failed to dispatch action');
        return false;
      }
      await fetchHistories();
      return true;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to dispatch action';
      setError(message);
      return false;
    } finally {
      setDispatching(false);
    }
  }, [fetchHistories]);

  const sortedHistories = useMemo(() => {
    return [...histories].sort((a, b) => b.id - a.id);
  }, [histories]);

  useEffect(() => {
    fetchHistories();
  }, [fetchHistories]);

  return {
    histories: sortedHistories,
    loading,
    dispatching,
    error,
    fetchHistories,
    dispatchAction
  };
};
