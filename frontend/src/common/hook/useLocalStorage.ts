import { useState, useCallback, useEffect } from 'react';

// SSR 환경에서 localStorage 사용 가능 여부 확인
const isLocalStorageAvailable = () => {
  try {
    return typeof window !== 'undefined' && window.localStorage !== null;
  } catch {
    return false;
  }
};

export const useLocalStorage = <T>(key: string, initialValue: T) => {
  const [storedValue, setStoredValue] = useState<T>(() => {
    if (!isLocalStorageAvailable()) {
      return initialValue;
    }

    try {
      const item = window.localStorage.getItem(key);
      return item ? JSON.parse(item) : initialValue;
    } catch (error) {
      console.error(`Error loading localStorage key "${key}":`, error);
      return initialValue;
    }
  });

  // SSR 시 초기값과 클라이언트 값의 불일치 해결
  useEffect(() => {
    if (!isLocalStorageAvailable()) return;

    try {
      const item = window.localStorage.getItem(key);
      const parsedItem = item ? JSON.parse(item) : initialValue;
      setStoredValue(parsedItem);
    } catch (error) {
      console.error(`Error syncing localStorage key "${key}":`, error);
      // 파싱 오류 시 초기값으로 폴백
      setStoredValue(initialValue);
    }
  }, [key, initialValue]);

  const setValue = useCallback((value: T | ((val: T) => T)) => {
    if (!isLocalStorageAvailable()) {
      setStoredValue(prevValue => value instanceof Function ? value(prevValue) : value);
      return;
    }

    try {
      setStoredValue(prevValue => {
        const valueToStore = value instanceof Function ? value(prevValue) : value;
        window.localStorage.setItem(key, JSON.stringify(valueToStore));
        return valueToStore;
      });
    } catch (error) {
      console.error(`Error setting localStorage key "${key}":`, error);
    }
  }, [key]);

  const removeValue = useCallback(() => {
    if (!isLocalStorageAvailable()) {
      setStoredValue(initialValue);
      return;
    }

    try {
      window.localStorage.removeItem(key);
      setStoredValue(initialValue);
    } catch (error) {
      console.error(`Error removing localStorage key "${key}":`, error);
    }
  }, [key, initialValue]);

  return {
    value: storedValue,
    setValue,
    removeValue,
  };
};