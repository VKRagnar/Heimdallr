import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { App as AntApp, ConfigProvider } from 'antd';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AlertsPage } from './AlertsPage';

function renderAlertsPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false } },
  });

  return render(
    <ConfigProvider>
      <AntApp>
        <QueryClientProvider client={client}>
          <AlertsPage />
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>,
  );
}

describe('AlertsPage', () => {
  beforeEach(() => {
    window.localStorage.setItem('heimdallr-token', 'admin-token');
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new TypeError('offline'))));
  });

  afterEach(() => {
    cleanup();
    window.localStorage.clear();
    vi.unstubAllGlobals();
  });

  it('renders alert workbench with mock fallback and closes an event with a reason', async () => {
    renderAlertsPage();

    expect(await screen.findByText('Alert Workbench')).toBeInTheDocument();
    expect(await screen.findByText('Kafka lag smoke')).toBeInTheDocument();

    fireEvent.click(await screen.findByRole('button', { name: /Close/ }));
    fireEvent.change(await screen.findByLabelText('Close reason'), { target: { value: 'Consumer lag cleared after restart' } });
    fireEvent.click(await screen.findByRole('button', { name: /Close event/ }));

    await waitFor(() => expect(screen.getAllByText('Closed').length).toBeGreaterThan(0));
  });
});
