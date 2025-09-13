import { renderHook, act } from '@testing-library/react';
import { useToggle } from './useToggle';

describe('useToggle', () => {
  it('기본값 false로 초기화된다', () => {
    const { result } = renderHook(() => useToggle());
    
    expect(result.current.value).toBe(false);
    expect(typeof result.current.toggle).toBe('function');
    expect(typeof result.current.setTrue).toBe('function');
    expect(typeof result.current.setFalse).toBe('function');
    expect(typeof result.current.setValue).toBe('function');
  });

  it('초기값을 설정할 수 있다', () => {
    const { result } = renderHook(() => useToggle(true));
    
    expect(result.current.value).toBe(true);
  });

  it('toggle 함수가 값을 토글한다', () => {
    const { result } = renderHook(() => useToggle(false));
    
    expect(result.current.value).toBe(false);
    
    act(() => {
      result.current.toggle();
    });
    
    expect(result.current.value).toBe(true);
    
    act(() => {
      result.current.toggle();
    });
    
    expect(result.current.value).toBe(false);
  });

  it('setTrue 함수가 값을 true로 설정한다', () => {
    const { result } = renderHook(() => useToggle(false));
    
    expect(result.current.value).toBe(false);
    
    act(() => {
      result.current.setTrue();
    });
    
    expect(result.current.value).toBe(true);
    
    // 이미 true일 때도 true로 유지
    act(() => {
      result.current.setTrue();
    });
    
    expect(result.current.value).toBe(true);
  });

  it('setFalse 함수가 값을 false로 설정한다', () => {
    const { result } = renderHook(() => useToggle(true));
    
    expect(result.current.value).toBe(true);
    
    act(() => {
      result.current.setFalse();
    });
    
    expect(result.current.value).toBe(false);
    
    // 이미 false일 때도 false로 유지
    act(() => {
      result.current.setFalse();
    });
    
    expect(result.current.value).toBe(false);
  });

  it('setValue 함수가 직접 값을 설정한다', () => {
    const { result } = renderHook(() => useToggle());
    
    expect(result.current.value).toBe(false);
    
    act(() => {
      result.current.setValue(true);
    });
    
    expect(result.current.value).toBe(true);
    
    act(() => {
      result.current.setValue(false);
    });
    
    expect(result.current.value).toBe(false);
  });

  it('모든 함수가 참조 안정성을 유지한다', () => {
    const { result, rerender } = renderHook(() => useToggle());
    
    const initialFunctions = {
      toggle: result.current.toggle,
      setTrue: result.current.setTrue,
      setFalse: result.current.setFalse,
      setValue: result.current.setValue,
    };
    
    // 상태가 변경되어도 함수 참조는 동일해야 함
    act(() => {
      result.current.toggle();
    });
    
    rerender();
    
    expect(result.current.toggle).toBe(initialFunctions.toggle);
    expect(result.current.setTrue).toBe(initialFunctions.setTrue);
    expect(result.current.setFalse).toBe(initialFunctions.setFalse);
    expect(result.current.setValue).toBe(initialFunctions.setValue);
  });

  it('복잡한 토글 시나리오가 올바르게 동작한다', () => {
    const { result } = renderHook(() => useToggle(false));
    
    // 여러 번의 복잡한 상태 변경
    act(() => {
      result.current.setTrue();
    });
    expect(result.current.value).toBe(true);
    
    act(() => {
      result.current.toggle();
    });
    expect(result.current.value).toBe(false);
    
    act(() => {
      result.current.toggle();
    });
    expect(result.current.value).toBe(true);
    
    act(() => {
      result.current.setFalse();
    });
    expect(result.current.value).toBe(false);
    
    act(() => {
      result.current.setValue(true);
    });
    expect(result.current.value).toBe(true);
  });
});