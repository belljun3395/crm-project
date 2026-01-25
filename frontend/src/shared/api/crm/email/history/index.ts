import { crmApi } from '../../instance';
import type {
  BrowseEmailSendHistoriesUseCaseOut,
  ApiResponse
} from 'shared/type';

export const emailHistoryAPI = {
  // 이메일 발송 이력 조회
  async getHistories(
    params?: {
      userId?: number;
      sendStatus?: string;
      page?: number;
      size?: number;
    }
  ): Promise<BrowseEmailSendHistoriesUseCaseOut | null> {
    try {
      const response = await crmApi.get<ApiResponse<BrowseEmailSendHistoriesUseCaseOut>>(
        `/emails/histories`,
        { params }
      );
      return response.data.data;
    } catch (error) {
      console.error('Error fetching email histories:', error);
      return null;
    }
  }
};
