// 이벤트 도메인 모델 정의
export interface EventDto {
  id: number;
  name: string;
  externalId?: string; // Optional based on schema
  properties: SearchEventPropertyDto[];
  createdAt: string;
}

export interface SearchEventPropertyDto {
  key: string;
  value: string;
}

export interface PostEventPropertyDto {
  key: string;
  value: string;
}
