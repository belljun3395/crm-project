// 사용자 응답 DTO 정의
import type { User } from './UserModel';

export interface EnrollUserUseCaseOut extends User {}

export interface BrowseUsersUseCaseOut {
  users: User[];
}

export interface GetTotalUserCountUseCaseOut {
  totalCount: number;
}
