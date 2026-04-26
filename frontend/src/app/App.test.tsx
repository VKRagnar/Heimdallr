import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { App } from './App';

describe('App', () => {
  it('renders the application shell', async () => {
    render(<App />);
    expect(await screen.findByText('Heimdallr')).toBeInTheDocument();
    expect(await screen.findByText('首页总览')).toBeInTheDocument();
  });
});
