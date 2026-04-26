import { Tag } from 'antd';
import type { AccessStatus, AgentStatus, AuditResult, HealthStatus, UserStatus } from '../types/domain';

type StatusValue = HealthStatus | AccessStatus | AgentStatus | UserStatus | AuditResult | string | number;

const statusMap: Record<string, { label: string; color: string }> = {
  healthy: { label: '健康', color: 'success' },
  warning: { label: '警告', color: 'warning' },
  critical: { label: '严重', color: 'error' },
  unknown: { label: '未知', color: 'default' },
  connected: { label: '已接入', color: 'success' },
  partial: { label: '部分接入', color: 'processing' },
  disconnected: { label: '未接入', color: 'default' },
  pending: { label: '接入中', color: 'warning' },
  online: { label: '在线', color: 'success' },
  offline: { label: '离线', color: 'default' },
  abnormal: { label: '异常', color: 'error' },
  uninstalled: { label: '未安装', color: 'default' },
  enabled: { label: '启用', color: 'success' },
  disabled: { label: '停用', color: 'default' },
  locked: { label: '锁定', color: 'error' },
  success: { label: '成功', color: 'success' },
  failed: { label: '失败', color: 'error' },
  denied: { label: '拒绝', color: 'warning' },
};

export function StatusTag({ value }: { value: StatusValue }) {
  const meta = statusMap[String(value)] ?? { label: String(value), color: 'default' };
  return <Tag color={meta.color}>{meta.label}</Tag>;
}
