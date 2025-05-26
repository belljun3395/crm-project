// 캠페인 도메인 모델 정의
export interface Campaign {
  id?: number;
  name: string;
  description?: string;
  status: 'Draft' | 'Active' | 'Archived';
}
