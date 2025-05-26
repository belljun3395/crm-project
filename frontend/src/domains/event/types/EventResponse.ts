// 이벤트 응답 DTO 정의
import type { EventDto } from './EventModel';
import type { PostCampaignPropertyDto } from './EventRequest';

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
