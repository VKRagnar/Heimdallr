import { Alert, Button, Input, Select, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { observabilityApi } from '../api/services';
import { DataTable } from '../components/DataTable';
import { FilterBar } from '../components/FilterBar';
import { PageHeader } from '../components/PageHeader';
import type { LogAggregation, LogEntry } from '../types/domain';

function toSearchParams(query: Record<string, string | undefined>) {
  return Object.fromEntries(Object.entries(query).filter((entry): entry is [string, string] => Boolean(entry[1])));
}

export function LogsSearchPage() {
  const [params, setParams] = useSearchParams();
  const [query, setQuery] = useState<Record<string, string | undefined>>(() => Object.fromEntries(params.entries()));
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ['logs-search', query], queryFn: () => observabilityApi.searchLogs(query) });
  const sourceHint = useMemo(() => params.get('alertId') ? `来源：告警 ${params.get('alertId')}，已带入应用、环境、时间窗口和日志级别。` : undefined, [params]);

  const aggregationColumns: ColumnsType<LogAggregation> = [
    { title: '错误签名', dataIndex: 'signature', ellipsis: true },
    { title: '异常类型', dataIndex: 'exceptionType' },
    { title: '接口', dataIndex: 'interfacePath' },
    { title: '实例数', dataIndex: 'instanceCount' },
    { title: '日志次数', dataIndex: 'logCount' },
    { title: 'Trace 数', dataIndex: 'traceCount' },
    { title: '最近出现', dataIndex: 'lastSeenAt' },
  ];

  const entryColumns: ColumnsType<LogEntry> = [
    { title: '时间', dataIndex: 'timestamp', width: 170 },
    { title: '级别', dataIndex: 'level', render: (value) => <Tag color={value === 'ERROR' ? 'red' : value === 'WARN' ? 'orange' : 'blue'}>{value}</Tag> },
    { title: '应用', dataIndex: 'application' },
    { title: '实例', dataIndex: 'instanceIp' },
    { title: 'Trace ID', dataIndex: 'traceId', render: (value) => value ? <Typography.Text copyable>{value}</Typography.Text> : '-' },
    { title: '接口', dataIndex: 'interfacePath' },
    { title: '摘要', dataIndex: 'summary', ellipsis: true },
    { title: '脱敏', dataIndex: 'masked', render: (value) => value ? <Tag>已脱敏</Tag> : '-' },
    { title: '操作', fixed: 'right', render: (_, row) => {
      const appId = row.applicationId ?? row.application;
      return <Space><Link to={`/applications/${appId}?instanceIp=${row.instanceIp}&traceId=${row.traceId ?? ''}`}>应用</Link><Link to={`/metrics?objectType=application&objectId=${appId}&env=${row.environment}&metricCodes=error_rate,qps,p95_latency&traceId=${row.traceId ?? ''}`}>指标</Link></Space>;
    } },
  ];

  return (
    <>
      <PageHeader title="日志查询" description="按应用、实例、级别、Trace ID 和关键字检索日志，支持错误聚合、上下文跳转和脱敏展示。" breadcrumb={['日志中心', '日志查询']} extra={<Button>导出</Button>} />
      <FilterBar onSearch={() => { setParams(toSearchParams(query)); void refetch(); }} onReset={() => { setQuery({}); setParams({}); }}>
        <Input placeholder="应用 ID / 名称" value={query.appId} onChange={(event) => setQuery((prev) => ({ ...prev, appId: event.target.value }))} />
        <Select placeholder="环境" allowClear value={query.env} className="filter-select" onChange={(value) => setQuery((prev) => ({ ...prev, env: value }))} options={['prod', 'pre', 'staging', 'test', 'dev'].map((env) => ({ label: env.toUpperCase(), value: env }))} />
        <Select placeholder="级别" allowClear value={query.level} className="filter-select" onChange={(value) => setQuery((prev) => ({ ...prev, level: value }))} options={['ERROR', 'WARN', 'INFO', 'DEBUG'].map((level) => ({ label: level, value: level }))} />
        <Input placeholder="Trace ID / Request ID" value={query.traceId} onChange={(event) => setQuery((prev) => ({ ...prev, traceId: event.target.value }))} />
        <Input placeholder="关键字 / 错误签名" value={query.keyword} onChange={(event) => setQuery((prev) => ({ ...prev, keyword: event.target.value }))} />
      </FilterBar>
      {sourceHint ? <Alert className="source-bar" type="info" showIcon message={sourceHint} /> : null}
      {data ? <Alert className="source-bar" type={data.sourceStatus === 'connected' ? 'success' : 'warning'} showIcon message={`日志源 ${data.sourceName}: ${data.sourceStatus}，最近成功 ${data.lastSuccessAt}`} /> : null}
      <Space direction="vertical" size={16} className="page-stack">
        <Table title={() => '错误聚合'} rowKey="signature" size="middle" pagination={false} loading={isLoading} columns={aggregationColumns} dataSource={data?.aggregations} />
        <DataTable columns={entryColumns} dataSource={data?.entries} loading={isLoading} error={error} onRetry={() => void refetch()} pagination={{ total: data?.entries.length }} />
      </Space>
    </>
  );
}
