// API Response Types
export interface ApiResponse<T> {
  data: T;
  message: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
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

export interface BrowseUsersUseCaseOut {
  users: PageResponse<User>;
}

export interface GetTotalUserCountUseCaseOut {
  totalCount: number;
}

// Event Types
export interface EventProperty {
  key: string;
  value: string;
}

export interface Event {
  id: number;
  name: string;
  externalId?: string;
  properties: EventProperty[];
  createdAt: string;
}

export interface CreateEventRequest {
  name: string;
  campaignName?: string;
  externalId: string;
  segmentId?: number;
  properties: EventProperty[];
}

export interface CreateCampaignRequest {
  name: string;
  properties: EventProperty[];
  segmentIds?: number[];
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

export interface TemplateVariableCatalogItem {
  key: string;
  source: 'USER' | 'CAMPAIGN' | string;
  description: string;
  required?: boolean;
}

export interface TemplateVariableCatalog {
  userVariables: TemplateVariableCatalogItem[];
  campaignVariables: TemplateVariableCatalogItem[];
}

// Email Types
export interface SendEmailRequest {
  campaignId?: number;
  templateId: number;
  templateVersion?: number;
  userIds?: number[];
  segmentId?: number;
}

export interface EmailSchedule {
  taskName: string;
  campaignId?: number;
  templateId: number;
  userIds: number[];
  segmentId?: number;
  expiredTime: string;
}

export interface CreateEmailScheduleRequest {
  campaignId?: number;
  templateId: number;
  templateVersion?: number;
  userIds: number[];
  segmentId?: number;
  expiredTime: string;
}

// Webhook Types
export interface WebhookResponse {
  id: number;
  name: string;
  url: string;
  events: string[];
  active: boolean;
  createdAt?: string;
}

export interface CreateWebhookRequest {
  name: string;
  url: string;
  events: string[];
  active?: boolean;
}

export interface UpdateWebhookRequest {
  name?: string;
  url?: string;
  events?: string[];
  active?: boolean;
}

export interface WebhookDeliveryLog {
  id: number;
  webhookId: number;
  eventId: string;
  eventType: string;
  deliveryStatus: string;
  attemptCount: number;
  responseStatus?: number;
  errorMessage?: string;
  deliveredAt?: string;
}

export interface WebhookDeadLetter {
  id: number;
  webhookId: number;
  eventId: string;
  eventType: string;
  payloadJson: string;
  deliveryStatus: string;
  attemptCount: number;
  responseStatus?: number;
  errorMessage?: string;
  createdAt?: string;
}

// Segment Types
export interface SegmentCondition {
  field: string;
  operator: string;
  valueType: string;
  value: unknown;
}

export interface Segment {
  id: number;
  name: string;
  description?: string;
  active: boolean;
  conditions: SegmentCondition[];
  createdAt?: string;
}

export interface SegmentMatchedUser {
  id: number;
  externalId: string;
  email?: string;
  name?: string;
  createdAt?: string;
}

export interface SegmentRequest {
  name: string;
  description?: string;
  active?: boolean;
  conditions: SegmentCondition[];
}

export interface SegmentUpdateRequest {
  name?: string;
  description?: string;
  active?: boolean;
  conditions?: SegmentCondition[];
}

// Journey Types
export interface JourneyStepRequest {
  stepOrder: number;
  stepType: string;
  channel?: string;
  destination?: string;
  subject?: string;
  body?: string;
  variables?: Record<string, string>;
  delayMillis?: number;
  conditionExpression?: string;
  retryCount?: number;
}

export interface CreateJourneyRequest {
  name: string;
  triggerType: string;
  triggerEventName?: string;
  triggerSegmentId?: number;
  triggerSegmentEvent?: string;
  triggerSegmentWatchFields?: string[];
  triggerSegmentCountThreshold?: number;
  active?: boolean;
  steps: JourneyStepRequest[];
}

export interface JourneyStep {
  id: number;
  stepOrder: number;
  stepType: string;
  channel?: string;
  destination?: string;
  subject?: string;
  body?: string;
  variables?: Record<string, string>;
  delayMillis?: number;
  conditionExpression?: string;
  retryCount?: number;
}

export interface Journey {
  id: number;
  name: string;
  triggerType: string;
  triggerEventName?: string;
  triggerSegmentId?: number;
  triggerSegmentEvent?: string;
  triggerSegmentWatchFields?: string[];
  triggerSegmentCountThreshold?: number;
  active: boolean;
  lifecycleStatus: string;
  version: number;
  steps: JourneyStep[];
  createdAt: string;
}

export interface JourneyExecution {
  id: number;
  journeyId: number;
  eventId: number;
  userId: number;
  status: string;
  currentStepOrder: number;
  lastError?: string;
  triggerKey: string;
  startedAt: string;
  completedAt?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface JourneyExecutionHistory {
  id: number;
  journeyExecutionId: number;
  journeyStepId: number;
  status: string;
  attempt: number;
  message?: string;
  idempotencyKey?: string;
  createdAt: string;
}

// Action Types
export interface ActionDispatchRequest {
  channel: string;
  destination: string;
  subject?: string;
  body: string;
  variables?: Record<string, string>;
  campaignId?: number;
  journeyExecutionId?: number;
}

export interface ActionDispatchOut {
  status: 'SUCCESS' | 'FAILED' | string;
  channel: 'EMAIL' | 'SLACK' | 'DISCORD' | string;
  destination: string;
  providerMessageId?: string;
  errorCode?: string;
  errorMessage?: string;
}

export interface ActionDispatchHistory {
  id: number;
  channel: string;
  status: string;
  destination: string;
  subject?: string;
  body: string;
  variables?: Record<string, string>;
  providerMessageId?: string;
  errorCode?: string;
  errorMessage?: string;
  campaignId?: number;
  journeyExecutionId?: number;
  createdAt: string;
}

// Audit Types
export interface AuditLog {
  id: number;
  actorId?: string;
  action: string;
  resourceType: string;
  resourceId?: string;
  requestMethod?: string;
  requestPath?: string;
  statusCode?: number;
  detail?: string;
  createdAt?: string;
}

// Campaign Dashboard Types
export type TimeWindowUnit = 'MINUTE' | 'HOUR' | 'DAY' | 'WEEK' | 'MONTH';

export interface MetricDto {
  id?: number;
  campaignId: number;
  metricType: string;
  metricValue: number;
  timeWindowStart: string;
  timeWindowEnd: string;
  timeWindowUnit: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface DashboardSummaryDto {
  campaignId: number;
  totalEvents: number;
  eventsLast24Hours: number;
  eventsLast7Days: number;
  lastUpdated: string;
}

export interface GetCampaignDashboardUseCaseOut {
  campaignId: number;
  metrics: MetricDto[];
  summary: DashboardSummaryDto;
}

export interface CampaignEventData {
  campaignId: number;
  eventId: number;
  userId: number;
  eventName: string;
  timestamp: string;
}

export interface CampaignSummaryResponse {
  campaignId: number;
  totalEvents: number;
  eventsLast24Hours: number;
  eventsLast7Days: number;
  lastUpdated: string;
}

export interface StreamStatusResponse {
  campaignId: number;
  streamLength: number;
  checkedAt: string;
}

export interface CampaignOption {
  id: number;
  name: string;
  createdAt?: string;
}

export interface CampaignDetail {
  id: number;
  name: string;
  properties: EventProperty[];
  segmentIds: number[];
  createdAt?: string;
}

// Email History Types
export interface EmailSendHistoryDto {
  id: number;
  userId: number;
  userEmail: string;
  emailMessageId: string;
  emailBody: string;
  sendStatus: string;
  createdAt: string;
  updatedAt: string;
}

export interface BrowseEmailSendHistoriesUseCaseOut {
  histories: EmailSendHistoryDto[];
  totalCount: number;
  page: number;
  size: number;
}

// UI State Types
export type TabType =
  | 'dashboard'
  | 'campaign-dashboard'
  | 'campaigns'
  | 'webhook'
  | 'user'
  | 'event'
  | 'email-template'
  | 'email-history'
  | 'email-schedule'
  | 'segments'
  | 'journeys'
  | 'actions'
  | 'audit-logs'
  | 'feature-guide';

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
  campaignId: string;
  segmentId: string;
  expiredTime: string;
}

// Dashboard Stream UI Types
export type StreamConnectionStatus =
  | 'idle'
  | 'connecting'
  | 'connected'
  | 'reconnecting'
  | 'error'
  | 'closed';
