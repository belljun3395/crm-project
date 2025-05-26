// 캠페인 응답 DTO 정의
export interface CampaignResponse {
  id: number;
  name: string;
  description?: string;
  status: 'Draft' | 'Active' | 'Archived';
}
