// Event 도메인 API 함수 모음
// 이벤트/캠페인 관련 API 요청 함수들을 정의합니다.
import api from '../../../shared/api/instance';
import type { EventDto, SearchEventPropertyDto, PostEventPropertyDto } from '../types/EventModel';
import type { PostEventRequest, PostCampaignPropertyDto, PostCampaignRequest } from '../types/EventRequest';
import type { SearchEventsUseCaseOut, PostEventUseCaseOut, PostCampaignUseCaseOut } from '../types/EventResponse';

// 이벤트 검색
export const searchEvents = async (eventName: string, where: string): Promise<SearchEventsUseCaseOut> => {
  const res = await api.get('/events', { params: { eventName, where } });
  return res.data.data;
};

// 이벤트 생성
export const createEvent = async (data: PostEventRequest): Promise<PostEventUseCaseOut> => {
  const res = await api.post('/events', data);
  return res.data.data;
};

// 캠페인 생성
export const createCampaign = async (data: PostCampaignRequest): Promise<PostCampaignUseCaseOut> => {
  const res = await api.post('/events/campaign', data);
  return res.data.data;
};
