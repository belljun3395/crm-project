import React, { useMemo, useState } from 'react';
import { Button, Input, Modal, Textarea } from 'common/component';
import { useSegments } from 'shared/hook';
import type { Segment, SegmentCondition, SegmentRequest, SegmentUpdateRequest } from 'shared/type';

interface SegmentFormState {
  name: string;
  description: string;
  active: boolean;
  conditionsJson: string;
}

const initialForm: SegmentFormState = {
  name: '',
  description: '',
  active: true,
  conditionsJson: JSON.stringify(
    [
      {
        field: 'country',
        operator: 'eq',
        valueType: 'STRING',
        value: 'KR'
      }
    ],
    null,
    2
  )
};

const parseConditions = (raw: string): SegmentCondition[] | null => {
  try {
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed) || parsed.length === 0) {
      return null;
    }

    const normalized: SegmentCondition[] = parsed.map((item) => {
      const condition = item as SegmentCondition;
      if (!condition.field || !condition.operator || !condition.valueType) {
        throw new Error('Invalid condition');
      }
      return condition;
    });

    return normalized;
  } catch {
    return null;
  }
};

const toForm = (segment: Segment): SegmentFormState => ({
  name: segment.name,
  description: segment.description ?? '',
  active: segment.active,
  conditionsJson: JSON.stringify(segment.conditions, null, 2)
});

export const SegmentManagementPage: React.FC = () => {
  const {
    segments,
    loading,
    saving,
    error,
    createSegment,
    updateSegment,
    deleteSegment
  } = useSegments();

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingSegment, setEditingSegment] = useState<Segment | null>(null);
  const [createForm, setCreateForm] = useState<SegmentFormState>(initialForm);
  const [editForm, setEditForm] = useState<SegmentFormState>(initialForm);
  const [formError, setFormError] = useState<string | null>(null);

  const sortedSegments = useMemo(() => {
    return [...segments].sort((a, b) => b.id - a.id);
  }, [segments]);

  const buildPayload = (form: SegmentFormState): SegmentRequest | SegmentUpdateRequest | null => {
    if (!form.name.trim()) {
      setFormError('세그먼트 이름은 필수입니다.');
      return null;
    }

    const parsedConditions = parseConditions(form.conditionsJson);
    if (!parsedConditions) {
      setFormError('conditions JSON 형식을 확인해주세요. 최소 1개 조건이 필요합니다.');
      return null;
    }

    setFormError(null);
    return {
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      active: form.active,
      conditions: parsedConditions
    };
  };

  const handleCreate = async () => {
    const payload = buildPayload(createForm);
    if (!payload) {
      return;
    }

    const success = await createSegment(payload as SegmentRequest);
    if (!success) {
      return;
    }

    setCreateForm(initialForm);
    setIsCreateOpen(false);
  };

  const openEdit = (segment: Segment) => {
    setEditingSegment(segment);
    setEditForm(toForm(segment));
    setFormError(null);
  };

  const handleUpdate = async () => {
    if (!editingSegment) {
      return;
    }

    const payload = buildPayload(editForm);
    if (!payload) {
      return;
    }

    const success = await updateSegment(editingSegment.id, payload as SegmentUpdateRequest);
    if (!success) {
      return;
    }

    setEditingSegment(null);
    setEditForm(initialForm);
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('세그먼트를 삭제할까요?')) {
      return;
    }
    await deleteSegment(id);
  };

  const renderForm = (
    form: SegmentFormState,
    setForm: React.Dispatch<React.SetStateAction<SegmentFormState>>,
    submitLabel: string,
    onSubmit: () => void,
    onCancel: () => void
  ) => (
    <div className="space-y-4">
      {(formError || error) && (
        <div className="rounded-xl border border-rose-700/60 bg-rose-900/20 p-3 text-sm text-rose-100">
          {formError || error}
        </div>
      )}

      <Input
        label="Name"
        value={form.name}
        onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
        placeholder="High-value users"
        required
      />

      <Input
        label="Description"
        value={form.description}
        onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))}
        placeholder="최근 7일 내 구매 유저"
      />

      <Textarea
        label="Conditions JSON"
        value={form.conditionsJson}
        onChange={(e) => setForm((prev) => ({ ...prev, conditionsJson: e.target.value }))}
        rows={8}
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
          취소
        </Button>
      </div>
    </div>
  );

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-white">Segments</h2>
          <p className="text-sm text-slate-300">OpenAPI `/segments` 기반 세그먼트 관리</p>
        </div>
        <Button onClick={() => setIsCreateOpen(true)}>새 세그먼트</Button>
      </div>

      {error && (
        <div className="rounded-xl border border-rose-700/60 bg-rose-900/20 p-3 text-sm text-rose-100">{error}</div>
      )}

      <div className="overflow-hidden rounded-2xl border border-slate-800/80 bg-slate-900/60 backdrop-blur">
        <table className="min-w-full divide-y divide-slate-800">
          <thead className="bg-slate-800/60">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">ID</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Name</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Conditions</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Status</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Created</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800">
            {loading ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-sm text-slate-300">
                  Loading segments...
                </td>
              </tr>
            ) : sortedSegments.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-sm text-slate-300">
                  No segments
                </td>
              </tr>
            ) : (
              sortedSegments.map((segment) => (
                <tr key={segment.id} className="hover:bg-slate-800/40">
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">{segment.id}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm font-semibold text-white">{segment.name}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{segment.conditions.length}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm">
                    <span
                      className={`rounded-full px-2 py-1 text-xs font-semibold ${
                        segment.active
                          ? 'bg-emerald-500/20 text-emerald-200'
                          : 'bg-slate-700 text-slate-200'
                      }`}
                    >
                      {segment.active ? 'ACTIVE' : 'INACTIVE'}
                    </span>
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">
                    {segment.createdAt ? new Date(segment.createdAt).toLocaleString() : '-'}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm">
                    <div className="flex gap-2">
                      <Button size="sm" variant="secondary" onClick={() => openEdit(segment)}>
                        Edit
                      </Button>
                      <Button size="sm" variant="danger" onClick={() => handleDelete(segment.id)} loading={saving}>
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

      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Create Segment" size="lg">
        {renderForm(createForm, setCreateForm, '생성', handleCreate, () => setIsCreateOpen(false))}
      </Modal>

      <Modal
        isOpen={Boolean(editingSegment)}
        onClose={() => setEditingSegment(null)}
        title={editingSegment ? `Edit Segment #${editingSegment.id}` : 'Edit Segment'}
        size="lg"
      >
        {renderForm(editForm, setEditForm, '수정', handleUpdate, () => setEditingSegment(null))}
      </Modal>
    </div>
  );
};
