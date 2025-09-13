import { useState, useEffect, useCallback, useRef } from 'react';
import { userAPI } from 'shared/api';
import type { User, CreateUserRequest } from 'shared/type';

export const useUsers = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [userCount, setUserCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [enrolling, setEnrolling] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  // 사용자 목록 조회
  const fetchUsers = useCallback(async () => {
    // Cancel previous request if still pending
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    
    const abortController = new AbortController();
    abortControllerRef.current = abortController;
    
    setLoading(true);
    setError(null);
    try {
      const data = await userAPI.getUsers();
      if (!abortController.signal.aborted) {
        setUsers(data);
      }
    } catch (err) {
      if (!abortController.signal.aborted) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to fetch users';
        setError(errorMessage);
        console.error('Failed to fetch users:', err);
      }
    } finally {
      if (!abortController.signal.aborted) {
        setLoading(false);
      }
    }
  }, []);

  // 사용자 수 조회
  const fetchUserCount = useCallback(async () => {
    try {
      const count = await userAPI.getUserCount();
      setUserCount(count);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to fetch user count';
      console.error('Failed to fetch user count:', errorMessage);
    }
  }, []);

  // 사용자 등록
  const enrollUser = useCallback(async (userData: CreateUserRequest): Promise<boolean> => {
    setEnrolling(true);
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
      const errorMessage = err instanceof Error ? err.message : 'Failed to enroll user';
      setError(errorMessage);
      console.error('Failed to enroll user:', err);
      return false;
    } finally {
      setEnrolling(false);
    }
  }, []);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
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
    enrolling,
    error,
    fetchUsers,
    fetchUserCount,
    enrollUser,
  };
};