// 이메일 응답 DTO 정의
import type { TemplateWithHistoryDto, EmailNotificationScheduleDto } from './EmailModel';

export interface BrowseTemplateUseCaseOut {
  templates: TemplateWithHistoryDto[];
}

export interface PostTemplateUseCaseOut {
  id: number;
  templateName: string;
  version: number;
}

export interface SendNotificationEmailUseCaseOut {
  isSuccess: boolean;
}

export interface PostEmailNotificationSchedulesUseCaseOut {
  newSchedule: string; // taskName
}

export interface BrowseEmailNotificationSchedulesUseCaseOut {
  schedules: EmailNotificationScheduleDto[];
}

export interface DeleteTemplateUseCaseOut {
  success: boolean;
}

export interface CancelNotificationEmailUseCaseOut {
  success: boolean;
}
