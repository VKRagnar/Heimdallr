import { Button, Form, Input, Modal, Select, Space, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { Link, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { EditOutlined, PlusOutlined } from '@ant-design/icons';
import { useState } from 'react';
import { assetsApi } from '../api/services';
import { DataTable } from '../components/DataTable';
import { FilterBar } from '../components/FilterBar';
import { PageHeader } from '../components/PageHeader';
import { StatusTag } from '../components/StatusTag';
import type { Application, ApplicationPayload } from '../types/domain';

const environmentOptions = ['prod', 'pre', 'test', 'dev'].map((env) => ({ label: env.toUpperCase(), value: env }));
const accessOptions = [
  { label: '已接入', value: 'connected' },
  { label: '部分接入', value: 'partial_connected' },
  { label: '未接入', value: 'not_connected' },
  { label: '采集异常', value: 'collector_error' },
];

interface ApplicationFormValues extends Omit<ApplicationPayload, 'ownerUserIds'> {
  ownerUserIds: string;
}

const { TextArea } = Input;

function splitValues(value: string) {
  return value.split(',').map((item) => item.trim()).filter(Boolean);
}

function toPayload(values: ApplicationFormValues): ApplicationPayload {
  return { ...values, ownerUserIds: splitValues(values.ownerUserIds) };
}

export function ApplicationsPage() {
  const [params, setParams] = useSearchParams();
  const [editing, setEditing] = useState<Application | null>(null);
  const [open, setOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const [form] = Form.useForm<ApplicationFormValues>();
  const [importForm] = Form.useForm<{ content: string }>();
  const queryClient = useQueryClient();
  const query = Object.fromEntries(params.entries());
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ['applications', query], queryFn: () => assetsApi.applications(query) });

  const saveApplication = useMutation({
    mutationFn: (payload: ApplicationPayload) => editing ? assetsApi.updateApplication(editing.id, payload) : assetsApi.createApplication(payload),
    onSuccess: async () => {
      message.success(editing ? '应用已更新' : '应用已新增');
      setOpen(false);
      setEditing(null);
      form.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['applications'] });
      await queryClient.invalidateQueries({ queryKey: ['audit-events'] });
    },
  });

  const importApplications = useMutation({
    mutationFn: (payload: ApplicationPayload[]) => assetsApi.importApplications(payload),
    onSuccess: async (result) => {
      message.success(`已导入 ${result.total} 个应用`);
      setImportOpen(false);
      importForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['applications'] });
      await queryClient.invalidateQueries({ queryKey: ['audit-events'] });
    },
  });

  const showEditor = (row?: Application) => {
    setEditing(row ?? null);
    form.setFieldsValue(row ? {
      id: row.id,
      name: row.name,
      code: row.code,
      environment: row.environment,
      businessLine: 'default',
      ownerUserIds: row.owner === '-' ? '' : row.owner,
      accessStatus: row.accessStatus,
    } : { environment: 'prod', businessLine: 'default', ownerUserIds: 'u-admin', accessStatus: 'not_connected' });
    setOpen(true);
  };

  const submitImport = (content: string) => {
    try {
      importApplications.mutate(JSON.parse(content) as ApplicationPayload[]);
    } catch {
      message.error('导入内容不是合法 JSON');
    }
  };

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
    { title: '操作', fixed: 'right', render: (_, row) => <Button type="link" icon={<EditOutlined />} onClick={() => showEditor(row)}>维护</Button> },
  ];

  return (
    <>
      <PageHeader
        title="应用列表"
        description="应用资产、健康状态、接入状态与授权范围闭环。"
        breadcrumb={['应用']}
        extra={<Space><Button onClick={() => setImportOpen(true)}>导入应用</Button><Button type="primary" icon={<PlusOutlined />} onClick={() => showEditor()}>新增应用</Button></Space>}
      />
      <FilterBar onSearch={() => setParams(query)} onReset={() => setParams({})}>
        <Input placeholder="应用名称 / 编码" defaultValue={params.get('keyword') ?? ''} onChange={(event) => query.keyword = event.target.value} />
        <Select placeholder="环境" allowClear defaultValue={params.get('env') ?? undefined} className="filter-select" onChange={(value) => query.env = value} options={environmentOptions} />
        <Select placeholder="接入状态" allowClear defaultValue={params.get('accessStatus') ?? undefined} className="filter-select" onChange={(value) => query.accessStatus = value} options={accessOptions} />
      </FilterBar>
      <Space direction="vertical" size={16} className="page-stack">
        <DataTable columns={columns} dataSource={data?.items} loading={isLoading} error={error} onRetry={() => void refetch()} pagination={{ total: data?.total }} />
      </Space>
      <Modal
        title={editing ? '维护应用' : '新增应用'}
        open={open}
        okText="保存"
        cancelText="取消"
        confirmLoading={saveApplication.isPending}
        onCancel={() => setOpen(false)}
        onOk={() => void form.validateFields().then((values) => saveApplication.mutate(toPayload(values)))}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="id" label="应用 ID"><Input disabled={Boolean(editing)} placeholder="留空由后端生成" /></Form.Item>
          <Form.Item name="name" label="应用名称" rules={[{ required: true, message: '请输入应用名称' }]}><Input /></Form.Item>
          <Form.Item name="code" label="应用编码" rules={[{ required: true, message: '请输入应用编码' }]}><Input disabled={Boolean(editing)} /></Form.Item>
          <Form.Item name="businessLine" label="业务线" rules={[{ required: true, message: '请输入业务线' }]}><Input /></Form.Item>
          <Form.Item name="environment" label="环境" rules={[{ required: true, message: '请选择环境' }]}><Select options={environmentOptions} /></Form.Item>
          <Form.Item name="ownerUserIds" label="负责人用户 ID" rules={[{ required: true, message: '请输入负责人用户 ID' }]}><Input placeholder="多个用英文逗号分隔，如 u-admin,u-sre" /></Form.Item>
          <Form.Item name="accessStatus" label="接入状态" rules={[{ required: true, message: '请选择接入状态' }]}><Select options={accessOptions} /></Form.Item>
        </Form>
      </Modal>
      <Modal
        title="导入应用"
        open={importOpen}
        okText="导入"
        cancelText="取消"
        confirmLoading={importApplications.isPending}
        onCancel={() => setImportOpen(false)}
        onOk={() => void importForm.validateFields().then(({ content }) => submitImport(content))}
      >
        <Form form={importForm} layout="vertical" preserve={false}>
          <Form.Item name="content" label="应用 JSON 数组" rules={[{ required: true, message: '请输入应用 JSON 数组' }]}>
            <TextArea rows={8} placeholder='[{"id":"app-demo","code":"DEMO","name":"Demo","businessLine":"core","environment":"prod","ownerUserIds":["u-admin"],"accessStatus":"connected"}]' />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}
