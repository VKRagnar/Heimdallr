import { Button, Empty, Result, Spin } from 'antd';
import { ApiError, errorCodeMap } from '../api/errors';

export function LoadingState({ tip = '正在加载数据' }: { tip?: string }) {
  return (
    <div className="state-block">
      <Spin tip={tip} />
    </div>
  );
}

export function EmptyState({ description = '暂无数据' }: { description?: string }) {
  return (
    <div className="state-block">
      <Empty description={description} />
    </div>
  );
}

export function ErrorState({ error, onRetry }: { error?: unknown; onRetry?: () => void }) {
  const code = error instanceof ApiError ? error.code : 'UNKNOWN';
  const meta = errorCodeMap[code];
  return (
    <Result
      className="state-result"
      status={meta.status === 404 ? '404' : 'error'}
      title={meta.title}
      subTitle={error instanceof Error ? error.message : meta.description}
      extra={onRetry ? <Button onClick={onRetry}>重试</Button> : undefined}
    />
  );
}

export function ForbiddenState({ title = '无权限访问', description = '当前账号没有访问该页面的权限。' }) {
  return <Result className="state-result" status="403" title={title} subTitle={description} />;
}
