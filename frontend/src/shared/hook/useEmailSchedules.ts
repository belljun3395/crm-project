import { useState, useEffect, useCallback } from 'react';
import { emailScheduleAPI } from 'shared/api';
import type { EmailSchedule, CreateEmailScheduleRequest, SendEmailRequest } from 'shared/type';

export const useEmailSchedules = () => {
  const [schedules, setSchedules] = useState<EmailSchedule[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 이메일 스케줄 목록 조회
  const fetchSchedules = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await emailScheduleAPI.getEmailSchedules();
      setSchedules(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch email schedules');
    } finally {
      setLoading(false);
    }
  }, []);

  // 알림 이메일 즉시 전송
  const sendNotificationEmail = useCallback(async (emailData: SendEmailRequest): Promise<boolean> => {
    setLoading(true);
    setError(null);
    try {
      const success = await emailScheduleAPI.sendNotificationEmail(emailData);
      return success;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to send notification email');
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  // 이메일 스케줄 생성
  const createSchedule = useCallback(async (scheduleData: CreateEmailScheduleRequest): Promise<boolean> => {
    setLoading(true);
    setError(null);
    try {
      const result = await emailScheduleAPI.postEmailSchedule(scheduleData);
      if (result) {
        // 성공 시 스케줄 목록 갱신
        await fetchSchedules();
        return true;
      }
      return false;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create email schedule');
      return false;
    } finally {
      setLoading(false);
    }
  }, [fetchSchedules]);

  // 이메일 스케줄 취소
  const cancelSchedule = useCallback(async (scheduleId: string): Promise<boolean> => {
    setLoading(true);
    setError(null);
    try {
      const success = await emailScheduleAPI.cancelEmailSchedule(scheduleId);
      if (success) {
        // 성공 시 로컬 상태에서도 제거
        setSchedules(prev => prev.filter(schedule => schedule.taskName !== scheduleId));
        return true;
      }
      return false;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to cancel email schedule');
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  // 초기 데이터 로드
  useEffect(() => {
    fetchSchedules();
  }, [fetchSchedules]);

  return {
    schedules,
    loading,
    error,
    fetchSchedules,
    sendNotificationEmail,
    createSchedule,
    cancelSchedule,
  };
};