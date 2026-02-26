import { crmApi } from '../instance';
import { createIdempotencyHeaders } from '../idempotency';
import type {
  ApiResponse,
  Segment,
  SegmentRequest,
  SegmentUpdateRequest
} from 'shared/type';

interface PostSegmentUseCaseOut {
  segment: Segment;
}

export const segmentAPI = {
  async getSegments(limit = 50): Promise<Segment[]> {
    try {
      const response = await crmApi.get<ApiResponse<Segment[]>>('/segments', {
        params: { limit }
      });
      return response.data.data;
    } catch (error) {
      console.error('Error fetching segments:', error);
      throw error;
    }
  },

  async getSegment(id: number): Promise<Segment | null> {
    try {
      const response = await crmApi.get<ApiResponse<Segment>>(`/segments/${id}`);
      return response.data.data;
    } catch (error) {
      console.error('Error fetching segment:', error);
      return null;
    }
  },

  async createSegment(payload: SegmentRequest): Promise<Segment | null> {
    try {
      const response = await crmApi.post<ApiResponse<PostSegmentUseCaseOut>>('/segments', payload, {
        headers: createIdempotencyHeaders('segment-create')
      });
      return response.data.data.segment;
    } catch (error) {
      console.error('Error creating segment:', error);
      return null;
    }
  },

  async updateSegment(id: number, payload: SegmentUpdateRequest): Promise<Segment | null> {
    try {
      const response = await crmApi.put<ApiResponse<PostSegmentUseCaseOut>>(`/segments/${id}`, payload, {
        headers: createIdempotencyHeaders(`segment-update-${id}`)
      });
      return response.data.data.segment;
    } catch (error) {
      console.error('Error updating segment:', error);
      return null;
    }
  },

  async deleteSegment(id: number): Promise<boolean> {
    try {
      await crmApi.delete(`/segments/${id}`);
      return true;
    } catch (error) {
      console.error('Error deleting segment:', error);
      return false;
    }
  }
};
