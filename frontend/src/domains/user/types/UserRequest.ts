// 사용자 요청 DTO 정의
export interface EnrollUserRequest {
  id?: number; // Optional for create, required for update
  externalId: string;
  userAttributes: string;
}
