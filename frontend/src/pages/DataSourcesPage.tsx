import { Alert, Button, Card, Descriptions, Drawer, Input, Select, Space, Statistic, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { accessManagementApi } from '../api/services';
import { DataTable } from '../components/DataTable';
import { FilterBar } from '../components/FilterBar';
import { PageHeader } from '../components/PageHeader';
import { StatusTag } from '../components/StatusTag';
import type { DataSource, DataSourceValidation } from '../types/domain';

export function DataSourcesPage() {
  const [query, setQuery] = useState<Record<string, string | undefined>>({});
  const [validation, setValidation] = useState<DataSourceValidation>();
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ['data-sources', query], queryFn: () => accessManagementApi.dataSources(query) });
  const validate = useMutation({
    mutationFn: (sourceId: string) => accessManagementApi.validateDataSource(sourceId),
    onSuccess: (result) => {
      setValidation(result);
      message.success('验证完成');
    },
  });

  const columns: ColumnsType<DataSource> = [
    { title: '数据源名称', dataIndex: 'name', render: (value, row) => <Space direction="vertical" size={0}><Typography.Text strong>{value}</Typography.Text><Typography.Text type="secondary">{row.id}</Typography.Text></Space> },
    { title: '类型', dataIndex: 'type' },
    { title: '地址', dataIndex: 'baseUrl', ellipsis: true },
    { title: '环境', dataIndex: 'environment', render: (value) => String(value).toUpperCase() },
    { title: '关联应用', dataIndex: 'relatedApplications', render: (value) => value ?? '-' },
    { title: '状态', dataIndex: 'status', render: (value) => <StatusTag value={value} /> },
    { title: '最近检查', dataIndex: 'lastCheckAt', render: (value) => value ?? '-' },
    { title: '最近错误', dataIndex: 'lastErrorCode', render: (_, row) => row.lastErrorCode ? `${row.lastErrorCode}: ${row.lastErrorMessage}` : '-' },
    { title: '操作', fixed: 'right', render: (_, row) => <Button type="link" loading={validate.isPending && validate.variables === row.id} onClick={() => validate.mutate(row.id)}>测试连接</Button> },
  ];

  const sources = data?.items ?? [];
  const validationChecks = validation?.checks ?? validation?.items ?? [];

  return (
    <>
      <PageHeader title="数据源管理" description="维护 Prometheus、日志源、Agent、Kafka 和 PostgreSQL 等采集源，并验证连通性、鉴权与映射。" breadcrumb={['接入管理', '数据源管理']} extra={<Button type="primary">新增数据源</Button>} />
      <FilterBar onSearch={() => void refetch()} onReset={() => setQuery({})} extra={<Button onClick={() => void refetch()}>健康检查</Button>}>
        <Input placeholder="名称 / 地址" value={query.keyword} onChange={(event) => setQuery((prev) => ({ ...prev, keyword: event.target.value }))} />
        <Select placeholder="类型" allowClear value={query.sourceType} className="filter-select" onChange={(value) => setQuery((prev) => ({ ...prev, sourceType: value }))} options={['prometheus', 'grafana', 'skywalking', 'elk', 'loki', 'agent', 'kafka', 'postgresql'].map((value) => ({ label: value, value }))} />
        <Select placeholder="状态" allowClear value={query.status} className="filter-select" onChange={(value) => setQuery((prev) => ({ ...prev, status: value }))} options={[{ label: '正常', value: 'enabled' }, { label: '异常', value: 'unhealthy' }, { label: '停用', value: 'disabled' }, { label: '验证中', value: 'verifying' }]} />
      </FilterBar>
      <div className="summary-strip">
        <Statistic title="全部" value={sources.length} />
        <Statistic title="正常" value={sources.filter((item) => ['enabled', 'ENABLED'].includes(String(item.status))).length} />
        <Statistic title="异常" value={sources.filter((item) => ['unhealthy', 'UNHEALTHY'].includes(String(item.status))).length} />
        <Statistic title="验证中" value={sources.filter((item) => item.status === 'verifying').length} />
      </div>
      <Alert className="source-bar" type="info" showIcon message="密钥只保存引用，测试结果不会返回 Token、Cookie、连接串等明文；配置变更和测试连接会写入审计。" />
      <DataTable columns={columns} dataSource={sources} loading={isLoading} error={error} onRetry={() => void refetch()} pagination={{ total: data?.total }} />
      <Drawer width={640} title="数据源验证结果" open={Boolean(validation)} onClose={() => setValidation(undefined)}>
        {validation ? (
          <Space direction="vertical" size={16} className="page-stack">
            <Descriptions column={2}>
              <Descriptions.Item label="状态"><StatusTag value={validation.status} /></Descriptions.Item>
              <Descriptions.Item label="耗时">{validation.durationMs === undefined ? '-' : `${validation.durationMs}ms`}</Descriptions.Item>
              <Descriptions.Item label="检查时间">{validation.checkedAt}</Descriptions.Item>
              <Descriptions.Item label="数据源">{validation.sourceId}</Descriptions.Item>
            </Descriptions>
            <Table rowKey="name" pagination={false} dataSource={validationChecks} columns={[{ title: '检查项', dataIndex: 'name' }, { title: '结果', dataIndex: 'status', render: (value) => <StatusTag value={value} /> }, { title: '说明', dataIndex: 'message' }]} />
            <Card size="small" title="审计提示">本次测试连接会记录操作人、操作时间、数据源和验证结果，可在系统管理 / 审计中查询。</Card>
          </Space>
        ) : null}
      </Drawer>
    </>
  );
}
