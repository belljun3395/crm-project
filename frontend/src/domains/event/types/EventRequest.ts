// 이벤트 요청 DTO 정의
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
