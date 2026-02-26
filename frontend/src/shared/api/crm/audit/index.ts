import { crmApi } from '../instance';
import type {
  ApiResponse,
  AuditLog
} from 'shared/type';

export const auditAPI = {
  async getAuditLogs(params?: {
    limit?: number;
    action?: string;
    resourceType?: string;
    actorId?: string;
  }): Promise<AuditLog[]> {
    try {
      const response = await crmApi.get<ApiResponse<AuditLog[]>>('/audit-logs', {
        params
      });
      return response.data.data;
    } catch (error) {
      console.error('Error fetching audit logs:', error);
      throw error;
    }
  }
};
