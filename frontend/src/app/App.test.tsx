import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { App } from './App';

describe('App', () => {
  it('renders the application shell', async () => {
    render(<App />);
    expect(await screen.findByText('统一监控平台')).toBeInTheDocument();
    expect(await screen.findByText('首页总览')).toBeInTheDocument();
  });
});
