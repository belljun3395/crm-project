import { useState, useEffect, useCallback } from 'react';
import { userAPI } from 'shared/api';
import type { User, CreateUserRequest } from 'shared/type';

export const useUsers = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [userCount, setUserCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 사용자 목록 조회
  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await userAPI.getUsers();
      setUsers(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch users');
    } finally {
      setLoading(false);
    }
  }, []);

  // 사용자 수 조회
  const fetchUserCount = useCallback(async () => {
    try {
      const count = await userAPI.getUserCount();
      setUserCount(count);
    } catch (err) {
      console.error('Failed to fetch user count:', err);
    }
  }, []);

  // 사용자 등록
  const enrollUser = useCallback(async (userData: CreateUserRequest): Promise<boolean> => {
    setLoading(true);
    setError(null);
    try {
      const newUser = await userAPI.enrollUser(userData);
      if (newUser) {
        setUsers(prev => [...prev, newUser]);
        setUserCount(prev => prev + 1);
        return true;
      }
      return false;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to enroll user');
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  // 초기 데이터 로드
  useEffect(() => {
    fetchUsers();
    fetchUserCount();
  }, [fetchUsers, fetchUserCount]);

  return {
    users,
    userCount,
    loading,
    error,
    fetchUsers,
    fetchUserCount,
    enrollUser,
  };
};