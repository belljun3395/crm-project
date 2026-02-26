import React, { useMemo, useState } from 'react';
import { Button, Input, Modal } from 'common/component';
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
  const { webhooks, loading, saving, deletingId, error, createWebhook, updateWebhook, deleteWebhook } = useWebhooks();

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [editingWebhook, setEditingWebhook] = useState<WebhookResponse | null>(null);

  const [createForm, setCreateForm] = useState<WebhookFormState>(initialFormState);
  const [editForm, setEditForm] = useState<WebhookFormState>(initialFormState);
  const [formError, setFormError] = useState<string | null>(null);

  const sortedWebhooks = useMemo(() => {
    return [...webhooks].sort((a, b) => (b.id ?? 0) - (a.id ?? 0));
  }, [webhooks]);

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

  const handleCloseCreateModal = () => {
    setIsCreateModalOpen(false);
    setFormError(null);
    setCreateForm(initialFormState);
  };

  const handleCloseEditModal = () => {
    setEditingWebhook(null);
    setFormError(null);
    setEditForm(initialFormState);
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
        <div className="rounded-lg border border-red-700 bg-red-900/40 p-3 text-sm text-red-200">{formError}</div>
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

      <label className="flex items-center gap-2 text-sm text-gray-300">
        <input
          type="checkbox"
          checked={form.active}
          onChange={(e) => setForm((prev) => ({ ...prev, active: e.target.checked }))}
          className="h-4 w-4 rounded border-gray-600 bg-gray-800 text-[#22c55e]"
        />
        Active
      </label>

      <div className="flex gap-3 pt-3">
        <Button onClick={onSubmit} loading={saving} className="flex-1">
          {submitLabel}
        </Button>
        <Button variant="secondary" onClick={onCancel} className="flex-1">
          Cancel
        </Button>
      </div>
    </div>
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">Webhook Management</h1>
        <Button onClick={() => setIsCreateModalOpen(true)}>
          <span className="mr-2 text-lg">+</span>
          New Webhook
        </Button>
      </div>

      {(error || formError) && !isCreateModalOpen && !editingWebhook && (
        <div className="rounded-lg border border-red-700 bg-red-900/40 p-3 text-sm text-red-200">{error || formError}</div>
      )}

      <div className="overflow-hidden rounded-xl border border-gray-800 bg-gray-900">
        <table className="min-w-full divide-y divide-gray-800">
          <thead className="bg-gray-800/50">
            <tr>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">ID</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">Name</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">URL</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">Events</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">Status</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-800">
            {loading ? (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-gray-400">
                  Loading webhooks...
                </td>
              </tr>
            ) : sortedWebhooks.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-gray-400">
                  No webhooks configured
                </td>
              </tr>
            ) : (
              sortedWebhooks.map((webhook) => (
                <tr key={webhook.id} className="hover:bg-gray-800/40">
                  <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-400">{webhook.id}</td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm font-semibold text-white">{webhook.name}</td>
                  <td className="max-w-xs truncate px-6 py-4 text-sm text-gray-300">{webhook.url}</td>
                  <td className="max-w-xs truncate px-6 py-4 text-sm text-gray-300">{webhook.events.join(', ')}</td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm">
                    <span
                      className={`rounded-full px-2 py-1 text-xs font-semibold ${
                        webhook.active ? 'bg-green-700 text-green-100' : 'bg-gray-700 text-gray-200'
                      }`}
                    >
                      {webhook.active ? 'ACTIVE' : 'INACTIVE'}
                    </span>
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm">
                    <div className="flex gap-2">
                      <Button size="sm" variant="secondary" onClick={() => handleEdit(webhook)}>
                        Edit
                      </Button>
                      <Button
                        size="sm"
                        variant="danger"
                        onClick={() => handleDelete(webhook.id)}
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
      </div>

      <Modal isOpen={isCreateModalOpen} onClose={handleCloseCreateModal} title="Create Webhook" size="lg">
        {renderForm(createForm, setCreateForm, 'Create', handleCreate, handleCloseCreateModal)}
      </Modal>

      <Modal
        isOpen={Boolean(editingWebhook)}
        onClose={handleCloseEditModal}
        title={`Edit Webhook${editingWebhook ? ` #${editingWebhook.id}` : ''}`}
        size="lg"
      >
        {renderForm(editForm, setEditForm, 'Update', handleUpdate, handleCloseEditModal)}
      </Modal>
    </div>
  );
};
