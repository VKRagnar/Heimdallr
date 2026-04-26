import { Input, Select, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { systemApi } from '../api/services';
import { DataTable } from '../components/DataTable';
import { FilterBar } from '../components/FilterBar';
import { PageHeader } from '../components/PageHeader';
import { StatusTag } from '../components/StatusTag';
import type { SystemUser } from '../types/domain';

const columns: ColumnsType<SystemUser> = [
  { title: '姓名', dataIndex: 'name' },
  { title: '账号', dataIndex: 'username' },
  { title: '邮箱', dataIndex: 'email' },
  { title: '状态', dataIndex: 'status', render: (value) => <StatusTag value={value} /> },
  { title: '角色', dataIndex: 'roles', render: (roles: string[]) => roles.map((role) => <Tag key={role}>{role}</Tag>) },
  { title: '最后登录', dataIndex: 'lastLoginAt', render: (value) => value ?? '-' },
];

export function SystemUsersPage() {
  const [params, setParams] = useSearchParams();
  const query = Object.fromEntries(params.entries());
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ['system-users', query], queryFn: () => systemApi.users(query) });

  return (
    <>
      <PageHeader title="系统用户" description="用户状态、角色关系与权限闭环入口。" breadcrumb={['系统管理', '系统用户']} />
      <FilterBar onSearch={() => setParams(query)} onReset={() => setParams({})}>
        <Input placeholder="姓名 / 账号 / 邮箱" defaultValue={params.get('keyword') ?? ''} onChange={(event) => query.keyword = event.target.value} />
        <Select
          placeholder="状态"
          allowClear
          defaultValue={params.get('status') ?? undefined}
          className="filter-select"
          onChange={(value) => query.status = value}
          options={[
            { label: '启用', value: 'enabled' },
            { label: '停用', value: 'disabled' },
            { label: '锁定', value: 'locked' },
          ]}
        />
      </FilterBar>
      <DataTable columns={columns} dataSource={data?.items} loading={isLoading} error={error} onRetry={() => void refetch()} pagination={{ total: data?.total }} />
    </>
  );
}
