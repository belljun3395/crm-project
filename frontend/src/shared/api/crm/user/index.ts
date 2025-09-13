import { crmApi } from '../instance';
import type { 
  User, 
  CreateUserRequest, 
  ApiResponse 
} from 'shared/type';

export const userAPI = {
  // 사용자 목록 조회
  async getUsers(): Promise<User[]> {
    try {
      const response = await crmApi.get<ApiResponse<{ users: User[] }>>('/users');
      return response.data.data.users;
    } catch (error) {
      console.error('Error fetching users:', error);
      return [];
    }
  },

  // 사용자 등록
  async enrollUser(user: CreateUserRequest): Promise<User | null> {
    try {
      const response = await crmApi.post<ApiResponse<User>>('/users', user);
      return response.data.data;
    } catch (error) {
      console.error('Error enrolling user:', error);
      return null;
    }
  },

  // 사용자 수 조회
  async getUserCount(): Promise<number> {
    try {
      const response = await crmApi.get<ApiResponse<{ totalCount: number }>>('/users/count');
      return response.data.data.totalCount;
    } catch (error) {
      console.error('Error fetching user count:', error);
      return 0;
    }
  }
};