import { rest } from 'msw';

const API_BASE_URL = 'http://localhost:8080/api';

export const handlers = [
  // User API handlers
  rest.get(`${API_BASE_URL}/users`, (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json([
        { id: 1, name: 'John Doe', email: 'john@example.com', createdAt: '2024-01-01T00:00:00Z' },
        { id: 2, name: 'Jane Smith', email: 'jane@example.com', createdAt: '2024-01-02T00:00:00Z' },
      ])
    );
  }),

  rest.get(`${API_BASE_URL}/users/:id`, (req, res, ctx) => {
    const { id } = req.params;
    return res(
      ctx.status(200),
      ctx.json({ id: Number(id), name: 'John Doe', email: 'john@example.com', createdAt: '2024-01-01T00:00:00Z' })
    );
  }),

  rest.post(`${API_BASE_URL}/users`, async (req, res, ctx) => {
    const userData = await req.json();
    return res(
      ctx.status(201),
      ctx.json({ id: 3, ...userData, createdAt: '2024-01-03T00:00:00Z' })
    );
  }),

  rest.put(`${API_BASE_URL}/users/:id`, async (req, res, ctx) => {
    const { id } = req.params;
    const userData = await req.json();
    return res(
      ctx.status(200),
      ctx.json({ id: Number(id), ...userData, createdAt: '2024-01-01T00:00:00Z' })
    );
  }),

  rest.delete(`${API_BASE_URL}/users/:id`, (req, res, ctx) => {
    return res(ctx.status(204));
  }),

  // Event API handlers
  rest.get(`${API_BASE_URL}/events`, (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json([
        { 
          id: 1, 
          name: 'Product Launch', 
          description: 'Launch event description',
          eventDate: '2024-06-01T10:00:00Z',
          createdAt: '2024-01-01T00:00:00Z' 
        },
        { 
          id: 2, 
          name: 'Team Meeting', 
          description: 'Weekly team meeting',
          eventDate: '2024-06-02T14:00:00Z',
          createdAt: '2024-01-02T00:00:00Z' 
        },
      ])
    );
  }),

  rest.post(`${API_BASE_URL}/events`, async (req, res, ctx) => {
    const eventData = await req.json();
    return res(
      ctx.status(201),
      ctx.json({ id: 3, ...eventData, createdAt: '2024-01-03T00:00:00Z' })
    );
  }),

  // Email Template API handlers
  rest.get(`${API_BASE_URL}/email/templates`, (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json([
        { 
          id: 1, 
          name: 'Welcome Email', 
          subject: 'Welcome to our platform',
          content: 'Welcome email content',
          createdAt: '2024-01-01T00:00:00Z' 
        },
        { 
          id: 2, 
          name: 'Newsletter', 
          subject: 'Monthly Newsletter',
          content: 'Newsletter content',
          createdAt: '2024-01-02T00:00:00Z' 
        },
      ])
    );
  }),

  rest.post(`${API_BASE_URL}/email/templates`, async (req, res, ctx) => {
    const templateData = await req.json();
    return res(
      ctx.status(201),
      ctx.json({ id: 3, ...templateData, createdAt: '2024-01-03T00:00:00Z' })
    );
  }),

  // Email Schedule API handlers
  rest.get(`${API_BASE_URL}/email/schedules`, (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json([
        { 
          id: 1, 
          templateId: 1,
          scheduledDate: '2024-06-01T09:00:00Z',
          status: 'scheduled',
          createdAt: '2024-01-01T00:00:00Z' 
        },
        { 
          id: 2, 
          templateId: 2,
          scheduledDate: '2024-06-02T10:00:00Z',
          status: 'sent',
          createdAt: '2024-01-02T00:00:00Z' 
        },
      ])
    );
  }),

  rest.post(`${API_BASE_URL}/email/schedules`, async (req, res, ctx) => {
    const scheduleData = await req.json();
    return res(
      ctx.status(201),
      ctx.json({ id: 3, ...scheduleData, createdAt: '2024-01-03T00:00:00Z' })
    );
  }),

  // Error handlers for testing
  rest.get(`${API_BASE_URL}/users/error`, (req, res, ctx) => {
    return res(ctx.status(500), ctx.json({ error: 'Internal Server Error' }));
  }),

  rest.get(`${API_BASE_URL}/users/not-found`, (req, res, ctx) => {
    return res(ctx.status(404), ctx.json({ error: 'User not found' }));
  }),
];