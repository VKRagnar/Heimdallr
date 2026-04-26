export type ApiErrorCode =
  | 'FORBIDDEN'
  | 'MENU_FORBIDDEN'
  | 'APP_DATA_FORBIDDEN'
  | 'ENV_FORBIDDEN'
  | 'SENSITIVE_FIELD_FORBIDDEN'
  | 'UNAUTHORIZED'
  | 'NOT_FOUND'
  | 'VALIDATION_FAILED'
  | 'NETWORK_ERROR'
  | 'UNKNOWN';

export interface ErrorMeta {
  title: string;
  description: string;
  status?: 401 | 403 | 404 | 422 | 500;
}

export const errorCodeMap: Record<ApiErrorCode, ErrorMeta> = {
  FORBIDDEN: {
    title: '无访问权限',
    description: '当前账号没有执行此操作的权限。',
    status: 403,
  },
  MENU_FORBIDDEN: {
    title: '菜单不可见',
    description: '当前角色未开通该菜单，可联系平台管理员授权。',
    status: 403,
  },
  APP_DATA_FORBIDDEN: {
    title: '应用数据不可见',
    description: '当前账号不在该应用的数据范围内。',
    status: 403,
  },
  ENV_FORBIDDEN: {
    title: '环境不可见',
    description: '当前账号没有该环境的数据访问权限。',
    status: 403,
  },
  SENSITIVE_FIELD_FORBIDDEN: {
    title: '敏感字段受控',
    description: '查看敏感明文需要额外授权并记录审计。',
    status: 403,
  },
  UNAUTHORIZED: {
    title: '登录状态失效',
    description: '请重新登录后继续访问。',
    status: 401,
  },
  NOT_FOUND: {
    title: '资源不存在',
    description: '目标资源不存在或已被删除。',
    status: 404,
  },
  VALIDATION_FAILED: {
    title: '参数校验失败',
    description: '请检查筛选条件或表单内容后重试。',
    status: 422,
  },
  NETWORK_ERROR: {
    title: '接口暂不可用',
    description: '当前无法连接服务端，已尝试使用开发数据兜底。',
    status: 500,
  },
  UNKNOWN: {
    title: '未知错误',
    description: '请求处理失败，请稍后重试。',
    status: 500,
  },
};

export class ApiError extends Error {
  code: ApiErrorCode;
  status?: number;
  requestId?: string;

  constructor(code: ApiErrorCode, message?: string, status?: number, requestId?: string) {
    super(message ?? errorCodeMap[code].description);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
    this.requestId = requestId;
  }
}

export function normalizeErrorCode(code?: string, status?: number): ApiErrorCode {
  if (code && code in errorCodeMap) {
    return code as ApiErrorCode;
  }
  if (status === 401) return 'UNAUTHORIZED';
  if (status === 403) return 'FORBIDDEN';
  if (status === 404) return 'NOT_FOUND';
  if (status === 422 || status === 400) return 'VALIDATION_FAILED';
  return 'UNKNOWN';
}
