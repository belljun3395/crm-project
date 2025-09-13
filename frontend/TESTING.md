# Testing Guide

CRM 프론트엔드 프로젝트의 테스트 가이드입니다.

## 테스트 환경

- **Testing Framework**: Jest + React Testing Library
- **Mock Server**: MSW (Mock Service Worker)
- **Component Documentation**: Storybook
- **Coverage Tool**: Jest Coverage

## 테스트 전략

### 1. 단위 테스트 (Unit Tests)
- **공통 컴포넌트**: Button, Input, Modal, Textarea
- **커스텀 훅**: useToggle, useLocalStorage
- **API 함수**: userAPI, eventAPI, templateAPI, scheduleAPI
- **유틸리티 함수**: 각종 헬퍼 함수들

### 2. 통합 테스트 (Integration Tests)
- **페이지 컴포넌트**: 사용자 상호작용과 API 호출 시나리오
- **복합 워크플로**: 폼 제출, 데이터 CRUD 등

### 3. 시각적 테스트 (Visual Tests)
- **Storybook**: 컴포넌트 디자인 시스템 관리
- **Stories**: 각 컴포넌트의 다양한 상태와 속성

## 실행 방법

### 의존성 설치
```bash
npm install
```

### 테스트 실행
```bash
# 모든 테스트 실행
npm test

# 테스트 커버리지 생성
npm run test:coverage

# CI 환경에서 테스트 (watch 모드 없음)
npm run test:ci
```

### Storybook 실행
```bash
# Storybook 개발 서버 시작
npm run storybook

# Storybook 빌드
npm run build-storybook
```

## 테스트 작성 가이드

### 컴포넌트 테스트
```typescript
// Button.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { Button } from './Button';

describe('Button', () => {
  it('렌더링이 올바르게 된다', () => {
    render(<Button>Click me</Button>);
    expect(screen.getByRole('button')).toBeInTheDocument();
  });

  it('onClick 핸들러가 호출된다', () => {
    const handleClick = jest.fn();
    render(<Button onClick={handleClick}>Click</Button>);
    
    fireEvent.click(screen.getByRole('button'));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });
});
```

### 커스텀 훅 테스트
```typescript
// useToggle.test.ts
import { renderHook, act } from '@testing-library/react';
import { useToggle } from './useToggle';

describe('useToggle', () => {
  it('기본값으로 초기화된다', () => {
    const { result } = renderHook(() => useToggle(false));
    expect(result.current.value).toBe(false);
  });

  it('토글 함수가 값을 변경한다', () => {
    const { result } = renderHook(() => useToggle(false));
    
    act(() => {
      result.current.toggle();
    });
    
    expect(result.current.value).toBe(true);
  });
});
```

### API 테스트 (MSW 사용)
```typescript
// userAPI.test.ts
import { userAPI } from './userAPI';
import { server } from '__tests__/mocks/server';
import { rest } from 'msw';

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('userAPI', () => {
  it('사용자 목록을 가져온다', async () => {
    const users = await userAPI.getUsers();
    expect(users).toHaveLength(2);
  });

  it('API 오류를 처리한다', async () => {
    server.use(
      rest.get('/api/users', (req, res, ctx) => {
        return res(ctx.status(500));
      })
    );

    const users = await userAPI.getUsers();
    expect(users).toEqual([]);
  });
});
```

## 커버리지 목표

프로젝트의 커버리지 목표는 다음과 같습니다:

- **Branches**: 80%
- **Functions**: 80%
- **Lines**: 80%
- **Statements**: 80%

## 테스트 파일 구조

```
src/
├── common/
│   ├── component/
│   │   ├── Button.tsx
│   │   ├── Button.test.tsx
│   │   └── Button.stories.tsx
│   └── hook/
│       ├── useToggle.ts
│       └── useToggle.test.ts
├── shared/
│   └── api/
│       └── crm/
│           └── user/
│               ├── index.ts
│               └── index.test.ts
├── page/
│   └── user/
│       ├── UserPage.tsx
│       └── UserPage.test.tsx
└── __tests__/
    └── mocks/
        ├── server.ts
        └── handlers.ts
```

## Best Practices

### 1. 테스트 명명 규칙
- 한글로 테스트 목적을 명확히 기술
- "~이 ~한다" 형식 사용
- 예: `'버튼 클릭 시 핸들러가 호출된다'`

### 2. Arrange-Act-Assert 패턴
```typescript
it('사용자가 추가된다', async () => {
  // Arrange
  const newUser = { name: 'John', email: 'john@example.com' };
  
  // Act
  const result = await userAPI.enrollUser(newUser);
  
  // Assert
  expect(result).toEqual(expect.objectContaining(newUser));
});
```

### 3. 의존성 모킹
- API 호출은 MSW로 모킹
- 외부 라이브러리는 Jest mock 사용
- 환경별 설정은 setupTests.ts에서 관리

### 4. 비동기 테스트
- `waitFor` 사용하여 비동기 상태 변화 대기
- `findBy*` 쿼리로 비동기 요소 찾기
- `act` 래퍼로 상태 업데이트 감싸기

### 5. 접근성 테스트
- `getByRole`, `getByLabelText` 등 의미있는 쿼리 우선 사용
- 스크린 리더 사용자 관점에서 테스트 작성

## 디버깅 팁

### 1. 테스트 디버깅
```typescript
// DOM 상태 확인
screen.debug();

// 특정 요소의 HTML 출력
console.log(screen.getByRole('button').outerHTML);
```

### 2. 비동기 테스트 디버깅
```typescript
// 요소가 나타날 때까지 대기
await waitFor(() => {
  expect(screen.getByText('Loading...')).toBeInTheDocument();
}, { timeout: 5000 });
```

### 3. MSW 디버깅
```typescript
// 네트워크 요청 로깅
server.use(
  rest.get('/api/*', (req, res, ctx) => {
    console.log('Request:', req.url.toString());
    return res(ctx.status(200));
  })
);
```

## 추가 도구

### VS Code 확장
- Jest Runner: 테스트 파일에서 개별 테스트 실행
- Coverage Gutters: 커버리지 정보를 에디터에 표시

### 추천 라이브러리
- `@testing-library/jest-dom`: 추가 matcher 제공
- `@testing-library/user-event`: 사용자 상호작용 시뮬레이션
- `msw`: HTTP 요청 모킹

이 가이드를 따라 일관되고 신뢰할 수 있는 테스트를 작성하세요!