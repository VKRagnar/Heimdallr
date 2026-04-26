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

export const mockCurrentUser: CurrentUser = {
  id: 'u-001',
  name: '平台管理员',
  username: 'admin',
  email: 'admin@example.com',
  roles: ['平台管理员'],
  permissions: ['*'],
  menus: ['home', 'applications', 'servers', 'system.users', 'system.roles', 'system.audit-events'],
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
  {
    id: 'app-ace',
    name: 'ACE 交易中心',
    code: 'ace-trade',
    environment: 'prod',
    owner: '陈然',
    status: 'warning',
    accessStatus: 'connected',
    alertCount: 3,
    instanceCount: 12,
    updatedAt: '2026-04-26 21:10:00',
  },
  {
    id: 'app-ipro',
    name: 'iPro 客户平台',
    code: 'ipro-customer',
    environment: 'pre',
    owner: '林知',
    status: 'healthy',
    accessStatus: 'partial',
    alertCount: 0,
    instanceCount: 8,
    updatedAt: '2026-04-26 20:48:00',
  },
  {
    id: 'app-cms',
    name: 'CMS 内容服务',
    code: 'cms-content',
    environment: 'test',
    owner: '许宁',
    status: 'critical',
    accessStatus: 'pending',
    alertCount: 7,
    instanceCount: 5,
    updatedAt: '2026-04-26 19:32:00',
  },
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
    },
  ]),
);

export const mockServers: Server[] = [
  {
    id: 'srv-001',
    hostname: 'prod-ace-01',
    ip: '10.12.8.21',
    environment: 'prod',
    status: 'warning',
    agentStatus: 'online',
    cpuUsage: 68,
    memoryUsage: 74,
    applicationCount: 3,
    updatedAt: '2026-04-26 21:05:00',
  },
  {
    id: 'srv-002',
    hostname: 'pre-ipro-02',
    ip: '10.13.4.18',
    environment: 'pre',
    status: 'healthy',
    agentStatus: 'online',
    cpuUsage: 31,
    memoryUsage: 45,
    applicationCount: 2,
    updatedAt: '2026-04-26 20:58:00',
  },
  {
    id: 'srv-003',
    hostname: 'test-cms-03',
    ip: '10.14.2.9',
    environment: 'test',
    status: 'critical',
    agentStatus: 'abnormal',
    cpuUsage: 92,
    memoryUsage: 88,
    applicationCount: 1,
    updatedAt: '2026-04-26 20:12:00',
  },
];

export const mockUsers: SystemUser[] = [
  { id: 'u-001', name: '平台管理员', username: 'admin', email: 'admin@example.com', status: 'enabled', roles: ['平台管理员'], lastLoginAt: '2026-04-26 21:00:00' },
  { id: 'u-002', name: '陈然', username: 'chenran', email: 'chenran@example.com', status: 'enabled', roles: ['应用负责人'], lastLoginAt: '2026-04-26 18:22:00' },
  { id: 'u-003', name: '林知', username: 'linzhi', email: 'linzhi@example.com', status: 'locked', roles: ['只读用户'] },
];

export const mockRoles: Role[] = [
  { id: 'r-001', name: '平台管理员', code: 'platform_admin', description: '拥有平台配置、审计和授权能力。', userCount: 2, permissions: ['*'] },
  { id: 'r-002', name: '运维/SRE', code: 'sre', description: '负责资产接入、告警处理和观测数据查询。', userCount: 8, permissions: ['assets:read', 'alerts:write'] },
  { id: 'r-003', name: '应用负责人', code: 'app_owner', description: '查看和处理授权应用内的问题。', userCount: 24, permissions: ['apps:read', 'alerts:handle'] },
];

export const mockAuditEvents: AuditEvent[] = [
  { id: 'ae-001', eventType: 'APPLICATION_GRANT', operator: '平台管理员', objectType: 'application', objectName: 'ACE 交易中心', result: 'success', requestId: 'req-20260426-001', createdAt: '2026-04-26 20:50:00' },
  { id: 'ae-002', eventType: 'LOGIN', operator: '陈然', objectType: 'user', objectName: 'chenran', result: 'success', requestId: 'req-20260426-002', createdAt: '2026-04-26 18:22:00' },
  { id: 'ae-003', eventType: 'SENSITIVE_VIEW', operator: '林知', objectType: 'log', objectName: 'trace-7fe2', result: 'denied', requestId: 'req-20260426-003', createdAt: '2026-04-26 17:12:00' },
];

export function toPage<T>(items: T[], page = 1, pageSize = 20): PageResult<T> {
  return {
    items,
    total: items.length,
    page,
    pageSize,
  };
}
