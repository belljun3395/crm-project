export interface EventDto {
  id: number;
  name: string;
  externalId?: string; // Optional based on schema
  properties: SearchEventPropertyDto[];
  createdAt: string;
}

export interface SearchEventPropertyDto {
  key: string;
  value: string;
}

export interface PostEventPropertyDto {
  key: string;
  value: string;
}

export interface PostEventRequest {
  name: string;
  campaignName?: string; // Optional based on schema
  externalId: string;
  properties: PostEventPropertyDto[];
}

export interface PostCampaignPropertyDto {
  key: string;
  value: string;
}

export interface PostCampaignRequest {
  name: string;
  properties: PostCampaignPropertyDto[];
}

// API 응답 데이터 부분의 타입들
export interface SearchEventsUseCaseOut {
  events: EventDto[];
}

export interface PostEventUseCaseOut {
  id: number;
  message: string;
}

export interface PostCampaignUseCaseOut {
  id: number;
  name: string;
  properties: PostCampaignPropertyDto[];
} 