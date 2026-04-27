import { Alert, Button, Input, Select, Space, Statistic, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { accessManagementApi } from '../api/services';
import { DataTable } from '../components/DataTable';
import { FilterBar } from '../components/FilterBar';
import { PageHeader } from '../components/PageHeader';
import { StatusTag } from '../components/StatusTag';
import type { ApplicationAccess } from '../types/domain';

export function ApplicationAccessPage() {
  const [query, setQuery] = useState<Record<string, string | undefined>>({});
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ['application-access', query], queryFn: () => accessManagementApi.applicationAccess(query) });
  const items = data?.items ?? [];

  const columns: ColumnsType<ApplicationAccess> = [
    { title: '应用', dataIndex: 'appName', render: (value, row) => <Space direction="vertical" size={0}><Link to={`/applications/${row.appId}`}>{value}</Link><Typography.Text type="secondary">{row.appCode}</Typography.Text></Space> },
    { title: '环境', dataIndex: 'environment', render: (value) => String(value).toUpperCase() },
    { title: '负责人', dataIndex: 'owner' },
    { title: '指标接入', dataIndex: 'metricsAccess' },
    { title: '链路接入', dataIndex: 'traceAccess' },
    { title: '日志接入', dataIndex: 'logsAccess' },
    { title: '健康检查', dataIndex: 'healthCheck' },
    { title: 'Agent', dataIndex: 'agentStatus', render: (value) => <StatusTag value={value} /> },
    { title: '状态', dataIndex: 'accessStatus', render: (value) => <StatusTag value={value} /> },
    { title: '最近验证', dataIndex: 'lastVerifiedAt' },
    { title: '操作', fixed: 'right', render: (_, row) => <Space><Link to={`/logs/search?appId=${row.appId}&env=${row.environment}&level=ERROR`}>日志</Link><Link to={`/metrics?objectType=application&objectId=${row.appId}&env=${row.environment}&metricCodes=error_rate,qps,p95_latency`}>指标</Link><Button type="link">验证</Button></Space> },
  ];

  return (
    <>
      <PageHeader title="应用接入" description="展示应用的指标、日志、链路、健康检查和 Agent 接入状态，区分未接入、采集异常和业务异常。" breadcrumb={['接入管理', '应用接入']} extra={<Button type="primary">新增接入</Button>} />
      <FilterBar onSearch={() => void refetch()} onReset={() => setQuery({})} extra={<Button>导入应用</Button>}>
        <Input placeholder="应用名称 / 编码" value={query.keyword} onChange={(event) => setQuery((prev) => ({ ...prev, keyword: event.target.value }))} />
        <Select placeholder="环境" allowClear value={query.env} className="filter-select" onChange={(value) => setQuery((prev) => ({ ...prev, env: value }))} options={['prod', 'pre', 'test', 'dev'].map((env) => ({ label: env.toUpperCase(), value: env }))} />
        <Select placeholder="接入状态" allowClear value={query.accessStatus} className="filter-select" onChange={(value) => setQuery((prev) => ({ ...prev, accessStatus: value }))} options={[{ label: '已接入', value: 'connected' }, { label: '部分接入', value: 'partial_connected' }, { label: '未接入', value: 'not_connected' }, { label: '采集异常', value: 'collector_error' }]} />
      </FilterBar>
      <div className="summary-strip">
        <Statistic title="全部" value={items.length} />
        <Statistic title="指标已接入" value={items.filter((item) => item.metricsAccess !== '异常' && item.metricsAccess !== '未接入').length} />
        <Statistic title="仅日志/仅指标" value={items.filter((item) => item.accessStatus === 'partial_connected').length} />
        <Statistic title="采集异常" value={items.filter((item) => item.accessStatus === 'collector_error').length} />
      </div>
      <Alert className="source-bar" type="warning" showIcon message="采集异常不等同于业务健康异常；列表会保留最近成功采集时间，避免把未接入对象误判为正常。" />
      <DataTable columns={columns} dataSource={items} loading={isLoading} error={error} onRetry={() => void refetch()} pagination={{ total: data?.total }} />
    </>
  );
}
