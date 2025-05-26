// 캠페인 요청 DTO 정의
export interface CreateCampaignRequest {
  name: string;
  description?: string;
  status: 'Draft' | 'Active' | 'Archived';
}

export interface UpdateCampaignRequest {
  id: number;
  name: string;
  description?: string;
  status: 'Draft' | 'Active' | 'Archived';
}
