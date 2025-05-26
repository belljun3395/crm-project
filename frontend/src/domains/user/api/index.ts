// User 도메인 API 함수 모음
// 사용자 관련 API 요청 함수들을 정의합니다.
import api from '../../../shared/api/instance';
import type { User } from '../types/UserModel';
import type { EnrollUserRequest } from '../types/UserRequest';
import type { EnrollUserUseCaseOut, BrowseUsersUseCaseOut, GetTotalUserCountUseCaseOut } from '../types/UserResponse';

// 모든 사용자 목록 조회
export const getUsers = async (): Promise<BrowseUsersUseCaseOut> => {
  const res = await api.get('/users');
  return res.data.data;
};

// 사용자 생성
export const createUser = async (userData: EnrollUserRequest): Promise<EnrollUserUseCaseOut> => {
  const res = await api.post('/users', userData);
  return res.data.data;
};

// 전체 사용자 수 조회
export const getTotalUserCount = async (): Promise<GetTotalUserCountUseCaseOut> => {
  const res = await api.get('/users/count');
  return res.data.data;
};
