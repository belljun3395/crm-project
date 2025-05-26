// Email 도메인 API 함수 모음
// 이메일/템플릿/스케줄 관련 API 요청 함수들을 정의합니다.
import api from '../../../shared/api/instance';
import type { TemplateDto, TemplateHistoryDto, TemplateWithHistoryDto, EmailNotificationScheduleDto } from '../types/EmailModel';
import type { PostTemplateRequest, SendNotificationEmailRequest, PostNotificationEmailRequest } from '../types/EmailRequest';
import type { BrowseTemplateUseCaseOut, PostTemplateUseCaseOut, SendNotificationEmailUseCaseOut, PostEmailNotificationSchedulesUseCaseOut, BrowseEmailNotificationSchedulesUseCaseOut, DeleteTemplateUseCaseOut, CancelNotificationEmailUseCaseOut } from '../types/EmailResponse';

// 이메일 템플릿 목록 조회
export const getEmailTemplates = async (history?: boolean): Promise<BrowseTemplateUseCaseOut> => {
  const res = await api.get('/emails/templates', { params: { history } });
  return res.data.data;
};

// 이메일 템플릿 생성
export const createEmailTemplate = async (data: PostTemplateRequest): Promise<PostTemplateUseCaseOut> => {
  const res = await api.post('/emails/templates', data);
  return res.data.data;
};

// 알림 이메일 전송
export const sendNotificationEmail = async (data: SendNotificationEmailRequest): Promise<SendNotificationEmailUseCaseOut> => {
  const res = await api.post('/emails/send/notifications', data);
  return res.data.data;
};

// 이메일 알림 스케줄 목록 조회
export const getEmailNotificationSchedules = async (): Promise<BrowseEmailNotificationSchedulesUseCaseOut> => {
  const res = await api.get('/emails/schedules/notifications/email');
  return res.data.data;
};

// 이메일 알림 스케줄 생성
export const createEmailNotificationSchedule = async (data: PostNotificationEmailRequest): Promise<PostEmailNotificationSchedulesUseCaseOut> => {
  const res = await api.post('/emails/schedules/notifications/email', data);
  return res.data.data;
};

// 이메일 템플릿 삭제
export const deleteEmailTemplate = async (templateId: number, force?: boolean): Promise<DeleteTemplateUseCaseOut> => {
  const res = await api.delete(`/emails/templates/${templateId}`, { params: { force } });
  return res.data.data;
};

// 이메일 알림 스케줄 취소
export const cancelEmailNotificationSchedule = async (scheduleId: string): Promise<CancelNotificationEmailUseCaseOut> => {
  const res = await api.delete(`/emails/schedules/notifications/email/${scheduleId}`);
  return res.data.data;
};
