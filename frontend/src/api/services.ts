import { apiRequest, withMockFallback } from './client';
import {
  mockApplicationDetails,
  mockApplications,
  mockAuditEvents,
  mockCurrentUser,
  mockDataScope,
  mockRoles,
  mockServers,
  mockUsers,
  toPage,
} from '../mocks/data';
import type {
  Application,
  ApplicationDetail,
  AuditEvent,
  CurrentUser,
  DataScope,
  PageResult,
  Role,
  Server,
  SystemUser,
} from '../types/domain';

export const accessApi = {
  me: () => withMockFallback(apiRequest<CurrentUser>('/api/v1/me'), mockCurrentUser),
  dataScope: () => withMockFallback(apiRequest<DataScope>('/api/v1/me/data-scope'), mockDataScope),
};

export const assetsApi = {
  applications: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<Application>>('/api/v1/applications', { query }), toPage(mockApplications)),
  applicationDetail: (appId: string) =>
    withMockFallback(
      apiRequest<ApplicationDetail>(`/api/v1/applications/${appId}`),
      mockApplicationDetails[appId] ?? mockApplicationDetails['app-ace'],
    ),
  servers: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<Server>>('/api/v1/servers', { query }), toPage(mockServers)),
};

export const systemApi = {
  users: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<SystemUser>>('/api/v1/system/users', { query }), toPage(mockUsers)),
  roles: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<Role>>('/api/v1/system/roles', { query }), toPage(mockRoles)),
  auditEvents: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<AuditEvent>>('/api/v1/system/audit-events', { query }), toPage(mockAuditEvents)),
};
