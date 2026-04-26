import { Card, Descriptions, List, Space } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { assetsApi } from '../api/services';
import { ErrorState, LoadingState } from '../components/AppState';
import { PageHeader } from '../components/PageHeader';
import { StatusTag } from '../components/StatusTag';

export function ApplicationDetailPage() {
  const { appId = '' } = useParams();
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ['application', appId], queryFn: () => assetsApi.applicationDetail(appId) });

  if (isLoading) return <LoadingState />;
  if (error || !data) return <ErrorState error={error} onRetry={() => void refetch()} />;

  return (
    <>
      <PageHeader title={data.name} description={data.description} breadcrumb={['应用', data.name]} />
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
      </Space>
    </>
  );
}
