import { renderHook, act } from '@testing-library/react';
import { useLocalStorage } from './useLocalStorage';

// localStorage mock 설정
const localStorageMock = (() => {
  let store: Record<string, string> = {};
  
  return {
    getItem: jest.fn((key: string) => store[key] || null),
    setItem: jest.fn((key: string, value: string) => {
      store[key] = value.toString();
    }),
    removeItem: jest.fn((key: string) => {
      delete store[key];
    }),
    clear: jest.fn(() => {
      store = {};
    }),
  };
})();

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock,
});

// console.error mock
const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});

describe('useLocalStorage', () => {
  beforeEach(() => {
    localStorageMock.clear();
    jest.clearAllMocks();
    consoleSpy.mockClear();
  });

  it('초기값으로 시작한다', () => {
    const { result } = renderHook(() => useLocalStorage('test-key', 'initial'));
    
    expect(result.current.value).toBe('initial');
    expect(localStorageMock.getItem).toHaveBeenCalledWith('test-key');
  });

  it('localStorage에 저장된 값을 로드한다', () => {
    localStorageMock.setItem('test-key', JSON.stringify('stored-value'));
    
    const { result } = renderHook(() => useLocalStorage('test-key', 'initial'));
    
    expect(result.current.value).toBe('stored-value');
  });

  it('객체 타입을 올바르게 처리한다', () => {
    const initialObject = { name: 'John', age: 30 };
    const storedObject = { name: 'Jane', age: 25 };
    
    localStorageMock.setItem('test-object', JSON.stringify(storedObject));
    
    const { result } = renderHook(() => useLocalStorage('test-object', initialObject));
    
    expect(result.current.value).toEqual(storedObject);
  });

  it('setValue 함수가 값을 업데이트하고 localStorage에 저장한다', () => {
    const { result } = renderHook(() => useLocalStorage('test-key', 'initial'));
    
    act(() => {
      result.current.setValue('new-value');
    });
    
    expect(result.current.value).toBe('new-value');
    expect(localStorageMock.setItem).toHaveBeenCalledWith('test-key', JSON.stringify('new-value'));
  });

  it('setValue 함수가 함수 형태의 업데이트를 지원한다', () => {
    const { result } = renderHook(() => useLocalStorage('test-counter', 0));
    
    act(() => {
      result.current.setValue(prev => prev + 1);
    });
    
    expect(result.current.value).toBe(1);
    expect(localStorageMock.setItem).toHaveBeenCalledWith('test-counter', JSON.stringify(1));
  });

  it('removeValue 함수가 값을 제거하고 초기값으로 되돌린다', () => {
    const { result } = renderHook(() => useLocalStorage('test-key', 'initial'));
    
    // 값을 설정
    act(() => {
      result.current.setValue('updated');
    });
    expect(result.current.value).toBe('updated');
    
    // 값을 제거
    act(() => {
      result.current.removeValue();
    });
    
    expect(result.current.value).toBe('initial');
    expect(localStorageMock.removeItem).toHaveBeenCalledWith('test-key');
  });

  it('잘못된 JSON 파싱 시 초기값을 반환한다', () => {
    localStorageMock.setItem('test-key', 'invalid-json');
    
    const { result } = renderHook(() => useLocalStorage('test-key', 'fallback'));
    
    expect(result.current.value).toBe('fallback');
    expect(consoleSpy).toHaveBeenCalledWith(
      'Error loading localStorage key "test-key":',
      expect.any(SyntaxError)
    );
  });

  it('localStorage 접근 오류 시 에러를 처리한다', () => {
    localStorageMock.setItem.mockImplementation(() => {
      throw new Error('LocalStorage access denied');
    });
    
    const { result } = renderHook(() => useLocalStorage('test-key', 'initial'));
    
    act(() => {
      result.current.setValue('new-value');
    });
    
    expect(consoleSpy).toHaveBeenCalledWith(
      'Error setting localStorage key "test-key":',
      expect.any(Error)
    );
  });

  it('localStorage 제거 오류 시 에러를 처리한다', () => {
    localStorageMock.removeItem.mockImplementation(() => {
      throw new Error('LocalStorage remove denied');
    });
    
    const { result } = renderHook(() => useLocalStorage('test-key', 'initial'));
    
    act(() => {
      result.current.removeValue();
    });
    
    expect(consoleSpy).toHaveBeenCalledWith(
      'Error removing localStorage key "test-key":',
      expect.any(Error)
    );
  });

  it('배열 타입을 올바르게 처리한다', () => {
    const initialArray = [1, 2, 3];
    const storedArray = [4, 5, 6];
    
    localStorageMock.setItem('test-array', JSON.stringify(storedArray));
    
    const { result } = renderHook(() => useLocalStorage('test-array', initialArray));
    
    expect(result.current.value).toEqual(storedArray);
    
    act(() => {
      result.current.setValue([7, 8, 9]);
    });
    
    expect(result.current.value).toEqual([7, 8, 9]);
  });

  it('복잡한 객체 구조를 올바르게 처리한다', () => {
    const complexObject = {
      user: { name: 'John', preferences: { theme: 'dark' } },
      settings: { notifications: true },
      data: [1, 2, 3]
    };
    
    const { result } = renderHook(() => useLocalStorage('complex-object', {}));
    
    act(() => {
      result.current.setValue(complexObject);
    });
    
    expect(result.current.value).toEqual(complexObject);
    expect(localStorageMock.setItem).toHaveBeenCalledWith(
      'complex-object', 
      JSON.stringify(complexObject)
    );
  });
});