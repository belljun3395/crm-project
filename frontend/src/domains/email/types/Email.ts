export interface TemplateDto {
  id: number;
  templateName: string;
  subject: string;
  body: string;
  variables: string[];
  version: number;
  createdAt: string;
}

export interface TemplateHistoryDto {
  id: number;
  templateId: number;
  subject: string;
  body: string;
  variables: string[];
  version: number;
  createdAt: string;
}

export interface TemplateWithHistoryDto {
  template: TemplateDto;
  histories: TemplateHistoryDto[];
}

export interface PostTemplateRequest {
  id?: number; // Update 시 사용될 수 있음
  templateName: string;
  subject?: string;
  version?: number; // 서버에서 관리될 수도 있지만, 스키마에 있으니 포함
  body: string;
  variables?: string[];
}

export interface SendNotificationEmailRequest {
  templateId: number;
  templateVersion?: number;
  userIds?: number[];
}

export interface PostNotificationEmailRequest {
  templateId: number;
  templateVersion?: number;
  userIds: number[];
  expiredTime: string; // date-time format
}

export interface EmailNotificationScheduleDto {
  taskName: string;
  templateId: number;
  userIds: number[];
  expiredTime: string; // date-time format
}

// API 응답 데이터 부분의 타입들
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