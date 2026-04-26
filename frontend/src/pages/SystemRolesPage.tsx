import { Input, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { systemApi } from '../api/services';
import { DataTable } from '../components/DataTable';
import { FilterBar } from '../components/FilterBar';
import { PageHeader } from '../components/PageHeader';
import type { Role } from '../types/domain';

const columns: ColumnsType<Role> = [
  { title: '角色名称', dataIndex: 'name' },
  { title: '编码', dataIndex: 'code' },
  { title: '说明', dataIndex: 'description' },
  { title: '用户数', dataIndex: 'userCount' },
  { title: '权限', dataIndex: 'permissions', render: (items: string[]) => items.slice(0, 4).map((item) => <Tag key={item}>{item}</Tag>) },
];

export function SystemRolesPage() {
  const [params, setParams] = useSearchParams();
  const query = Object.fromEntries(params.entries());
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ['system-roles', query], queryFn: () => systemApi.roles(query) });

  return (
    <>
      <PageHeader title="角色" description="角色、菜单权限和操作权限的基础模型。" breadcrumb={['系统管理', '角色']} />
      <FilterBar onSearch={() => setParams(query)} onReset={() => setParams({})}>
        <Input placeholder="角色名称 / 编码" defaultValue={params.get('keyword') ?? ''} onChange={(event) => query.keyword = event.target.value} />
      </FilterBar>
      <DataTable columns={columns} dataSource={data?.items} loading={isLoading} error={error} onRetry={() => void refetch()} pagination={{ total: data?.total }} />
    </>
  );
}
