import React, { useState } from 'react';
import { Button, GuidePanel, Input, Modal } from 'common/component';
import { useToggle } from 'common/hook';
import { useEmailSchedules, useTemplates } from 'shared/hook';
import type { EmailScheduleFormData } from 'shared/type';

interface SendNowFormState {
  templateId: number;
  userIds: string;
  campaignId: string;
  segmentId: string;
}

const parseIdList = (raw: string): number[] =>
  raw
    .split(',')
    .map((id) => Number(id.trim()))
    .filter((id) => Number.isInteger(id) && id > 0);

const parseOptionalPositiveNumber = (raw: string): number | undefined => {
  const parsed = Number(raw);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return undefined;
  }
  return parsed;
};

export const ScheduleManagementPage: React.FC = () => {
  const { schedules, loading, createSchedule, cancelSchedule, sendNotificationEmail } = useEmailSchedules();
  const { templates } = useTemplates();
  const { value: isModalOpen, setTrue: openModal, setFalse: closeModal } = useToggle();
  const [isSendNowOpen, setIsSendNowOpen] = useState(false);
  const [formData, setFormData] = useState<EmailScheduleFormData>({
    templateId: 0,
    userIds: '',
    segmentId: '',
    expiredTime: ''
  });
  const [sendNowForm, setSendNowForm] = useState<SendNowFormState>({
    templateId: 0,
    userIds: '',
    campaignId: '',
    segmentId: ''
  });

  // 폼 제출
  const handleSubmit = async () => {
    if (!formData.templateId || !formData.expiredTime) return;

    const userIds = parseIdList(formData.userIds);
    const segmentId = parseOptionalPositiveNumber(formData.segmentId);
    if (userIds.length === 0 && !segmentId) return;

    const success = await createSchedule({
      templateId: formData.templateId,
      userIds,
      segmentId,
      expiredTime: formData.expiredTime
    });

    if (success) {
      setFormData({
        templateId: 0,
        userIds: '',
        segmentId: '',
        expiredTime: ''
      });
      closeModal();
    }
  };

  // 스케줄 취소
  const handleCancel = async (scheduleId: string) => {
    if (window.confirm('Are you sure you want to cancel this email schedule?')) {
      await cancelSchedule(scheduleId);
    }
  };

  // 즉시 발송
  const handleSendNow = async () => {
    if (!sendNowForm.templateId) return;

    const userIds = parseIdList(sendNowForm.userIds);
    const campaignId = parseOptionalPositiveNumber(sendNowForm.campaignId);
    const segmentId = parseOptionalPositiveNumber(sendNowForm.segmentId);
    if (userIds.length === 0 && !segmentId) return;

    const success = await sendNotificationEmail({
      templateId: sendNowForm.templateId,
      userIds,
      campaignId,
      segmentId
    });

    if (success) {
      setSendNowForm({
        templateId: 0,
        userIds: '',
        campaignId: '',
        segmentId: ''
      });
      setIsSendNowOpen(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">Email Schedules</h1>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={() => setIsSendNowOpen(true)}>
            Send Now
          </Button>
          <Button onClick={openModal}>
            <span className="text-lg mr-2">+</span>
            Schedule Email
          </Button>
        </div>
      </div>

      <GuidePanel
        description="특정 시점까지 반복 또는 예약 발송할 이메일 작업을 등록하는 화면입니다."
        items={[
          'Schedule Email에서 템플릿, 대상 사용자/세그먼트, 만료 시간을 입력합니다.',
          'Send Now는 즉시 발송 API를 호출해 현재 시점에 바로 전송합니다.',
          'User IDs는 쉼표로 여러 명을 입력할 수 있습니다.',
          '목록에서 Cancel을 누르면 예약 작업을 중지합니다.'
        ]}
        note="만료 시간이 지나면 해당 스케줄은 자동 종료됩니다."
      />

      {/* 스케줄 테이블 */}
      <div className="overflow-hidden rounded-xl border border-gray-800 bg-gray-900">
        <table className="min-w-full divide-y divide-gray-800">
          <thead className="bg-gray-800/50">
            <tr>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">Task Name</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">Template ID</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">User IDs</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">Expiry Time</th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-white">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-800">
            {loading ? (
              <tr>
                <td colSpan={5} className="px-6 py-8 text-center text-gray-400">
                  Loading schedules...
                </td>
              </tr>
            ) : schedules.length > 0 ? (
              schedules.map((schedule, index) => (
                <tr key={index} className="hover:bg-gray-800/50">
                  <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-white">
                    {schedule.taskName}
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-400">
                    {schedule.templateId}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-400 max-w-xs truncate">
                    {schedule.userIds.join(', ')}
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-400">
                    {new Date(schedule.expiredTime).toLocaleString()}
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm">
                    <Button
                      variant="danger"
                      size="sm"
                      onClick={() => handleCancel(schedule.taskName)}
                    >
                      Cancel
                    </Button>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={5} className="px-6 py-8 text-center text-gray-400">
                  No email schedules found
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* 이메일 스케줄 생성 모달 */}
      <Modal isOpen={isModalOpen} onClose={closeModal} title="Schedule Email">
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">
              Template <span className="text-red-400">*</span>
            </label>
            <select
              value={formData.templateId}
              onChange={(e) => setFormData({...formData, templateId: parseInt(e.target.value)})}
              className="w-full rounded-lg border-gray-700 bg-gray-800 text-white px-4 py-2 focus:ring-[#22c55e] focus:border-[#22c55e]"
              required
            >
              <option value={0}>Select Template</option>
              {templates.map(template => (
                <option key={template.id} value={template.id}>
                  {template.templateName}
                </option>
              ))}
            </select>
          </div>
          
          <Input
            label="User IDs"
            value={formData.userIds}
            onChange={(e) => setFormData({...formData, userIds: e.target.value})}
            placeholder="Enter user IDs (comma-separated)"
          />

          <Input
            label="Segment ID (optional)"
            type="number"
            value={formData.segmentId}
            onChange={(e) => setFormData({...formData, segmentId: e.target.value})}
            placeholder="1"
          />

          <Input
            label="Expiry Time"
            type="datetime-local"
            value={formData.expiredTime}
            onChange={(e) => setFormData({...formData, expiredTime: e.target.value})}
            required
          />

          <div className="flex gap-3 pt-4">
            <Button onClick={handleSubmit} loading={loading} className="flex-1">
              Schedule
            </Button>
            <Button onClick={closeModal} variant="secondary" className="flex-1">
              Cancel
            </Button>
          </div>
        </div>
      </Modal>

      <Modal isOpen={isSendNowOpen} onClose={() => setIsSendNowOpen(false)} title="Send Notification Now">
        <div className="space-y-4">
          <div>
            <label className="mb-2 block text-sm font-medium text-gray-300">
              Template <span className="text-red-400">*</span>
            </label>
            <select
              value={sendNowForm.templateId}
              onChange={(e) => setSendNowForm({ ...sendNowForm, templateId: Number(e.target.value) })}
              className="w-full rounded-lg border-gray-700 bg-gray-800 px-4 py-2 text-white focus:border-[#22c55e] focus:ring-[#22c55e]"
              required
            >
              <option value={0}>Select Template</option>
              {templates.map((template) => (
                <option key={template.id} value={template.id}>
                  {template.templateName}
                </option>
              ))}
            </select>
          </div>

          <Input
            label="User IDs"
            value={sendNowForm.userIds}
            onChange={(e) => setSendNowForm({ ...sendNowForm, userIds: e.target.value })}
            placeholder="1001, 1002"
          />

          <Input
            label="Segment ID (optional)"
            type="number"
            value={sendNowForm.segmentId}
            onChange={(e) => setSendNowForm({ ...sendNowForm, segmentId: e.target.value })}
            placeholder="1"
          />

          <Input
            label="Campaign ID (optional)"
            type="number"
            value={sendNowForm.campaignId}
            onChange={(e) => setSendNowForm({ ...sendNowForm, campaignId: e.target.value })}
            placeholder="1"
          />

          <p className="text-xs text-slate-400">
            User IDs 또는 Segment ID 중 하나는 필수입니다.
          </p>

          <div className="flex gap-3 pt-2">
            <Button onClick={handleSendNow} loading={loading} className="flex-1">
              Send
            </Button>
            <Button variant="secondary" onClick={() => setIsSendNowOpen(false)} className="flex-1">
              Cancel
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
