import type { ReactNode } from 'react';
import { Breadcrumb, Typography } from 'antd';

interface PageHeaderProps {
  title: string;
  description?: string;
  breadcrumb?: string[];
  extra?: ReactNode;
}

export function PageHeader({ title, description, breadcrumb, extra }: PageHeaderProps) {
  return (
    <div className="page-header">
      <div>
        {breadcrumb ? <Breadcrumb items={breadcrumb.map((item) => ({ title: item }))} /> : null}
        <Typography.Title level={3}>{title}</Typography.Title>
        {description ? <Typography.Text type="secondary">{description}</Typography.Text> : null}
      </div>
      {extra ? <div className="page-header-extra">{extra}</div> : null}
    </div>
  );
}
