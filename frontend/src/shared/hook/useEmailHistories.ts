import { useCallback, useEffect, useState } from 'react';
import { emailHistoryAPI } from 'shared/api';
import type { EmailSendHistoryDto } from 'shared/type';

export const useEmailHistories = () => {
  const [histories, setHistories] = useState<EmailSendHistoryDto[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchHistories = useCallback(async (params?: {
    userId?: number;
    sendStatus?: string;
    page?: number;
    size?: number;
  }): Promise<void> => {
    setLoading(true);
    setError(null);

    try {
      const response = await emailHistoryAPI.getHistories(params);
      if (!response) {
        setHistories([]);
        setTotalCount(0);
        return;
      }

      setHistories(response.histories);
      setTotalCount(response.totalCount);
      setPage(response.page);
      setSize(response.size);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to fetch email histories';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchHistories({ page: 0, size: 20 });
  }, [fetchHistories]);

  return {
    histories,
    totalCount,
    page,
    size,
    loading,
    error,
    fetchHistories
  };
};
