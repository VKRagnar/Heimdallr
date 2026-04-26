import type { ReactNode } from 'react';
import { Button, Space } from 'antd';
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';

interface FilterBarProps {
  children: ReactNode;
  onSearch?: () => void;
  onReset?: () => void;
  extra?: ReactNode;
}

export function FilterBar({ children, onSearch, onReset, extra }: FilterBarProps) {
  return (
    <div className="filter-bar">
      <Space size={12} wrap>
        {children}
        <Button type="primary" icon={<SearchOutlined />} onClick={onSearch}>
          查询
        </Button>
        <Button icon={<ReloadOutlined />} onClick={onReset}>
          重置
        </Button>
      </Space>
      {extra ? <div className="filter-extra">{extra}</div> : null}
    </div>
  );
}
