// 이메일 도메인 모델 정의
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

export interface EmailNotificationScheduleDto {
  taskName: string;
  templateId: number;
  userIds: number[];
  expiredTime: string; // date-time format
}
