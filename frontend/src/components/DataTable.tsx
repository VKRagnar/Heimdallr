import { Table } from 'antd';
import type { TableProps } from 'antd';
import { EmptyState, ErrorState, LoadingState } from './AppState';

interface DataTableProps<T extends object> extends TableProps<T> {
  error?: unknown;
  onRetry?: () => void;
}

export function DataTable<T extends object>({ error, onRetry, loading, locale, ...props }: DataTableProps<T>) {
  if (error) {
    return <ErrorState error={error} onRetry={onRetry} />;
  }

  if (loading) {
    return <LoadingState />;
  }

  return (
    <Table<T>
      rowKey="id"
      size="middle"
      pagination={{ showSizeChanger: true, showTotal: (total) => `共 ${total} 条`, ...props.pagination }}
      locale={{ emptyText: <EmptyState />, ...locale }}
      {...props}
    />
  );
}
