import { crmApi } from '../instance';
import { createIdempotencyHeaders } from '../idempotency';
import type { 
  Event, 
  CreateEventRequest,
  CreateCampaignRequest,
  ApiResponse 
} from 'shared/type';

export const eventAPI = {
  // 전체 이벤트 조회
  async getAllEvents(limit = 200): Promise<Event[]> {
    try {
      const response = await crmApi.get<ApiResponse<{ events: Event[] }>>('/events/all', {
        params: { limit }
      });
      return response.data.data.events;
    } catch (error) {
      console.error('Error browsing events:', error);
      throw error;
    }
  },

  // 이벤트 검색
  async searchEvents(eventName: string, where: string): Promise<Event[]> {
    try {
      const response = await crmApi.get<ApiResponse<{ events: Event[] }>>('/events', {
        params: { eventName, where }
      });
      return response.data.data.events;
    } catch (error) {
      console.error('Error searching events:', error);
      throw error;
    }
  },

  // 이벤트 생성
  async postEvent(event: CreateEventRequest): Promise<any> {
    try {
      const response = await crmApi.post<ApiResponse<any>>('/events', event, {
        headers: createIdempotencyHeaders('event-create')
      });
      return response.data.data;
    } catch (error) {
      console.error('Error posting event:', error);
      return null;
    }
  },

  // 캠페인 생성
  async postCampaign(campaign: CreateCampaignRequest): Promise<any> {
    try {
      const response = await crmApi.post<ApiResponse<any>>('/events/campaign', campaign, {
        headers: createIdempotencyHeaders('campaign-create')
      });
      return response.data.data;
    } catch (error) {
      console.error('Error posting campaign:', error);
      return null;
    }
  }
};
