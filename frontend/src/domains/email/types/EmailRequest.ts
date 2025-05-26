// 이메일 요청 DTO 정의
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
