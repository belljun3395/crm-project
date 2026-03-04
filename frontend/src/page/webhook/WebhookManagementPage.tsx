import React, { useEffect, useMemo, useState } from 'react';
import { Button, GuidePanel, Input, Modal } from 'common/component';
import { useWebhooks } from 'shared/hook';
import type { CreateWebhookRequest, UpdateWebhookRequest, WebhookResponse } from 'shared/type';

interface WebhookFormState {
  name: string;
  url: string;
  events: string;
  active: boolean;
}

const initialFormState: WebhookFormState = {
  name: '',
  url: '',
  events: '',
  active: true
};

const parseEvents = (events: string): string[] =>
  events
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0);

const isValidUrl = (url: string): boolean => {
  try {
    const parsed = new URL(url);
    return parsed.protocol === 'http:' || parsed.protocol === 'https:';
  } catch {
    return false;
  }
};

const toForm = (webhook: WebhookResponse): WebhookFormState => ({
  name: webhook.name,
  url: webhook.url,
  events: webhook.events.join(', '),
  active: webhook.active
});

export const WebhookManagementPage: React.FC = () => {
  const {
    webhooks,
    deliveriesByWebhook,
    deadLettersByWebhook,
    loading,
    saving,
    deletingId,
    deliveryLoadingId,
    deadLetterLoadingId,
    retryingDeadLetterId,
    batchRetryingWebhookId,
    error,
    createWebhook,
    updateWebhook,
    deleteWebhook,
    fetchDeliveries,
    fetchDeadLetters,
    retryDeadLetter,
    retryDeadLetters
  } = useWebhooks();

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [editingWebhook, setEditingWebhook] = useState<WebhookResponse | null>(null);
  const [selectedWebhookId, setSelectedWebhookId] = useState<number | null>(null);
  const [createForm, setCreateForm] = useState<WebhookFormState>(initialFormState);
  const [editForm, setEditForm] = useState<WebhookFormState>(initialFormState);
  const [formError, setFormError] = useState<string | null>(null);

  const sortedWebhooks = useMemo(() => {
    return [...webhooks].sort((a, b) => b.id - a.id);
  }, [webhooks]);

  useEffect(() => {
    if (sortedWebhooks.length === 0) {
      setSelectedWebhookId(null);
      return;
    }

    if (!selectedWebhookId || !sortedWebhooks.find((webhook) => webhook.id === selectedWebhookId)) {
      setSelectedWebhookId(sortedWebhooks[0]?.id ?? null);
    }
  }, [sortedWebhooks, selectedWebhookId]);

  useEffect(() => {
    if (!selectedWebhookId) {
      return;
    }

    fetchDeliveries(selectedWebhookId);
    fetchDeadLetters(selectedWebhookId);
  }, [selectedWebhookId, fetchDeliveries, fetchDeadLetters]);

  const deliveries = selectedWebhookId ? deliveriesByWebhook[selectedWebhookId] ?? [] : [];
  const deadLetters = selectedWebhookId ? deadLettersByWebhook[selectedWebhookId] ?? [] : [];

  const validateForm = (form: WebhookFormState): string | null => {
    if (!form.name.trim()) {
      return 'Webhook name is required';
    }
    if (!form.url.trim()) {
      return 'Webhook URL is required';
    }
    if (!isValidUrl(form.url.trim())) {
      return 'Webhook URL must start with http:// or https://';
    }

    const events = parseEvents(form.events);
    if (events.length === 0) {
      return 'At least one event type is required';
    }

    return null;
  };

  const handleCreate = async () => {
    const validationError = validateForm(createForm);
    if (validationError) {
      setFormError(validationError);
      return;
    }

    const payload: CreateWebhookRequest = {
      name: createForm.name.trim(),
      url: createForm.url.trim(),
      events: parseEvents(createForm.events),
      active: createForm.active
    };

    const success = await createWebhook(payload);
    if (success) {
      setFormError(null);
      setCreateForm(initialFormState);
      setIsCreateModalOpen(false);
    }
  };

  const handleEdit = (webhook: WebhookResponse) => {
    setEditingWebhook(webhook);
    setEditForm(toForm(webhook));
    setFormError(null);
  };

  const handleUpdate = async () => {
    if (!editingWebhook) {
      return;
    }

    const validationError = validateForm(editForm);
    if (validationError) {
      setFormError(validationError);
      return;
    }

    const payload: UpdateWebhookRequest = {
      name: editForm.name.trim(),
      url: editForm.url.trim(),
      events: parseEvents(editForm.events),
      active: editForm.active
    };

    const success = await updateWebhook(editingWebhook.id, payload);
    if (success) {
      setEditingWebhook(null);
      setEditForm(initialFormState);
      setFormError(null);
    }
  };

  const handleDelete = async (webhookId: number) => {
    if (!window.confirm('Delete this webhook?')) {
      return;
    }
    await deleteWebhook(webhookId);
  };

  const renderForm = (
    form: WebhookFormState,
    setForm: React.Dispatch<React.SetStateAction<WebhookFormState>>,
    submitLabel: string,
    onSubmit: () => void,
    onCancel: () => void
  ) => (
    <div className="space-y-4">
      {formError && (
        <div className="rounded-xl border border-rose-700/60 bg-rose-900/20 p-3 text-sm text-rose-100">{formError}</div>
      )}

      <Input
        label="Name"
        value={form.name}
        onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
        placeholder="order-events-webhook"
        required
      />

      <Input
        label="URL"
        type="url"
        value={form.url}
        onChange={(e) => setForm((prev) => ({ ...prev, url: e.target.value }))}
        placeholder="https://example.com/webhooks/events"
        required
      />

      <Input
        label="Events (comma separated)"
        value={form.events}
        onChange={(e) => setForm((prev) => ({ ...prev, events: e.target.value }))}
        placeholder="EVENT_CREATED, EMAIL_SENT"
        required
      />

      <label className="flex items-center gap-2 text-sm text-slate-300">
        <input
          type="checkbox"
          checked={form.active}
          onChange={(e) => setForm((prev) => ({ ...prev, active: e.target.checked }))}
          className="h-4 w-4 rounded border-slate-600 bg-slate-800"
        />
        Active
      </label>

      <div className="grid grid-cols-2 gap-3 pt-2">
        <Button onClick={onSubmit} loading={saving}>
          {submitLabel}
        </Button>
        <Button variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </div>
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-white">Webhooks</h2>
          <p className="text-sm text-slate-300">외부 시스템 연동과 전달 상태를 관리합니다.</p>
        </div>
        <Button onClick={() => setIsCreateModalOpen(true)}>New Webhook</Button>
      </div>

      {(error || formError) && !isCreateModalOpen && !editingWebhook && (
        <div className="rounded-xl border border-rose-700/60 bg-rose-900/20 p-3 text-sm text-rose-100">{error || formError}</div>
      )}

      <GuidePanel
        description="외부 시스템으로 이벤트를 자동 전달하는 연결을 관리하는 화면입니다."
        items={[
          'New Webhook에서 이름, 수신 URL, 이벤트 종류를 입력해 등록합니다.',
          'Active를 끄면 해당 Webhook 전달을 일시 중지할 수 있습니다.',
          '아래 Delivery와 Dead Letter를 확인해 전달 성공/실패를 추적합니다.'
        ]}
        note="URL은 http 또는 https 형식만 허용됩니다."
      />

      <section className="overflow-hidden rounded-2xl border border-slate-800/80 bg-slate-900/60 backdrop-blur">
        <table className="min-w-full divide-y divide-slate-800">
          <thead className="bg-slate-800/60">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">ID</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Name</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">URL</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Events</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Status</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800">
            {loading ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-slate-300">
                  Loading webhooks...
                </td>
              </tr>
            ) : sortedWebhooks.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-slate-300">
                  No webhooks configured
                </td>
              </tr>
            ) : (
              sortedWebhooks.map((webhook) => (
                <tr
                  key={webhook.id}
                  className={`cursor-pointer hover:bg-slate-800/40 ${selectedWebhookId === webhook.id ? 'bg-slate-800/30' : ''}`}
                  onClick={() => setSelectedWebhookId(webhook.id)}
                >
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">{webhook.id}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm font-semibold text-white">{webhook.name}</td>
                  <td className="max-w-xs truncate px-4 py-3 text-sm text-slate-300">{webhook.url}</td>
                  <td className="max-w-xs truncate px-4 py-3 text-sm text-slate-300">{webhook.events.join(', ')}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm">
                    <span
                      className={`rounded-full px-2 py-1 text-xs font-semibold ${
                        webhook.active ? 'bg-emerald-500/20 text-emerald-200' : 'bg-slate-700 text-slate-200'
                      }`}
                    >
                      {webhook.active ? 'ACTIVE' : 'INACTIVE'}
                    </span>
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm">
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedWebhookId(webhook.id);
                        }}
                      >
                        Logs
                      </Button>
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleEdit(webhook);
                        }}
                      >
                        Edit
                      </Button>
                      <Button
                        size="sm"
                        variant="danger"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDelete(webhook.id);
                        }}
                        loading={deletingId === webhook.id}
                      >
                        Delete
                      </Button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </section>

      <section className="grid grid-cols-1 gap-5 xl:grid-cols-2">
        <div className="overflow-hidden rounded-2xl border border-slate-800/80 bg-slate-900/60 backdrop-blur">
          <div className="flex items-center justify-between border-b border-slate-800 px-4 py-3">
            <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-300">Delivery Logs</h3>
            {selectedWebhookId && (
              <Button
                size="sm"
                variant="secondary"
                onClick={() => fetchDeliveries(selectedWebhookId)}
                loading={deliveryLoadingId === selectedWebhookId}
              >
                Refresh
              </Button>
            )}
          </div>
          <div className="max-h-[320px] overflow-auto">
            <table className="min-w-full divide-y divide-slate-800">
              <thead className="bg-slate-800/60">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">ID</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Event</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Status</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Attempt</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {!selectedWebhookId ? (
                  <tr>
                    <td colSpan={4} className="px-4 py-8 text-center text-sm text-slate-300">
                      Select webhook
                    </td>
                  </tr>
                ) : deliveryLoadingId === selectedWebhookId ? (
                  <tr>
                    <td colSpan={4} className="px-4 py-8 text-center text-sm text-slate-300">
                      Loading deliveries...
                    </td>
                  </tr>
                ) : deliveries.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="px-4 py-8 text-center text-sm text-slate-300">
                      No delivery logs
                    </td>
                  </tr>
                ) : (
                  deliveries.map((log) => (
                    <tr key={log.id} className="hover:bg-slate-800/30">
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">{log.id}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{log.eventType}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-white">{log.deliveryStatus}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{log.attemptCount}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        <div className="overflow-hidden rounded-2xl border border-slate-800/80 bg-slate-900/60 backdrop-blur">
          <div className="flex items-center justify-between border-b border-slate-800 px-4 py-3">
            <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-300">Dead Letters</h3>
            <div className="flex gap-2">
              {selectedWebhookId && (
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() => void retryDeadLetters(selectedWebhookId)}
                  loading={batchRetryingWebhookId === selectedWebhookId}
                  disabled={deadLetters.length === 0}
                >
                  Retry All
                </Button>
              )}
              {selectedWebhookId && (
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() => fetchDeadLetters(selectedWebhookId)}
                  loading={deadLetterLoadingId === selectedWebhookId}
                >
                  Refresh
                </Button>
              )}
            </div>
          </div>
          <div className="max-h-[320px] overflow-auto">
            <table className="min-w-full divide-y divide-slate-800">
              <thead className="bg-slate-800/60">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">ID</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Event</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Status</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Error</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {!selectedWebhookId ? (
                  <tr>
                    <td colSpan={5} className="px-4 py-8 text-center text-sm text-slate-300">
                      Select webhook
                    </td>
                  </tr>
                ) : deadLetterLoadingId === selectedWebhookId ? (
                  <tr>
                    <td colSpan={5} className="px-4 py-8 text-center text-sm text-slate-300">
                      Loading dead letters...
                    </td>
                  </tr>
                ) : deadLetters.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-4 py-8 text-center text-sm text-slate-300">
                      No dead letters
                    </td>
                  </tr>
                ) : (
                  deadLetters.map((log) => (
                    <tr key={log.id} className="hover:bg-slate-800/30">
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">{log.id}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{log.eventType}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-white">{log.deliveryStatus}</td>
                      <td className="px-4 py-3 text-sm text-slate-300">{log.errorMessage || '-'}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-sm">
                        <Button
                          size="sm"
                          variant="secondary"
                          loading={retryingDeadLetterId === log.id}
                          onClick={() => {
                            if (!selectedWebhookId) {
                              return;
                            }
                            void retryDeadLetter(selectedWebhookId, log.id);
                          }}
                        >
                          Retry
                        </Button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <Modal isOpen={isCreateModalOpen} onClose={() => setIsCreateModalOpen(false)} title="Create Webhook" size="lg">
        {renderForm(createForm, setCreateForm, 'Create', handleCreate, () => setIsCreateModalOpen(false))}
      </Modal>

      <Modal
        isOpen={Boolean(editingWebhook)}
        onClose={() => setEditingWebhook(null)}
        title={`Edit Webhook${editingWebhook ? ` #${editingWebhook.id}` : ''}`}
        size="lg"
      >
        {renderForm(editForm, setEditForm, 'Update', handleUpdate, () => setEditingWebhook(null))}
      </Modal>
    </div>
  );
};
