import { crmApi } from '../../instance';
import type { 
  EmailSchedule,
  CreateEmailScheduleRequest,
  SendEmailRequest,
  ApiResponse 
} from 'shared/type';

export const emailScheduleAPI = {
  // 알림 이메일 전송
  async sendNotificationEmail(data: SendEmailRequest): Promise<boolean> {
    try {
      const response = await crmApi.post<ApiResponse<{ isSuccess: boolean }>>('/emails/send/notifications', data);
      return response.data.data.isSuccess;
    } catch (error) {
      console.error('Error sending notification email:', error);
      return false;
    }
  },

  // 이메일 스케줄 목록 조회
  async getEmailSchedules(): Promise<EmailSchedule[]> {
    try {
      const response = await crmApi.get<ApiResponse<{ schedules: EmailSchedule[] }>>('/emails/schedules/notifications/email');
      return response.data.data.schedules;
    } catch (error) {
      console.error('Error fetching email schedules:', error);
      return [];
    }
  },

  // 이메일 스케줄 생성
  async postEmailSchedule(data: CreateEmailScheduleRequest): Promise<string | null> {
    try {
      const response = await crmApi.post<ApiResponse<{ newSchedule: string }>>('/emails/schedules/notifications/email', data);
      return response.data.data.newSchedule;
    } catch (error) {
      console.error('Error posting email schedule:', error);
      return null;
    }
  },

  // 이메일 스케줄 취소
  async cancelEmailSchedule(scheduleId: string): Promise<boolean> {
    try {
      await crmApi.delete(`/emails/schedules/notifications/email/${scheduleId}`);
      return true;
    } catch (error) {
      console.error('Error canceling email schedule:', error);
      return false;
    }
  }
};