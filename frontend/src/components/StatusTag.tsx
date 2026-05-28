import { Tag } from 'antd';
import type { AccessStatus, AgentStatus, AuditResult, DataSourceStatus, HealthStatus, UserStatus } from '../types/domain';

type StatusValue = HealthStatus | AccessStatus | AgentStatus | UserStatus | AuditResult | DataSourceStatus | string | number | boolean;

const statusMap: Record<string, { label: string; color: string }> = {
  healthy: { label: '健康', color: 'success' },
  warning: { label: '预警', color: 'warning' },
  critical: { label: '严重', color: 'error' },
  unknown: { label: '未知', color: 'default' },
  connected: { label: '已接入', color: 'success' },
  CONNECTED: { label: '已接入', color: 'success' },
  partial: { label: '部分接入', color: 'processing' },
  partial_connected: { label: '部分接入', color: 'processing' },
  PARTIAL_CONNECTED: { label: '部分接入', color: 'processing' },
  disconnected: { label: '未接入', color: 'default' },
  not_connected: { label: '未接入', color: 'default' },
  pending: { label: '接入中', color: 'warning' },
  collector_error: { label: '采集异常', color: 'error' },
  source_unavailable: { label: '数据源异常', color: 'error' },
  SOURCE_UNAVAILABLE: { label: '数据源异常', color: 'error' },
  no_recent_data: { label: '无近期数据', color: 'warning' },
  NO_RECENT_DATA: { label: '无近期数据', color: 'warning' },
  mapping_invalid: { label: '映射错误', color: 'error' },
  online: { label: '在线', color: 'success' },
  ONLINE: { label: '在线', color: 'success' },
  offline: { label: '离线', color: 'default' },
  abnormal: { label: '异常', color: 'error' },
  uninstalled: { label: '未安装', color: 'default' },
  normal: { label: '正常', color: 'success' },
  no_heartbeat: { label: '无心跳', color: 'error' },
  NO_HEARTBEAT: { label: '无心跳', color: 'error' },
  version_low: { label: '版本过低', color: 'warning' },
  config_error: { label: '配置异常', color: 'error' },
  CONFIG_ERROR: { label: '配置异常', color: 'error' },
  not_installed: { label: '未安装', color: 'default' },
  unhealthy: { label: '异常', color: 'error' },
  UNHEALTHY: { label: '异常', color: 'error' },
  verifying: { label: '验证中', color: 'processing' },
  enabled: { label: '启用', color: 'success' },
  ENABLED: { label: '启用', color: 'success' },
  disabled: { label: '停用', color: 'default' },
  DISABLED: { label: '停用', color: 'default' },
  enabled_user: { label: '启用', color: 'success' },
  locked: { label: '锁定', color: 'error' },
  success: { label: '成功', color: 'success' },
  SUCCESS: { label: '成功', color: 'success' },
  failed: { label: '失败', color: 'error' },
  FAILED: { label: '失败', color: 'error' },
  denied: { label: '拒绝', color: 'warning' },
  passed: { label: '通过', color: 'success' },
  PASSED: { label: '通过', color: 'success' },
  running: { label: '运行中', color: 'processing' },
  TRIGGERED: { label: 'Triggered', color: 'error' },
  NOTIFIED: { label: 'Notified', color: 'processing' },
  NOTIFICATION_FAILED: { label: 'Notify failed', color: 'error' },
  ACKNOWLEDGED: { label: 'Acknowledged', color: 'warning' },
  PROCESSING: { label: 'Processing', color: 'processing' },
  RECOVERED: { label: 'Recovered', color: 'success' },
  CLOSED: { label: 'Closed', color: 'default' },
  SENT: { label: 'Sent', color: 'success' },
};

export function StatusTag({ value }: { value: StatusValue }) {
  const key = String(value);
  const meta = statusMap[key] ?? { label: key, color: 'default' };
  return <Tag color={meta.color}>{meta.label}</Tag>;
}
