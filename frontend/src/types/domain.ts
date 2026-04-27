export type Environment = 'prod' | 'pre' | 'staging' | 'test' | 'dev';

export type HealthStatus = 'healthy' | 'warning' | 'critical' | 'unknown';
export type AccessStatus =
  | 'connected'
  | 'CONNECTED'
  | 'partial'
  | 'disconnected'
  | 'pending'
  | 'partial_connected'
  | 'PARTIAL_CONNECTED'
  | 'not_connected'
  | 'collector_error'
  | 'source_unavailable'
  | 'SOURCE_UNAVAILABLE'
  | 'no_recent_data'
  | 'NO_RECENT_DATA'
  | 'mapping_invalid';
export type AgentStatus = 'online' | 'ONLINE' | 'offline' | 'abnormal' | 'uninstalled' | 'normal' | 'no_heartbeat' | 'NO_HEARTBEAT' | 'version_low' | 'config_error' | 'CONFIG_ERROR' | 'not_installed';
export type UserStatus = 'enabled' | 'disabled' | 'locked';
export type AuditResult = 'success' | 'failed' | 'denied';
export type DataSourceType = 'prometheus' | 'PROMETHEUS' | 'grafana' | 'GRAFANA' | 'skywalking' | 'SKYWALKING' | 'elk' | 'ELK' | 'loki' | 'LOKI' | 'agent' | 'AGENT' | 'kafka' | 'KAFKA' | 'postgresql' | 'POSTGRESQL';
export type DataSourceStatus = 'enabled' | 'ENABLED' | 'disabled' | 'DISABLED' | 'unhealthy' | 'UNHEALTHY' | 'verifying';
export type LogLevel = 'ERROR' | 'WARN' | 'INFO' | 'DEBUG';
export type MonitorObjectType = 'application' | 'server' | 'kafka' | 'postgresql';

export interface PageResult<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

export interface CurrentUser {
  id: string;
  name: string;
  username: string;
  email?: string;
  roles: string[];
  permissions: string[];
  menus: string[];
}

export interface DataScope {
  environments: Environment[];
  businessLines: Array<{ id: string; name: string }>;
  applicationIds: string[];
  isGlobal: boolean;
}

export interface Application {
  id: string;
  name: string;
  code: string;
  environment: Environment;
  owner: string;
  status: HealthStatus;
  accessStatus: AccessStatus;
  alertCount: number;
  instanceCount: number;
  updatedAt: string;
}

export interface ApplicationDetail extends Application {
  description?: string;
  servers: string[];
  dependencies: Array<{ name: string; type: string; status: HealthStatus }>;
  accessChannels?: AccessChannel[];
  defaultMetricCodes?: string[];
}

export interface Server {
  id: string;
  hostname: string;
  ip: string;
  environment: Environment;
  status: HealthStatus;
  agentStatus: AgentStatus;
  cpuUsage: number;
  memoryUsage: number;
  applicationCount: number;
  updatedAt: string;
}

export interface SystemUser {
  id: string;
  name: string;
  username: string;
  email: string;
  status: UserStatus;
  roles: string[];
  lastLoginAt?: string;
}

export interface Role {
  id: string;
  name: string;
  code: string;
  description: string;
  userCount: number;
  permissions: string[];
}

export interface AuditEvent {
  id: string;
  eventType: string;
  operator: string;
  objectType: string;
  objectName: string;
  result: AuditResult;
  requestId: string;
  createdAt: string;
}

export interface DataSource {
  id: string;
  name: string;
  type: DataSourceType;
  environment: Environment;
  baseUrl: string;
  healthCheckPath: string;
  authType: 'none' | 'basic' | 'token' | 'aksk' | 'custom';
  status: DataSourceStatus;
  relatedApplications?: number;
  timeoutSeconds: number;
  retryCount: number;
  lastCheckAt: string;
  lastSuccessAt?: string;
  lastErrorCode?: string;
  lastErrorMessage?: string;
}

export interface DataSourceValidation {
  id?: string;
  sourceId: string;
  status: 'passed' | 'failed' | 'running' | 'PASSED' | 'FAILED';
  durationMs?: number;
  checkedAt: string;
  checks?: Array<{ name: string; status: 'passed' | 'failed' | 'running' | 'PASSED' | 'FAILED'; message: string }>;
  items?: Array<{ name: string; status: 'PASSED' | 'FAILED'; code?: string; message: string }>;
}

export interface AccessChannel {
  name: string;
  type: 'metrics' | 'logs' | 'trace' | 'agent' | 'health';
  sourceName: string;
  status: AccessStatus;
  lastSeenAt?: string;
  latencySeconds?: number;
  failureReason?: string;
}

export interface ApplicationAccess {
  id: string;
  appId: string;
  appName: string;
  appCode: string;
  environment: Environment;
  owner: string;
  metricsAccess: string;
  traceAccess: string;
  logsAccess: string;
  healthCheck: string;
  agentStatus: AgentStatus;
  accessStatus: AccessStatus;
  lastVerifiedAt: string;
}

export interface AgentInstance {
  id: string;
  hostname: string;
  ip?: string;
  environment: Environment;
  status: AgentStatus;
  version: string;
  baselineVersion?: string;
  collectProfile?: string;
  configVersion?: string;
  cpuUsage?: number;
  memoryUsage?: number;
  lastHeartbeatAt?: string;
  failureReason?: string;
}

export interface MetricDefinition {
  code: string;
  name: string;
  unit: string;
  objectType: MonitorObjectType | Uppercase<MonitorObjectType>;
  sourceType: DataSourceType;
  promqlTemplate?: string;
  defaultQueryTemplate?: string;
  defaultEnabled?: boolean;
}

export interface DefaultMetricMapping {
  id: string;
  objectType: MonitorObjectType | Uppercase<MonitorObjectType> | 'REDIS';
  metricCode: string;
  sourceType: DataSourceType;
  externalMetric?: string;
  externalMetricName?: string;
  queryTemplate: string;
  unit: string;
  defaultLabels?: Record<string, string>;
  labelMappings?: Record<string, string>;
}

export interface MetricQueryResult {
  objectType: MonitorObjectType;
  objectId: string;
  objectName: string;
  environment: Environment;
  timeRange: string;
  granularity: string;
  dataQuality: {
    sourceName: string;
    status: AccessStatus;
    lastSuccessAt: string;
    missingPointRate: number;
  };
  series: Array<{
    metricCode: string;
    metricName: string;
    unit: string;
    threshold?: number;
    points: Array<{ time: string; value: number; alert?: boolean; logHit?: boolean }>;
  }>;
}

export interface LogAggregation {
  signature: string;
  exceptionType: string;
  application: string;
  interfacePath: string;
  instanceCount: number;
  logCount: number;
  traceCount: number;
  firstSeenAt: string;
  lastSeenAt: string;
}

export interface LogEntry {
  id: string;
  timestamp: string;
  applicationId?: string;
  level: LogLevel;
  application: string;
  environment: Environment;
  instanceIp: string;
  traceId?: string;
  thread: string;
  interfacePath: string;
  summary: string;
  masked: boolean;
}

export interface LogSearchResult {
  sourceStatus: AccessStatus;
  sourceName: string;
  lastSuccessAt: string;
  aggregations: LogAggregation[];
  entries: LogEntry[];
}
