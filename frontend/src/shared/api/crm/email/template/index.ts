import { crmApi } from '../../instance';
import type { 
  Template,
  TemplateVariableCatalog,
  CreateTemplateRequest,
  ApiResponse 
} from 'shared/type';

export const templateAPI = {
  // 템플릿 목록 조회
  async getTemplates(history = false): Promise<Template[]> {
    try {
      const response = await crmApi.get<ApiResponse<{ templates: Array<{ template: Template }> }>>('/emails/templates', {
        params: { history }
      });
      return response.data.data.templates.map((item) => item.template);
    } catch (error) {
      console.error('Error fetching templates:', error);
      return [];
    }
  },

  // 템플릿 생성
  async postTemplate(template: CreateTemplateRequest): Promise<any> {
    try {
      const response = await crmApi.post<ApiResponse<any>>('/emails/templates', template);
      return response.data.data;
    } catch (error) {
      console.error('Error posting template:', error);
      return null;
    }
  },

  async getVariableCatalog(campaignId?: number): Promise<TemplateVariableCatalog> {
    try {
      const response = await crmApi.get<ApiResponse<TemplateVariableCatalog>>('/emails/templates/variable-catalog', {
        params: campaignId ? { campaignId } : undefined
      });
      return response.data.data;
    } catch (error) {
      console.error('Error fetching template variable catalog:', error);
      return {
        userVariables: [],
        campaignVariables: []
      };
    }
  },

  // 템플릿 삭제
  async deleteTemplate(templateId: number, force = false): Promise<boolean> {
    try {
      await crmApi.delete(`/emails/templates/${templateId}`, {
        params: { force }
      });
      return true;
    } catch (error) {
      console.error('Error deleting template:', error);
      return false;
    }
  }
};
