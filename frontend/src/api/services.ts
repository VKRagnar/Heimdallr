import { apiRequest, isMockFallbackEnabled, withMockFallback } from './client';
import { ApiError } from './errors';
import {
  mockAlertEventHistory,
  mockAlertEvents,
  mockAlertRules,
  mockAgents,
  mockApplicationAccess,
  mockApplicationDetails,
  mockApplications,
  mockAuditEvents,
  mockCurrentUser,
  mockDataScope,
  mockDataSourceValidation,
  mockDataSources,
  mockLogSearchResult,
  mockMetricDefinitions,
  mockMetricQueryResult,
  mockNotificationRecords,
  mockOnCallGroups,
  mockRoles,
  mockServers,
  mockUsers,
  toPage,
} from '../mocks/data';
import type {
  AgentInstance,
  AlertEvent,
  AlertEventHistory,
  AlertRule,
  AlertRulePayload,
  Application,
  ApplicationAccess,
  ApplicationAccessGrantPayload,
  ApplicationPayload,
  AuditEvent,
  CurrentUser,
  DataScope,
  DataSource,
  DataSourceValidation,
  DefaultMetricMapping,
  Environment,
  LogSearchResult,
  MetricDefinition,
  MetricQueryResult,
  NotificationRecord,
  OnCallGroup,
  PageResult,
  Role,
  Server,
  ServerPayload,
  SystemUser,
} from '../types/domain';

interface BackendApplicationAsset {
  id?: string;
  code: string;
  name: string;
  businessLine: string;
  environment: string;
  ownerUserIds?: string[];
  accessStatus?: string;
}

interface BackendServerAsset {
  id?: string;
  hostname: string;
  ip: string;
  environment: string;
  applicationIds?: string[];
  accessStatus?: string;
}

interface BackendAuditEvent {
  id: string;
  actorUserId?: string;
  action?: string;
  targetType?: string;
  targetId?: string;
  result: string;
  occurredAt?: string;
  eventType?: string;
  operator?: string;
  objectType?: string;
  objectName?: string;
  requestId?: string;
  createdAt?: string;
}

interface BackendUserInfo {
  id: string;
  username: string;
  displayName?: string;
  name?: string;
  roles?: Array<{ name?: string; code?: string }> | string[];
  businessLines?: string[];
  menus?: string[];
  email?: string;
  status?: string;
}

interface BackendRoleInfo {
  id: string;
  code: string;
  name: string;
  permissions?: string[];
}

interface BackendCurrentUser extends Omit<BackendUserInfo, 'roles'> {
  roles?: BackendRoleInfo[] | string[];
  permissions?: string[];
}

interface BackendDataScope {
  platformAdmin?: boolean;
  applicationIds?: string[];
  businessLines?: string[];
  environments?: Environment[];
}

interface BackendValidation {
  sourceId: string;
  status: 'PASSED' | 'FAILED';
  checkedAt: string;
  items: Array<{ name: string; status: 'PASSED' | 'FAILED'; code?: string; message: string }>;
}

interface BackendMetricSeries {
  metricCode: string;
  objectId: string;
  objectName: string;
  unit: string;
  sourceId: string;
  from: string;
  to: string;
  samples: Array<{ timestamp: string; value: number }>;
}

interface BackendLogEntry {
  id: string;
  timestamp: string;
  applicationId: string;
  objectId: string;
  environment: string;
  level: 'ERROR' | 'WARN' | 'INFO' | 'DEBUG';
  message: string;
  traceId?: string;
  labels: Record<string, string>;
}

type BackendAlertRule = AlertRule;
type BackendAlertEvent = AlertEvent;
type BackendAlertEventHistory = AlertEventHistory;
type BackendNotificationRecord = NotificationRecord;
type BackendOnCallGroup = OnCallGroup;

function firstValue(value: string | undefined, fallback: string) {
  return value?.split(',').find(Boolean) ?? fallback;
}

function normalizeEnv(value: string | undefined): Environment {
  return (value === 'staging' ? 'pre' : value ?? 'prod') as Environment;
}

function toDataSourceValidation(result: BackendValidation): DataSourceValidation {
  return {
    sourceId: result.sourceId,
    status: result.status,
    checkedAt: result.checkedAt,
    checks: result.items.map((item) => ({
      name: item.name,
      status: item.status,
      message: item.message,
    })),
  };
}

function toMetricQueryResult(result: BackendMetricSeries, query?: Record<string, string | undefined>): MetricQueryResult {
  return {
    objectType: (query?.objectType ?? 'application') as MetricQueryResult['objectType'],
    objectId: result.objectId,
    objectName: result.objectName,
    environment: normalizeEnv(query?.env),
    timeRange: result.from && result.to ? `${result.from} 至 ${result.to}` : '最近 30 分钟',
    granularity: query?.step ?? '10m',
    dataQuality: {
      sourceName: result.sourceId,
      status: 'connected',
      lastSuccessAt: result.samples[result.samples.length - 1]?.timestamp ?? '-',
      missingPointRate: 0,
    },
    series: [
      {
        metricCode: result.metricCode,
        metricName: result.metricCode,
        unit: result.unit,
        points: result.samples.map((sample) => ({
          time: sample.timestamp.slice(11, 16),
          value: sample.value,
        })),
      },
    ],
  };
}

function toLogSearchResult(result: PageResult<BackendLogEntry>): LogSearchResult {
  return {
    sourceStatus: 'connected',
    sourceName: 'logs-api',
    lastSuccessAt: result.items[0]?.timestamp ?? '-',
    aggregations: [],
    entries: result.items.map((item) => ({
      id: item.id,
      timestamp: item.timestamp,
      applicationId: item.applicationId,
      level: item.level,
      application: item.applicationId,
      environment: normalizeEnv(item.environment),
      instanceIp: item.labels.instance ?? item.labels.service ?? '-',
      traceId: item.traceId,
      thread: item.labels.thread ?? '-',
      interfacePath: item.labels.interfacePath ?? item.objectId,
      summary: item.message,
      masked: item.message.includes('***'),
    })),
  };
}

function toApplication(asset: BackendApplicationAsset): Application {
  return {
    id: asset.id ?? `app-${asset.code}`,
    name: asset.name,
    code: asset.code,
    environment: normalizeEnv(asset.environment),
    owner: asset.ownerUserIds?.join(', ') || '-',
    status: 'unknown',
    accessStatus: (asset.accessStatus ?? 'connected').toLowerCase() as Application['accessStatus'],
    alertCount: 0,
    instanceCount: 0,
    updatedAt: '-',
  };
}

function toServer(asset: BackendServerAsset): Server {
  return {
    id: asset.id ?? `srv-${asset.hostname}`,
    hostname: asset.hostname,
    ip: asset.ip,
    environment: normalizeEnv(asset.environment),
    status: 'unknown',
    agentStatus: 'normal',
    cpuUsage: 0,
    memoryUsage: 0,
    applicationCount: asset.applicationIds?.length ?? 0,
    updatedAt: '-',
  };
}

function toBackendApplicationPayload(payload: ApplicationPayload): BackendApplicationAsset {
  return {
    id: payload.id,
    code: payload.code,
    name: payload.name,
    businessLine: payload.businessLine,
    environment: payload.environment,
    ownerUserIds: payload.ownerUserIds,
    accessStatus: String(payload.accessStatus).toUpperCase(),
  };
}

function toBackendServerPayload(payload: ServerPayload): BackendServerAsset {
  return {
    id: payload.id,
    hostname: payload.hostname,
    ip: payload.ip,
    environment: payload.environment,
    applicationIds: payload.applicationIds,
    accessStatus: String(payload.accessStatus).toUpperCase(),
  };
}

function toAuditEvent(event: BackendAuditEvent): AuditEvent {
  return {
    id: event.id,
    eventType: event.eventType ?? event.action ?? '-',
    operator: event.operator ?? event.actorUserId ?? '-',
    objectType: event.objectType ?? event.targetType ?? '-',
    objectName: event.objectName ?? event.targetId ?? '-',
    result: String(event.result).toLowerCase() as AuditEvent['result'],
    requestId: event.requestId ?? event.id,
    createdAt: event.createdAt ?? event.occurredAt ?? '-',
  };
}

function toSystemUser(user: BackendUserInfo): SystemUser {
  return {
    id: user.id,
    username: user.username,
    name: user.displayName ?? user.name ?? user.username,
    email: user.email ?? '-',
    status: (user.status?.toLowerCase() ?? 'enabled') as SystemUser['status'],
    roles: (user.roles ?? []).map((role) => typeof role === 'string' ? role : role.name ?? role.code ?? '-'),
  };
}

function toCurrentUser(user: BackendCurrentUser): CurrentUser {
  const roles = user.roles ?? [];
  const roleNames = roles.map((role) => typeof role === 'string' ? role : role.name ?? role.code ?? '-');
  const rolePermissions = roles.flatMap((role) => typeof role === 'string' ? [] : role.permissions ?? []);
  return {
    id: user.id,
    username: user.username,
    name: user.displayName ?? user.name ?? user.username,
    email: user.email,
    roles: roleNames,
    permissions: Array.from(new Set([...(user.permissions ?? []), ...rolePermissions])),
    menus: user.menus ?? [],
  };
}

function toDataScope(scope: BackendDataScope): DataScope {
  return {
    environments: scope.environments ?? ['prod'],
    businessLines: (scope.businessLines ?? []).map((line) => ({ id: line, name: line })),
    applicationIds: scope.applicationIds ?? [],
    isGlobal: scope.platformAdmin ?? false,
  };
}

function toRole(role: BackendRoleInfo): Role {
  return {
    id: role.id,
    name: role.name,
    code: role.code,
    description: '-',
    userCount: 0,
    permissions: role.permissions ?? [],
  };
}

function toPageWithItems<TInput, TOutput>(page: PageResult<TInput>, mapper: (item: TInput) => TOutput): PageResult<TOutput> {
  return { ...page, items: page.items.map(mapper) };
}

async function withMockMutationFallback<T>(request: Promise<T>, fallback: () => T): Promise<T> {
  if (!isMockFallbackEnabled()) {
    return request;
  }
  try {
    return await request;
  } catch (error) {
    if (error instanceof ApiError && (error.code === 'NETWORK_ERROR' || error.status === 404)) {
      console.warn(`[api] mock mutation fallback enabled for ${error.code}${error.status ? `/${error.status}` : ''}`);
      return fallback();
    }
    throw error;
  }
}

function nowText() {
  return new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-');
}

function addAuditEvent(eventType: string, objectType: string, objectName: string, result: 'success' | 'failed' | 'denied' = 'success') {
  mockAuditEvents.unshift({
    id: `ae-${Date.now()}`,
    eventType,
    operator: mockCurrentUser.name,
    objectType,
    objectName,
    result,
    requestId: `req-ui-${Date.now()}`,
    createdAt: nowText(),
  });
}

function upsertMockApplication(payload: ApplicationPayload, id?: string): Application {
  const existingIndex = id ? mockApplications.findIndex((item) => item.id === id) : -1;
  const application: Application = {
    ...(existingIndex >= 0 ? mockApplications[existingIndex] : { id: payload.id ?? `app-${payload.code || Date.now()}`, alertCount: 0, instanceCount: 0, status: 'unknown' as const, updatedAt: nowText() }),
    name: payload.name,
    code: payload.code,
    environment: payload.environment,
    owner: payload.ownerUserIds.join(', ') || '-',
    accessStatus: payload.accessStatus,
    updatedAt: nowText(),
  };
  if (existingIndex >= 0) {
    mockApplications.splice(existingIndex, 1, application);
  } else {
    mockApplications.unshift(application);
  }
  mockApplicationDetails[application.id] = {
    ...mockApplicationDetails[application.id],
    ...application,
    description: mockApplicationDetails[application.id]?.description ?? `${application.name} 的资产、接入和权限概览。`,
    servers: mockApplicationDetails[application.id]?.servers ?? [],
    dependencies: mockApplicationDetails[application.id]?.dependencies ?? [],
  };
  addAuditEvent(existingIndex >= 0 ? 'APPLICATION_UPDATE' : 'APPLICATION_CREATE', 'application', application.name);
  return application;
}

function upsertMockServer(payload: ServerPayload, id?: string): Server {
  const existingIndex = id ? mockServers.findIndex((item) => item.id === id) : -1;
  const server: Server = {
    ...(existingIndex >= 0 ? mockServers[existingIndex] : { id: payload.id ?? `srv-${Date.now()}`, status: 'unknown' as const, agentStatus: 'normal' as const, cpuUsage: 0, memoryUsage: 0, updatedAt: nowText() }),
    hostname: payload.hostname,
    ip: payload.ip,
    environment: payload.environment,
    applicationCount: payload.applicationIds.length,
    updatedAt: nowText(),
  };
  if (existingIndex >= 0) {
    mockServers.splice(existingIndex, 1, server);
  } else {
    mockServers.unshift(server);
  }
  addAuditEvent(existingIndex >= 0 ? 'SERVER_UPDATE' : 'SERVER_CREATE', 'server', server.hostname);
  return server;
}

function auditMockAccess(payload: ApplicationAccessGrantPayload) {
  const action = payload.scopeType === 'application'
    ? payload.action === 'grant' ? 'ACCESS_GRANT_APPLICATION' : 'ACCESS_REVOKE_APPLICATION'
    : payload.action === 'grant' ? 'ACCESS_GRANT_BUSINESS_LINE' : 'ACCESS_REVOKE_BUSINESS_LINE';
  addAuditEvent(action, 'USER', payload.userId);
  return mockUsers.find((user) => user.id === payload.userId) ?? mockUsers[0];
}

function touchAlertRule(rule: AlertRule): AlertRule {
  return { ...rule, updatedAt: nowText() };
}

function upsertMockAlertRule(payload: AlertRulePayload, id?: string): AlertRule {
  const existingIndex = id ? mockAlertRules.findIndex((item) => item.id === id) : -1;
  const rule: AlertRule = touchAlertRule({
    ...(existingIndex >= 0 ? mockAlertRules[existingIndex] : { id: `alert-rule-${Date.now()}`, createdAt: nowText() }),
    ...payload,
    objectName: payload.objectId,
  });
  if (existingIndex >= 0) {
    mockAlertRules.splice(existingIndex, 1, rule);
  } else {
    mockAlertRules.unshift(rule);
  }
  addAuditEvent(existingIndex >= 0 ? 'ALERT_RULE_UPDATE' : 'ALERT_RULE_CREATE', 'alert_rule', rule.name);
  return rule;
}

function setMockAlertRuleEnabled(ruleId: string, enabled: boolean): AlertRule {
  const index = mockAlertRules.findIndex((item) => item.id === ruleId);
  const rule = touchAlertRule({ ...(mockAlertRules[index] ?? mockAlertRules[0]), enabled });
  if (index >= 0) {
    mockAlertRules.splice(index, 1, rule);
  }
  addAuditEvent(enabled ? 'ALERT_RULE_ENABLE' : 'ALERT_RULE_DISABLE', 'alert_rule', rule.name);
  return rule;
}

function matchesAlertKeyword(values: Array<string | undefined>, keyword?: string) {
  if (!keyword) {
    return true;
  }
  const normalizedKeyword = keyword.trim().toLowerCase();
  if (!normalizedKeyword) {
    return true;
  }
  return values.some((value) => value?.toLowerCase().includes(normalizedKeyword));
}

function filterMockAlertRules(query?: Record<string, string | undefined>) {
  return mockAlertRules.filter((rule) => {
    const severityMatches = !query?.severity || String(rule.severity).toLowerCase() === query.severity.toLowerCase();
    const enabledMatches = !query?.enabled || String(rule.enabled) === query.enabled;
    const keywordMatches = matchesAlertKeyword([rule.name, rule.objectId, rule.objectName, rule.metricCode], query?.keyword);
    return severityMatches && enabledMatches && keywordMatches;
  });
}

function filterMockAlertEvents(query?: Record<string, string | undefined>) {
  return mockAlertEvents.filter((event) => {
    const statusMatches = !query?.status || String(event.status).toLowerCase() === query.status.toLowerCase();
    const severityMatches = !query?.severity || String(event.severity).toLowerCase() === query.severity.toLowerCase();
    const keywordMatches = matchesAlertKeyword([event.ruleName, event.objectId, event.objectName, event.metricCode], query?.keyword);
    return statusMatches && severityMatches && keywordMatches;
  });
}

function evaluateMockAlertRule(ruleId: string): AlertEvent {
  const rule = mockAlertRules.find((item) => item.id === ruleId) ?? mockAlertRules[0];
  const activeEvent = mockAlertEvents.find((item) => item.ruleId === rule.id && !['CLOSED', 'RECOVERED', 'closed', 'recovered'].includes(item.status));
  if (activeEvent) {
    return activeEvent;
  }
  const event: AlertEvent = {
    id: `alert-event-${Date.now()}`,
    ruleId: rule.id,
    ruleName: rule.name,
    objectId: rule.objectId,
    objectName: rule.objectName,
    metricCode: rule.metricCode,
    severity: rule.severity,
    status: 'NOTIFIED',
    triggerValue: rule.threshold + 1,
    threshold: rule.threshold,
    operator: rule.operator,
    triggeredAt: nowText(),
    notifiedAt: nowText(),
    updatedAt: nowText(),
  };
  mockAlertEvents.unshift(event);
  mockAlertEventHistory.unshift({ id: `alert-history-${Date.now()}`, eventId: event.id, toStatus: 'TRIGGERED', action: 'TRIGGER', operatorUserId: 'system', message: 'Threshold condition matched', operatedAt: nowText() });
  mockNotificationRecords.unshift({ id: `notification-${Date.now()}`, eventId: event.id, ruleId: rule.id, channelType: 'EMAIL', receiver: rule.onCallGroupId ?? 'u-admin', status: 'SENT', retryCount: 0, sentAt: nowText(), createdAt: nowText() });
  addAuditEvent('ALERT_EVENT_TRIGGER', 'alert_event', event.id);
  return event;
}

function transitionMockAlertEvent(eventId: string, action: string, message?: string): AlertEvent {
  const index = mockAlertEvents.findIndex((item) => item.id === eventId);
  const event = mockAlertEvents[index] ?? mockAlertEvents[0];
  const status = action === 'ACKNOWLEDGE' ? 'ACKNOWLEDGED'
    : action === 'PROCESS' ? 'PROCESSING'
      : action === 'RECOVER' ? 'RECOVERED'
        : action === 'CLOSE' ? 'CLOSED'
          : event.status;
  const updated: AlertEvent = {
    ...event,
    status,
    assigneeUserId: ['ACKNOWLEDGE', 'PROCESS'].includes(action) ? mockCurrentUser.id : event.assigneeUserId,
    closeReason: action === 'CLOSE' ? message : event.closeReason,
    acknowledgedAt: action === 'ACKNOWLEDGE' ? nowText() : event.acknowledgedAt,
    processingAt: action === 'PROCESS' ? nowText() : event.processingAt,
    recoveredAt: action === 'RECOVER' ? nowText() : event.recoveredAt,
    closedAt: action === 'CLOSE' ? nowText() : event.closedAt,
    updatedAt: nowText(),
  };
  if (index >= 0) {
    mockAlertEvents.splice(index, 1, updated);
  }
  mockAlertEventHistory.unshift({ id: `alert-history-${Date.now()}`, eventId, fromStatus: event.status, toStatus: status, action, operatorUserId: mockCurrentUser.id, message, operatedAt: nowText() });
  addAuditEvent(`ALERT_EVENT_${action}`, 'alert_event', eventId);
  return updated;
}

export const accessApi = {
  me: () => withMockFallback(apiRequest<BackendCurrentUser>('/api/v1/me').then(toCurrentUser), mockCurrentUser),
  dataScope: () => withMockFallback(apiRequest<BackendDataScope>('/api/v1/me/data-scope').then(toDataScope), mockDataScope),
};

export const assetsApi = {
  applications: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<BackendApplicationAsset>>('/api/v1/applications', { query }).then((page) => toPageWithItems(page, toApplication)), toPage(mockApplications)),
  applicationDetail: (appId: string) =>
    withMockFallback(apiRequest<BackendApplicationAsset>(`/api/v1/applications/${appId}`).then((asset) => ({ ...mockApplicationDetails[appId], ...toApplication(asset), dependencies: mockApplicationDetails[appId]?.dependencies ?? [], servers: mockApplicationDetails[appId]?.servers ?? [] })), mockApplicationDetails[appId] ?? mockApplicationDetails['app-ace']),
  createApplication: (payload: ApplicationPayload) =>
    withMockMutationFallback(apiRequest<BackendApplicationAsset>('/api/v1/applications', { method: 'POST', body: toBackendApplicationPayload(payload) }).then(toApplication), () => upsertMockApplication(payload)),
  updateApplication: (appId: string, payload: ApplicationPayload) =>
    withMockMutationFallback(apiRequest<BackendApplicationAsset>(`/api/v1/applications/${appId}`, { method: 'PUT', body: toBackendApplicationPayload(payload) }).then(toApplication), () => upsertMockApplication(payload, appId)),
  importApplications: (payload: ApplicationPayload[]) =>
    withMockMutationFallback(apiRequest<PageResult<BackendApplicationAsset>>('/api/v1/applications/import', { method: 'POST', body: payload.map(toBackendApplicationPayload) }).then((page) => toPageWithItems(page, toApplication)), () => toPage(payload.map((item) => upsertMockApplication(item)))),
  servers: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<BackendServerAsset>>('/api/v1/servers', { query }).then((page) => toPageWithItems(page, toServer)), toPage(mockServers)),
  createServer: (payload: ServerPayload) =>
    withMockMutationFallback(apiRequest<BackendServerAsset>('/api/v1/servers', { method: 'POST', body: toBackendServerPayload(payload) }).then(toServer), () => upsertMockServer(payload)),
  updateServer: (serverId: string, payload: ServerPayload) =>
    withMockMutationFallback(apiRequest<BackendServerAsset>(`/api/v1/servers/${serverId}`, { method: 'PUT', body: toBackendServerPayload(payload) }).then(toServer), () => upsertMockServer(payload, serverId)),
  importServers: (payload: ServerPayload[]) =>
    withMockMutationFallback(apiRequest<PageResult<BackendServerAsset>>('/api/v1/servers/import', { method: 'POST', body: payload.map(toBackendServerPayload) }).then((page) => toPageWithItems(page, toServer)), () => toPage(payload.map((item) => upsertMockServer(item)))),
};

export const systemApi = {
  users: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<BackendUserInfo>>('/api/v1/access/users', { query }).then((page) => toPageWithItems(page, toSystemUser)), toPage(mockUsers)),
  roles: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<BackendRoleInfo>>('/api/v1/access/roles', { query }).then((page) => toPageWithItems(page, toRole)), toPage(mockRoles)),
  auditEvents: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<BackendAuditEvent>>('/api/v1/system/audit-events', { query }).then((page) => toPageWithItems(page, toAuditEvent)), toPage(mockAuditEvents)),
};

export const accessManagementApi = {
  users: () =>
    withMockFallback(apiRequest<PageResult<BackendUserInfo>>('/api/v1/access/users').then((page) => toPageWithItems(page, toSystemUser)), toPage(mockUsers)),
  dataSources: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<DataSource>>('/api/v1/data-sources', { query }), toPage(mockDataSources)),
  validateDataSource: (sourceId: string) =>
    withMockFallback(apiRequest<BackendValidation>(`/api/v1/data-sources/${sourceId}/validate`, { method: 'POST' }).then(toDataSourceValidation), {
      ...mockDataSourceValidation,
      sourceId,
    }),
  applicationAccess: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<ApplicationAccess>>('/api/v1/data-sources/access-status', { query }), toPage(mockApplicationAccess)),
  grantApplicationAccess: (userId: string, applicationId: string) =>
    withMockMutationFallback(apiRequest<SystemUser>(`/api/v1/access/users/${userId}/applications`, { method: 'POST', body: { applicationId } }), () => auditMockAccess({ userId, applicationId, scopeType: 'application', action: 'grant' })),
  revokeApplicationAccess: (userId: string, applicationId: string) =>
    withMockMutationFallback(apiRequest<SystemUser>(`/api/v1/access/users/${userId}/applications/${applicationId}`, { method: 'DELETE' }), () => auditMockAccess({ userId, applicationId, scopeType: 'application', action: 'revoke' })),
  grantBusinessLineAccess: (userId: string, businessLine: string) =>
    withMockMutationFallback(apiRequest<SystemUser>(`/api/v1/access/users/${userId}/business-lines`, { method: 'POST', body: { businessLine } }), () => auditMockAccess({ userId, businessLine, scopeType: 'businessLine', action: 'grant' })),
  revokeBusinessLineAccess: (userId: string, businessLine: string) =>
    withMockMutationFallback(apiRequest<SystemUser>(`/api/v1/access/users/${userId}/business-lines/${businessLine}`, { method: 'DELETE' }), () => auditMockAccess({ userId, businessLine, scopeType: 'businessLine', action: 'revoke' })),
  agents: (query?: Record<string, string | undefined>) => withMockFallback(apiRequest<PageResult<AgentInstance>>('/api/v1/agents', { query }), toPage(mockAgents)),
};

export const observabilityApi = {
  metricDefinitions: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<MetricDefinition>>('/api/v1/metrics/definitions', { query }), toPage(mockMetricDefinitions)),
  defaultMappings: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<DefaultMetricMapping>>('/api/v1/metrics/default-mappings', { query }), toPage([])),
  queryMetrics: (query?: Record<string, string | undefined>) =>
    withMockFallback(
      apiRequest<BackendMetricSeries>('/api/v1/metrics/query', {
        method: 'POST',
        body: {
          metricCode: firstValue(query?.metricCodes ?? query?.metricCode, 'http_5xx_rate'),
          objectId: query?.objectId || 'obj-ipro-api',
          objectType: query?.objectType,
          environment: query?.env,
          from: query?.from,
          to: query?.to,
          step: query?.step,
        },
      }).then((result) => toMetricQueryResult(result, query)),
      mockMetricQueryResult,
    ),
  searchLogs: (query?: Record<string, string | undefined>) =>
    withMockFallback(
      apiRequest<PageResult<BackendLogEntry>>('/api/v1/logs/search', {
        method: 'POST',
        body: {
          applicationId: query?.appId,
          objectId: query?.objectId,
          environment: query?.env,
          level: query?.level,
          keyword: query?.keyword,
          traceId: query?.traceId,
          from: query?.from,
          to: query?.to,
          pageNo: query?.pageNo,
          pageSize: query?.pageSize,
        },
      }).then(toLogSearchResult),
      mockLogSearchResult,
    ),
};

export const alertsApi = {
  rules: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<BackendAlertRule>>('/api/v1/alerts/rules', { query }), toPage(filterMockAlertRules(query))),
  createRule: (payload: AlertRulePayload) =>
    withMockMutationFallback(apiRequest<BackendAlertRule>('/api/v1/alerts/rules', { method: 'POST', body: payload }), () => upsertMockAlertRule(payload)),
  updateRule: (ruleId: string, payload: AlertRulePayload) =>
    withMockMutationFallback(apiRequest<BackendAlertRule>(`/api/v1/alerts/rules/${ruleId}`, { method: 'PUT', body: payload }), () => upsertMockAlertRule(payload, ruleId)),
  enableRule: (ruleId: string) =>
    withMockMutationFallback(apiRequest<BackendAlertRule>(`/api/v1/alerts/rules/${ruleId}/enable`, { method: 'POST' }), () => setMockAlertRuleEnabled(ruleId, true)),
  disableRule: (ruleId: string) =>
    withMockMutationFallback(apiRequest<BackendAlertRule>(`/api/v1/alerts/rules/${ruleId}/disable`, { method: 'POST' }), () => setMockAlertRuleEnabled(ruleId, false)),
  evaluateRule: (ruleId: string) =>
    withMockMutationFallback(apiRequest<BackendAlertEvent>(`/api/v1/alerts/rules/${ruleId}/evaluate`, { method: 'POST' }), () => evaluateMockAlertRule(ruleId)),
  evaluateEnabledRules: () =>
    withMockMutationFallback(apiRequest<PageResult<BackendAlertEvent>>('/api/v1/alerts/rules/evaluate', { method: 'POST' }), () => toPage(mockAlertRules.filter((rule) => rule.enabled).map((rule) => evaluateMockAlertRule(rule.id)))),
  events: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<BackendAlertEvent>>('/api/v1/alerts/events', { query }), toPage(filterMockAlertEvents(query))),
  transitionEvent: (eventId: string, action: string, message?: string) =>
    withMockMutationFallback(apiRequest<BackendAlertEvent>(`/api/v1/alerts/events/${eventId}/actions`, { method: 'POST', body: { action, message } }), () => transitionMockAlertEvent(eventId, action, message)),
  eventHistory: (eventId: string) =>
    withMockFallback(apiRequest<PageResult<BackendAlertEventHistory>>(`/api/v1/alerts/events/${eventId}/history`), toPage(mockAlertEventHistory.filter((item) => item.eventId === eventId))),
  notifications: (eventId?: string) =>
    withMockFallback(apiRequest<PageResult<BackendNotificationRecord>>('/api/v1/alerts/notifications', { query: { eventId } }), toPage(mockNotificationRecords.filter((item) => !eventId || item.eventId === eventId))),
  onCallGroups: () =>
    withMockFallback(apiRequest<PageResult<BackendOnCallGroup>>('/api/v1/alerts/on-call-groups'), toPage(mockOnCallGroups)),
};
