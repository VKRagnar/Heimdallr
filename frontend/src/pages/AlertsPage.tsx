import { CheckOutlined, CloseOutlined, EditOutlined, FileSearchOutlined, LineChartOutlined, PlayCircleOutlined, PlusOutlined, SyncOutlined, ToolOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Descriptions, Drawer, Form, Input, InputNumber, Modal, Select, Space, Statistic, Switch, Table, Tabs, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { alertsApi } from '../api/services';
import { DataTable } from '../components/DataTable';
import { PageHeader } from '../components/PageHeader';
import { StatusTag } from '../components/StatusTag';
import type { AlertEvent, AlertRule, AlertRulePayload } from '../types/domain';

const eventStatusOptions = [
  { label: 'All events', value: undefined },
  { label: 'Triggered', value: 'triggered' },
  { label: 'Notified', value: 'notified' },
  { label: 'Notify failed', value: 'notification_failed' },
  { label: 'Acknowledged', value: 'acknowledged' },
  { label: 'Processing', value: 'processing' },
  { label: 'Recovered', value: 'recovered' },
  { label: 'Closed', value: 'closed' },
];

const severityOptions = [
  { label: 'All severities', value: undefined },
  { label: 'P0', value: 'P0' },
  { label: 'P1', value: 'P1' },
  { label: 'P2', value: 'P2' },
  { label: 'P3', value: 'P3' },
];

const ruleEnabledOptions = [
  { label: 'All rules', value: undefined },
  { label: 'Enabled only', value: 'true' },
  { label: 'Disabled only', value: 'false' },
];

const defaultRuleValues: AlertRulePayload = {
  name: '',
  objectId: 'obj-kafka-orders',
  metricCode: 'mq_lag',
  operator: '>',
  threshold: 1000,
  windowSeconds: 300,
  durationSeconds: 60,
  evaluationIntervalSeconds: 60,
  severity: 'P2',
  enabled: true,
};

function severityTag(value: string) {
  const color = value === 'P0' || value === 'P1' ? 'red' : value === 'P2' ? 'orange' : 'blue';
  return <Tag color={color}>{value}</Tag>;
}

function normalizeActionStatus(status: string) {
  return status.toUpperCase();
}

export function AlertsPage() {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState<string | undefined>();
  const [eventSeverityFilter, setEventSeverityFilter] = useState<string | undefined>();
  const [eventKeywordFilter, setEventKeywordFilter] = useState('');
  const [ruleSeverityFilter, setRuleSeverityFilter] = useState<string | undefined>();
  const [ruleEnabledFilter, setRuleEnabledFilter] = useState<string | undefined>();
  const [ruleKeywordFilter, setRuleKeywordFilter] = useState('');
  const [ruleModalOpen, setRuleModalOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<AlertRule>();
  const [pendingEventAction, setPendingEventAction] = useState<{ event: AlertEvent; action: 'ACKNOWLEDGE' | 'PROCESS' | 'CLOSE' }>();
  const [selectedEvent, setSelectedEvent] = useState<AlertEvent>();
  const [form] = Form.useForm<AlertRulePayload>();
  const [eventActionForm] = Form.useForm<{ detail: string }>();

  const rulesQuery = useQuery({
    queryKey: ['alert-rules', ruleSeverityFilter, ruleEnabledFilter, ruleKeywordFilter],
    queryFn: () => alertsApi.rules({ severity: ruleSeverityFilter, enabled: ruleEnabledFilter, keyword: ruleKeywordFilter || undefined }),
  });
  const eventsQuery = useQuery({
    queryKey: ['alert-events', statusFilter, eventSeverityFilter, eventKeywordFilter],
    queryFn: () => alertsApi.events({ status: statusFilter, severity: eventSeverityFilter, keyword: eventKeywordFilter || undefined }),
  });
  const groupsQuery = useQuery({ queryKey: ['alert-on-call-groups'], queryFn: alertsApi.onCallGroups });
  const historyQuery = useQuery({
    queryKey: ['alert-event-history', selectedEvent?.id],
    queryFn: () => alertsApi.eventHistory(selectedEvent?.id ?? ''),
    enabled: Boolean(selectedEvent),
  });
  const notificationsQuery = useQuery({
    queryKey: ['alert-notifications', selectedEvent?.id],
    queryFn: () => alertsApi.notifications(selectedEvent?.id),
    enabled: Boolean(selectedEvent),
  });

  const refreshAlerts = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['alert-rules'] }),
      queryClient.invalidateQueries({ queryKey: ['alert-events'] }),
      queryClient.invalidateQueries({ queryKey: ['alert-event-history'] }),
      queryClient.invalidateQueries({ queryKey: ['alert-notifications'] }),
    ]);
  };

  const saveRule = useMutation({
    mutationFn: ({ ruleId, payload }: { ruleId?: string; payload: AlertRulePayload }) => ruleId ? alertsApi.updateRule(ruleId, payload) : alertsApi.createRule(payload),
    onSuccess: async (_, variables) => {
      message.success(variables.ruleId ? 'Rule saved' : 'Rule created');
      setRuleModalOpen(false);
      setEditingRule(undefined);
      form.resetFields();
      await refreshAlerts();
    },
  });

  const toggleRule = useMutation({
    mutationFn: ({ ruleId, enabled }: { ruleId: string; enabled: boolean }) => enabled ? alertsApi.enableRule(ruleId) : alertsApi.disableRule(ruleId),
    onSuccess: async () => {
      message.success('Rule updated');
      await refreshAlerts();
    },
  });

  const evaluateRule = useMutation({
    mutationFn: (ruleId: string) => alertsApi.evaluateRule(ruleId),
    onSuccess: async () => {
      message.success('Evaluation completed');
      await refreshAlerts();
    },
  });

  const evaluateEnabledRules = useMutation({
    mutationFn: alertsApi.evaluateEnabledRules,
    onSuccess: async () => {
      message.success('Enabled rules evaluated');
      await refreshAlerts();
    },
  });

  const transitionEvent = useMutation({
    mutationFn: ({ eventId, action, detail }: { eventId: string; action: string; detail?: string }) => alertsApi.transitionEvent(eventId, action, detail),
    onSuccess: async (event) => {
      message.success('Event updated');
      setPendingEventAction(undefined);
      eventActionForm.resetFields();
      setSelectedEvent(event);
      await refreshAlerts();
    },
  });

  const rules = rulesQuery.data?.items ?? [];
  const events = eventsQuery.data?.items ?? [];
  const activeEvents = events.filter((event) => !['CLOSED', 'RECOVERED', 'closed', 'recovered'].includes(event.status));

  const openNewRule = () => {
    setEditingRule(undefined);
    form.setFieldsValue({ ...defaultRuleValues, onCallGroupId: groupsQuery.data?.items[0]?.id });
    setRuleModalOpen(true);
  };

  const openEditRule = (rule: AlertRule) => {
    setEditingRule(rule);
    form.setFieldsValue({
      name: rule.name,
      objectId: rule.objectId,
      metricCode: rule.metricCode,
      operator: rule.operator,
      threshold: rule.threshold,
      windowSeconds: rule.windowSeconds,
      durationSeconds: rule.durationSeconds,
      evaluationIntervalSeconds: rule.evaluationIntervalSeconds,
      severity: rule.severity,
      enabled: rule.enabled,
      onCallGroupId: rule.onCallGroupId,
    });
    setRuleModalOpen(true);
  };

  const openEventAction = (event: AlertEvent, action: 'ACKNOWLEDGE' | 'PROCESS' | 'CLOSE') => {
    setPendingEventAction({ event, action });
    eventActionForm.setFieldsValue({ detail: '' });
  };

  const ruleColumns: ColumnsType<AlertRule> = [
    {
      title: 'Rule',
      dataIndex: 'name',
      render: (value, row) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{value}</Typography.Text>
          <Typography.Text type="secondary">{row.objectName ?? row.objectId}</Typography.Text>
        </Space>
      ),
    },
    { title: 'Metric', dataIndex: 'metricCode' },
    { title: 'Condition', render: (_, row) => `${row.metricCode} ${row.operator} ${row.threshold}` },
    { title: 'Severity', dataIndex: 'severity', render: severityTag },
    { title: 'Interval', dataIndex: 'evaluationIntervalSeconds', render: (value) => `${value}s` },
    {
      title: 'Enabled',
      dataIndex: 'enabled',
      render: (value, row) => (
        <Switch
          checked={value}
          loading={toggleRule.isPending && toggleRule.variables?.ruleId === row.id}
          onChange={(checked) => toggleRule.mutate({ ruleId: row.id, enabled: checked })}
        />
      ),
    },
    {
      title: 'Actions',
      fixed: 'right',
      render: (_, row) => (
        <Space wrap>
          <Button icon={<EditOutlined />} onClick={() => openEditRule(row)}>Edit</Button>
          <Button
            icon={<PlayCircleOutlined />}
            loading={evaluateRule.isPending && evaluateRule.variables === row.id}
            onClick={() => evaluateRule.mutate(row.id)}
          >
            Evaluate
          </Button>
        </Space>
      ),
    },
  ];

  const eventColumns: ColumnsType<AlertEvent> = [
    {
      title: 'Event',
      dataIndex: 'ruleName',
      render: (value, row) => (
        <Space direction="vertical" size={0}>
          <Button type="link" onClick={() => setSelectedEvent(row)}>{value}</Button>
          <Typography.Text type="secondary">{row.objectName ?? row.objectId}</Typography.Text>
        </Space>
      ),
    },
    { title: 'Status', dataIndex: 'status', render: (value) => <StatusTag value={normalizeActionStatus(value)} /> },
    { title: 'Severity', dataIndex: 'severity', render: severityTag },
    { title: 'Value', render: (_, row) => `${row.triggerValue} ${row.operator} ${row.threshold}` },
    { title: 'Updated', dataIndex: 'updatedAt', render: (value) => value ?? '-' },
    {
      title: 'Actions',
      fixed: 'right',
      render: (_, row) => (
        <Space wrap>
          <Button icon={<CheckOutlined />} disabled={['ACKNOWLEDGED', 'PROCESSING', 'CLOSED', 'RECOVERED'].includes(normalizeActionStatus(row.status))} onClick={() => openEventAction(row, 'ACKNOWLEDGE')}>Acknowledge</Button>
          <Button icon={<ToolOutlined />} disabled={['PROCESSING', 'CLOSED', 'RECOVERED'].includes(normalizeActionStatus(row.status))} onClick={() => openEventAction(row, 'PROCESS')}>Process</Button>
          <Button danger icon={<CloseOutlined />} disabled={['CLOSED', 'RECOVERED'].includes(normalizeActionStatus(row.status))} onClick={() => openEventAction(row, 'CLOSE')}>Close</Button>
        </Space>
      ),
    },
  ];

  return (
    <>
      <PageHeader
        title="Alert Workbench"
        description="Manage threshold rules, run lightweight evaluations, and handle alert events."
        breadcrumb={['Observability', 'Alerts']}
        extra={(
          <Space>
            <Button icon={<SyncOutlined />} loading={evaluateEnabledRules.isPending} onClick={() => evaluateEnabledRules.mutate()}>Evaluate enabled</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={openNewRule}>New rule</Button>
          </Space>
        )}
      />

      <div className="summary-strip">
        <Statistic title="Active events" value={activeEvents.length} />
        <Statistic title="Rules" value={rules.length} />
        <Statistic title="Enabled rules" value={rules.filter((rule) => rule.enabled).length} />
        <Statistic title="Notifications" value={notificationsQuery.data?.total ?? 0} />
      </div>

      <Tabs
        items={[
          {
            key: 'events',
            label: 'Events',
            children: (
              <Space direction="vertical" size={16} className="page-stack">
                <Space wrap>
                  <Select
                    aria-label="Event status"
                    allowClear
                    value={statusFilter}
                    placeholder="Filter status"
                    options={eventStatusOptions}
                    onChange={setStatusFilter}
                    className="filter-select"
                  />
                  <Select
                    aria-label="Event severity"
                    allowClear
                    value={eventSeverityFilter}
                    placeholder="Filter severity"
                    options={severityOptions}
                    onChange={setEventSeverityFilter}
                    className="filter-select"
                  />
                  <Input.Search
                    aria-label="Event keyword"
                    allowClear
                    placeholder="Search rule, object, metric"
                    value={eventKeywordFilter}
                    onChange={(event) => setEventKeywordFilter(event.target.value)}
                    className="filter-select"
                  />
                </Space>
                <DataTable columns={eventColumns} dataSource={events} loading={eventsQuery.isLoading} error={eventsQuery.error} onRetry={() => void eventsQuery.refetch()} pagination={{ total: eventsQuery.data?.total }} />
              </Space>
            ),
          },
          {
            key: 'rules',
            label: 'Rules',
            children: (
              <Space direction="vertical" size={16} className="page-stack">
                <Space wrap>
                  <Select
                    aria-label="Rule severity"
                    allowClear
                    value={ruleSeverityFilter}
                    placeholder="Filter severity"
                    options={severityOptions}
                    onChange={setRuleSeverityFilter}
                    className="filter-select"
                  />
                  <Select
                    aria-label="Rule enabled"
                    allowClear
                    value={ruleEnabledFilter}
                    placeholder="Rule state"
                    options={ruleEnabledOptions}
                    onChange={setRuleEnabledFilter}
                    className="filter-select"
                  />
                  <Input.Search
                    aria-label="Rule keyword"
                    allowClear
                    placeholder="Search name, object, metric"
                    value={ruleKeywordFilter}
                    onChange={(event) => setRuleKeywordFilter(event.target.value)}
                    className="filter-select"
                  />
                </Space>
                <DataTable columns={ruleColumns} dataSource={rules} loading={rulesQuery.isLoading} error={rulesQuery.error} onRetry={() => void rulesQuery.refetch()} pagination={{ total: rulesQuery.data?.total }} />
              </Space>
            ),
          },
        ]}
      />

      <Drawer width={720} title="Alert event detail" open={Boolean(selectedEvent)} onClose={() => setSelectedEvent(undefined)}>
        {selectedEvent ? (
          <Space direction="vertical" size={16} className="page-stack">
            <Descriptions column={2}>
              <Descriptions.Item label="Rule">{selectedEvent.ruleName}</Descriptions.Item>
              <Descriptions.Item label="Status"><StatusTag value={normalizeActionStatus(selectedEvent.status)} /></Descriptions.Item>
              <Descriptions.Item label="Object">{selectedEvent.objectName ?? selectedEvent.objectId}</Descriptions.Item>
              <Descriptions.Item label="Metric">{selectedEvent.metricCode}</Descriptions.Item>
              <Descriptions.Item label="Trigger">{selectedEvent.triggerValue} {selectedEvent.operator} {selectedEvent.threshold}</Descriptions.Item>
              <Descriptions.Item label="Assignee">{selectedEvent.assigneeUserId ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="Close reason" span={2}>{selectedEvent.closeReason ?? '-'}</Descriptions.Item>
            </Descriptions>
            <Space wrap>
              <Button icon={<FileSearchOutlined />} href={`/logs/search?alertId=${encodeURIComponent(selectedEvent.id)}&objectId=${encodeURIComponent(selectedEvent.objectId)}&keyword=${encodeURIComponent(selectedEvent.ruleName)}`}>Query logs</Button>
              <Button icon={<LineChartOutlined />} href={`/metrics?alertId=${encodeURIComponent(selectedEvent.id)}&objectId=${encodeURIComponent(selectedEvent.objectId)}&metricCode=${encodeURIComponent(selectedEvent.metricCode)}`}>Query metrics</Button>
            </Space>
            <Tabs
              size="small"
              items={[
                {
                  key: 'history',
                  label: `History (${historyQuery.data?.total ?? 0})`,
                  children: (
                    <Table
                      rowKey="id"
                      size="small"
                      pagination={false}
                      loading={historyQuery.isLoading}
                      dataSource={historyQuery.data?.items ?? []}
                      columns={[
                        { title: 'Action', dataIndex: 'action' },
                        { title: 'From', dataIndex: 'fromStatus', render: (value) => value ?? '-' },
                        { title: 'To', dataIndex: 'toStatus', render: (value) => <StatusTag value={normalizeActionStatus(String(value))} /> },
                        { title: 'Message', dataIndex: 'message', render: (value) => value ?? '-' },
                        { title: 'Time', dataIndex: 'operatedAt' },
                      ]}
                    />
                  ),
                },
                {
                  key: 'notifications',
                  label: `Notifications (${notificationsQuery.data?.total ?? 0})`,
                  children: (
                    <Table
                      rowKey="id"
                      size="small"
                      pagination={false}
                      loading={notificationsQuery.isLoading}
                      dataSource={notificationsQuery.data?.items ?? []}
                      columns={[
                        { title: 'Channel', dataIndex: 'channelType' },
                        { title: 'Receiver', dataIndex: 'receiver' },
                        { title: 'Status', dataIndex: 'status', render: (value) => <StatusTag value={value} /> },
                        { title: 'Retry', dataIndex: 'retryCount' },
                        { title: 'Failure', dataIndex: 'failureReason', render: (value) => value ?? '-' },
                        { title: 'Sent at', dataIndex: 'sentAt', render: (value) => value ?? '-' },
                      ]}
                    />
                  ),
                },
              ]}
            />
          </Space>
        ) : null}
      </Drawer>

      <Modal
        title={editingRule ? 'Edit alert rule' : 'New alert rule'}
        open={ruleModalOpen}
        okText={editingRule ? 'Save' : 'Create'}
        confirmLoading={saveRule.isPending}
        onCancel={() => {
          setRuleModalOpen(false);
          setEditingRule(undefined);
          form.resetFields();
        }}
        onOk={() => form.submit()}
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={defaultRuleValues}
          onFinish={(values) => saveRule.mutate({ ruleId: editingRule?.id, payload: values })}
        >
          <Form.Item label="Name" name="name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item label="Object ID" name="objectId" rules={[{ required: true }]}>
            <Input placeholder="obj-kafka-orders" />
          </Form.Item>
          <Form.Item label="Metric code" name="metricCode" rules={[{ required: true }]}>
            <Input placeholder="mq_lag" />
          </Form.Item>
          <Space align="start">
            <Form.Item label="Operator" name="operator" rules={[{ required: true }]}>
              <Select options={['>', '>=', '<', '<=', '=', '!='].map((value) => ({ label: value, value }))} />
            </Form.Item>
            <Form.Item label="Threshold" name="threshold" rules={[{ required: true }]}>
              <InputNumber />
            </Form.Item>
            <Form.Item label="Severity" name="severity">
              <Select options={['P0', 'P1', 'P2', 'P3'].map((value) => ({ label: value, value }))} />
            </Form.Item>
          </Space>
          <Space align="start">
            <Form.Item label="Window seconds" name="windowSeconds">
              <InputNumber min={1} />
            </Form.Item>
            <Form.Item label="Duration seconds" name="durationSeconds">
              <InputNumber min={0} />
            </Form.Item>
            <Form.Item label="Interval seconds" name="evaluationIntervalSeconds">
              <InputNumber min={1} />
            </Form.Item>
          </Space>
          <Form.Item label="On-call group" name="onCallGroupId">
            <Select loading={groupsQuery.isLoading} options={(groupsQuery.data?.items ?? []).map((group) => ({ label: group.name, value: group.id }))} />
          </Form.Item>
          <Form.Item label="Enabled" name="enabled" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={pendingEventAction?.action === 'CLOSE' ? 'Close alert event' : pendingEventAction?.action === 'PROCESS' ? 'Process alert event' : 'Acknowledge alert event'}
        open={Boolean(pendingEventAction)}
        okText={pendingEventAction?.action === 'CLOSE' ? 'Close event' : 'Save'}
        confirmLoading={transitionEvent.isPending}
        onCancel={() => {
          setPendingEventAction(undefined);
          eventActionForm.resetFields();
        }}
        onOk={() => eventActionForm.submit()}
      >
        <Form
          form={eventActionForm}
          layout="vertical"
          onFinish={({ detail }) => {
            if (!pendingEventAction) return;
            transitionEvent.mutate({ eventId: pendingEventAction.event.id, action: pendingEventAction.action, detail });
          }}
        >
          <Form.Item
            label={pendingEventAction?.action === 'CLOSE' ? 'Close reason' : 'Handling note'}
            name="detail"
            rules={[{ required: pendingEventAction?.action === 'CLOSE', message: 'Please enter a reason before closing the event' }]}
          >
            <Input.TextArea rows={4} placeholder={pendingEventAction?.action === 'CLOSE' ? 'e.g. Lag recovered after consumer restart; no further action needed.' : 'Add operator context for the next handoff.'} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}
