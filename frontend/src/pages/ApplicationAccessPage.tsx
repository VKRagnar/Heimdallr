import { Alert, Button, Form, Input, Modal, Radio, Select, Space, Statistic, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { SafetyCertificateOutlined } from '@ant-design/icons';
import { accessManagementApi } from '../api/services';
import { DataTable } from '../components/DataTable';
import { FilterBar } from '../components/FilterBar';
import { PageHeader } from '../components/PageHeader';
import { StatusTag } from '../components/StatusTag';
import type { ApplicationAccess, ApplicationAccessGrantPayload } from '../types/domain';

const environmentOptions = ['prod', 'pre', 'test', 'dev'].map((env) => ({ label: env.toUpperCase(), value: env }));
export function ApplicationAccessPage() {
  const [query, setQuery] = useState<Record<string, string | undefined>>({});
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm<ApplicationAccessGrantPayload>();
  const queryClient = useQueryClient();
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ['application-access', query], queryFn: () => accessManagementApi.applicationAccess(query) });
  const { data: userData } = useQuery({ queryKey: ['access-users'], queryFn: () => accessManagementApi.users() });
  const items = data?.items ?? [];

  const changeGrant = useMutation({
    mutationFn: (payload: ApplicationAccessGrantPayload) => {
      if (payload.scopeType === 'application' && payload.applicationId) {
        return payload.action === 'grant'
          ? accessManagementApi.grantApplicationAccess(payload.userId, payload.applicationId)
          : accessManagementApi.revokeApplicationAccess(payload.userId, payload.applicationId);
      }
      if (payload.scopeType === 'businessLine' && payload.businessLine) {
        return payload.action === 'grant'
          ? accessManagementApi.grantBusinessLineAccess(payload.userId, payload.businessLine)
          : accessManagementApi.revokeBusinessLineAccess(payload.userId, payload.businessLine);
      }
      return Promise.reject(new Error('请选择授权范围'));
    },
    onSuccess: async () => {
      message.success('授权操作已提交');
      setOpen(false);
      form.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['application-access'] });
      await queryClient.invalidateQueries({ queryKey: ['applications'] });
      await queryClient.invalidateQueries({ queryKey: ['access-users'] });
      await queryClient.invalidateQueries({ queryKey: ['audit-events'] });
    },
  });

  const openGrantModal = (row?: ApplicationAccess) => {
    form.setFieldsValue(row ? {
      userId: userData?.items[0]?.id ?? 'u-admin',
      scopeType: 'application',
      action: 'grant',
      applicationId: row.appId,
    } : { userId: userData?.items[0]?.id ?? 'u-admin', scopeType: 'application', action: 'grant' });
    setOpen(true);
  };

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
    {
      title: '操作',
      fixed: 'right',
      render: (_, row) => (
        <Space>
          <Link to={`/logs/search?appId=${row.appId}&env=${row.environment}&level=ERROR`}>日志</Link>
          <Link to={`/metrics?objectType=application&objectId=${row.appId}&env=${row.environment}&metricCodes=error_rate,qps,p95_latency`}>指标</Link>
          <Button type="link" onClick={() => openGrantModal(row)}>授权</Button>
        </Space>
      ),
    },
  ];

  return (
    <>
      <PageHeader title="应用接入" description="展示应用的指标、日志、链路、健康检查和 Agent 接入状态，区分未接入、采集异常和业务异常。" breadcrumb={['接入管理', '应用接入']} extra={<Button type="primary" onClick={() => openGrantModal()}>授权/回收</Button>} />
      <FilterBar onSearch={() => void refetch()} onReset={() => setQuery({})} extra={<Button icon={<SafetyCertificateOutlined />} onClick={() => void refetch()}>刷新接入状态</Button>}>
        <Input placeholder="应用名称 / 编码" value={query.keyword} onChange={(event) => setQuery((prev) => ({ ...prev, keyword: event.target.value }))} />
        <Select placeholder="环境" allowClear value={query.env} className="filter-select" onChange={(value) => setQuery((prev) => ({ ...prev, env: value }))} options={environmentOptions} />
        <Select placeholder="接入状态" allowClear value={query.accessStatus} className="filter-select" onChange={(value) => setQuery((prev) => ({ ...prev, accessStatus: value }))} options={[{ label: '已接入', value: 'connected' }, { label: '部分接入', value: 'partial_connected' }, { label: '未接入', value: 'not_connected' }, { label: '采集异常', value: 'collector_error' }]} />
      </FilterBar>
      <div className="summary-strip">
        <Statistic title="全部" value={items.length} />
        <Statistic title="指标已接入" value={items.filter((item) => item.metricsAccess !== '异常' && item.metricsAccess !== '未接入').length} />
        <Statistic title="仅日志/仅指标" value={items.filter((item) => item.accessStatus === 'partial_connected').length} />
        <Statistic title="采集异常" value={items.filter((item) => item.accessStatus === 'collector_error').length} />
      </div>
      <Alert className="source-bar" type="warning" showIcon message="采集异常不等同于业务健康异常；应用或业务线授权/回收会记录审计，可在系统管理 / 审计中查询。" />
      <DataTable columns={columns} dataSource={items} loading={isLoading} error={error} onRetry={() => void refetch()} pagination={{ total: data?.total }} />
      <Modal
        title="访问授权"
        open={open}
        okText="提交"
        cancelText="取消"
        confirmLoading={changeGrant.isPending}
        onCancel={() => setOpen(false)}
        onOk={() => void form.validateFields().then((values) => changeGrant.mutate(values))}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="userId" label="用户" rules={[{ required: true, message: '请选择用户' }]}>
            <Select options={(userData?.items ?? []).map((user) => ({ label: `${user.name} (${user.username})`, value: user.id }))} />
          </Form.Item>
          <Form.Item name="action" label="操作" rules={[{ required: true, message: '请选择操作' }]}>
            <Radio.Group optionType="button" buttonStyle="solid" options={[{ label: '授权', value: 'grant' }, { label: '回收', value: 'revoke' }]} />
          </Form.Item>
          <Form.Item name="scopeType" label="范围" rules={[{ required: true, message: '请选择范围' }]}>
            <Radio.Group optionType="button" buttonStyle="solid" options={[{ label: '应用', value: 'application' }, { label: '业务线', value: 'businessLine' }]} />
          </Form.Item>
          <Form.Item noStyle shouldUpdate={(prev, next) => prev.scopeType !== next.scopeType}>
            {({ getFieldValue }) => getFieldValue('scopeType') === 'businessLine' ? (
              <Form.Item name="businessLine" label="业务线" rules={[{ required: true, message: '请输入业务线' }]}><Input /></Form.Item>
            ) : (
              <Form.Item name="applicationId" label="应用 ID" rules={[{ required: true, message: '请输入应用 ID' }]}><Input /></Form.Item>
            )}
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}
