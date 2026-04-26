export type Environment = 'prod' | 'pre' | 'test' | 'dev';

export type HealthStatus = 'healthy' | 'warning' | 'critical' | 'unknown';
export type AccessStatus = 'connected' | 'partial' | 'disconnected' | 'pending';
export type AgentStatus = 'online' | 'offline' | 'abnormal' | 'uninstalled';
export type UserStatus = 'enabled' | 'disabled' | 'locked';
export type AuditResult = 'success' | 'failed' | 'denied';

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
