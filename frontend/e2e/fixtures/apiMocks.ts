import { Page, Route } from '@playwright/test';

type JsonRecord = Record<string, unknown>;

interface FailureRule {
  method?: string;
  path: string | RegExp;
  status?: number;
  message?: string;
  once?: boolean;
}

interface RegisterMockOptions {
  failureRules?: FailureRule[];
}

interface MockState {
  nextIds: {
    user: number;
    event: number;
    template: number;
    schedule: number;
    webhook: number;
    segment: number;
    journey: number;
    actionHistory: number;
    audit: number;
  };
  users: Array<{
    id: number;
    externalId: string;
    userAttributes: string;
    createdAt: string;
  }>;
  events: Array<{
    id: number;
    name: string;
    externalId: string;
    properties: Array<{ key: string; value: string }>;
    createdAt: string;
  }>;
  templates: Array<{
    id: number;
    templateName: string;
    subject: string;
    body: string;
    variables: string[];
    version: number;
    createdAt: string;
  }>;
  emailHistories: Array<{
    id: number;
    userId: number;
    userEmail: string;
    emailMessageId: string;
    emailBody: string;
    sendStatus: string;
    createdAt: string;
    updatedAt: string;
  }>;
  schedules: Array<{
    taskName: string;
    templateId: number;
    userIds: number[];
    expiredTime: string;
  }>;
  webhooks: Array<{
    id: number;
    name: string;
    url: string;
    events: string[];
    active: boolean;
    createdAt: string;
  }>;
  deliveriesByWebhook: Record<number, Array<{
    id: number;
    webhookId: number;
    eventId: string;
    eventType: string;
    deliveryStatus: string;
    attemptCount: number;
    responseStatus: number;
    deliveredAt: string;
  }>>;
  deadLettersByWebhook: Record<number, Array<{
    id: number;
    webhookId: number;
    eventId: string;
    eventType: string;
    payloadJson: string;
    deliveryStatus: string;
    attemptCount: number;
    responseStatus: number;
    errorMessage: string;
    createdAt: string;
  }>>;
  segments: Array<{
    id: number;
    name: string;
    description?: string;
    active: boolean;
    conditions: Array<{
      field: string;
      operator: string;
      valueType: string;
      value: unknown;
    }>;
    createdAt: string;
  }>;
  journeys: Array<{
    id: number;
    name: string;
    triggerType: string;
    triggerEventName?: string;
    triggerSegmentId?: number;
    active: boolean;
    steps: Array<{
      id: number;
      stepOrder: number;
      stepType: string;
      channel?: string;
      destination?: string;
      subject?: string;
      body?: string;
      variables?: Record<string, string>;
      delayMillis?: number;
      conditionExpression?: string;
      retryCount?: number;
    }>;
    createdAt: string;
  }>;
  journeyExecutions: Array<{
    id: number;
    journeyId: number;
    eventId: number;
    userId: number;
    status: string;
    currentStepOrder: number;
    triggerKey: string;
    startedAt: string;
    completedAt?: string;
    createdAt: string;
  }>;
  executionHistoriesByExecution: Record<number, Array<{
    id: number;
    journeyExecutionId: number;
    journeyStepId: number;
    status: string;
    attempt: number;
    message?: string;
    createdAt: string;
  }>>;
  actionHistories: Array<{
    id: number;
    channel: string;
    status: string;
    destination: string;
    subject?: string;
    body: string;
    variables?: Record<string, string>;
    providerMessageId?: string;
    campaignId?: number;
    journeyExecutionId?: number;
    createdAt: string;
  }>;
  auditLogs: Array<{
    id: number;
    actorId: string;
    action: string;
    resourceType: string;
    resourceId: string;
    requestMethod: string;
    requestPath: string;
    statusCode: number;
    createdAt: string;
  }>;
}

const ok = <T>(data: T) => ({
  message: 'ok',
  data
});

const isObject = (value: unknown): value is JsonRecord =>
  typeof value === 'object' && value !== null && !Array.isArray(value);

const readBody = <T>(route: Route): T | null => {
  const request = route.request();
  const raw = request.postData();

  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
};

const toPositiveNumber = (value: string | null): number | undefined => {
  if (!value) {
    return undefined;
  }

  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return undefined;
  }

  return parsed;
};

const formatIso = (offsetMinutes: number): string => {
  const base = new Date('2026-02-26T00:00:00.000Z').getTime();
  return new Date(base + offsetMinutes * 60_000).toISOString();
};

const createInitialState = (): MockState => ({
  nextIds: {
    user: 103,
    event: 2003,
    template: 2,
    schedule: 1000,
    webhook: 7002,
    segment: 402,
    journey: 502,
    actionHistory: 802,
    audit: 902
  },
  users: [
    {
      id: 101,
      externalId: 'usr-alpha',
      userAttributes: '{"tier":"gold","region":"seoul"}',
      createdAt: '2026-02-24T09:00:00Z'
    },
    {
      id: 102,
      externalId: 'usr-bravo',
      userAttributes: '{"tier":"silver","region":"busan"}',
      createdAt: '2026-02-25T10:30:00Z'
    }
  ],
  events: [
    {
      id: 2001,
      name: 'view_product',
      externalId: 'evt-2001',
      properties: [{ key: 'category', value: 'electronics' }],
      createdAt: '2026-02-25T02:00:00Z'
    },
    {
      id: 2002,
      name: 'order_completed',
      externalId: 'evt-2002',
      properties: [{ key: 'orderAmount', value: '189000' }],
      createdAt: '2026-02-25T03:00:00Z'
    }
  ],
  templates: [
    {
      id: 1,
      templateName: 'welcome-template',
      subject: 'Welcome',
      body: 'Hello {{name}}',
      variables: ['name'],
      version: 1,
      createdAt: '2026-02-20T00:00:00Z'
    }
  ],
  emailHistories: [
    {
      id: 301,
      userId: 101,
      userEmail: 'alpha@example.com',
      emailMessageId: 'msg-301',
      emailBody: 'Welcome alpha',
      sendStatus: 'SENT',
      createdAt: '2026-02-25T11:00:00Z',
      updatedAt: '2026-02-25T11:00:00Z'
    },
    {
      id: 302,
      userId: 102,
      userEmail: 'bravo@example.com',
      emailMessageId: 'msg-302',
      emailBody: 'Welcome bravo',
      sendStatus: 'FAILED',
      createdAt: '2026-02-25T12:00:00Z',
      updatedAt: '2026-02-25T12:00:00Z'
    }
  ],
  schedules: [
    {
      taskName: 'daily-reminder',
      templateId: 1,
      userIds: [101, 102],
      expiredTime: '2026-12-31T00:00:00Z'
    }
  ],
  webhooks: [
    {
      id: 7001,
      name: 'order-events',
      url: 'https://hooks.example.com/orders',
      events: ['ORDER_CREATED', 'ORDER_UPDATED'],
      active: true,
      createdAt: '2026-02-26T01:00:00Z'
    }
  ],
  deliveriesByWebhook: {
    7001: [
      {
        id: 9001,
        webhookId: 7001,
        eventId: 'evt-1',
        eventType: 'ORDER_CREATED',
        deliveryStatus: 'SUCCESS',
        attemptCount: 1,
        responseStatus: 200,
        deliveredAt: '2026-02-26T01:05:00Z'
      }
    ]
  },
  deadLettersByWebhook: {
    7001: []
  },
  segments: [
    {
      id: 401,
      name: 'high-value',
      description: 'LTV high',
      active: true,
      conditions: [
        {
          field: 'tier',
          operator: 'eq',
          valueType: 'STRING',
          value: 'gold'
        }
      ],
      createdAt: '2026-02-18T00:00:00Z'
    }
  ],
  journeys: [
    {
      id: 501,
      name: 'welcome-journey',
      triggerType: 'EVENT',
      triggerEventName: 'USER_SIGNUP',
      active: true,
      steps: [
        {
          id: 1,
          stepOrder: 1,
          stepType: 'ACTION',
          channel: 'EMAIL',
          destination: 'alpha@example.com',
          subject: 'Welcome',
          body: 'Welcome onboard',
          retryCount: 1
        }
      ],
      createdAt: '2026-02-18T00:00:00Z'
    }
  ],
  journeyExecutions: [
    {
      id: 601,
      journeyId: 501,
      eventId: 2001,
      userId: 101,
      status: 'COMPLETED',
      currentStepOrder: 1,
      triggerKey: 'signup-101',
      startedAt: '2026-02-25T08:00:00Z',
      completedAt: '2026-02-25T08:05:00Z',
      createdAt: '2026-02-25T08:00:00Z'
    }
  ],
  executionHistoriesByExecution: {
    601: [
      {
        id: 10001,
        journeyExecutionId: 601,
        journeyStepId: 1,
        status: 'SUCCESS',
        attempt: 1,
        message: 'sent',
        createdAt: '2026-02-25T08:01:00Z'
      }
    ]
  },
  actionHistories: [
    {
      id: 801,
      channel: 'EMAIL',
      status: 'SUCCESS',
      destination: 'alpha@example.com',
      subject: 'Welcome',
      body: 'hello',
      createdAt: '2026-02-26T02:00:00Z'
    }
  ],
  auditLogs: [
    {
      id: 901,
      actorId: 'system',
      action: 'USER_CREATED',
      resourceType: 'USER',
      resourceId: '101',
      requestMethod: 'POST',
      requestPath: '/api/v1/users',
      statusCode: 200,
      createdAt: '2026-02-25T08:00:00Z'
    }
  ]
});

const fulfill = async (route: Route, payload: unknown, status = 200) => {
  await route.fulfill({
    status,
    contentType: 'application/json; charset=utf-8',
    body: JSON.stringify(payload)
  });
};

const matchFailureRule = (
  rule: FailureRule,
  method: string,
  path: string
): boolean => {
  const expectedMethod = rule.method?.toUpperCase();
  if (expectedMethod && expectedMethod !== method) {
    return false;
  }

  if (typeof rule.path === 'string') {
    return rule.path === path;
  }

  return rule.path.test(path);
};

const appendAudit = (
  state: MockState,
  action: string,
  resourceType: string,
  resourceId: string,
  requestMethod: string,
  requestPath: string,
  statusCode = 200
) => {
  state.auditLogs.push({
    id: state.nextIds.audit++,
    actorId: 'system',
    action,
    resourceType,
    resourceId,
    requestMethod,
    requestPath,
    statusCode,
    createdAt: formatIso(state.nextIds.audit)
  });
};

const handleUsers = async (
  state: MockState,
  route: Route,
  method: string,
  path: string
): Promise<boolean> => {
  if (path === '/api/v1/users' && method === 'GET') {
    await fulfill(route, ok({ users: { content: state.users } }));
    return true;
  }

  if (path === '/api/v1/users/count' && method === 'GET') {
    await fulfill(route, ok({ totalCount: state.users.length }));
    return true;
  }

  if (path === '/api/v1/users' && method === 'POST') {
    const body = readBody<{
      externalId?: string;
      userAttributes?: string;
    }>(route);

    const newUser = {
      id: state.nextIds.user++,
      externalId: body?.externalId?.trim() || `usr-${state.nextIds.user}`,
      userAttributes: body?.userAttributes?.trim() || '{}',
      createdAt: formatIso(state.nextIds.user)
    };

    state.users.push(newUser);
    appendAudit(state, 'USER_CREATED', 'USER', String(newUser.id), method, path);
    await fulfill(route, ok(newUser));
    return true;
  }

  return false;
};

const handleEvents = async (
  state: MockState,
  route: Route,
  method: string,
  path: string,
  url: URL
): Promise<boolean> => {
  if (path === '/api/v1/events' && method === 'GET') {
    const eventName = url.searchParams.get('eventName')?.trim().toLowerCase() ?? '';
    const filtered = state.events.filter((event) =>
      event.name.toLowerCase().includes(eventName)
    );
    await fulfill(route, ok({ events: filtered }));
    return true;
  }

  if (path === '/api/v1/events' && method === 'POST') {
    const body = readBody<{
      name?: string;
      externalId?: string;
      properties?: Array<{ key?: string; value?: string }>;
    }>(route);

    const newEvent = {
      id: state.nextIds.event++,
      name: body?.name?.trim() || `event-${state.nextIds.event}`,
      externalId: body?.externalId?.trim() || `evt-${state.nextIds.event}`,
      properties: Array.isArray(body?.properties)
        ? body.properties
            .filter((item) => typeof item?.key === 'string')
            .map((item) => ({ key: item.key as string, value: String(item.value ?? '') }))
        : [],
      createdAt: formatIso(state.nextIds.event)
    };

    state.events.unshift(newEvent);
    appendAudit(state, 'EVENT_CREATED', 'EVENT', String(newEvent.id), method, path);
    await fulfill(route, ok({ eventId: newEvent.id }));
    return true;
  }

  if (path === '/api/v1/events/campaign' && method === 'POST') {
    await fulfill(route, ok({ campaignId: 1, name: 'mock-campaign' }));
    return true;
  }

  return false;
};

const handleTemplates = async (
  state: MockState,
  route: Route,
  method: string,
  path: string
): Promise<boolean> => {
  if (path === '/api/v1/emails/templates' && method === 'GET') {
    await fulfill(route, ok({ templates: state.templates.map((template) => ({ template })) }));
    return true;
  }

  if (path === '/api/v1/emails/templates' && method === 'POST') {
    const body = readBody<{
      templateName?: string;
      subject?: string;
      body?: string;
      variables?: string[];
      version?: number;
    }>(route);

    const newTemplate = {
      id: state.nextIds.template++,
      templateName: body?.templateName?.trim() || `template-${state.nextIds.template}`,
      subject: body?.subject?.trim() || '(no-subject)',
      body: body?.body?.trim() || '',
      variables: Array.isArray(body?.variables) ? body.variables.map((item) => String(item)) : [],
      version: Number(body?.version) || 1,
      createdAt: formatIso(state.nextIds.template)
    };

    state.templates.unshift(newTemplate);
    appendAudit(state, 'EMAIL_TEMPLATE_CREATED', 'EMAIL_TEMPLATE', String(newTemplate.id), method, path);
    await fulfill(route, ok({ template: newTemplate }));
    return true;
  }

  const templateIdMatch = path.match(/^\/api\/v1\/emails\/templates\/(\d+)$/);
  if (templateIdMatch && method === 'DELETE') {
    const templateId = Number(templateIdMatch[1]);
    state.templates = state.templates.filter((template) => template.id !== templateId);
    appendAudit(state, 'EMAIL_TEMPLATE_DELETED', 'EMAIL_TEMPLATE', String(templateId), method, path);
    await fulfill(route, ok({}));
    return true;
  }

  return false;
};

const handleEmailSchedules = async (
  state: MockState,
  route: Route,
  method: string,
  path: string
): Promise<boolean> => {
  if (path === '/api/v1/emails/schedules/notifications/email' && method === 'GET') {
    await fulfill(route, ok({ schedules: state.schedules }));
    return true;
  }

  if (path === '/api/v1/emails/schedules/notifications/email' && method === 'POST') {
    const body = readBody<{
      templateId?: number;
      userIds?: number[];
      expiredTime?: string;
    }>(route);

    const taskName = `scheduled-${state.nextIds.schedule++}`;
    const schedule = {
      taskName,
      templateId: Number(body?.templateId) || 1,
      userIds: Array.isArray(body?.userIds) ? body.userIds.map((id) => Number(id)).filter((id) => Number.isFinite(id)) : [],
      expiredTime: body?.expiredTime || '2026-12-31T00:00:00Z'
    };

    state.schedules.unshift(schedule);
    appendAudit(state, 'EMAIL_SCHEDULE_CREATED', 'EMAIL_SCHEDULE', taskName, method, path);
    await fulfill(route, ok({ newSchedule: taskName }));
    return true;
  }

  const scheduleMatch = path.match(/^\/api\/v1\/emails\/schedules\/notifications\/email\/(.+)$/);
  if (scheduleMatch && method === 'DELETE') {
    const scheduleId = decodeURIComponent(scheduleMatch[1]);
    state.schedules = state.schedules.filter((schedule) => schedule.taskName !== scheduleId);
    appendAudit(state, 'EMAIL_SCHEDULE_CANCELLED', 'EMAIL_SCHEDULE', scheduleId, method, path);
    await fulfill(route, ok({}));
    return true;
  }

  return false;
};

const handleEmailHistories = async (
  state: MockState,
  route: Route,
  method: string,
  path: string,
  url: URL
): Promise<boolean> => {
  if (path !== '/api/v1/emails/histories' || method !== 'GET') {
    return false;
  }

  const userId = toPositiveNumber(url.searchParams.get('userId'));
  const sendStatus = url.searchParams.get('sendStatus')?.trim().toLowerCase();
  const page = Number(url.searchParams.get('page') ?? '0');
  const size = Number(url.searchParams.get('size') ?? '20');

  let filtered = [...state.emailHistories];
  if (userId) {
    filtered = filtered.filter((history) => history.userId === userId);
  }

  if (sendStatus) {
    filtered = filtered.filter((history) => history.sendStatus.toLowerCase() === sendStatus);
  }

  const safePage = Number.isFinite(page) && page >= 0 ? page : 0;
  const safeSize = Number.isFinite(size) && size > 0 ? size : 20;
  const start = safePage * safeSize;
  const end = start + safeSize;

  await fulfill(
    route,
    ok({
      histories: filtered.slice(start, end),
      totalCount: filtered.length,
      page: safePage,
      size: safeSize
    })
  );
  return true;
};

const handleWebhooks = async (
  state: MockState,
  route: Route,
  method: string,
  path: string
): Promise<boolean> => {
  if (path === '/api/v1/webhooks' && method === 'GET') {
    await fulfill(route, ok(state.webhooks));
    return true;
  }

  if (path === '/api/v1/webhooks' && method === 'POST') {
    const body = readBody<{
      name?: string;
      url?: string;
      events?: string[];
      active?: boolean;
    }>(route);

    const webhook = {
      id: state.nextIds.webhook++,
      name: body?.name?.trim() || `webhook-${state.nextIds.webhook}`,
      url: body?.url?.trim() || 'https://hooks.example.com/default',
      events: Array.isArray(body?.events) ? body.events.map((event) => String(event)) : ['EVENT_CREATED'],
      active: typeof body?.active === 'boolean' ? body.active : true,
      createdAt: formatIso(state.nextIds.webhook)
    };

    state.webhooks.unshift(webhook);
    state.deliveriesByWebhook[webhook.id] = [
      {
        id: state.nextIds.webhook * 10,
        webhookId: webhook.id,
        eventId: 'evt-new',
        eventType: webhook.events[0] || 'EVENT_CREATED',
        deliveryStatus: 'SUCCESS',
        attemptCount: 1,
        responseStatus: 200,
        deliveredAt: formatIso(state.nextIds.webhook)
      }
    ];
    state.deadLettersByWebhook[webhook.id] = [];
    appendAudit(state, 'WEBHOOK_CREATED', 'WEBHOOK', String(webhook.id), method, path);
    await fulfill(route, ok(webhook));
    return true;
  }

  const webhookIdMatch = path.match(/^\/api\/v1\/webhooks\/(\d+)$/);
  if (webhookIdMatch && method === 'PUT') {
    const webhookId = Number(webhookIdMatch[1]);
    const body = readBody<{
      name?: string;
      url?: string;
      events?: string[];
      active?: boolean;
    }>(route);
    const target = state.webhooks.find((webhook) => webhook.id === webhookId);

    if (!target) {
      await fulfill(route, ok({}), 404);
      return true;
    }

    target.name = body?.name?.trim() || target.name;
    target.url = body?.url?.trim() || target.url;
    target.events = Array.isArray(body?.events) ? body.events.map((event) => String(event)) : target.events;
    target.active = typeof body?.active === 'boolean' ? body.active : target.active;
    appendAudit(state, 'WEBHOOK_UPDATED', 'WEBHOOK', String(webhookId), method, path);
    await fulfill(route, ok(target));
    return true;
  }

  if (webhookIdMatch && method === 'DELETE') {
    const webhookId = Number(webhookIdMatch[1]);
    state.webhooks = state.webhooks.filter((webhook) => webhook.id !== webhookId);
    delete state.deliveriesByWebhook[webhookId];
    delete state.deadLettersByWebhook[webhookId];
    appendAudit(state, 'WEBHOOK_DELETED', 'WEBHOOK', String(webhookId), method, path);
    await fulfill(route, ok({}));
    return true;
  }

  const deliveryMatch = path.match(/^\/api\/v1\/webhooks\/(\d+)\/deliveries$/);
  if (deliveryMatch && method === 'GET') {
    const webhookId = Number(deliveryMatch[1]);
    await fulfill(route, ok(state.deliveriesByWebhook[webhookId] ?? []));
    return true;
  }

  const deadLetterMatch = path.match(/^\/api\/v1\/webhooks\/(\d+)\/dead-letters$/);
  if (deadLetterMatch && method === 'GET') {
    const webhookId = Number(deadLetterMatch[1]);
    await fulfill(route, ok(state.deadLettersByWebhook[webhookId] ?? []));
    return true;
  }

  return false;
};

const handleSegments = async (
  state: MockState,
  route: Route,
  method: string,
  path: string
): Promise<boolean> => {
  if (path === '/api/v1/segments' && method === 'GET') {
    await fulfill(route, ok(state.segments));
    return true;
  }

  if (path === '/api/v1/segments' && method === 'POST') {
    const body = readBody<{
      name?: string;
      description?: string;
      active?: boolean;
      conditions?: Array<{
        field?: string;
        operator?: string;
        valueType?: string;
        value?: unknown;
      }>;
    }>(route);

    const segment = {
      id: state.nextIds.segment++,
      name: body?.name?.trim() || `segment-${state.nextIds.segment}`,
      description: body?.description?.trim(),
      active: typeof body?.active === 'boolean' ? body.active : true,
      conditions: Array.isArray(body?.conditions)
        ? body.conditions
            .filter((condition) => isObject(condition))
            .map((condition) => ({
              field: String(condition.field ?? 'field'),
              operator: String(condition.operator ?? 'eq'),
              valueType: String(condition.valueType ?? 'STRING'),
              value: condition.value ?? ''
            }))
        : [],
      createdAt: formatIso(state.nextIds.segment)
    };

    state.segments.unshift(segment);
    appendAudit(state, 'SEGMENT_CREATED', 'SEGMENT', String(segment.id), method, path);
    await fulfill(route, ok({ segment }));
    return true;
  }

  const segmentIdMatch = path.match(/^\/api\/v1\/segments\/(\d+)$/);
  if (segmentIdMatch && method === 'PUT') {
    const segmentId = Number(segmentIdMatch[1]);
    const body = readBody<{
      name?: string;
      description?: string;
      active?: boolean;
      conditions?: Array<{
        field?: string;
        operator?: string;
        valueType?: string;
        value?: unknown;
      }>;
    }>(route);
    const target = state.segments.find((segment) => segment.id === segmentId);

    if (!target) {
      await fulfill(route, ok({}), 404);
      return true;
    }

    target.name = body?.name?.trim() || target.name;
    target.description = body?.description?.trim() || target.description;
    if (typeof body?.active === 'boolean') {
      target.active = body.active;
    }
    if (Array.isArray(body?.conditions)) {
      target.conditions = body.conditions
        .filter((condition) => isObject(condition))
        .map((condition) => ({
          field: String(condition.field ?? 'field'),
          operator: String(condition.operator ?? 'eq'),
          valueType: String(condition.valueType ?? 'STRING'),
          value: condition.value ?? ''
        }));
    }

    appendAudit(state, 'SEGMENT_UPDATED', 'SEGMENT', String(segmentId), method, path);
    await fulfill(route, ok({ segment: target }));
    return true;
  }

  if (segmentIdMatch && method === 'DELETE') {
    const segmentId = Number(segmentIdMatch[1]);
    state.segments = state.segments.filter((segment) => segment.id !== segmentId);
    appendAudit(state, 'SEGMENT_DELETED', 'SEGMENT', String(segmentId), method, path);
    await fulfill(route, ok({}));
    return true;
  }

  return false;
};

const handleJourneys = async (
  state: MockState,
  route: Route,
  method: string,
  path: string,
  url: URL
): Promise<boolean> => {
  if (path === '/api/v1/journeys' && method === 'GET') {
    await fulfill(route, ok(state.journeys));
    return true;
  }

  if (path === '/api/v1/journeys' && method === 'POST') {
    const body = readBody<{
      name?: string;
      triggerType?: string;
      triggerEventName?: string;
      triggerSegmentId?: number;
      active?: boolean;
      steps?: Array<{
        stepOrder?: number;
        stepType?: string;
        channel?: string;
        destination?: string;
        subject?: string;
        body?: string;
        variables?: Record<string, string>;
        delayMillis?: number;
        conditionExpression?: string;
        retryCount?: number;
      }>;
    }>(route);

    const stepSeed = state.nextIds.journey * 10;
    const journey = {
      id: state.nextIds.journey++,
      name: body?.name?.trim() || `journey-${state.nextIds.journey}`,
      triggerType: body?.triggerType?.trim() || 'EVENT',
      triggerEventName: body?.triggerEventName?.trim() || undefined,
      triggerSegmentId: Number(body?.triggerSegmentId) || undefined,
      active: typeof body?.active === 'boolean' ? body.active : true,
      steps: Array.isArray(body?.steps)
        ? body.steps.map((step, index) => ({
            id: stepSeed + index + 1,
            stepOrder: Number(step.stepOrder) || index + 1,
            stepType: step.stepType?.trim() || 'ACTION',
            channel: step.channel,
            destination: step.destination,
            subject: step.subject,
            body: step.body,
            variables: step.variables,
            delayMillis: step.delayMillis,
            conditionExpression: step.conditionExpression,
            retryCount: step.retryCount
          }))
        : [],
      createdAt: formatIso(state.nextIds.journey)
    };

    state.journeys.unshift(journey);
    appendAudit(state, 'JOURNEY_CREATED', 'JOURNEY', String(journey.id), method, path);
    await fulfill(route, ok(journey));
    return true;
  }

  if (path === '/api/v1/journeys/executions' && method === 'GET') {
    let executions = [...state.journeyExecutions];
    const journeyId = toPositiveNumber(url.searchParams.get('journeyId'));
    const eventId = toPositiveNumber(url.searchParams.get('eventId'));
    const userId = toPositiveNumber(url.searchParams.get('userId'));

    if (journeyId) {
      executions = executions.filter((execution) => execution.journeyId === journeyId);
    }
    if (eventId) {
      executions = executions.filter((execution) => execution.eventId === eventId);
    }
    if (userId) {
      executions = executions.filter((execution) => execution.userId === userId);
    }

    await fulfill(route, ok(executions));
    return true;
  }

  const executionHistoryMatch = path.match(/^\/api\/v1\/journeys\/executions\/(\d+)\/histories$/);
  if (executionHistoryMatch && method === 'GET') {
    const executionId = Number(executionHistoryMatch[1]);
    await fulfill(route, ok(state.executionHistoriesByExecution[executionId] ?? []));
    return true;
  }

  return false;
};

const handleActions = async (
  state: MockState,
  route: Route,
  method: string,
  path: string,
  url: URL
): Promise<boolean> => {
  if (path === '/api/v1/actions/dispatch/histories' && method === 'GET') {
    let histories = [...state.actionHistories];
    const campaignId = toPositiveNumber(url.searchParams.get('campaignId'));
    const journeyExecutionId = toPositiveNumber(url.searchParams.get('journeyExecutionId'));

    if (campaignId) {
      histories = histories.filter((history) => history.campaignId === campaignId);
    }
    if (journeyExecutionId) {
      histories = histories.filter((history) => history.journeyExecutionId === journeyExecutionId);
    }

    await fulfill(route, ok(histories));
    return true;
  }

  if (path === '/api/v1/actions/dispatch' && method === 'POST') {
    const body = readBody<{
      channel?: string;
      destination?: string;
      subject?: string;
      body?: string;
      variables?: Record<string, string>;
      campaignId?: number;
      journeyExecutionId?: number;
    }>(route);

    const history = {
      id: state.nextIds.actionHistory++,
      channel: body?.channel || 'EMAIL',
      status: 'SUCCESS',
      destination: body?.destination || 'unknown@example.com',
      subject: body?.subject,
      body: body?.body || '',
      variables: body?.variables,
      providerMessageId: `provider-${state.nextIds.actionHistory}`,
      campaignId: body?.campaignId,
      journeyExecutionId: body?.journeyExecutionId,
      createdAt: formatIso(state.nextIds.actionHistory)
    };

    state.actionHistories.unshift(history);
    appendAudit(state, 'ACTION_DISPATCHED', 'ACTION_DISPATCH', String(history.id), method, path);

    await fulfill(
      route,
      ok({
        status: history.status,
        channel: history.channel,
        destination: history.destination,
        providerMessageId: history.providerMessageId
      })
    );
    return true;
  }

  return false;
};

const handleAuditLogs = async (
  state: MockState,
  route: Route,
  method: string,
  path: string,
  url: URL
): Promise<boolean> => {
  if (path !== '/api/v1/audit-logs' || method !== 'GET') {
    return false;
  }

  const limit = Number(url.searchParams.get('limit') ?? '50');
  const action = url.searchParams.get('action')?.trim().toLowerCase();
  const resourceType = url.searchParams.get('resourceType')?.trim().toLowerCase();
  const actorId = url.searchParams.get('actorId')?.trim().toLowerCase();

  let logs = [...state.auditLogs];

  if (action) {
    logs = logs.filter((log) => log.action.toLowerCase().includes(action));
  }
  if (resourceType) {
    logs = logs.filter((log) => log.resourceType.toLowerCase().includes(resourceType));
  }
  if (actorId) {
    logs = logs.filter((log) => log.actorId.toLowerCase().includes(actorId));
  }

  const safeLimit = Number.isFinite(limit) && limit > 0 ? limit : 50;
  await fulfill(route, ok(logs.slice(0, safeLimit)));
  return true;
};

const handleCampaignDashboard = async (
  _state: MockState,
  route: Route,
  method: string,
  path: string
): Promise<boolean> => {
  const dashboardMatch = path.match(/^\/api\/v1\/campaigns\/(\d+)\/dashboard$/);
  if (dashboardMatch && method === 'GET') {
    const campaignId = Number(dashboardMatch[1]);
    await fulfill(
      route,
      ok({
        campaignId,
        summary: {
          campaignId,
          totalEvents: 128,
          eventsLast24Hours: 32,
          eventsLast7Days: 104,
          lastUpdated: '2026-02-26T03:10:00Z'
        },
        metrics: [
          {
            id: 1,
            campaignId,
            metricType: 'DELIVERED',
            metricValue: 90,
            timeWindowStart: '2026-02-26T02:00:00Z',
            timeWindowEnd: '2026-02-26T03:00:00Z',
            timeWindowUnit: 'HOUR'
          },
          {
            id: 2,
            campaignId,
            metricType: 'OPENED',
            metricValue: 54,
            timeWindowStart: '2026-02-26T02:00:00Z',
            timeWindowEnd: '2026-02-26T03:00:00Z',
            timeWindowUnit: 'HOUR'
          }
        ]
      })
    );
    return true;
  }

  const summaryMatch = path.match(/^\/api\/v1\/campaigns\/(\d+)\/dashboard\/summary$/);
  if (summaryMatch && method === 'GET') {
    const campaignId = Number(summaryMatch[1]);
    await fulfill(
      route,
      ok({
        campaignId,
        totalEvents: 128,
        eventsLast24Hours: 32,
        eventsLast7Days: 104,
        lastUpdated: '2026-02-26T03:10:00Z'
      })
    );
    return true;
  }

  const streamStatusMatch = path.match(/^\/api\/v1\/campaigns\/(\d+)\/dashboard\/stream\/status$/);
  if (streamStatusMatch && method === 'GET') {
    const campaignId = Number(streamStatusMatch[1]);
    await fulfill(
      route,
      ok({
        campaignId,
        streamLength: 3,
        checkedAt: '2026-02-26T03:12:00Z'
      })
    );
    return true;
  }

  return false;
};

const routeHandlers = [
  handleUsers,
  handleEvents,
  handleTemplates,
  handleEmailSchedules,
  handleEmailHistories,
  handleWebhooks,
  handleSegments,
  handleJourneys,
  handleActions,
  handleAuditLogs,
  handleCampaignDashboard
] as const;

export const registerCrmApiMocks = async (
  page: Page,
  options?: RegisterMockOptions
) => {
  const state = createInitialState();
  const failureRules = options?.failureRules ? [...options.failureRules] : [];

  await page.route('**/api/v1/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method().toUpperCase();
    const path = url.pathname;

    const failureRuleIndex = failureRules.findIndex((rule) =>
      matchFailureRule(rule, method, path)
    );
    if (failureRuleIndex >= 0) {
      const failureRule = failureRules[failureRuleIndex];
      if (failureRule.once) {
        failureRules.splice(failureRuleIndex, 1);
      }

      await fulfill(
        route,
        {
          message: failureRule.message ?? 'mock failure',
          data: null
        },
        failureRule.status ?? 500
      );
      return;
    }

    for (const handler of routeHandlers) {
      const handled = await handler(state, route, method, path, url);
      if (handled) {
        return;
      }
    }

    await fulfill(route, ok({}));
  });
};
