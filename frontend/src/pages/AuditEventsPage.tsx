import { Input, Select } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { systemApi } from '../api/services';
import { DataTable } from '../components/DataTable';
import { FilterBar } from '../components/FilterBar';
import { PageHeader } from '../components/PageHeader';
import { StatusTag } from '../components/StatusTag';
import type { AuditEvent } from '../types/domain';

const columns: ColumnsType<AuditEvent> = [
  { title: '事件类型', dataIndex: 'eventType' },
  { title: '操作人', dataIndex: 'operator' },
  { title: '对象类型', dataIndex: 'objectType' },
  { title: '对象', dataIndex: 'objectName' },
  { title: '结果', dataIndex: 'result', render: (value) => <StatusTag value={value} /> },
  { title: '请求 ID', dataIndex: 'requestId' },
  { title: '时间', dataIndex: 'createdAt' },
];

export function AuditEventsPage() {
  const [params, setParams] = useSearchParams();
  const query = Object.fromEntries(params.entries());
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ['audit-events', query], queryFn: () => systemApi.auditEvents(query) });

  return (
    <>
      <PageHeader title="审计" description="权限、敏感数据和关键配置操作的审计查询。" breadcrumb={['系统管理', '审计']} />
      <FilterBar onSearch={() => setParams(query)} onReset={() => setParams({})}>
        <Input placeholder="事件 / 操作人 / 对象" defaultValue={params.get('keyword') ?? ''} onChange={(event) => query.keyword = event.target.value} />
        <Select
          placeholder="结果"
          allowClear
          defaultValue={params.get('result') ?? undefined}
          className="filter-select"
          onChange={(value) => query.result = value}
          options={[
            { label: '成功', value: 'success' },
            { label: '失败', value: 'failed' },
            { label: '拒绝', value: 'denied' },
          ]}
        />
      </FilterBar>
      <DataTable columns={columns} dataSource={data?.items} loading={isLoading} error={error} onRetry={() => void refetch()} pagination={{ total: data?.total }} />
    </>
  );
}
