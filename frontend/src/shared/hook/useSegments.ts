import { useCallback, useEffect, useState } from 'react';
import { segmentAPI } from 'shared/api';
import type {
  Segment,
  SegmentRequest,
  SegmentUpdateRequest
} from 'shared/type';

export const useSegments = () => {
  const [segments, setSegments] = useState<Segment[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const fetchSegments = useCallback(async (limit = 50): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const data = await segmentAPI.getSegments(limit);
      setSegments(data);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to fetch segments';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  const createSegment = useCallback(async (payload: SegmentRequest): Promise<boolean> => {
    setSaving(true);
    setError(null);
    try {
      const created = await segmentAPI.createSegment(payload);
      if (!created) {
        setError('Failed to create segment');
        return false;
      }
      await fetchSegments();
      return true;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to create segment';
      setError(message);
      return false;
    } finally {
      setSaving(false);
    }
  }, [fetchSegments]);

  const updateSegment = useCallback(async (id: number, payload: SegmentUpdateRequest): Promise<boolean> => {
    setSaving(true);
    setError(null);
    try {
      const updated = await segmentAPI.updateSegment(id, payload);
      if (!updated) {
        setError('Failed to update segment');
        return false;
      }
      await fetchSegments();
      return true;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to update segment';
      setError(message);
      return false;
    } finally {
      setSaving(false);
    }
  }, [fetchSegments]);

  const deleteSegment = useCallback(async (id: number): Promise<boolean> => {
    setDeletingId(id);
    setError(null);
    try {
      const success = await segmentAPI.deleteSegment(id);
      if (!success) {
        setError('Failed to delete segment');
        return false;
      }
      await fetchSegments();
      return true;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to delete segment';
      setError(message);
      return false;
    } finally {
      setDeletingId(null);
    }
  }, [fetchSegments]);

  useEffect(() => {
    fetchSegments();
  }, [fetchSegments]);

  return {
    segments,
    loading,
    saving,
    deletingId,
    error,
    fetchSegments,
    createSegment,
    updateSegment,
    deleteSegment
  };
};
