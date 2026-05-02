import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';
import { App } from './App';

describe('App', () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it('renders the login page before authentication', async () => {
    render(<App />);
    expect(await screen.findByText('Heimdallr')).toBeInTheDocument();
    expect(await screen.findByText('登录身份')).toBeInTheDocument();
  });

  it('logs in and renders the application shell', async () => {
    render(<App />);
    fireEvent.click(await screen.findByRole('button', { name: /登录/ }));
    expect(await screen.findByText('首页总览')).toBeInTheDocument();
  });
});
