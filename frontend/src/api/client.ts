import { ApiError, normalizeErrorCode } from './errors';

interface ApiEnvelope<T> {
  code?: string;
  message?: string;
  data?: T;
  requestId?: string;
  success?: boolean;
}

export interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown;
  query?: Record<string, string | number | boolean | undefined | null>;
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';
const TOKEN_KEY = 'heimdallr-token';

function createRequestId() {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID();
  }
  return `req-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function buildUrl(path: string, query?: RequestOptions['query']) {
  const url = new URL(`${API_BASE_URL}${path}`, window.location.origin);
  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, String(value));
    }
  });
  return `${url.pathname}${url.search}`;
}

async function readJson<T>(response: Response): Promise<ApiEnvelope<T> | T | undefined> {
  const text = await response.text();
  if (!text) return undefined;
  return JSON.parse(text) as ApiEnvelope<T> | T;
}

function isEnvelope<T>(payload: ApiEnvelope<T> | T | undefined): payload is ApiEnvelope<T> {
  return Boolean(payload && typeof payload === 'object' && ('data' in payload || 'code' in payload));
}

export function getAuthToken() {
  return window.localStorage.getItem(TOKEN_KEY);
}

export function setAuthToken(token: string) {
  window.localStorage.setItem(TOKEN_KEY, token);
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const token = getAuthToken();
  const requestId = createRequestId();
  const headers = new Headers(options.headers);
  headers.set('Accept', 'application/json');
  headers.set('X-Request-Id', requestId);
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json');
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  let response: Response;
  try {
    response = await fetch(buildUrl(path, options.query), {
      ...options,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });
  } catch {
    throw new ApiError('NETWORK_ERROR', undefined, 0, requestId);
  }

  const payload = await readJson<T>(response);
  const responseRequestId = response.headers.get('X-Request-Id') ?? requestId;

  if (!response.ok) {
    const envelope = isEnvelope<T>(payload) ? payload : undefined;
    const code = normalizeErrorCode(envelope?.code, response.status);
    throw new ApiError(code, envelope?.message, response.status, envelope?.requestId ?? responseRequestId);
  }

  if (!isEnvelope<T>(payload)) {
    return payload as T;
  }

  if (payload.code && !['SUCCESS', 'OK'].includes(payload.code) && payload.success !== true) {
    const code = normalizeErrorCode(payload.code, response.status);
    throw new ApiError(code, payload.message, response.status, payload.requestId ?? responseRequestId);
  }

  return payload.data as T;
}

export async function withMockFallback<T>(request: Promise<T>, fallback: T): Promise<T> {
  if (import.meta.env.PROD) {
    return request;
  }
  try {
    return await request;
  } catch (error) {
    if (error instanceof ApiError && (error.code === 'NETWORK_ERROR' || error.status === 404)) {
      return fallback;
    }
    throw error;
  }
}
