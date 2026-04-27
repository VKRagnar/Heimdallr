import { Alert, Input, Progress, Select, Statistic } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { accessManagementApi } from '../api/services';
import { DataTable } from '../components/DataTable';
import { FilterBar } from '../components/FilterBar';
import { PageHeader } from '../components/PageHeader';
import { StatusTag } from '../components/StatusTag';
import type { AgentInstance } from '../types/domain';

export function AgentsPage() {
  const [query, setQuery] = useState<Record<string, string | undefined>>({});
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ['agents', query], queryFn: () => accessManagementApi.agents(query) });
  const items = data?.items ?? [];
  const columns: ColumnsType<AgentInstance> = [
    { title: '主机名', dataIndex: 'hostname' },
    { title: 'IP', dataIndex: 'ip', render: (value) => value ?? '-' },
    { title: '环境', dataIndex: 'environment', render: (value) => String(value).toUpperCase() },
    { title: '状态', dataIndex: 'status', render: (value) => <StatusTag value={value} /> },
    { title: '版本', render: (_, row) => `${row.version} / 基线 ${row.baselineVersion ?? '-'}` },
    { title: '采集配置', dataIndex: 'collectProfile', render: (_, row) => row.collectProfile ?? row.configVersion ?? '-' },
    { title: 'Agent CPU', dataIndex: 'cpuUsage', render: (value: number) => <Progress percent={value ?? 0} size="small" /> },
    { title: 'Agent 内存', dataIndex: 'memoryUsage', render: (value: number) => value === undefined ? '-' : `${value} MB` },
    { title: '最近心跳', dataIndex: 'lastHeartbeatAt', render: (value) => value ?? '-' },
    { title: '异常原因', dataIndex: 'failureReason', render: (value) => value ?? '-' },
  ];

  return (
    <>
      <PageHeader title="Agent 管理" description="查看主机 Agent 在线状态、版本基线、采集配置和最近心跳。" breadcrumb={['接入管理', 'Agent 管理']} />
      <FilterBar onSearch={() => void refetch()} onReset={() => setQuery({})}>
        <Input placeholder="主机名 / IP" value={query.keyword} onChange={(event) => setQuery((prev) => ({ ...prev, keyword: event.target.value }))} />
        <Select placeholder="环境" allowClear value={query.env} className="filter-select" onChange={(value) => setQuery((prev) => ({ ...prev, env: value }))} options={['prod', 'pre', 'test', 'dev'].map((env) => ({ label: env.toUpperCase(), value: env }))} />
        <Select placeholder="Agent 状态" allowClear value={query.agentStatus} className="filter-select" onChange={(value) => setQuery((prev) => ({ ...prev, agentStatus: value }))} options={[{ label: '正常', value: 'normal' }, { label: '无心跳', value: 'no_heartbeat' }, { label: '版本过低', value: 'version_low' }, { label: '配置异常', value: 'config_error' }, { label: '未安装', value: 'not_installed' }]} />
      </FilterBar>
      <div className="summary-strip">
        <Statistic title="全部 Agent" value={items.length} />
        <Statistic title="在线正常" value={items.filter((item) => ['normal', 'ONLINE'].includes(String(item.status))).length} />
        <Statistic title="无心跳" value={items.filter((item) => ['no_heartbeat', 'NO_HEARTBEAT'].includes(String(item.status))).length} />
        <Statistic title="版本风险" value={items.filter((item) => item.status === 'version_low').length} />
      </div>
      <Alert className="source-bar" type="info" showIcon message="Agent 心跳异常会进入接入异常，不直接触发业务指标告警；详情中保留采集版本和最后上报时间。" />
      <DataTable columns={columns} dataSource={items} loading={isLoading} error={error} onRetry={() => void refetch()} pagination={{ total: data?.total }} />
    </>
  );
}
