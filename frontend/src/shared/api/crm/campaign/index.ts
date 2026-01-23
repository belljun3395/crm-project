import { crmApi } from '../instance';
import type {
  GetCampaignDashboardUseCaseOut,
  CampaignSummaryResponse,
  StreamStatusResponse,
  TimeWindowUnit,
  ApiResponse
} from 'shared/type';

export const campaignAPI = {
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
  }
};
