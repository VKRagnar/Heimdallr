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
  LogSearchResult,
  MetricDefinition,
  MetricQueryResult,
  PageResult,
  Role,
  Server,
  SystemUser,
} from '../types/domain';

export const mockCurrentUser: CurrentUser = {
  id: 'u-001',
  name: '平台管理员',
  username: 'admin',
  email: 'admin@example.com',
  roles: ['平台管理员'],
  permissions: ['*'],
  menus: ['home', 'applications', 'servers', 'logs', 'metrics', 'access.data-sources', 'access.applications', 'access.agents', 'system.users', 'system.roles', 'system.audit-events'],
};

export const mockDataScope: DataScope = {
  environments: ['prod', 'pre', 'test', 'dev'],
  businessLines: [
    { id: 'core', name: '核心业务线' },
    { id: 'ops', name: '运维平台' },
  ],
  applicationIds: ['app-ace', 'app-ipro', 'app-cms'],
  isGlobal: true,
};

export const mockApplications: Application[] = [
  { id: 'app-ace', name: 'ACE 交易中心', code: 'ace-trade', environment: 'prod', owner: '陈然', status: 'warning', accessStatus: 'connected', alertCount: 3, instanceCount: 12, updatedAt: '2026-04-28 09:56:30' },
  { id: 'app-ipro', name: 'iPro 客户平台', code: 'ipro-customer', environment: 'pre', owner: '林知', status: 'healthy', accessStatus: 'partial_connected', alertCount: 0, instanceCount: 8, updatedAt: '2026-04-28 09:42:08' },
  { id: 'app-cms', name: 'CMS 内容服务', code: 'cms-content', environment: 'test', owner: '许宁', status: 'critical', accessStatus: 'collector_error', alertCount: 7, instanceCount: 5, updatedAt: '2026-04-28 09:12:14' },
];

export const mockApplicationDetails: Record<string, ApplicationDetail> = Object.fromEntries(
  mockApplications.map((app) => [
    app.id,
    {
      ...app,
      description: `${app.name} 的资产、接入和权限概览。`,
      servers: ['srv-001', 'srv-002'],
      dependencies: [
        { name: 'Prometheus', type: 'metrics', status: 'healthy' },
        { name: 'ELK 日志集群', type: 'logs', status: app.status },
      ],
      accessChannels: [
        { name: '指标源', type: 'metrics', sourceName: 'prom-prod', status: app.id === 'app-cms' ? 'source_unavailable' : 'connected', lastSeenAt: app.updatedAt, latencySeconds: 28 },
        { name: '日志源', type: 'logs', sourceName: app.id === 'app-ipro' ? 'loki-pre' : 'elk-prod', status: app.id === 'app-ipro' ? 'partial_connected' : 'connected', lastSeenAt: app.updatedAt, latencySeconds: 46 },
        { name: '链路源', type: 'trace', sourceName: 'skywalking-prod', status: app.id === 'app-ace' ? 'connected' : 'not_connected', lastSeenAt: app.id === 'app-ace' ? app.updatedAt : undefined },
        { name: 'Agent', type: 'agent', sourceName: 'heimdallr-agent', status: app.id === 'app-cms' ? 'collector_error' : 'connected', lastSeenAt: app.updatedAt },
      ],
      defaultMetricCodes: ['error_rate', 'qps', 'p95_latency', 'cpu_usage', 'memory_usage'],
    },
  ]),
);

export const mockServers: Server[] = [
  { id: 'srv-001', hostname: 'prod-ace-01', ip: '10.12.8.21', environment: 'prod', status: 'warning', agentStatus: 'online', cpuUsage: 68, memoryUsage: 74, applicationCount: 3, updatedAt: '2026-04-28 09:55:00' },
  { id: 'srv-002', hostname: 'pre-ipro-02', ip: '10.13.4.18', environment: 'pre', status: 'healthy', agentStatus: 'online', cpuUsage: 31, memoryUsage: 45, applicationCount: 2, updatedAt: '2026-04-28 09:52:00' },
  { id: 'srv-003', hostname: 'test-cms-03', ip: '10.14.2.9', environment: 'test', status: 'critical', agentStatus: 'abnormal', cpuUsage: 92, memoryUsage: 88, applicationCount: 1, updatedAt: '2026-04-28 09:12:00' },
];

export const mockUsers: SystemUser[] = [
  { id: 'u-001', name: '平台管理员', username: 'admin', email: 'admin@example.com', status: 'enabled', roles: ['平台管理员'], lastLoginAt: '2026-04-28 09:00:00' },
  { id: 'u-002', name: '陈然', username: 'chenran', email: 'chenran@example.com', status: 'enabled', roles: ['应用负责人'], lastLoginAt: '2026-04-28 08:22:00' },
  { id: 'u-003', name: '林知', username: 'linzhi', email: 'linzhi@example.com', status: 'locked', roles: ['只读用户'] },
];

export const mockRoles: Role[] = [
  { id: 'r-001', name: '平台管理员', code: 'platform_admin', description: '拥有平台配置、审计和授权能力。', userCount: 2, permissions: ['*'] },
  { id: 'r-002', name: '运维/SRE', code: 'sre', description: '负责资产接入、告警处理和观测数据查询。', userCount: 8, permissions: ['assets:read', 'alerts:write'] },
  { id: 'r-003', name: '应用负责人', code: 'app_owner', description: '查看和处理授权应用内的问题。', userCount: 24, permissions: ['apps:read', 'alerts:handle'] },
];

export const mockAuditEvents: AuditEvent[] = [
  { id: 'ae-001', eventType: 'DATA_SOURCE_VALIDATE', operator: '平台管理员', objectType: 'data_source', objectName: 'prom-prod', result: 'success', requestId: 'req-20260428-001', createdAt: '2026-04-28 09:50:00' },
  { id: 'ae-002', eventType: 'LOG_SEARCH', operator: '陈然', objectType: 'log', objectName: 'trace-a1', result: 'success', requestId: 'req-20260428-002', createdAt: '2026-04-28 09:42:00' },
  { id: 'ae-003', eventType: 'SENSITIVE_VIEW', operator: '林知', objectType: 'log', objectName: 'trace-7fe2', result: 'denied', requestId: 'req-20260428-003', createdAt: '2026-04-28 09:12:00' },
];

export const mockDataSources: DataSource[] = [
  { id: 'ds-prom-prod', name: 'prom-prod', type: 'prometheus', environment: 'prod', baseUrl: 'https://prometheus.prod.example.com', healthCheckPath: '/-/healthy', authType: 'token', status: 'enabled', relatedApplications: 18, timeoutSeconds: 8, retryCount: 2, lastCheckAt: '2026-04-28 09:56:30', lastSuccessAt: '2026-04-28 09:56:29' },
  { id: 'ds-elk-prod', name: 'elk-prod', type: 'elk', environment: 'prod', baseUrl: 'https://elk.prod.example.com', healthCheckPath: '/_cluster/health', authType: 'basic', status: 'enabled', relatedApplications: 16, timeoutSeconds: 10, retryCount: 1, lastCheckAt: '2026-04-28 09:55:20', lastSuccessAt: '2026-04-28 09:55:18' },
  { id: 'ds-sky-prod', name: 'skywalking-prod', type: 'skywalking', environment: 'prod', baseUrl: 'https://skywalking.prod.example.com', healthCheckPath: '/graphql', authType: 'none', status: 'unhealthy', relatedApplications: 6, timeoutSeconds: 10, retryCount: 1, lastCheckAt: '2026-04-28 09:54:11', lastErrorCode: 'CONNECT_TIMEOUT', lastErrorMessage: '连接超时，最近一次成功在 08:31:12' },
  { id: 'ds-kafka-prod', name: 'kafka-exporter-prod', type: 'kafka', environment: 'prod', baseUrl: 'https://kafka-exporter.prod.example.com', healthCheckPath: '/metrics', authType: 'aksk', status: 'verifying', relatedApplications: 5, timeoutSeconds: 6, retryCount: 2, lastCheckAt: '2026-04-28 09:58:10' },
];

export const mockDataSourceValidation: DataSourceValidation = {
  id: 'val-001',
  sourceId: 'ds-prom-prod',
  status: 'passed',
  durationMs: 824,
  checkedAt: '2026-04-28 09:59:00',
  checks: [
    { name: '基础配置', status: 'passed', message: 'URL、超时和鉴权配置完整' },
    { name: '连通性', status: 'passed', message: '健康检查返回 200' },
    { name: '字段映射', status: 'passed', message: '应用、实例和环境标签可解析' },
    { name: '样本数据', status: 'passed', message: '最近 5 分钟存在采样点' },
  ],
};

export const mockApplicationAccess: ApplicationAccess[] = [
  { id: 'access-ace', appId: 'app-ace', appName: 'ACE 交易中心', appCode: 'ace-trade', environment: 'prod', owner: '陈然', metricsAccess: 'Prometheus/Grafana', traceAccess: 'SkyWalking', logsAccess: 'ELK', healthCheck: '正常', agentStatus: 'normal', accessStatus: 'connected', lastVerifiedAt: '2026-04-28 09:56:30' },
  { id: 'access-ipro', appId: 'app-ipro', appName: 'iPro 客户平台', appCode: 'ipro-customer', environment: 'pre', owner: '林知', metricsAccess: 'Prometheus', traceAccess: '未接入', logsAccess: 'Loki', healthCheck: '验证中', agentStatus: 'version_low', accessStatus: 'partial_connected', lastVerifiedAt: '2026-04-28 09:42:08' },
  { id: 'access-cms', appId: 'app-cms', appName: 'CMS 内容服务', appCode: 'cms-content', environment: 'test', owner: '许宁', metricsAccess: '异常', traceAccess: '未接入', logsAccess: '未接入', healthCheck: '失败', agentStatus: 'no_heartbeat', accessStatus: 'collector_error', lastVerifiedAt: '2026-04-28 09:12:14' },
];

export const mockAgents: AgentInstance[] = [
  { id: 'agent-001', hostname: 'prod-ace-01', ip: '10.12.8.21', environment: 'prod', status: 'normal', version: '2.4.1', baselineVersion: '2.4.0', collectProfile: 'java-app', cpuUsage: 3, memoryUsage: 128, lastHeartbeatAt: '2026-04-28 09:59:10' },
  { id: 'agent-002', hostname: 'pre-ipro-02', ip: '10.13.4.18', environment: 'pre', status: 'version_low', version: '2.2.9', baselineVersion: '2.4.0', collectProfile: 'java-app', cpuUsage: 5, memoryUsage: 151, lastHeartbeatAt: '2026-04-28 09:58:39', failureReason: '低于基线版本，部分采集项不可用' },
  { id: 'agent-003', hostname: 'test-cms-03', ip: '10.14.2.9', environment: 'test', status: 'no_heartbeat', version: '2.4.0', baselineVersion: '2.4.0', collectProfile: 'node-app', cpuUsage: 0, memoryUsage: 0, lastHeartbeatAt: '2026-04-28 08:17:05', failureReason: '超过 30 分钟无心跳' },
];

export const mockMetricDefinitions: MetricDefinition[] = [
  { code: 'error_rate', name: '错误率', unit: '%', objectType: 'application', sourceType: 'prometheus', promqlTemplate: 'sum(rate(http_requests_total{status=~"5.."}[5m])) / sum(rate(http_requests_total[5m])) * 100', defaultEnabled: true },
  { code: 'qps', name: 'QPS', unit: 'req/s', objectType: 'application', sourceType: 'prometheus', promqlTemplate: 'sum(rate(http_requests_total[1m])) by (app)', defaultEnabled: true },
  { code: 'p95_latency', name: 'P95 响应时间', unit: 'ms', objectType: 'application', sourceType: 'prometheus', promqlTemplate: 'histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le, app)) * 1000', defaultEnabled: true },
  { code: 'cpu_usage', name: 'CPU 使用率', unit: '%', objectType: 'server', sourceType: 'agent', promqlTemplate: '100 - avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100', defaultEnabled: true },
  { code: 'kafka_lag', name: 'Kafka 消费积压', unit: '条', objectType: 'kafka', sourceType: 'kafka', promqlTemplate: 'sum(kafka_consumergroup_lag) by (topic, consumergroup)', defaultEnabled: true },
  { code: 'pg_connections', name: 'PostgreSQL 连接数', unit: '个', objectType: 'postgresql', sourceType: 'postgresql', promqlTemplate: 'sum(pg_stat_activity_count) by (instance)', defaultEnabled: true },
];

export const mockMetricQueryResult: MetricQueryResult = {
  objectType: 'application',
  objectId: 'app-ace',
  objectName: 'ACE 交易中心',
  environment: 'prod',
  timeRange: '最近 30 分钟',
  granularity: '1m',
  dataQuality: { sourceName: 'prom-prod', status: 'connected', lastSuccessAt: '2026-04-28 09:59:00', missingPointRate: 0.02 },
  series: [
    { metricCode: 'error_rate', metricName: '错误率', unit: '%', threshold: 5, points: [{ time: '09:30', value: 1.2 }, { time: '09:35', value: 1.8 }, { time: '09:40', value: 3.9, logHit: true }, { time: '09:45', value: 6.2, alert: true }, { time: '09:50', value: 4.4 }, { time: '09:55', value: 2.1 }] },
    { metricCode: 'qps', metricName: 'QPS', unit: 'req/s', points: [{ time: '09:30', value: 820 }, { time: '09:35', value: 960 }, { time: '09:40', value: 1140 }, { time: '09:45', value: 1210, alert: true }, { time: '09:50', value: 1018 }, { time: '09:55', value: 936 }] },
    { metricCode: 'p95_latency', metricName: 'P95 响应时间', unit: 'ms', threshold: 1500, points: [{ time: '09:30', value: 420 }, { time: '09:35', value: 580 }, { time: '09:40', value: 980, logHit: true }, { time: '09:45', value: 2310, alert: true }, { time: '09:50', value: 1620 }, { time: '09:55', value: 870 }] },
  ],
};

export const mockLogSearchResult: LogSearchResult = {
  sourceStatus: 'connected',
  sourceName: 'elk-prod',
  lastSuccessAt: '2026-04-28 09:58:50',
  aggregations: [
    { signature: 'NullPointerException at PaymentService.pay', exceptionType: 'NullPointerException', application: 'ACE 交易中心', interfacePath: '/api/pay', instanceCount: 3, logCount: 142, traceCount: 39, firstSeenAt: '2026-04-28 09:37:11', lastSeenAt: '2026-04-28 09:56:01' },
    { signature: 'TimeoutException from risk-gateway', exceptionType: 'TimeoutException', application: 'ACE 交易中心', interfacePath: '/api/risk/check', instanceCount: 2, logCount: 46, traceCount: 18, firstSeenAt: '2026-04-28 09:42:20', lastSeenAt: '2026-04-28 09:55:19' },
  ],
  entries: [
    { id: 'log-001', timestamp: '2026-04-28 09:56:01', level: 'ERROR', application: 'ACE 交易中心', environment: 'prod', instanceIp: '10.12.8.21', traceId: 'trace-a1', thread: 'http-nio-8080-42', interfacePath: '/api/pay', summary: 'NullPointerException: paymentMethod is null, user=13****89 token=***', masked: true },
    { id: 'log-002', timestamp: '2026-04-28 09:55:19', level: 'ERROR', application: 'ACE 交易中心', environment: 'prod', instanceIp: '10.12.8.22', traceId: 'trace-b8', thread: 'http-nio-8080-18', interfacePath: '/api/risk/check', summary: 'TimeoutException: risk-gateway request timeout after 3000ms', masked: true },
    { id: 'log-003', timestamp: '2026-04-28 09:54:34', level: 'WARN', application: 'ACE 交易中心', environment: 'prod', instanceIp: '10.12.8.21', traceId: 'trace-a1', thread: 'pool-7', interfacePath: '/api/pay', summary: 'Retrying PostgreSQL transaction, sql=select *** from order_payment', masked: true },
  ],
};

export function toPage<T>(items: T[], page = 1, pageSize = 20): PageResult<T> {
  return {
    items,
    total: items.length,
    page,
    pageSize,
  };
}
