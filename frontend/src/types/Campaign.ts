export interface Campaign {
  id?: number;
  name: string;
  description?: string;
  status: 'Draft' | 'Active' | 'Archived';
}

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

export interface CampaignResponse {
  id: number;
  name: string;
  description?: string;
  status: 'Draft' | 'Active' | 'Archived';
}
