import { useState, useEffect, useCallback } from 'react';
import { templateAPI } from 'shared/api';
import type { Template, CreateTemplateRequest } from 'shared/type';

export const useTemplates = () => {
  const [templates, setTemplates] = useState<Template[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 템플릿 목록 조회
  const fetchTemplates = useCallback(async (history = false) => {
    setLoading(true);
    setError(null);
    try {
      const data = await templateAPI.getTemplates(history);
      setTemplates(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch templates');
    } finally {
      setLoading(false);
    }
  }, []);

  // 템플릿 생성
  const createTemplate = useCallback(async (templateData: CreateTemplateRequest): Promise<boolean> => {
    setLoading(true);
    setError(null);
    try {
      const result = await templateAPI.postTemplate(templateData);
      if (result) {
        // 성공 시 템플릿 목록 갱신
        await fetchTemplates();
        return true;
      }
      return false;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create template');
      return false;
    } finally {
      setLoading(false);
    }
  }, [fetchTemplates]);

  // 템플릿 삭제
  const deleteTemplate = useCallback(async (templateId: number, force = false): Promise<boolean> => {
    setLoading(true);
    setError(null);
    try {
      const success = await templateAPI.deleteTemplate(templateId, force);
      if (success) {
        // 성공 시 로컬 상태에서도 제거
        setTemplates(prev => prev.filter(template => template.id !== templateId));
        return true;
      }
      return false;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete template');
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  // 초기 데이터 로드
  useEffect(() => {
    fetchTemplates();
  }, [fetchTemplates]);

  return {
    templates,
    loading,
    error,
    fetchTemplates,
    createTemplate,
    deleteTemplate,
  };
};