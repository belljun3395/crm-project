import { useCallback, useEffect, useMemo, useState } from 'react';
import { auditAPI } from 'shared/api';
import type { AuditLog } from 'shared/type';

export const useAuditLogs = () => {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchLogs = useCallback(async (params?: {
    limit?: number;
    action?: string;
    resourceType?: string;
    actorId?: string;
  }): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const data = await auditAPI.getAuditLogs(params);
      setLogs(data);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to fetch audit logs';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  const sortedLogs = useMemo(() => {
    return [...logs].sort((a, b) => b.id - a.id);
  }, [logs]);

  useEffect(() => {
    fetchLogs({ limit: 50 });
  }, [fetchLogs]);

  return {
    logs: sortedLogs,
    loading,
    error,
    fetchLogs
  };
};
