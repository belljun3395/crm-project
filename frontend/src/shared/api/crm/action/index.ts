import { crmApi } from '../instance';
import { createIdempotencyHeaders } from '../idempotency';
import type {
  ApiResponse,
  ActionDispatchRequest,
  ActionDispatchOut,
  ActionDispatchHistory
} from 'shared/type';

export const actionAPI = {
  async dispatch(payload: ActionDispatchRequest): Promise<ActionDispatchOut | null> {
    try {
      const response = await crmApi.post<ApiResponse<ActionDispatchOut>>('/actions/dispatch', payload, {
        headers: createIdempotencyHeaders('action-dispatch')
      });
      return response.data.data;
    } catch (error) {
      console.error('Error dispatching action:', error);
      return null;
    }
  },

  async getDispatchHistories(params?: {
    campaignId?: number;
    journeyExecutionId?: number;
  }): Promise<ActionDispatchHistory[]> {
    try {
      const response = await crmApi.get<ApiResponse<ActionDispatchHistory[]>>('/actions/dispatch/histories', {
        params
      });
      return response.data.data;
    } catch (error) {
      console.error('Error fetching action dispatch histories:', error);
      return [];
    }
  }
};
