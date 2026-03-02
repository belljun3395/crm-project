import React, { useMemo, useState } from 'react';
import { Button, GuidePanel, Input, Modal } from 'common/component';
import { useCampaigns, useSegments } from 'shared/hook';
import type { EventProperty } from 'shared/type';

interface CampaignFormState {
  name: string;
  properties: EventProperty[];
  segmentIds: number[];
}

const initialForm: CampaignFormState = {
  name: '',
  properties: [{ key: '', value: '' }],
  segmentIds: []
};

const formatDateTime = (value?: string): string => {
  if (!value) {
    return '-';
  }

  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? '-' : parsed.toLocaleString();
};

export const CampaignManagementPage: React.FC = () => {
  const {
    campaigns,
    campaignDetailsById,
    loading,
    saving,
    deletingId,
    detailLoadingId,
    error,
    fetchCampaignDetail,
    createCampaign,
    updateCampaign,
    deleteCampaign
  } = useCampaigns();
  const { segments } = useSegments();

  const [searchTerm, setSearchTerm] = useState('');
  const [segmentSearch, setSegmentSearch] = useState('');
  const [editSegmentSearch, setEditSegmentSearch] = useState('');
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [selectedCampaignId, setSelectedCampaignId] = useState<number | null>(null);
  const [editingCampaignId, setEditingCampaignId] = useState<number | null>(null);
  const [form, setForm] = useState<CampaignFormState>(initialForm);
  const [editForm, setEditForm] = useState<CampaignFormState>(initialForm);
  const [formError, setFormError] = useState<string | null>(null);

  const selectedCampaign =
    selectedCampaignId !== null ? campaignDetailsById[selectedCampaignId] ?? null : null;

  const filteredCampaigns = useMemo(() => {
    const query = searchTerm.trim().toLowerCase();
    if (!query) {
      return campaigns;
    }

    return campaigns.filter((campaign) => campaign.name.toLowerCase().includes(query));
  }, [campaigns, searchTerm]);

  const filteredSegments = useMemo(() => {
    const query = segmentSearch.trim().toLowerCase();
    if (!query) {
      return segments;
    }

    return segments.filter((segment) => segment.name.toLowerCase().includes(query));
  }, [segments, segmentSearch]);

  const filteredEditSegments = useMemo(() => {
    const query = editSegmentSearch.trim().toLowerCase();
    if (!query) {
      return segments;
    }

    return segments.filter((segment) => segment.name.toLowerCase().includes(query));
  }, [segments, editSegmentSearch]);

  const handleOpenDetail = async (campaignId: number) => {
    setSelectedCampaignId(campaignId);
    if (!campaignDetailsById[campaignId]) {
      await fetchCampaignDetail(campaignId);
    }
  };

  const handleAddProperty = () => {
    setForm((prev) => ({
      ...prev,
      properties: [...prev.properties, { key: '', value: '' }]
    }));
  };

  const handleUpdateProperty = (index: number, next: EventProperty) => {
    setForm((prev) => ({
      ...prev,
      properties: prev.properties.map((property, propertyIndex) => (propertyIndex === index ? next : property))
    }));
  };

  const handleRemoveProperty = (index: number) => {
    setForm((prev) => ({
      ...prev,
      properties: prev.properties.filter((_, propertyIndex) => propertyIndex !== index)
    }));
  };

  const handleToggleSegment = (segmentId: number) => {
    setForm((prev) => {
      const exists = prev.segmentIds.includes(segmentId);
      return {
        ...prev,
        segmentIds: exists
          ? prev.segmentIds.filter((id) => id !== segmentId)
          : [...prev.segmentIds, segmentId]
      };
    });
  };

  const handleToggleEditSegment = (segmentId: number) => {
    setEditForm((prev) => {
      const exists = prev.segmentIds.includes(segmentId);
      return {
        ...prev,
        segmentIds: exists
          ? prev.segmentIds.filter((id) => id !== segmentId)
          : [...prev.segmentIds, segmentId]
      };
    });
  };

  const normalizeForm = (raw: CampaignFormState): { name: string; properties: EventProperty[] } | null => {
    const normalizedName = raw.name.trim();
    if (!normalizedName) {
      setFormError('캠페인 이름은 필수입니다.');
      return null;
    }

    const normalizedProperties = raw.properties
      .map((property) => ({ key: property.key.trim(), value: property.value.trim() }))
      .filter((property) => property.key && property.value);

    if (normalizedProperties.length === 0) {
      setFormError('최소 1개 이상의 속성 키/값이 필요합니다.');
      return null;
    }

    return {
      name: normalizedName,
      properties: normalizedProperties
    };
  };

  const handleCreate = async () => {
    const normalized = normalizeForm(form);
    if (!normalized) {
      return;
    }

    setFormError(null);
    const success = await createCampaign({
      name: normalized.name,
      properties: normalized.properties,
      segmentIds: form.segmentIds
    });

    if (!success) {
      return;
    }

    setForm(initialForm);
    setSegmentSearch('');
    setIsCreateModalOpen(false);
  };

  const handleOpenEdit = async (campaignId: number) => {
    setFormError(null);
    setEditingCampaignId(campaignId);
    let detail = campaignDetailsById[campaignId];
    if (!detail) {
      detail = await fetchCampaignDetail(campaignId);
    }
    if (!detail) {
      return;
    }
    setEditForm({
      name: detail.name,
      properties: detail.properties.length > 0 ? detail.properties : [{ key: '', value: '' }],
      segmentIds: detail.segmentIds
    });
    setEditSegmentSearch('');
    setIsEditModalOpen(true);
  };

  const handleEditProperty = (index: number, next: EventProperty) => {
    setEditForm((prev) => ({
      ...prev,
      properties: prev.properties.map((property, propertyIndex) => (propertyIndex === index ? next : property))
    }));
  };

  const handleAddEditProperty = () => {
    setEditForm((prev) => ({
      ...prev,
      properties: [...prev.properties, { key: '', value: '' }]
    }));
  };

  const handleRemoveEditProperty = (index: number) => {
    setEditForm((prev) => ({
      ...prev,
      properties: prev.properties.filter((_, propertyIndex) => propertyIndex !== index)
    }));
  };

  const handleSaveEdit = async () => {
    if (editingCampaignId === null) {
      return;
    }

    const normalized = normalizeForm(editForm);
    if (!normalized) {
      return;
    }

    setFormError(null);
    const success = await updateCampaign(editingCampaignId, {
      name: normalized.name,
      properties: normalized.properties,
      segmentIds: editForm.segmentIds
    });

    if (!success) {
      return;
    }

    setIsEditModalOpen(false);
    if (selectedCampaignId === editingCampaignId) {
      await fetchCampaignDetail(editingCampaignId);
    }
  };

  const handleDelete = async (campaignId: number) => {
    if (!window.confirm('정말 이 Campaign을 삭제하시겠습니까?')) {
      return;
    }
    const success = await deleteCampaign(campaignId);
    if (!success) {
      return;
    }
    if (selectedCampaignId === campaignId) {
      setSelectedCampaignId(null);
    }
  };

  const segmentNameById = useMemo(() => {
    return new Map(segments.map((segment) => [segment.id, segment.name]));
  }, [segments]);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-semibold text-white">Campaigns</h2>
        <Button onClick={() => setIsCreateModalOpen(true)}>New Campaign</Button>
      </div>

      <GuidePanel
        description="Campaign은 이벤트를 묶어 성과를 보는 운영 단위이고, Segment는 사용자 집합을 고르는 조건 단위입니다."
        items={[
          'Segment: 어떤 사용자 집합에 적용할지 결정합니다. (대상 선정)',
          'Campaign: 어떤 이벤트 흐름/성과를 추적할지 정의합니다. (이벤트 컨텍스트)',
          'Campaign 속성 키는 템플릿의 campaign.* 변수 카탈로그에 자동 반영됩니다.',
          'Campaign 생성 시 Segment를 연결해 두면 대상 전략을 함께 관리할 수 있습니다.'
        ]}
        note="요약: Segment는 대상, Campaign은 이벤트/성과 컨텍스트입니다."
      />

      {(error || formError) && (
        <div className="rounded-xl border border-rose-700/60 bg-rose-900/20 p-3 text-sm text-rose-100">
          {formError || error}
        </div>
      )}

      <section className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-5 backdrop-blur">
        <Input
          label="Search Campaign"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="campaign name..."
        />
      </section>

      <section className="overflow-hidden rounded-2xl border border-slate-800/80 bg-slate-900/60 backdrop-blur">
        <table className="min-w-full divide-y divide-slate-800">
          <thead className="bg-slate-800/60">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">ID</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Name</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Created</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-300">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800">
            {loading ? (
              <tr>
                <td colSpan={4} className="px-4 py-8 text-center text-sm text-slate-300">
                  Loading campaigns...
                </td>
              </tr>
            ) : filteredCampaigns.length === 0 ? (
              <tr>
                <td colSpan={4} className="px-4 py-8 text-center text-sm text-slate-300">
                  No campaigns
                </td>
              </tr>
            ) : (
              filteredCampaigns.map((campaign) => (
                <tr
                  key={campaign.id}
                  className="cursor-pointer hover:bg-slate-800/30"
                  onClick={() => handleOpenDetail(campaign.id)}
                >
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">{campaign.id}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm font-semibold text-white">{campaign.name}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-400">
                    {formatDateTime(campaign.createdAt)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm">
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleOpenEdit(campaign.id);
                        }}
                      >
                        Edit
                      </Button>
                      <Button
                        size="sm"
                        variant="danger"
                        loading={deletingId === campaign.id}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDelete(campaign.id);
                        }}
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

      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Create Campaign"
        size="lg"
      >
        <div className="space-y-4">
          <Input
            label="Campaign Name"
            value={form.name}
            onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
            placeholder="campaign_black_friday_2026"
            required
          />

          <div className="space-y-2 rounded-lg border border-slate-700/70 bg-slate-950/40 p-3">
            <div className="flex items-center justify-between">
              <p className="text-sm font-semibold text-slate-200">Property Schema</p>
              <Button size="sm" variant="secondary" onClick={handleAddProperty}>
                Add Property
              </Button>
            </div>
            {form.properties.map((property, index) => (
              <div key={`property-${index}`} className="grid grid-cols-1 gap-2 md:grid-cols-[1fr,1fr,auto]">
                <Input
                  value={property.key}
                  onChange={(e) => handleUpdateProperty(index, { ...property, key: e.target.value })}
                  placeholder="key (예: eventCount)"
                />
                <Input
                  value={property.value}
                  onChange={(e) => handleUpdateProperty(index, { ...property, value: e.target.value })}
                  placeholder="sample/default value (예: 0)"
                />
                <Button
                  size="sm"
                  variant="danger"
                  onClick={() => handleRemoveProperty(index)}
                  disabled={form.properties.length <= 1}
                >
                  Remove
                </Button>
              </div>
            ))}
          </div>

          <div className="space-y-2 rounded-lg border border-slate-700/70 bg-slate-950/40 p-3">
            <Input
              label="Linked Segments Search"
              value={segmentSearch}
              onChange={(e) => setSegmentSearch(e.target.value)}
              placeholder="segment name..."
            />
            <div className="max-h-44 space-y-2 overflow-auto pr-1">
              {filteredSegments.map((segment) => (
                <label
                  key={segment.id}
                  className="flex cursor-pointer items-center gap-2 rounded-md border border-slate-700/70 px-2 py-2 text-sm text-slate-200 hover:border-slate-500"
                >
                  <input
                    type="checkbox"
                    checked={form.segmentIds.includes(segment.id)}
                    onChange={() => handleToggleSegment(segment.id)}
                  />
                  <span>#{segment.id} - {segment.name}</span>
                </label>
              ))}
              {filteredSegments.length === 0 && (
                <p className="text-xs text-slate-500">검색 결과가 없습니다.</p>
              )}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3 pt-2">
            <Button onClick={handleCreate} loading={saving}>Create</Button>
            <Button variant="secondary" onClick={() => setIsCreateModalOpen(false)}>Cancel</Button>
          </div>
        </div>
      </Modal>

      <Modal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        title={editingCampaignId !== null ? `Edit Campaign #${editingCampaignId}` : 'Edit Campaign'}
        size="lg"
      >
        <div className="space-y-4">
          <Input
            label="Campaign Name"
            value={editForm.name}
            onChange={(e) => setEditForm((prev) => ({ ...prev, name: e.target.value }))}
            placeholder="campaign_name"
            required
          />

          <div className="space-y-2 rounded-lg border border-slate-700/70 bg-slate-950/40 p-3">
            <div className="flex items-center justify-between">
              <p className="text-sm font-semibold text-slate-200">Property Schema</p>
              <Button size="sm" variant="secondary" onClick={handleAddEditProperty}>
                Add Property
              </Button>
            </div>
            {editForm.properties.map((property, index) => (
              <div key={`edit-property-${index}`} className="grid grid-cols-1 gap-2 md:grid-cols-[1fr,1fr,auto]">
                <Input
                  value={property.key}
                  onChange={(e) => handleEditProperty(index, { ...property, key: e.target.value })}
                  placeholder="key"
                />
                <Input
                  value={property.value}
                  onChange={(e) => handleEditProperty(index, { ...property, value: e.target.value })}
                  placeholder="value"
                />
                <Button
                  size="sm"
                  variant="danger"
                  onClick={() => handleRemoveEditProperty(index)}
                  disabled={editForm.properties.length <= 1}
                >
                  Remove
                </Button>
              </div>
            ))}
          </div>

          <div className="space-y-2 rounded-lg border border-slate-700/70 bg-slate-950/40 p-3">
            <Input
              label="Linked Segments Search"
              value={editSegmentSearch}
              onChange={(e) => setEditSegmentSearch(e.target.value)}
              placeholder="segment name..."
            />
            <div className="max-h-44 space-y-2 overflow-auto pr-1">
              {filteredEditSegments.map((segment) => (
                <label
                  key={`edit-segment-${segment.id}`}
                  className="flex cursor-pointer items-center gap-2 rounded-md border border-slate-700/70 px-2 py-2 text-sm text-slate-200 hover:border-slate-500"
                >
                  <input
                    type="checkbox"
                    checked={editForm.segmentIds.includes(segment.id)}
                    onChange={() => handleToggleEditSegment(segment.id)}
                  />
                  <span>#{segment.id} - {segment.name}</span>
                </label>
              ))}
              {filteredEditSegments.length === 0 && (
                <p className="text-xs text-slate-500">검색 결과가 없습니다.</p>
              )}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3 pt-2">
            <Button onClick={handleSaveEdit} loading={saving}>Save</Button>
            <Button variant="secondary" onClick={() => setIsEditModalOpen(false)}>Cancel</Button>
          </div>
        </div>
      </Modal>

      <Modal
        isOpen={selectedCampaignId !== null}
        onClose={() => setSelectedCampaignId(null)}
        title={selectedCampaignId !== null ? `Campaign #${selectedCampaignId}` : 'Campaign Detail'}
        size="lg"
      >
        {selectedCampaignId !== null && detailLoadingId === selectedCampaignId && !selectedCampaign ? (
          <p className="text-sm text-slate-300">Loading campaign detail...</p>
        ) : !selectedCampaign ? (
          <p className="text-sm text-slate-300">상세 정보를 불러오지 못했습니다.</p>
        ) : (
          <div className="space-y-4">
            <div className="grid grid-cols-1 gap-3 text-sm text-slate-200 md:grid-cols-2">
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Name</p>
                <p>{selectedCampaign.name}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Created</p>
                <p>{formatDateTime(selectedCampaign.createdAt)}</p>
              </div>
            </div>

            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Properties</p>
              <div className="mt-2 max-h-48 overflow-auto rounded-lg border border-slate-700 bg-slate-950/70">
                <table className="min-w-full divide-y divide-slate-800">
                  <thead className="bg-slate-800/50">
                    <tr>
                      <th className="px-3 py-2 text-left text-xs text-slate-300">Key</th>
                      <th className="px-3 py-2 text-left text-xs text-slate-300">Value</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {selectedCampaign.properties.map((property, index) => (
                      <tr key={`${property.key}-${index}`}>
                        <td className="px-3 py-2 text-xs text-slate-200">{property.key}</td>
                        <td className="px-3 py-2 text-xs text-slate-300">{property.value}</td>
                      </tr>
                    ))}
                    {selectedCampaign.properties.length === 0 && (
                      <tr>
                        <td colSpan={2} className="px-3 py-3 text-center text-xs text-slate-400">
                          No properties
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Linked Segments</p>
              <ul className="mt-2 space-y-1 text-sm text-slate-300">
                {selectedCampaign.segmentIds.length === 0 ? (
                  <li className="text-slate-400">연결된 segment가 없습니다.</li>
                ) : (
                  selectedCampaign.segmentIds.map((segmentId) => (
                    <li key={segmentId}>
                      #{segmentId} {segmentNameById.get(segmentId) ? `- ${segmentNameById.get(segmentId)}` : ''}
                    </li>
                  ))
                )}
              </ul>
            </div>

            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Raw JSON</p>
              <pre className="mt-2 max-h-60 overflow-auto rounded-lg border border-slate-700 bg-slate-950/80 p-3 text-xs text-slate-200">
                {JSON.stringify(selectedCampaign, null, 2)}
              </pre>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
