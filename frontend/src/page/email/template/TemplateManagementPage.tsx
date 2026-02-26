import React, { useState } from 'react';
import { Button, GuidePanel, Input, Textarea, Modal } from 'common/component';
import { useToggle } from 'common/hook';
import { useTemplates } from 'shared/hook';
import type { TemplateFormData } from 'shared/type';

export const TemplateManagementPage: React.FC = () => {
  const { templates, loading, createTemplate, deleteTemplate } = useTemplates();
  const { value: isModalOpen, setTrue: openModal, setFalse: closeModal } = useToggle();
  const [searchTerm, setSearchTerm] = useState('');
  const [formData, setFormData] = useState<TemplateFormData>({
    templateName: '',
    subject: '',
    body: '',
    variables: [''],
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
    
    const success = await createTemplate({
      ...formData,
      variables: formData.variables.filter(v => v.trim() !== '')
    });
    if (success) {
      setFormData({
        templateName: '',
        subject: '',
        body: '',
        variables: [''],
        version: 1.0
      });
      closeModal();
    }
  };

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
          'Variables에 변수 이름을 입력하면 발송 시 개인화 값으로 대체할 수 있습니다.',
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
                <tr key={template.id} className="hover:bg-gray-800/50">
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
                      onClick={() => handleDelete(template.id)}
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
            onChange={(e) => setFormData({
              ...formData, 
              variables: e.target.value.split(',').map(v => v.trim())
            })}
            placeholder="Enter variables (comma-separated)"
          />
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
