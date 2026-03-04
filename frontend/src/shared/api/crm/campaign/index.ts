import { crmApi } from '../instance';
import { createIdempotencyHeaders } from '../idempotency';
import type {
  CreateCampaignRequest,
  GetCampaignDashboardUseCaseOut,
  CampaignFunnelAnalyticsResponse,
  CampaignSegmentComparisonResponse,
  CampaignSummaryResponse,
  CampaignDetail,
  CampaignOption,
  StreamStatusResponse,
  TimeWindowUnit,
  ApiResponse
} from 'shared/type';

export const campaignAPI = {
  async getCampaigns(limit = 100): Promise<CampaignOption[]> {
    try {
      const response = await crmApi.get<ApiResponse<CampaignOption[]>>('/campaigns', {
        params: { limit }
      });
      return response.data.data;
    } catch (error) {
      console.error('Error fetching campaigns:', error);
      return [];
    }
  },

  async getCampaign(campaignId: number): Promise<CampaignDetail | null> {
    try {
      const response = await crmApi.get<ApiResponse<CampaignDetail>>(`/campaigns/${campaignId}`);
      return response.data.data;
    } catch (error) {
      console.error('Error fetching campaign detail:', error);
      return null;
    }
  },

  async postCampaign(payload: CreateCampaignRequest): Promise<CampaignDetail | null> {
    try {
      const response = await crmApi.post<ApiResponse<CampaignDetail>>('/campaigns', payload, {
        headers: createIdempotencyHeaders('campaign-create')
      });
      return response.data.data;
    } catch (error) {
      console.error('Error creating campaign:', error);
      return null;
    }
  },

  async putCampaign(campaignId: number, payload: CreateCampaignRequest): Promise<CampaignDetail | null> {
    try {
      const response = await crmApi.put<ApiResponse<CampaignDetail>>(`/campaigns/${campaignId}`, payload, {
        headers: createIdempotencyHeaders(`campaign-update-${campaignId}`)
      });
      return response.data.data;
    } catch (error) {
      console.error('Error updating campaign:', error);
      return null;
    }
  },

  async deleteCampaign(campaignId: number): Promise<boolean> {
    try {
      await crmApi.delete(`/campaigns/${campaignId}`);
      return true;
    } catch (error) {
      console.error('Error deleting campaign:', error);
      return false;
    }
  },

  // 캠페인 대시보드 조회
  async getDashboard(
    campaignId: number,
    params?: {
      startTime?: string;
      endTime?: string;
      timeWindowUnit?: TimeWindowUnit;
    }
  ): Promise<GetCampaignDashboardUseCaseOut | null> {
    try {
      const response = await crmApi.get<ApiResponse<GetCampaignDashboardUseCaseOut>>(
        `/campaigns/${campaignId}/dashboard`,
        { params }
      );
      return response.data.data;
    } catch (error) {
      console.error('Error fetching campaign dashboard:', error);
      return null;
    }
  },

  // 캠페인 요약 정보 조회
  async getSummary(campaignId: number): Promise<CampaignSummaryResponse | null> {
    try {
      const response = await crmApi.get<ApiResponse<CampaignSummaryResponse>>(
        `/campaigns/${campaignId}/dashboard/summary`
      );
      return response.data.data;
    } catch (error) {
      console.error('Error fetching campaign summary:', error);
      return null;
    }
  },

  // 캠페인 스트림 상태 조회
  async getStreamStatus(campaignId: number): Promise<StreamStatusResponse | null> {
    try {
      const response = await crmApi.get<ApiResponse<StreamStatusResponse>>(
        `/campaigns/${campaignId}/dashboard/stream/status`
      );
      return response.data.data;
    } catch (error) {
      console.error('Error fetching stream status:', error);
      return null;
    }
  },

  async getFunnelAnalytics(
    campaignId: number,
    params: {
      steps: string[];
      startTime?: string;
      endTime?: string;
    }
  ): Promise<CampaignFunnelAnalyticsResponse | null> {
    try {
      const response = await crmApi.get<ApiResponse<CampaignFunnelAnalyticsResponse>>(
        `/campaigns/${campaignId}/analytics/funnel`,
        {
          params: {
            ...params,
            steps: params.steps.join(',')
          }
        }
      );
      return response.data.data;
    } catch (error) {
      console.error('Error fetching funnel analytics:', error);
      return null;
    }
  },

  async getSegmentComparison(
    campaignId: number,
    params: {
      segmentIds: number[];
      eventName?: string;
      startTime?: string;
      endTime?: string;
    }
  ): Promise<CampaignSegmentComparisonResponse | null> {
    try {
      const response = await crmApi.get<ApiResponse<CampaignSegmentComparisonResponse>>(
        `/campaigns/${campaignId}/analytics/segment-comparison`,
        {
          params: {
            ...params,
            segmentIds: params.segmentIds.join(',')
          }
        }
      );
      return response.data.data;
    } catch (error) {
      console.error('Error fetching segment comparison:', error);
      return null;
    }
  },

  // SSE stream URL 생성 (EventSource용)
  getStreamUrl(
    campaignId: number,
    params?: {
      durationSeconds?: number;
      lastEventId?: string;
    }
  ): string {
    const baseURL = (crmApi.defaults.baseURL || '/api/v1').replace(/\/$/, '');
    const url = new URL(
      `${baseURL}/campaigns/${campaignId}/dashboard/stream`,
      window.location.origin
    );

    if (params?.durationSeconds) {
      url.searchParams.set('durationSeconds', String(params.durationSeconds));
    }
    if (params?.lastEventId) {
      url.searchParams.set('lastEventId', params.lastEventId);
    }

    if (baseURL.startsWith('http://') || baseURL.startsWith('https://')) {
      return url.toString();
    }

    return `${url.pathname}${url.search}`;
  }
};
