import { userAPI } from './index';
import { server } from '__tests__/mocks/server';
import { rest } from 'msw';

// MSW 서버 설정
beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

// console.error mock
const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});

describe('userAPI', () => {
  beforeEach(() => {
    consoleSpy.mockClear();
  });

  describe('getUsers', () => {
    it('사용자 목록을 성공적으로 가져온다', async () => {
      // MSW handler를 덮어씀 - API 응답 형식에 맞게 수정
      server.use(
        rest.get('http://localhost:8080/api/users', (req, res, ctx) => {
          return res(
            ctx.status(200),
            ctx.json({
              success: true,
              data: {
                users: [
                  { id: 1, name: 'John Doe', email: 'john@example.com', createdAt: '2024-01-01T00:00:00Z' },
                  { id: 2, name: 'Jane Smith', email: 'jane@example.com', createdAt: '2024-01-02T00:00:00Z' },
                ]
              }
            })
          );
        })
      );

      const users = await userAPI.getUsers();

      expect(users).toHaveLength(2);
      expect(users[0]).toEqual({
        id: 1,
        name: 'John Doe',
        email: 'john@example.com',
        createdAt: '2024-01-01T00:00:00Z'
      });
      expect(users[1]).toEqual({
        id: 2,
        name: 'Jane Smith',
        email: 'jane@example.com',
        createdAt: '2024-01-02T00:00:00Z'
      });
    });

    it('API 오류 시 빈 배열을 반환하고 에러를 로그한다', async () => {
      server.use(
        rest.get('http://localhost:8080/api/users', (req, res, ctx) => {
          return res(ctx.status(500), ctx.json({ error: 'Internal Server Error' }));
        })
      );

      const users = await userAPI.getUsers();

      expect(users).toEqual([]);
      expect(consoleSpy).toHaveBeenCalledWith('Error fetching users:', expect.any(Error));
    });

    it('네트워크 오류 시 빈 배열을 반환한다', async () => {
      server.use(
        rest.get('http://localhost:8080/api/users', (req, res, ctx) => {
          return res.networkError('Network error');
        })
      );

      const users = await userAPI.getUsers();

      expect(users).toEqual([]);
      expect(consoleSpy).toHaveBeenCalledWith('Error fetching users:', expect.any(Error));
    });
  });

  describe('enrollUser', () => {
    it('사용자를 성공적으로 등록한다', async () => {
      const newUser = {
        name: 'New User',
        email: 'newuser@example.com'
      };

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
        })
      );

      const result = await userAPI.enrollUser(newUser);

      expect(result).toEqual({
        id: 3,
        name: 'New User',
        email: 'newuser@example.com',
        createdAt: '2024-01-03T00:00:00Z'
      });
    });

    it('API 오류 시 null을 반환하고 에러를 로그한다', async () => {
      const newUser = {
        name: 'New User',
        email: 'newuser@example.com'
      };

      server.use(
        rest.post('http://localhost:8080/api/users', (req, res, ctx) => {
          return res(ctx.status(400), ctx.json({ error: 'Bad Request' }));
        })
      );

      const result = await userAPI.enrollUser(newUser);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalledWith('Error enrolling user:', expect.any(Error));
    });

    it('중복 이메일 오류를 처리한다', async () => {
      const duplicateUser = {
        name: 'Duplicate User',
        email: 'existing@example.com'
      };

      server.use(
        rest.post('http://localhost:8080/api/users', (req, res, ctx) => {
          return res(
            ctx.status(409),
            ctx.json({ error: 'Email already exists' })
          );
        })
      );

      const result = await userAPI.enrollUser(duplicateUser);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalledWith('Error enrolling user:', expect.any(Error));
    });
  });

  describe('getUserCount', () => {
    it('사용자 수를 성공적으로 가져온다', async () => {
      server.use(
        rest.get('http://localhost:8080/api/users/count', (req, res, ctx) => {
          return res(
            ctx.status(200),
            ctx.json({
              success: true,
              data: { totalCount: 42 }
            })
          );
        })
      );

      const count = await userAPI.getUserCount();

      expect(count).toBe(42);
    });

    it('API 오류 시 0을 반환하고 에러를 로그한다', async () => {
      server.use(
        rest.get('http://localhost:8080/api/users/count', (req, res, ctx) => {
          return res(ctx.status(500), ctx.json({ error: 'Internal Server Error' }));
        })
      );

      const count = await userAPI.getUserCount();

      expect(count).toBe(0);
      expect(consoleSpy).toHaveBeenCalledWith('Error fetching user count:', expect.any(Error));
    });

    it('빈 데이터 응답을 처리한다', async () => {
      server.use(
        rest.get('http://localhost:8080/api/users/count', (req, res, ctx) => {
          return res(
            ctx.status(200),
            ctx.json({
              success: true,
              data: { totalCount: 0 }
            })
          );
        })
      );

      const count = await userAPI.getUserCount();

      expect(count).toBe(0);
    });
  });
});