import axios from 'axios';
import type { User, EnrollUserRequest, EnrollUserUseCaseOut, BrowseUsersUseCaseOut, GetTotalUserCountUseCaseOut } from './types/User';
import type { 
  TemplateDto, TemplateHistoryDto, TemplateWithHistoryDto, PostTemplateRequest,
  SendNotificationEmailRequest, PostNotificationEmailRequest, EmailNotificationScheduleDto,
  BrowseTemplateUseCaseOut, PostTemplateUseCaseOut, SendNotificationEmailUseCaseOut,
  PostEmailNotificationSchedulesUseCaseOut, BrowseEmailNotificationSchedulesUseCaseOut,
  DeleteTemplateUseCaseOut, CancelNotificationEmailUseCaseOut
} from './types/Email';

import type {
  EventDto, SearchEventPropertyDto, PostEventPropertyDto, PostEventRequest,
  PostCampaignPropertyDto, PostCampaignRequest,
  SearchEventsUseCaseOut, PostEventUseCaseOut, PostCampaignUseCaseOut
} from './types/Event';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1'
});

// User API
export const getUsers = async (): Promise<BrowseUsersUseCaseOut> => {
  const res = await api.get('/users');
  return res.data.data;
};

export const createUser = async (userData: EnrollUserRequest): Promise<EnrollUserUseCaseOut> => {
  const res = await api.post('/users', userData);
  return res.data.data;
};

export const getTotalUserCount = async (): Promise<GetTotalUserCountUseCaseOut> => {
  const res = await api.get('/users/count');
  return res.data.data;
};

// Email API
export const getEmailTemplates = async (history?: boolean): Promise<BrowseTemplateUseCaseOut> => {
  const res = await api.get('/emails/templates', { params: { history } });
  return res.data.data;
};

export const createEmailTemplate = async (data: PostTemplateRequest): Promise<PostTemplateUseCaseOut> => {
  const res = await api.post('/emails/templates', data);
  return res.data.data;
};

export const sendNotificationEmail = async (data: SendNotificationEmailRequest): Promise<SendNotificationEmailUseCaseOut> => {
  const res = await api.post('/emails/send/notifications', data);
  return res.data.data;
};

export const getEmailNotificationSchedules = async (): Promise<BrowseEmailNotificationSchedulesUseCaseOut> => {
  const res = await api.get('/emails/schedules/notifications/email');
  return res.data.data;
};

export const createEmailNotificationSchedule = async (data: PostNotificationEmailRequest): Promise<PostEmailNotificationSchedulesUseCaseOut> => {
  const res = await api.post('/emails/schedules/notifications/email', data);
  return res.data.data;
};

export const deleteEmailTemplate = async (templateId: number, force?: boolean): Promise<DeleteTemplateUseCaseOut> => {
  const res = await api.delete(`/emails/templates/${templateId}`, { params: { force } });
  return res.data.data;
};

export const cancelEmailNotificationSchedule = async (scheduleId: string): Promise<CancelNotificationEmailUseCaseOut> => {
  const res = await api.delete(`/emails/schedules/notifications/email/${scheduleId}`);
  return res.data.data;
};

// Event API
export const searchEvents = async (eventName: string, where: string): Promise<SearchEventsUseCaseOut> => {
  const res = await api.get('/events', { params: { eventName, where } });
  return res.data.data;
};

export const createEvent = async (data: PostEventRequest): Promise<PostEventUseCaseOut> => {
  const res = await api.post('/events', data);
  return res.data.data;
};

export const createCampaign = async (data: PostCampaignRequest): Promise<PostCampaignUseCaseOut> => {
  const res = await api.post('/events/campaign', data);
  return res.data.data;
};

// Export all as default for compatibility
export default {
  getUsers,
  createUser,
  getTotalUserCount,
  getEmailTemplates,
  createEmailTemplate,
  sendNotificationEmail,
  getEmailNotificationSchedules,
  createEmailNotificationSchedule,
  deleteEmailTemplate,
  cancelEmailNotificationSchedule,
  searchEvents,
  createEvent,
  createCampaign,
}; 