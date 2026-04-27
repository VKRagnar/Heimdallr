import { Alert, Card, Col, Row, Statistic } from 'antd';
import { PageHeader } from '../components/PageHeader';
import { useDataScope } from '../hooks/useCurrentUser';

export function HomePage() {
  const { data: scope } = useDataScope();

  return (
    <>
      <PageHeader
        title="首页总览"
        description="面向排障和值班的监控入口，Sprint 2 接入数据源、Agent、日志与指标联调状态。"
        breadcrumb={['首页']}
      />
      <Alert
        className="source-bar"
        type="info"
        showIcon
        message={`当前数据范围：${scope?.isGlobal ? '全局' : '受限'}，可见环境 ${scope?.environments.join(' / ') ?? '-'}`}
      />
      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card>
            <Statistic title="待处理告警" value={10} valueStyle={{ color: '#cf1322' }} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic title="接入应用" value={3} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic title="在线 Agent" value={2} suffix="/ 3" />
          </Card>
        </Col>
      </Row>
    </>
  );
}
