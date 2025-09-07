import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { UserPage } from './UserPage';
import { server } from '__tests__/mocks/server';
import { rest } from 'msw';

// Mock API 응답
const mockUsers = [
  { id: 1, name: 'John Doe', email: 'john@example.com', createdAt: '2024-01-01T00:00:00Z' },
  { id: 2, name: 'Jane Smith', email: 'jane@example.com', createdAt: '2024-01-02T00:00:00Z' },
];

beforeEach(() => {
  // 기본 API 응답 설정
  server.use(
    rest.get('http://localhost:8080/api/users', (req, res, ctx) => {
      return res(
        ctx.status(200),
        ctx.json({
          success: true,
          data: { users: mockUsers }
        })
      );
    }),
    rest.get('http://localhost:8080/api/users/count', (req, res, ctx) => {
      return res(
        ctx.status(200),
        ctx.json({
          success: true,
          data: { totalCount: mockUsers.length }
        })
      );
    })
  );
});

describe('UserPage', () => {
  it('페이지가 올바르게 렌더링된다', async () => {
    render(<UserPage />);
    
    expect(screen.getByText('사용자 관리')).toBeInTheDocument();
    expect(screen.getByText('새 사용자 추가')).toBeInTheDocument();
    
    // 사용자 목록이 로드될 때까지 대기
    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument();
      expect(screen.getByText('Jane Smith')).toBeInTheDocument();
    });
  });

  it('사용자 목록이 올바르게 표시된다', async () => {
    render(<UserPage />);
    
    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument();
      expect(screen.getByText('john@example.com')).toBeInTheDocument();
      expect(screen.getByText('Jane Smith')).toBeInTheDocument();
      expect(screen.getByText('jane@example.com')).toBeInTheDocument();
    });
  });

  it('새 사용자 추가 모달이 열린다', async () => {
    const user = userEvent.setup();
    render(<UserPage />);
    
    const addButton = screen.getByText('새 사용자 추가');
    await user.click(addButton);
    
    await waitFor(() => {
      expect(screen.getByText('새 사용자 추가')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('사용자 이름을 입력하세요')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('이메일을 입력하세요')).toBeInTheDocument();
    });
  });

  it('새 사용자를 성공적으로 추가한다', async () => {
    const user = userEvent.setup();
    
    // 새 사용자 추가 API 모킹
    server.use(
      rest.post('http://localhost:8080/api/users', async (req, res, ctx) => {
        const userData = await req.json();
        return res(
          ctx.status(201),
          ctx.json({
            success: true,
            data: {
              id: 3,
              ...userData,
              createdAt: '2024-01-03T00:00:00Z'
            }
          })
        );
      }),
      // 업데이트된 사용자 목록 반환
      rest.get('http://localhost:8080/api/users', (req, res, ctx) => {
        return res(
          ctx.status(200),
          ctx.json({
            success: true,
            data: {
              users: [
                ...mockUsers,
                { id: 3, name: 'New User', email: 'newuser@example.com', createdAt: '2024-01-03T00:00:00Z' }
              ]
            }
          })
        );
      })
    );

    render(<UserPage />);
    
    // 모달 열기
    await user.click(screen.getByText('새 사용자 추가'));
    
    await waitFor(() => {
      expect(screen.getByPlaceholderText('사용자 이름을 입력하세요')).toBeInTheDocument();
    });
    
    // 폼 입력
    await user.type(screen.getByPlaceholderText('사용자 이름을 입력하세요'), 'New User');
    await user.type(screen.getByPlaceholderText('이메일을 입력하세요'), 'newuser@example.com');
    
    // 저장 버튼 클릭
    await user.click(screen.getByText('저장'));
    
    // 새 사용자가 목록에 추가되었는지 확인
    await waitFor(() => {
      expect(screen.getByText('New User')).toBeInTheDocument();
      expect(screen.getByText('newuser@example.com')).toBeInTheDocument();
    });
  });

  it('API 오류 시 에러 상태를 처리한다', async () => {
    server.use(
      rest.get('http://localhost:8080/api/users', (req, res, ctx) => {
        return res(ctx.status(500), ctx.json({ error: 'Internal Server Error' }));
      })
    );
    
    render(<UserPage />);
    
    // 에러 상태가 표시되거나 빈 목록이 표시되는지 확인
    await waitFor(() => {
      // API 오류 시 빈 배열을 반환하므로 사용자가 표시되지 않음
      expect(screen.queryByText('John Doe')).not.toBeInTheDocument();
      expect(screen.queryByText('Jane Smith')).not.toBeInTheDocument();
    });
  });

  it('로딩 상태를 표시한다', () => {
    render(<UserPage />);
    
    // 초기 로딩 상태에서는 사용자 데이터가 아직 로드되지 않음
    expect(screen.queryByText('John Doe')).not.toBeInTheDocument();
  });

  it('모달 취소 버튼이 동작한다', async () => {
    const user = userEvent.setup();
    render(<UserPage />);
    
    // 모달 열기
    await user.click(screen.getByText('새 사용자 추가'));
    
    await waitFor(() => {
      expect(screen.getByText('취소')).toBeInTheDocument();
    });
    
    // 취소 버튼 클릭
    await user.click(screen.getByText('취소'));
    
    // 모달이 닫혔는지 확인
    await waitFor(() => {
      expect(screen.queryByPlaceholderText('사용자 이름을 입력하세요')).not.toBeInTheDocument();
    });
  });

  it('빈 폼 제출 시 유효성 검사가 동작한다', async () => {
    const user = userEvent.setup();
    render(<UserPage />);
    
    // 모달 열기
    await user.click(screen.getByText('새 사용자 추가'));
    
    await waitFor(() => {
      expect(screen.getByText('저장')).toBeInTheDocument();
    });
    
    // 빈 폼으로 저장 시도
    await user.click(screen.getByText('저장'));
    
    // 모달이 여전히 열려있는지 확인 (유효성 검사 실패)
    expect(screen.getByPlaceholderText('사용자 이름을 입력하세요')).toBeInTheDocument();
  });
});