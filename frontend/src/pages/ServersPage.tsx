import { Input, Progress, Select } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { assetsApi } from '../api/services';
import { DataTable } from '../components/DataTable';
import { FilterBar } from '../components/FilterBar';
import { PageHeader } from '../components/PageHeader';
import { StatusTag } from '../components/StatusTag';
import type { Server } from '../types/domain';

const columns: ColumnsType<Server> = [
  { title: '主机名', dataIndex: 'hostname' },
  { title: 'IP', dataIndex: 'ip' },
  { title: '环境', dataIndex: 'environment', render: (value) => String(value).toUpperCase() },
  { title: '健康', dataIndex: 'status', render: (value) => <StatusTag value={value} /> },
  { title: 'Agent', dataIndex: 'agentStatus', render: (value) => <StatusTag value={value} /> },
  { title: 'CPU', dataIndex: 'cpuUsage', render: (value: number) => <Progress percent={value} size="small" /> },
  { title: '内存', dataIndex: 'memoryUsage', render: (value: number) => <Progress percent={value} size="small" /> },
  { title: '部署应用', dataIndex: 'applicationCount' },
  { title: '更新时间', dataIndex: 'updatedAt' },
];

export function ServersPage() {
  const [params, setParams] = useSearchParams();
  const query = Object.fromEntries(params.entries());
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ['servers', query], queryFn: () => assetsApi.servers(query) });

  return (
    <>
      <PageHeader title="服务器列表" description="主机资源、Agent 状态与部署应用摘要。" breadcrumb={['服务器']} />
      <FilterBar onSearch={() => setParams(query)} onReset={() => setParams({})}>
        <Input placeholder="主机名 / IP" defaultValue={params.get('keyword') ?? ''} onChange={(event) => query.keyword = event.target.value} />
        <Select
          placeholder="Agent 状态"
          allowClear
          defaultValue={params.get('agentStatus') ?? undefined}
          className="filter-select"
          onChange={(value) => query.agentStatus = value}
          options={[
            { label: '在线', value: 'online' },
            { label: '离线', value: 'offline' },
            { label: '异常', value: 'abnormal' },
            { label: '未安装', value: 'uninstalled' },
          ]}
        />
      </FilterBar>
      <DataTable columns={columns} dataSource={data?.items} loading={isLoading} error={error} onRetry={() => void refetch()} pagination={{ total: data?.total }} />
    </>
  );
}
