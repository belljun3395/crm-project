import { useCallback, useEffect, useMemo, useState } from 'react';
import { campaignAPI } from 'shared/api';
import type { CampaignDetail, CampaignOption, CreateCampaignRequest } from 'shared/type';

export const useCampaigns = () => {
  const [campaigns, setCampaigns] = useState<CampaignOption[]>([]);
  const [campaignDetailsById, setCampaignDetailsById] = useState<Record<number, CampaignDetail>>({});
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [detailLoadingId, setDetailLoadingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const fetchCampaigns = useCallback(async (limit = 100): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const data = await campaignAPI.getCampaigns(limit);
      setCampaigns(data);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to fetch campaigns';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  const sortedCampaigns = useMemo(() => {
    return [...campaigns].sort((a, b) => b.id - a.id);
  }, [campaigns]);

  useEffect(() => {
    fetchCampaigns();
  }, [fetchCampaigns]);

  const fetchCampaignDetail = useCallback(async (campaignId: number): Promise<CampaignDetail | null> => {
    setDetailLoadingId(campaignId);
    try {
      const detail = await campaignAPI.getCampaign(campaignId);
      if (detail) {
        setCampaignDetailsById((prev) => ({ ...prev, [campaignId]: detail }));
      }
      return detail;
    } finally {
      setDetailLoadingId(null);
    }
  }, []);

  const createCampaign = useCallback(async (payload: CreateCampaignRequest): Promise<boolean> => {
    setSaving(true);
    setError(null);
    try {
      const created = await campaignAPI.postCampaign(payload);
      if (!created) {
        return false;
      }
      setCampaignDetailsById((prev) => ({ ...prev, [created.id]: created }));
      await fetchCampaigns();
      return true;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to create campaign';
      setError(message);
      return false;
    } finally {
      setSaving(false);
    }
  }, [fetchCampaigns]);

  const updateCampaign = useCallback(async (campaignId: number, payload: CreateCampaignRequest): Promise<boolean> => {
    setSaving(true);
    setError(null);
    try {
      const updated = await campaignAPI.putCampaign(campaignId, payload);
      if (!updated) {
        return false;
      }
      setCampaignDetailsById((prev) => ({ ...prev, [campaignId]: updated }));
      await fetchCampaigns();
      return true;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to update campaign';
      setError(message);
      return false;
    } finally {
      setSaving(false);
    }
  }, [fetchCampaigns]);

  const deleteCampaign = useCallback(async (campaignId: number): Promise<boolean> => {
    setDeletingId(campaignId);
    setError(null);
    try {
      const success = await campaignAPI.deleteCampaign(campaignId);
      if (!success) {
        return false;
      }
      setCampaignDetailsById((prev) => {
        const next = { ...prev };
        delete next[campaignId];
        return next;
      });
      await fetchCampaigns();
      return true;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to delete campaign';
      setError(message);
      return false;
    } finally {
      setDeletingId(null);
    }
  }, [fetchCampaigns]);

  return {
    campaigns: sortedCampaigns,
    campaignDetailsById,
    loading,
    saving,
    deletingId,
    detailLoadingId,
    error,
    fetchCampaigns,
    fetchCampaignDetail,
    createCampaign,
    updateCampaign,
    deleteCampaign
  };
};
