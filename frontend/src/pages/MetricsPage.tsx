import { Alert, Card, Col, Input, Row, Select, Space, Statistic, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { observabilityApi } from '../api/services';
import { FilterBar } from '../components/FilterBar';
import { PageHeader } from '../components/PageHeader';
import { StatusTag } from '../components/StatusTag';
import type { DefaultMetricMapping, MetricDefinition, MetricQueryResult } from '../types/domain';

function toSearchParams(query: Record<string, string | undefined>) {
  return Object.fromEntries(Object.entries(query).filter((entry): entry is [string, string] => Boolean(entry[1])));
}

function MetricSparkline({ series }: { series: MetricQueryResult['series'][number] }) {
  const width = 520;
  const height = 120;
  const max = Math.max(...series.points.map((point) => point.value), series.threshold ?? 0, 1);
  const step = width / Math.max(series.points.length - 1, 1);
  const points = series.points.map((point, index) => `${index * step},${height - (point.value / max) * (height - 18) - 8}`).join(' ');
  const thresholdY = series.threshold ? height - (series.threshold / max) * (height - 18) - 8 : undefined;

  return (
    <svg className="metric-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label={series.metricName}>
      <polyline points={points} fill="none" stroke="#1677ff" strokeWidth="3" />
      {thresholdY ? <line x1="0" x2={width} y1={thresholdY} y2={thresholdY} stroke="#f5222d" strokeDasharray="6 4" /> : null}
      {series.points.map((point, index) => {
        const cx = index * step;
        const cy = height - (point.value / max) * (height - 18) - 8;
        return <circle key={point.time} cx={cx} cy={cy} r={point.alert ? 5 : point.logHit ? 4 : 3} fill={point.alert ? '#f5222d' : point.logHit ? '#faad14' : '#1677ff'} />;
      })}
      {series.points.map((point, index) => <text key={point.time} x={index * step} y={height - 2} fontSize="10" textAnchor={index === 0 ? 'start' : index === series.points.length - 1 ? 'end' : 'middle'} fill="#667085">{point.time}</text>)}
    </svg>
  );
}

export function MetricsPage() {
  const [params, setParams] = useSearchParams();
  const [query, setQuery] = useState<Record<string, string | undefined>>(() => Object.fromEntries(params.entries()));
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ['metrics-query', query], queryFn: () => observabilityApi.queryMetrics(query) });
  const definitions = useQuery({ queryKey: ['metric-definitions', query.objectType], queryFn: () => observabilityApi.metricDefinitions({ objectType: query.objectType }) });
  const mappings = useQuery({ queryKey: ['metric-default-mappings', query.objectType], queryFn: () => observabilityApi.defaultMappings({ objectType: query.objectType }) });

  const definitionColumns: ColumnsType<MetricDefinition> = [
    { title: '指标', dataIndex: 'name', render: (value, row) => <Space direction="vertical" size={0}><Typography.Text strong>{value}</Typography.Text><Typography.Text type="secondary">{row.code}</Typography.Text></Space> },
    { title: '对象类型', dataIndex: 'objectType' },
    { title: '来源', dataIndex: 'sourceType' },
    { title: '单位', dataIndex: 'unit' },
    { title: '默认启用', dataIndex: 'defaultEnabled', render: (value) => value ? <Tag color="green">默认</Tag> : '-' },
    { title: '查询模板', render: (_, row) => row.promqlTemplate ?? row.defaultQueryTemplate ?? '-', ellipsis: true },
  ];
  const mappingColumns: ColumnsType<DefaultMetricMapping> = [
    { title: '对象类型', dataIndex: 'objectType' },
    { title: '指标编码', dataIndex: 'metricCode' },
    { title: '来源', dataIndex: 'sourceType' },
    { title: '外部指标', render: (_, row) => row.externalMetricName ?? row.externalMetric ?? '-' },
    { title: '单位', dataIndex: 'unit' },
    { title: '映射模板', dataIndex: 'queryTemplate', ellipsis: true },
  ];

  return (
    <>
      <PageHeader title="指标查询" description="按对象、环境、指标组和时间范围查询趋势，展示阈值线、告警点、日志命中点和数据质量。" breadcrumb={['指标中心', '指标查询']} />
      <FilterBar onSearch={() => { setParams(toSearchParams(query)); void refetch(); }} onReset={() => { setQuery({}); setParams({}); }}>
        <Select placeholder="对象类型" value={query.objectType ?? 'application'} className="filter-select" onChange={(value) => setQuery((prev) => ({ ...prev, objectType: value }))} options={[{ label: '应用', value: 'application' }, { label: '服务器', value: 'server' }, { label: 'Kafka', value: 'kafka' }, { label: 'PostgreSQL', value: 'postgresql' }]} />
        <Input placeholder="对象 ID / 名称" value={query.objectId} onChange={(event) => setQuery((prev) => ({ ...prev, objectId: event.target.value }))} />
        <Select placeholder="环境" allowClear value={query.env} className="filter-select" onChange={(value) => setQuery((prev) => ({ ...prev, env: value }))} options={['prod', 'pre', 'staging', 'test', 'dev'].map((env) => ({ label: env.toUpperCase(), value: env }))} />
        <Select mode="multiple" placeholder="指标" value={query.metricCodes?.split(',').filter(Boolean)} className="metric-select" onChange={(value) => setQuery((prev) => ({ ...prev, metricCodes: value.join(',') }))} options={(definitions.data?.items ?? []).map((metric) => ({ label: metric.name, value: metric.code }))} />
      </FilterBar>
      {error ? <Alert className="source-bar" type="error" showIcon message="指标查询失败，可缩小时间范围或重试。" /> : null}
      {data ? <Alert className="source-bar" type="info" showIcon message={`数据质量：${data.dataQuality.sourceName} / ${data.dataQuality.status} / 最近成功 ${data.dataQuality.lastSuccessAt} / 缺失点 ${(data.dataQuality.missingPointRate * 100).toFixed(1)}%`} /> : null}
      <Row gutter={[16, 16]}>
        {(data?.series ?? []).map((series) => {
          const current = series.points[series.points.length - 1]?.value ?? 0;
          const max = Math.max(...series.points.map((point) => point.value));
          const avg = series.points.reduce((sum, point) => sum + point.value, 0) / series.points.length;
          return (
            <Col xs={24} lg={8} key={series.metricCode}>
              <Card title={<Space>{series.metricName}<StatusTag value={data?.dataQuality.status ?? 'unknown'} /></Space>} loading={isLoading}>
                <Statistic title={`当前值（${series.unit}）`} value={current} precision={series.unit === '%' ? 2 : 0} />
                <Space size={16} className="metric-stats"><Typography.Text type="secondary">最大 {max.toFixed(2)}</Typography.Text><Typography.Text type="secondary">平均 {avg.toFixed(2)}</Typography.Text>{series.threshold ? <Typography.Text type="secondary">阈值 {series.threshold}</Typography.Text> : null}</Space>
                <MetricSparkline series={series} />
              </Card>
            </Col>
          );
        })}
      </Row>
      <Card title="指标定义" className="table-card">
        <Table rowKey="code" size="middle" loading={definitions.isLoading} columns={definitionColumns} dataSource={definitions.data?.items} pagination={false} />
      </Card>
      <Card title="默认指标映射" className="table-card">
        <Table rowKey="id" size="middle" loading={mappings.isLoading} columns={mappingColumns} dataSource={mappings.data?.items} pagination={false} />
      </Card>
    </>
  );
}
