import { Button, Form, Input, Modal, Progress, Select, Space, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { EditOutlined, PlusOutlined } from '@ant-design/icons';
import { useState } from 'react';
import { assetsApi } from '../api/services';
import { DataTable } from '../components/DataTable';
import { FilterBar } from '../components/FilterBar';
import { PageHeader } from '../components/PageHeader';
import { StatusTag } from '../components/StatusTag';
import type { Server, ServerPayload } from '../types/domain';

const environmentOptions = ['prod', 'pre', 'test', 'dev'].map((env) => ({ label: env.toUpperCase(), value: env }));
const agentOptions = [
  { label: '在线', value: 'online' },
  { label: '离线', value: 'offline' },
  { label: '异常', value: 'abnormal' },
  { label: '未安装', value: 'uninstalled' },
];
const accessOptions = [
  { label: '已接入', value: 'connected' },
  { label: '部分接入', value: 'partial_connected' },
  { label: '未接入', value: 'not_connected' },
  { label: '采集异常', value: 'collector_error' },
];

interface ServerFormValues extends Omit<ServerPayload, 'applicationIds'> {
  applicationIds: string;
}

const { TextArea } = Input;

function splitValues(value: string) {
  return value.split(',').map((item) => item.trim()).filter(Boolean);
}

function toPayload(values: ServerFormValues): ServerPayload {
  return { ...values, applicationIds: splitValues(values.applicationIds) };
}

export function ServersPage() {
  const [params, setParams] = useSearchParams();
  const [editing, setEditing] = useState<Server | null>(null);
  const [open, setOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const [form] = Form.useForm<ServerFormValues>();
  const [importForm] = Form.useForm<{ content: string }>();
  const queryClient = useQueryClient();
  const query = Object.fromEntries(params.entries());
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ['servers', query], queryFn: () => assetsApi.servers(query) });

  const saveServer = useMutation({
    mutationFn: (payload: ServerPayload) => editing ? assetsApi.updateServer(editing.id, payload) : assetsApi.createServer(payload),
    onSuccess: async () => {
      message.success(editing ? '服务器已更新' : '服务器已新增');
      setOpen(false);
      setEditing(null);
      form.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['servers'] });
      await queryClient.invalidateQueries({ queryKey: ['audit-events'] });
    },
  });

  const importServers = useMutation({
    mutationFn: (payload: ServerPayload[]) => assetsApi.importServers(payload),
    onSuccess: async (result) => {
      message.success(`已导入 ${result.total} 台服务器`);
      setImportOpen(false);
      importForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['servers'] });
      await queryClient.invalidateQueries({ queryKey: ['audit-events'] });
    },
  });

  const showEditor = (row?: Server) => {
    setEditing(row ?? null);
    form.setFieldsValue(row ? {
      id: row.id,
      hostname: row.hostname,
      ip: row.ip,
      environment: row.environment,
      applicationIds: '',
      accessStatus: 'connected',
    } : { environment: 'prod', applicationIds: 'app-ace', accessStatus: 'connected' });
    setOpen(true);
  };

  const submitImport = (content: string) => {
    try {
      importServers.mutate(JSON.parse(content) as ServerPayload[]);
    } catch {
      message.error('导入内容不是合法 JSON');
    }
  };

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
    { title: '操作', fixed: 'right', render: (_, row) => <Button type="link" icon={<EditOutlined />} onClick={() => showEditor(row)}>维护</Button> },
  ];

  return (
    <>
      <PageHeader
        title="服务器列表"
        description="主机资源、Agent 状态与部署应用摘要。"
        breadcrumb={['服务器']}
        extra={<Space><Button onClick={() => setImportOpen(true)}>导入服务器</Button><Button type="primary" icon={<PlusOutlined />} onClick={() => showEditor()}>新增服务器</Button></Space>}
      />
      <FilterBar onSearch={() => setParams(query)} onReset={() => setParams({})}>
        <Input placeholder="主机名 / IP" defaultValue={params.get('keyword') ?? ''} onChange={(event) => query.keyword = event.target.value} />
        <Select placeholder="Agent 状态" allowClear defaultValue={params.get('agentStatus') ?? undefined} className="filter-select" onChange={(value) => query.agentStatus = value} options={agentOptions} />
      </FilterBar>
      <DataTable columns={columns} dataSource={data?.items} loading={isLoading} error={error} onRetry={() => void refetch()} pagination={{ total: data?.total }} />
      <Modal
        title={editing ? '维护服务器' : '新增服务器'}
        open={open}
        okText="保存"
        cancelText="取消"
        confirmLoading={saveServer.isPending}
        onCancel={() => setOpen(false)}
        onOk={() => void form.validateFields().then((values) => saveServer.mutate(toPayload(values)))}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="id" label="服务器 ID"><Input disabled={Boolean(editing)} placeholder="留空由后端生成" /></Form.Item>
          <Form.Item name="hostname" label="主机名" rules={[{ required: true, message: '请输入主机名' }]}><Input /></Form.Item>
          <Form.Item name="ip" label="IP" rules={[{ required: true, message: '请输入 IP' }]}><Input /></Form.Item>
          <Form.Item name="environment" label="环境" rules={[{ required: true, message: '请选择环境' }]}><Select options={environmentOptions} /></Form.Item>
          <Form.Item name="applicationIds" label="关联应用 ID" rules={[{ required: true, message: '请输入关联应用 ID' }]}><Input placeholder="多个用英文逗号分隔，如 app-ace,app-ipro" /></Form.Item>
          <Form.Item name="accessStatus" label="接入状态" rules={[{ required: true, message: '请选择接入状态' }]}><Select options={accessOptions} /></Form.Item>
        </Form>
      </Modal>
      <Modal
        title="导入服务器"
        open={importOpen}
        okText="导入"
        cancelText="取消"
        confirmLoading={importServers.isPending}
        onCancel={() => setImportOpen(false)}
        onOk={() => void importForm.validateFields().then(({ content }) => submitImport(content))}
      >
        <Form form={importForm} layout="vertical" preserve={false}>
          <Form.Item name="content" label="服务器 JSON 数组" rules={[{ required: true, message: '请输入服务器 JSON 数组' }]}>
            <TextArea rows={8} placeholder='[{"id":"srv-demo","hostname":"demo-01","ip":"10.0.0.1","environment":"prod","applicationIds":["app-ace"],"accessStatus":"connected"}]' />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}
