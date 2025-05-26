// 사용자 도메인 모델 정의
export interface User {
  id: number;
  externalId: string;
  userAttributes: string; // JSON string
  createdAt: string;
  updatedAt: string;
}
