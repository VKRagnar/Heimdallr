import { apiRequest, withMockFallback } from './client';
import {
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
  mockRoles,
  mockServers,
  mockUsers,
  toPage,
} from '../mocks/data';
import type {
  AgentInstance,
  Application,
  ApplicationAccess,
  ApplicationDetail,
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
  PageResult,
  Role,
  Server,
  SystemUser,
} from '../types/domain';

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

export const accessApi = {
  me: () => withMockFallback(apiRequest<CurrentUser>('/api/v1/me'), mockCurrentUser),
  dataScope: () => withMockFallback(apiRequest<DataScope>('/api/v1/me/data-scope'), mockDataScope),
};

export const assetsApi = {
  applications: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<Application>>('/api/v1/applications', { query }), toPage(mockApplications)),
  applicationDetail: (appId: string) =>
    withMockFallback(apiRequest<ApplicationDetail>(`/api/v1/applications/${appId}`), mockApplicationDetails[appId] ?? mockApplicationDetails['app-ace']),
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

export const accessManagementApi = {
  dataSources: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<DataSource>>('/api/v1/data-sources', { query }), toPage(mockDataSources)),
  validateDataSource: (sourceId: string) =>
    withMockFallback(apiRequest<BackendValidation>(`/api/v1/data-sources/${sourceId}/validate`, { method: 'POST' }).then(toDataSourceValidation), {
      ...mockDataSourceValidation,
      sourceId,
    }),
  applicationAccess: (query?: Record<string, string | undefined>) =>
    withMockFallback(apiRequest<PageResult<ApplicationAccess>>('/api/v1/data-sources/access-status', { query }), toPage(mockApplicationAccess)),
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
