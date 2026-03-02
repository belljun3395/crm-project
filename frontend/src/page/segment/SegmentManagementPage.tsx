import React, { useMemo, useState } from 'react';
import { Button, GuidePanel, Input, Modal, Textarea } from 'common/component';
import { useCampaigns, useSegments } from 'shared/hook';
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
      if (condition.value === null || condition.value === undefined) {
        throw new Error('Invalid condition: missing value');
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
    deletingId,
    segmentUsersById,
    userLoadingSegmentId,
    error,
    fetchSegmentUsers,
    createSegment,
    updateSegment,
    deleteSegment
  } = useSegments();
  const { campaigns } = useCampaigns();

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingSegment, setEditingSegment] = useState<Segment | null>(null);
  const [selectedSegment, setSelectedSegment] = useState<Segment | null>(null);
  const [selectedCampaignScopeId, setSelectedCampaignScopeId] = useState<number | null>(null);
  const [createForm, setCreateForm] = useState<SegmentFormState>(initialForm);
  const [editForm, setEditForm] = useState<SegmentFormState>(initialForm);
  const [formError, setFormError] = useState<string | null>(null);

  const sortedSegments = useMemo(() => {
    return [...segments].sort((a, b) => b.id - a.id);
  }, [segments]);
  const selectedSegmentUsers = useMemo(() => {
    if (!selectedSegment) {
      return [];
    }
    return segmentUsersById[selectedSegment.id] ?? [];
  }, [segmentUsersById, selectedSegment]);

  const buildCreatePayload = (form: SegmentFormState): SegmentRequest | null => {
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

  const buildUpdatePayload = (form: SegmentFormState): SegmentUpdateRequest | null => {
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
    const payload = buildCreatePayload(createForm);
    if (!payload) {
      return;
    }

    const success = await createSegment(payload);
    if (!success) {
      return;
    }

    setCreateForm(initialForm);
    setFormError(null);
    setIsCreateOpen(false);
  };

  const openCreateModal = () => {
    setFormError(null);
    setCreateForm(initialForm);
    setIsCreateOpen(true);
  };

  const closeCreateModal = () => {
    setFormError(null);
    setIsCreateOpen(false);
  };

  const openEdit = (segment: Segment) => {
    setEditingSegment(segment);
    setEditForm(toForm(segment));
    setFormError(null);
  };

  const openDetail = (segment: Segment) => {
    setSelectedSegment(segment);
    setSelectedCampaignScopeId(null);
    void fetchSegmentUsers(segment.id);
  };

  const closeDetailModal = () => {
    setSelectedSegment(null);
    setSelectedCampaignScopeId(null);
  };

  const handleCampaignScopeChange = (value: string) => {
    if (!selectedSegment) {
      return;
    }

    if (!value) {
      setSelectedCampaignScopeId(null);
      void fetchSegmentUsers(selectedSegment.id);
      return;
    }

    const parsedCampaignId = Number(value);
    if (Number.isNaN(parsedCampaignId)) {
      return;
    }

    setSelectedCampaignScopeId(parsedCampaignId);
    void fetchSegmentUsers(selectedSegment.id, parsedCampaignId);
  };

  const closeEditModal = () => {
    setFormError(null);
    setEditingSegment(null);
  };

  const handleUpdate = async () => {
    if (!editingSegment) {
      return;
    }

    const payload = buildUpdatePayload(editForm);
    if (!payload) {
      return;
    }

    const success = await updateSegment(editingSegment.id, payload);
    if (!success) {
      return;
    }

    setFormError(null);
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
          <p className="text-sm text-slate-300">조건 기반 고객 그룹을 만들고 관리합니다.</p>
        </div>
        <Button onClick={openCreateModal}>새 세그먼트</Button>
      </div>

      {error && (
        <div className="rounded-xl border border-rose-700/60 bg-rose-900/20 p-3 text-sm text-rose-100">{error}</div>
      )}

      <GuidePanel
        description="조건을 기준으로 고객 묶음(세그먼트)을 만들어 타겟팅에 사용하는 화면입니다."
        items={[
          '새 세그먼트에서 이름과 설명을 입력하고 조건 JSON을 작성합니다.',
          '조건은 배열 형태로 입력하며, 각 항목에 field/operator/valueType/value가 필요합니다.',
          'Active를 켜면 다른 기능(여정/캠페인)에서 즉시 사용할 수 있습니다.'
        ]}
        note="처음에는 기본 예시 JSON을 복사해 값만 바꾸는 방식이 가장 안전합니다."
      />

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
                <tr
                  key={segment.id}
                  className="cursor-pointer hover:bg-slate-800/40"
                  onClick={() => openDetail(segment)}
                >
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
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={(e) => {
                          e.stopPropagation();
                          openEdit(segment);
                        }}
                      >
                        Edit
                      </Button>
                      <Button
                        size="sm"
                        variant="danger"
                        onClick={(e) => {
                          e.stopPropagation();
                          void handleDelete(segment.id);
                        }}
                        loading={deletingId === segment.id}
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

      <Modal isOpen={isCreateOpen} onClose={closeCreateModal} title="Create Segment" size="lg">
        {renderForm(createForm, setCreateForm, '생성', handleCreate, closeCreateModal)}
      </Modal>

      <Modal
        isOpen={Boolean(editingSegment)}
        onClose={closeEditModal}
        title={editingSegment ? `Edit Segment #${editingSegment.id}` : 'Edit Segment'}
        size="lg"
      >
        {renderForm(editForm, setEditForm, '수정', handleUpdate, closeEditModal)}
      </Modal>

      <Modal
        isOpen={Boolean(selectedSegment)}
        onClose={closeDetailModal}
        title={selectedSegment ? `Segment #${selectedSegment.id}` : 'Segment Detail'}
        size="xl"
      >
        {selectedSegment ? (
          <div className="space-y-4">
            <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
              <div className="rounded-lg border border-slate-700/80 bg-slate-900/60 p-3">
                <p className="text-xs uppercase tracking-wide text-slate-400">Name</p>
                <p className="mt-1 text-sm text-white">{selectedSegment.name}</p>
              </div>
              <div className="rounded-lg border border-slate-700/80 bg-slate-900/60 p-3">
                <p className="text-xs uppercase tracking-wide text-slate-400">Status</p>
                <p className="mt-1 text-sm text-white">{selectedSegment.active ? 'ACTIVE' : 'INACTIVE'}</p>
              </div>
              <div className="rounded-lg border border-slate-700/80 bg-slate-900/60 p-3">
                <p className="text-xs uppercase tracking-wide text-slate-400">Description</p>
                <p className="mt-1 text-sm text-white">{selectedSegment.description || '-'}</p>
              </div>
              <div className="rounded-lg border border-slate-700/80 bg-slate-900/60 p-3">
                <p className="text-xs uppercase tracking-wide text-slate-400">Created</p>
                <p className="mt-1 text-sm text-white">
                  {selectedSegment.createdAt ? new Date(selectedSegment.createdAt).toLocaleString() : '-'}
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-slate-700/80 bg-slate-900/60 p-3">
              <p className="text-sm font-semibold text-slate-100">Conditions JSON</p>
              <pre className="mt-2 max-h-56 overflow-auto rounded-lg border border-slate-700 bg-slate-950/60 p-3 text-xs text-slate-200">
                {JSON.stringify(selectedSegment.conditions, null, 2)}
              </pre>
            </div>

            <div className="rounded-xl border border-slate-700/80 bg-slate-900/60">
              <div className="space-y-3 border-b border-slate-700 px-4 py-3">
                <div className="flex items-center justify-between">
                  <p className="text-sm font-semibold text-slate-100">Matched Users</p>
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => void fetchSegmentUsers(selectedSegment.id, selectedCampaignScopeId ?? undefined)}
                  >
                    Refresh
                  </Button>
                </div>
                <div className="grid gap-2 md:grid-cols-[180px_1fr] md:items-center">
                  <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Campaign Scope</p>
                  <select
                    value={selectedCampaignScopeId ?? ''}
                    onChange={(e) => handleCampaignScopeChange(e.target.value)}
                    className="w-full rounded-xl border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-white outline-none transition focus:border-cyan-400"
                  >
                    <option value="">전체 이벤트 범위</option>
                    {campaigns.map((campaign) => (
                      <option key={campaign.id} value={campaign.id}>
                        #{campaign.id} - {campaign.name}
                      </option>
                    ))}
                  </select>
                </div>
                <p className="text-xs text-slate-400">
                  캠페인을 선택하면 해당 캠페인에 포함된 이벤트 기준으로 세그먼트 사용자 미리보기를 좁혀서 조회합니다.
                </p>
              </div>
              <div className="max-h-[280px] overflow-auto">
                <table className="min-w-full divide-y divide-slate-800">
                  <thead className="bg-slate-800/60">
                    <tr>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">
                        ID
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">
                        External ID
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">
                        Email
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">
                        Name
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">
                        Created
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {userLoadingSegmentId === selectedSegment.id ? (
                      <tr>
                        <td colSpan={5} className="px-4 py-8 text-center text-sm text-slate-300">
                          Loading matched users...
                        </td>
                      </tr>
                    ) : selectedSegmentUsers.length === 0 ? (
                      <tr>
                        <td colSpan={5} className="px-4 py-8 text-center text-sm text-slate-300">
                          매칭된 유저가 없습니다.
                        </td>
                      </tr>
                    ) : (
                      selectedSegmentUsers.map((user) => (
                        <tr key={user.id}>
                          <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{user.id}</td>
                          <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-300">{user.externalId}</td>
                          <td className="px-4 py-3 text-sm text-slate-300">{user.email || '-'}</td>
                          <td className="px-4 py-3 text-sm text-slate-300">{user.name || '-'}</td>
                          <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">
                            {user.createdAt ? new Date(user.createdAt).toLocaleString() : '-'}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        ) : null}
      </Modal>
    </div>
  );
};
