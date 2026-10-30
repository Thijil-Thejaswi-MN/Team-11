import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import InsurerAdminDashboardView from '../views/insurerAdmin/InsurerAdminDashboardView';
import { AuthProvider } from '../context/AuthContext';
import apiClient from '../api/apiClient';

vi.mock('../api/apiClient');

const mockPlans = [
  {
    id: 1,
    planName: 'SafeShield Motor Comprehensive (4W)',
    planType: 'MOTOR_FOUR_WHEELER',
    status: 'ACTIVE',
  },
  {
    id: 2,
    planName: 'SafeShield Health Shield Max',
    planType: 'HEALTH',
    status: 'ACTIVE',
  },
  {
    id: 3,
    planName: 'SafeShield Two-Wheeler Protect',
    planType: 'MOTOR_TWO_WHEELER',
    status: 'INACTIVE',
  },
];

describe('InsurerAdminDashboardView (INS-F-003, INS-F-021 Deferral, Issue #168)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders deferred metrics notice (Decision D-10) and live plans count on overview tab', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockPlans });

    render(
      <MemoryRouter>
        <AuthProvider>
          <InsurerAdminDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    // Verify deferred notice per D-10 is prominently displayed
    const notice = await screen.findByTestId('metrics-deferred-notice');
    expect(notice).toBeInTheDocument();
    expect(
      screen.getByText(/Portfolio metrics are not part of this release \(dashboards deferred, decision D-10\)/i)
    ).toBeInTheDocument();

    // Verify live plan count rendered from API
    const countElem = await screen.findByTestId('real-plans-count');
    expect(countElem).toHaveTextContent('3');
    expect(screen.getByText(/\(2 Active\)/i)).toBeInTheDocument();

    // Verify NO hardcoded invented numbers appear
    expect(screen.queryByText('1,420')).not.toBeInTheDocument();
    expect(screen.queryByText('₹1.18 Cr')).not.toBeInTheDocument();
    expect(screen.queryByText('62.4%')).not.toBeInTheDocument();
  });

  it('switches to Plans tab and renders live plans table from API', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockPlans });

    render(
      <MemoryRouter>
        <AuthProvider>
          <InsurerAdminDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await screen.findByTestId('real-plans-count');

    // Click on Plans tab
    fireEvent.click(screen.getByTestId('tab-plans'));

    expect(await screen.findByText('SafeShield Motor Comprehensive (4W)')).toBeInTheDocument();
    expect(screen.getByText('SafeShield Health Shield Max')).toBeInTheDocument();
    expect(screen.getByText('SafeShield Two-Wheeler Protect')).toBeInTheDocument();
  });

  it('switches to Cancellations tab and renders empty cancellation queue notice without fake data', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockPlans });

    render(
      <MemoryRouter>
        <AuthProvider>
          <InsurerAdminDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await screen.findByTestId('real-plans-count');

    // Click on Cancellations tab
    fireEvent.click(screen.getByTestId('tab-cancellations'));

    expect(screen.getByText(/No pending policy cancellation requests/i)).toBeInTheDocument();
    expect(screen.queryByText('CAN-2026-001')).not.toBeInTheDocument();
    expect(screen.queryByText('POL-2026-4W-0088')).not.toBeInTheDocument();
  });

  it('handles API error gracefully on plans fetch without crashing', async () => {
    apiClient.get.mockRejectedValueOnce(new Error('Network error loading plans'));

    render(
      <MemoryRouter>
        <AuthProvider>
          <InsurerAdminDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    // Switch to Plans tab to view error
    fireEvent.click(screen.getByTestId('tab-plans'));

    expect(await screen.findByText(/Network error loading plans/i)).toBeInTheDocument();
  });
});

