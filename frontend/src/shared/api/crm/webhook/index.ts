import { crmApi } from '../instance';
import type {
  WebhookResponse,
  CreateWebhookRequest,
  UpdateWebhookRequest,
  ApiResponse
} from 'shared/type';

const createIdempotencyKey = (prefix: string): string => {
  const random = Math.random().toString(36).slice(2, 10);
  return `${prefix}-${Date.now()}-${random}`;
};

export const webhookAPI = {
  // 웹훅 생성
  async createWebhook(data: CreateWebhookRequest): Promise<WebhookResponse | null> {
    try {
      const response = await crmApi.post<ApiResponse<WebhookResponse>>('/webhooks', data, {
        headers: {
          'Idempotency-Key': createIdempotencyKey('webhook-create')
        }
      });
      return response.data.data;
    } catch (error) {
      console.error('Error creating webhook:', error);
      return null;
    }
  },

  // 웹훅 수정
  async updateWebhook(id: number, data: UpdateWebhookRequest): Promise<WebhookResponse | null> {
    try {
      const response = await crmApi.put<ApiResponse<WebhookResponse>>(`/webhooks/${id}`, data, {
        headers: {
          'Idempotency-Key': createIdempotencyKey(`webhook-update-${id}`)
        }
      });
      return response.data.data;
    } catch (error) {
      console.error('Error updating webhook:', error);
      return null;
    }
  },

  // 웹훅 삭제
  async deleteWebhook(id: number): Promise<boolean> {
    try {
      await crmApi.delete(`/webhooks/${id}`);
      return true;
    } catch (error) {
      console.error('Error deleting webhook:', error);
      return false;
    }
  },

  // 웹훅 목록 조회
  async getWebhooks(): Promise<WebhookResponse[] | null> {
    try {
      const response = await crmApi.get<ApiResponse<WebhookResponse[]>>('/webhooks');
      return response.data.data;
    } catch (error) {
      console.error('Error fetching webhooks:', error);
      return null;
    }
  },

  // 웹훅 상세 조회
  async getWebhook(id: number): Promise<WebhookResponse | null> {
    try {
      const response = await crmApi.get<ApiResponse<WebhookResponse>>(`/webhooks/${id}`);
      return response.data.data;
    } catch (error) {
      console.error('Error fetching webhook:', error);
      return null;
    }
  }
};
