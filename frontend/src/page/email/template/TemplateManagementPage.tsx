import React, { useEffect, useMemo, useState } from 'react';
import { Button, GuidePanel, Input, Textarea, Modal } from 'common/component';
import { useToggle } from 'common/hook';
import { useCampaigns, useTemplates } from 'shared/hook';
import { COLLECTED_USER_FIELDS, EMAIL_TEMPLATE_VARIABLE_GUIDE } from 'shared/variableGuide';
import type { TemplateFormData } from 'shared/type';

export const TemplateManagementPage: React.FC = () => {
  const { templates, variableCatalog, loading, catalogLoading, createTemplate, deleteTemplate, fetchVariableCatalog } = useTemplates();
  const { campaigns } = useCampaigns();
  const { value: isModalOpen, setTrue: openModal, setFalse: closeModal } = useToggle();
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedTemplate, setSelectedTemplate] = useState<(typeof templates)[number] | null>(null);
  const [variableSearch, setVariableSearch] = useState('');
  const [catalogCampaignId, setCatalogCampaignId] = useState('');
  const [formData, setFormData] = useState<TemplateFormData>({
    templateName: '',
    subject: '',
    body: '',
    variables: [],
    version: 1.0
  });

  // 검색 필터링
  const filteredTemplates = templates.filter(template => 
    template.templateName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    template.subject.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // 폼 제출
  const handleSubmit = async () => {
    if (!formData.templateName || !formData.body) return;

    const normalizedVariables = Array.from(new Set(formData.variables.filter(v => v.trim() !== ''))).sort();

    const success = await createTemplate({
      ...formData,
      variables: normalizedVariables
    });
    if (success) {
      setFormData({
        templateName: '',
        subject: '',
        body: '',
        variables: [],
        version: 1.0
      });
      setVariableSearch('');
      setCatalogCampaignId('');
      closeModal();
    }
  };

  const toggleVariable = (key: string) => {
    setFormData((prev) => {
      const exists = prev.variables.includes(key);
      return {
        ...prev,
        variables: exists ? prev.variables.filter((variable) => variable !== key) : [...prev.variables, key]
      };
    });
  };

  useEffect(() => {
    if (!isModalOpen) {
      return;
    }

    const campaignId = Number(catalogCampaignId);
    if (Number.isInteger(campaignId) && campaignId > 0) {
      fetchVariableCatalog(campaignId);
      return;
    }

    fetchVariableCatalog();
  }, [isModalOpen, catalogCampaignId, fetchVariableCatalog]);

  const filteredUserVariables = useMemo(() => {
    const query = variableSearch.trim().toLowerCase();
    if (!query) {
      return variableCatalog.userVariables;
    }

    return variableCatalog.userVariables.filter((variable) =>
      variable.key.toLowerCase().includes(query) || variable.description.toLowerCase().includes(query)
    );
  }, [variableCatalog.userVariables, variableSearch]);

  const filteredCampaignVariables = useMemo(() => {
    const query = variableSearch.trim().toLowerCase();
    if (!query) {
      return variableCatalog.campaignVariables;
    }

    return variableCatalog.campaignVariables.filter((variable) =>
      variable.key.toLowerCase().includes(query) || variable.description.toLowerCase().includes(query)
    );
  }, [variableCatalog.campaignVariables, variableSearch]);

  // 템플릿 삭제
  const handleDelete = async (templateId: number) => {
    if (window.confirm('Are you sure you want to delete this template?')) {
      await deleteTemplate(templateId);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">Email Templates</h1>
        <Button onClick={openModal}>
          <span className="text-lg mr-2">+</span>
          New Template
        </Button>
      </div>

      <GuidePanel
        description="반복 발송에 사용할 이메일 문구를 미리 만들어 관리하는 화면입니다."
        items={[
          'New Template에서 제목과 본문을 입력해 템플릿을 등록합니다.',
          'Variables는 검색 + 체크박스 방식으로 선택할 수 있습니다.',
          '공통 사용자 필드는 externalId, userAttributes.email(필수), userAttributes.name(권장)입니다.',
          '검색창으로 템플릿 이름이나 제목을 빠르게 찾을 수 있습니다.'
        ]}
        note="자주 바뀌는 문구를 템플릿으로 분리해두면 운영 속도가 빨라집니다."
      />

      {/* 검색 */}
      <div className="relative">
        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">🔍</span>
        <Input
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="Search templates..."
          className="pl-12"
        />
      </div>

      {/* 템플릿 테이블 */}
      <div className="overflow-hidden rounded-xl border border-gray-800 bg-gray-900">
        <table className="min-w-full divide-y divide-gray-800">
          <thead className="bg-gray-800/50">
            <tr>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">Template Name</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">Subject</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">Version</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">Created</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-800">
            {loading ? (
              <tr>
                <td colSpan={5} className="px-6 py-8 text-center text-gray-400">
                  Loading templates...
                </td>
              </tr>
            ) : filteredTemplates.length > 0 ? (
              filteredTemplates.map((template) => (
                <tr
                  key={template.id}
                  className="cursor-pointer hover:bg-gray-800/50"
                  onClick={() => setSelectedTemplate(template)}
                >
                  <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-white">
                    {template.templateName}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-400 max-w-xs truncate">
                    {template.subject}
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-400">
                    {template.version}
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-400">
                    {new Date(template.createdAt).toLocaleDateString()}
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm">
                    <Button
                      variant="danger"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDelete(template.id);
                      }}
                    >
                      Delete
                    </Button>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={5} className="px-6 py-8 text-center text-gray-400">
                  No templates found
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <Modal
        isOpen={Boolean(selectedTemplate)}
        onClose={() => setSelectedTemplate(null)}
        title={selectedTemplate ? `Template #${selectedTemplate.id}` : 'Template Detail'}
        size="lg"
      >
        {selectedTemplate && (
          <div className="space-y-4">
            <div className="grid grid-cols-1 gap-3 text-sm text-slate-200 md:grid-cols-2">
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Template Name</p>
                <p>{selectedTemplate.templateName}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Version</p>
                <p>{selectedTemplate.version}</p>
              </div>
              <div className="md:col-span-2">
                <p className="text-xs uppercase tracking-wide text-slate-400">Subject</p>
                <p>{selectedTemplate.subject || '-'}</p>
              </div>
              <div className="md:col-span-2">
                <p className="text-xs uppercase tracking-wide text-slate-400">Variables</p>
                <p>{selectedTemplate.variables.join(', ') || '-'}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">Created</p>
                <p>{new Date(selectedTemplate.createdAt).toLocaleString()}</p>
              </div>
            </div>

            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Body</p>
              <pre className="mt-2 max-h-[260px] overflow-auto whitespace-pre-wrap rounded-lg border border-slate-700 bg-slate-950/80 p-3 text-xs text-slate-200">
                {selectedTemplate.body}
              </pre>
            </div>

            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Raw JSON</p>
              <pre className="mt-2 max-h-[240px] overflow-auto rounded-lg border border-slate-700 bg-slate-950/80 p-3 text-xs text-slate-200">
                {JSON.stringify(selectedTemplate, null, 2)}
              </pre>
            </div>
          </div>
        )}
      </Modal>

      {/* 템플릿 생성 모달 */}
      <Modal isOpen={isModalOpen} onClose={closeModal} title="Create Template" size="lg">
        <div className="space-y-4">
          <Input
            label="Template Name"
            value={formData.templateName}
            onChange={(e) => setFormData({...formData, templateName: e.target.value})}
            placeholder="Enter template name"
            required
          />
          <Input
            label="Email Subject"
            value={formData.subject}
            onChange={(e) => setFormData({...formData, subject: e.target.value})}
            placeholder="Enter email subject"
          />
          <Textarea
            label="Email Body"
            value={formData.body}
            onChange={(e) => setFormData({...formData, body: e.target.value})}
            placeholder="Enter email body"
            rows={6}
            required
          />
          <Input
            label="Variables"
            value={formData.variables.join(', ')}
            readOnly
            placeholder="아래 목록에서 변수를 선택하세요"
          />

          <div className="space-y-3 rounded-lg border border-slate-700/70 bg-slate-900/60 p-3">
            <p className="text-sm font-semibold text-slate-100">변수 카탈로그 (검색 + 선택)</p>
            <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
              <Input
                label="Variable Search"
                value={variableSearch}
                onChange={(e) => setVariableSearch(e.target.value)}
                placeholder="user.email, campaign.eventCount"
              />
              <div>
                <label className="mb-2 block text-sm font-medium text-slate-200">Campaign Catalog Scope</label>
                <select
                  value={catalogCampaignId}
                  onChange={(e) => setCatalogCampaignId(e.target.value)}
                  className="w-full rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-white focus:border-[#22c55e] focus:outline-none focus:ring-2 focus:ring-[#22c55e]"
                >
                  <option value="">전체 캠페인 기준</option>
                  {campaigns.map((campaign) => (
                    <option key={campaign.id} value={campaign.id}>
                      #{campaign.id} - {campaign.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
              <div className="rounded-lg border border-slate-700/60 bg-slate-950/40 p-3">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-300">User Variables</p>
                <div className="mt-2 max-h-48 space-y-2 overflow-auto pr-1">
                  {filteredUserVariables.length === 0 ? (
                    <p className="text-xs text-slate-500">검색 결과 없음</p>
                  ) : (
                    filteredUserVariables.map((variable) => (
                      <label key={variable.key} className="block cursor-pointer rounded-md border border-slate-700/70 px-2 py-2 text-xs text-slate-200 hover:border-slate-500">
                        <input
                          type="checkbox"
                          className="mr-2 align-middle"
                          checked={formData.variables.includes(variable.key)}
                          onChange={() => toggleVariable(variable.key)}
                        />
                        <span className="font-mono">{variable.key}</span>
                        <span className="ml-2 text-slate-400">{variable.description}</span>
                      </label>
                    ))
                  )}
                </div>
              </div>

              <div className="rounded-lg border border-slate-700/60 bg-slate-950/40 p-3">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-300">Campaign Variables</p>
                <div className="mt-2 max-h-48 space-y-2 overflow-auto pr-1">
                  {catalogLoading ? (
                    <p className="text-xs text-slate-500">변수 카탈로그 로딩 중...</p>
                  ) : filteredCampaignVariables.length === 0 ? (
                    <p className="text-xs text-slate-500">선택 가능한 캠페인 변수가 없습니다.</p>
                  ) : (
                    filteredCampaignVariables.map((variable) => (
                      <label key={variable.key} className="block cursor-pointer rounded-md border border-slate-700/70 px-2 py-2 text-xs text-slate-200 hover:border-slate-500">
                        <input
                          type="checkbox"
                          className="mr-2 align-middle"
                          checked={formData.variables.includes(variable.key)}
                          onChange={() => toggleVariable(variable.key)}
                        />
                        <span className="font-mono">{variable.key}</span>
                        <span className="ml-2 text-slate-400">{variable.description}</span>
                      </label>
                    ))
                  )}
                </div>
              </div>
            </div>
          </div>

          <div className="rounded-lg border border-slate-700/70 bg-slate-900/60 p-3">
            <p className="text-sm font-semibold text-slate-100">변수 가이드</p>
            <ul className="mt-2 space-y-1 text-xs text-slate-300">
              {EMAIL_TEMPLATE_VARIABLE_GUIDE.map((guide) => (
                <li key={guide}>{guide}</li>
              ))}
            </ul>
            <p className="mt-3 text-xs font-semibold text-slate-200">공통 수집 필드</p>
            <ul className="mt-1 space-y-1 text-xs text-slate-300">
              {COLLECTED_USER_FIELDS.map((field) => (
                <li key={field.key}>
                  <span className="font-mono text-slate-200">{field.key}</span>
                  {' '}({field.required ? '필수' : '선택'})
                </li>
              ))}
            </ul>
          </div>

          <div className="flex gap-3 pt-4">
            <Button onClick={handleSubmit} loading={loading} className="flex-1">
              Create
            </Button>
            <Button onClick={closeModal} variant="secondary" className="flex-1">
              Cancel
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
