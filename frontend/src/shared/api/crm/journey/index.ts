import { crmApi } from '../instance';
import { createIdempotencyHeaders } from '../idempotency';
import type {
  ApiResponse,
  CreateJourneyRequest,
  Journey,
  JourneyExecution,
  JourneyExecutionHistory
} from 'shared/type';

export const journeyAPI = {
  async getJourneys(): Promise<Journey[]> {
    try {
      const response = await crmApi.get<ApiResponse<Journey[]>>('/journeys');
      return response.data.data;
    } catch (error) {
      console.error('Error fetching journeys:', error);
      return [];
    }
  },

  async createJourney(payload: CreateJourneyRequest): Promise<Journey | null> {
    try {
      const response = await crmApi.post<ApiResponse<Journey>>('/journeys', payload, {
        headers: createIdempotencyHeaders('journey-create')
      });
      return response.data.data;
    } catch (error) {
      console.error('Error creating journey:', error);
      return null;
    }
  },

  async getExecutions(params?: {
    journeyId?: number;
    eventId?: number;
    userId?: number;
  }): Promise<JourneyExecution[]> {
    try {
      const response = await crmApi.get<ApiResponse<JourneyExecution[]>>('/journeys/executions', { params });
      return response.data.data;
    } catch (error) {
      console.error('Error fetching journey executions:', error);
      return [];
    }
  },

  async getExecutionHistories(executionId: number): Promise<JourneyExecutionHistory[]> {
    try {
      const response = await crmApi.get<ApiResponse<JourneyExecutionHistory[]>>(
        `/journeys/executions/${executionId}/histories`
      );
      return response.data.data;
    } catch (error) {
      console.error('Error fetching journey execution histories:', error);
      return [];
    }
  }
};
