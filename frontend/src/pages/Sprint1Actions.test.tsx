import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App as AntApp, ConfigProvider } from 'antd';
import type { ReactNode } from 'react';
import { ApplicationsPage } from './ApplicationsPage';
import { ApplicationAccessPage } from './ApplicationAccessPage';
import { AuditEventsPage } from './AuditEventsPage';

function renderPage(page: ReactNode) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false } },
  });

  return render(
    <ConfigProvider>
      <AntApp>
        <QueryClientProvider client={client}>
          <MemoryRouter>{page}</MemoryRouter>
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>,
  );
}

function clickPrimaryModalButton() {
  const button = document.querySelector<HTMLButtonElement>('.ant-modal-footer .ant-btn-primary');
  expect(button).toBeTruthy();
  fireEvent.click(button as HTMLButtonElement);
}

describe('Sprint 1 action entries', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new TypeError('offline'))));
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it('creates an application from the list entry and exposes an audit event', async () => {
    renderPage(<ApplicationsPage />);

    fireEvent.click(await screen.findByRole('button', { name: /新增应用/ }));
    fireEvent.change(screen.getByLabelText('应用 ID'), { target: { value: 'app-risk-platform' } });
    fireEvent.change(screen.getByLabelText('应用名称'), { target: { value: '风控平台' } });
    fireEvent.change(screen.getByLabelText('应用编码'), { target: { value: 'risk-platform' } });
    fireEvent.change(screen.getByLabelText('业务线'), { target: { value: 'risk' } });
    fireEvent.change(screen.getByLabelText('负责人用户 ID'), { target: { value: 'u-admin' } });
    clickPrimaryModalButton();

    expect(await screen.findByText('风控平台')).toBeInTheDocument();

    cleanup();
    renderPage(<AuditEventsPage />);

    expect(await screen.findByText('APPLICATION_CREATE')).toBeInTheDocument();
    expect(await screen.findByText('风控平台')).toBeInTheDocument();
  });

  it('grants application access and records the operation for audit', async () => {
    renderPage(<ApplicationAccessPage />);

    fireEvent.click((await screen.findAllByRole('button', { name: '授权' }))[0]);
    clickPrimaryModalButton();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

    cleanup();
    renderPage(<AuditEventsPage />);

    expect(await screen.findByText('ACCESS_GRANT_APPLICATION')).toBeInTheDocument();
  });
});
