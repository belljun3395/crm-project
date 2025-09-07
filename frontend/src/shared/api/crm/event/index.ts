import { crmApi } from '../instance';
import type { 
  Event, 
  CreateEventRequest,
  CreateCampaignRequest,
  ApiResponse 
} from 'shared/type';

export const eventAPI = {
  // 이벤트 검색
  async searchEvents(eventName: string, where: string): Promise<Event[]> {
    try {
      const response = await crmApi.get<ApiResponse<{ events: Event[] }>>('/events', {
        params: { eventName, where }
      });
      return response.data.data.events;
    } catch (error) {
      console.error('Error searching events:', error);
      return [];
    }
  },

  // 이벤트 생성
  async postEvent(event: CreateEventRequest): Promise<any> {
    try {
      const response = await crmApi.post<ApiResponse<any>>('/events', event);
      return response.data.data;
    } catch (error) {
      console.error('Error posting event:', error);
      return null;
    }
  },

  // 캠페인 생성
  async postCampaign(campaign: CreateCampaignRequest): Promise<any> {
    try {
      const response = await crmApi.post<ApiResponse<any>>('/events/campaign', campaign);
      return response.data.data;
    } catch (error) {
      console.error('Error posting campaign:', error);
      return null;
    }
  }
};