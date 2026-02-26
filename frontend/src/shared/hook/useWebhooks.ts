import { useCallback, useEffect, useState } from 'react';
import { webhookAPI } from 'shared/api';
import type {
  CreateWebhookRequest,
  UpdateWebhookRequest,
  WebhookResponse,
  WebhookDeliveryLog,
  WebhookDeadLetter
} from 'shared/type';

export const useWebhooks = () => {
  const [webhooks, setWebhooks] = useState<WebhookResponse[]>([]);
  const [deliveriesByWebhook, setDeliveriesByWebhook] = useState<Record<number, WebhookDeliveryLog[]>>({});
  const [deadLettersByWebhook, setDeadLettersByWebhook] = useState<Record<number, WebhookDeadLetter[]>>({});
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [deliveryLoadingId, setDeliveryLoadingId] = useState<number | null>(null);
  const [deadLetterLoadingId, setDeadLetterLoadingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const fetchWebhooks = useCallback(async (): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const list = await webhookAPI.getWebhooks();
      if (!list) {
        setError('Failed to load webhooks');
        return;
      }
      setWebhooks(list);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load webhooks';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  const createWebhook = useCallback(async (payload: CreateWebhookRequest): Promise<boolean> => {
    setSaving(true);
    setError(null);
    try {
      const created = await webhookAPI.createWebhook(payload);
      if (!created) {
        setError('Failed to create webhook');
        return false;
      }
      await fetchWebhooks();
      return true;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to create webhook';
      setError(message);
      return false;
    } finally {
      setSaving(false);
    }
  }, [fetchWebhooks]);

  const updateWebhook = useCallback(async (id: number, payload: UpdateWebhookRequest): Promise<boolean> => {
    setSaving(true);
    setError(null);
    try {
      const updated = await webhookAPI.updateWebhook(id, payload);
      if (!updated) {
        setError('Failed to update webhook');
        return false;
      }
      await fetchWebhooks();
      return true;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to update webhook';
      setError(message);
      return false;
    } finally {
      setSaving(false);
    }
  }, [fetchWebhooks]);

  const deleteWebhook = useCallback(async (id: number): Promise<boolean> => {
    setDeletingId(id);
    setError(null);
    try {
      const success = await webhookAPI.deleteWebhook(id);
      if (!success) {
        setError('Failed to delete webhook');
        return false;
      }
      await fetchWebhooks();
      return true;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to delete webhook';
      setError(message);
      return false;
    } finally {
      setDeletingId(null);
    }
  }, [fetchWebhooks]);

  const fetchDeliveries = useCallback(async (webhookId: number): Promise<void> => {
    setDeliveryLoadingId(webhookId);
    setError(null);
    try {
      const data = await webhookAPI.getWebhookDeliveries(webhookId);
      setDeliveriesByWebhook((prev) => ({ ...prev, [webhookId]: data }));
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load webhook deliveries';
      setError(message);
    } finally {
      setDeliveryLoadingId(null);
    }
  }, []);

  const fetchDeadLetters = useCallback(async (webhookId: number): Promise<void> => {
    setDeadLetterLoadingId(webhookId);
    setError(null);
    try {
      const data = await webhookAPI.getWebhookDeadLetters(webhookId);
      setDeadLettersByWebhook((prev) => ({ ...prev, [webhookId]: data }));
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load webhook dead letters';
      setError(message);
    } finally {
      setDeadLetterLoadingId(null);
    }
  }, []);

  useEffect(() => {
    fetchWebhooks();
  }, [fetchWebhooks]);

  return {
    webhooks,
    deliveriesByWebhook,
    deadLettersByWebhook,
    loading,
    saving,
    deletingId,
    deliveryLoadingId,
    deadLetterLoadingId,
    error,
    fetchWebhooks,
    createWebhook,
    updateWebhook,
    deleteWebhook,
    fetchDeliveries,
    fetchDeadLetters
  };
};
