import { Input, Select, Space } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { Link, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { assetsApi } from '../api/services';
import { DataTable } from '../components/DataTable';
import { FilterBar } from '../components/FilterBar';
import { PageHeader } from '../components/PageHeader';
import { StatusTag } from '../components/StatusTag';
import type { Application } from '../types/domain';

const columns: ColumnsType<Application> = [
  { title: '应用', dataIndex: 'name', render: (_, row) => <Link to={`/applications/${row.id}`}>{row.name}</Link> },
  { title: '编码', dataIndex: 'code' },
  { title: '环境', dataIndex: 'environment', render: (value) => String(value).toUpperCase() },
  { title: '健康', dataIndex: 'status', render: (value) => <StatusTag value={value} /> },
  { title: '接入', dataIndex: 'accessStatus', render: (value) => <StatusTag value={value} /> },
  { title: '负责人', dataIndex: 'owner' },
  { title: '实例', dataIndex: 'instanceCount' },
  { title: '告警', dataIndex: 'alertCount' },
  { title: '更新时间', dataIndex: 'updatedAt' },
];

export function ApplicationsPage() {
  const [params, setParams] = useSearchParams();
  const query = Object.fromEntries(params.entries());
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ['applications', query], queryFn: () => assetsApi.applications(query) });

  return (
    <>
      <PageHeader title="应用列表" description="应用资产、健康状态、接入状态与授权范围闭环。" breadcrumb={['应用']} />
      <FilterBar onSearch={() => setParams(query)} onReset={() => setParams({})}>
        <Input placeholder="应用名称 / 编码" defaultValue={params.get('keyword') ?? ''} onChange={(event) => query.keyword = event.target.value} />
        <Select
          placeholder="环境"
          allowClear
          defaultValue={params.get('env') ?? undefined}
          className="filter-select"
          onChange={(value) => query.env = value}
          options={['prod', 'pre', 'test', 'dev'].map((env) => ({ label: env.toUpperCase(), value: env }))}
        />
        <Select
          placeholder="健康状态"
          allowClear
          defaultValue={params.get('status') ?? undefined}
          className="filter-select"
          onChange={(value) => query.status = value}
          options={[
            { label: '健康', value: 'healthy' },
            { label: '警告', value: 'warning' },
            { label: '严重', value: 'critical' },
          ]}
        />
      </FilterBar>
      <Space direction="vertical" size={16} className="page-stack">
        <DataTable columns={columns} dataSource={data?.items} loading={isLoading} error={error} onRetry={() => void refetch()} pagination={{ total: data?.total }} />
      </Space>
    </>
  );
}
