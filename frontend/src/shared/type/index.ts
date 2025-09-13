// API Response Types
export interface ApiResponse<T> {
  data: T;
  message: string;
}

// User Types
export interface User {
  id: number;
  externalId: string;
  userAttributes: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateUserRequest {
  externalId: string;
  userAttributes: string;
}

// Event Types
export interface Event {
  id: number;
  name: string;
  externalId?: string;
  properties: EventProperty[];
  createdAt: string;
}

export interface EventProperty {
  key: string;
  value: string;
}

export interface CreateEventRequest {
  name: string;
  campaignName?: string;
  externalId: string;
  properties: EventProperty[];
}

// Campaign Types
export interface CreateCampaignRequest {
  name: string;
  properties: EventProperty[];
}

// Template Types
export interface Template {
  id: number;
  templateName: string;
  subject: string;
  body: string;
  variables: string[];
  version: number;
  createdAt: string;
}

export interface CreateTemplateRequest {
  templateName: string;
  subject?: string;
  body: string;
  variables?: string[];
  version?: number;
}

// Email Types
export interface SendEmailRequest {
  campaignId?: number;
  templateId: number;
  templateVersion?: number;
  userIds?: number[];
}

export interface EmailSchedule {
  taskName: string;
  templateId: number;
  userIds: number[];
  expiredTime: string;
}

export interface CreateEmailScheduleRequest {
  templateId: number;
  templateVersion?: number;
  userIds: number[];
  expiredTime: string;
}

// UI State Types
export type TabType = 'dashboard' | 'user' | 'event' | 'email-template' | 'email-schedule';

// Form State Types
export interface UserFormData {
  externalId: string;
  userAttributes: string;
}

export interface EventFormData {
  name: string;
  campaignName: string;
  externalId: string;
  properties: EventProperty[];
}

export interface TemplateFormData {
  templateName: string;
  subject: string;
  body: string;
  variables: string[];
  version: number;
}

export interface EmailScheduleFormData {
  templateId: number;
  userIds: string;
  expiredTime: string;
}