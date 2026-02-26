const sanitizeScope = (scope: string): string => {
  return scope
    .toLowerCase()
    .replace(/[^a-z0-9:_-]/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '') || 'idempotency';
};

const createIdempotencyKey = (scope: string): string => {
  const normalizedScope = sanitizeScope(scope);
  const timestamp = Date.now().toString(36);
  const random = Math.random().toString(36).slice(2, 10);
  return `${normalizedScope}-${timestamp}-${random}`;
};

export const createIdempotencyHeaders = (scope: string): Record<string, string> => {
  return {
    'Idempotency-Key': createIdempotencyKey(scope)
  };
};
