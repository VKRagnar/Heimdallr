import { Button, Card, Descriptions, List, Space, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { assetsApi } from '../api/services';
import { ErrorState, LoadingState } from '../components/AppState';
import { PageHeader } from '../components/PageHeader';
import { StatusTag } from '../components/StatusTag';
import type { AccessChannel } from '../types/domain';

const accessColumns: ColumnsType<AccessChannel> = [
  { title: '接入项', dataIndex: 'name' },
  { title: '数据源', dataIndex: 'sourceName' },
  { title: '状态', dataIndex: 'status', render: (value) => <StatusTag value={value} /> },
  { title: '最近采集', dataIndex: 'lastSeenAt', render: (value) => value ?? '-' },
  { title: '延迟', dataIndex: 'latencySeconds', render: (value) => value === undefined ? '-' : `${value}s` },
  { title: '异常原因', dataIndex: 'failureReason', render: (value) => value ?? '-' },
];

export function ApplicationDetailPage() {
  const { appId = '' } = useParams();
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ['application', appId], queryFn: () => assetsApi.applicationDetail(appId) });

  if (isLoading) return <LoadingState />;
  if (error || !data) return <ErrorState error={error} onRetry={() => void refetch()} />;

  return (
    <>
      <PageHeader
        title={data.name}
        description={data.description}
        breadcrumb={['应用', data.name]}
        extra={<Space><Link to={`/logs/search?appId=${data.id}&env=${data.environment}&level=ERROR`}><Button>查看日志</Button></Link><Link to={`/metrics?objectType=application&objectId=${data.id}&env=${data.environment}&metricCodes=${data.defaultMetricCodes?.join(',') ?? 'error_rate,qps,p95_latency'}`}><Button type="primary">指标趋势</Button></Link></Space>}
      />
      <Space direction="vertical" size={16} className="page-stack">
        <Card title="基础信息">
          <Descriptions column={{ xs: 1, md: 3 }}>
            <Descriptions.Item label="应用编码">{data.code}</Descriptions.Item>
            <Descriptions.Item label="环境">{data.environment.toUpperCase()}</Descriptions.Item>
            <Descriptions.Item label="负责人">{data.owner}</Descriptions.Item>
            <Descriptions.Item label="健康状态"><StatusTag value={data.status} /></Descriptions.Item>
            <Descriptions.Item label="接入状态"><StatusTag value={data.accessStatus} /></Descriptions.Item>
            <Descriptions.Item label="实例数">{data.instanceCount}</Descriptions.Item>
          </Descriptions>
        </Card>
        <Card title="依赖与接入">
          <List
            dataSource={data.dependencies}
            renderItem={(item) => (
              <List.Item actions={[<StatusTag key="status" value={item.status} />]}>
                <List.Item.Meta title={item.name} description={item.type} />
              </List.Item>
            )}
          />
        </Card>
        <Card title="接入状态">
          <Table rowKey="name" pagination={false} columns={accessColumns} dataSource={data.accessChannels} />
        </Card>
      </Space>
    </>
  );
}
