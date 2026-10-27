import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import InsurerAdminPlansView from '../views/insurerAdmin/InsurerAdminPlansView';
import apiClient from '../api/apiClient';

vi.mock('../api/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
  setAuthToken: vi.fn(),
  getAuthToken: vi.fn(),
  clearAuthToken: vi.fn(),
  onUnauthorized: vi.fn(),
}));

const MOCK_PLANS = [
  {
    planId: 10,
    id: 10,
    code: 'PLAN-SAFE-4W-01',
    insurerId: 1,
    insurerName: 'SafeShield General',
    type: 'MOTOR_FOUR_WHEELER',
    name: 'SafeShield Car Comprehensive',
    coverage: 500000,
    deductible: 1000,
    waitingPeriodMonths: 0,
    basePremium: 3500,
    premiumRulesJson: '{"baseRatePercent": 2.5}',
    underwritingRulesJson: '{}',
    addOns: ['ZERO_DEP'],
    active: true,
  },
  {
    planId: 11,
    id: 11,
    code: 'PLAN-SAFE-2W-01',
    insurerId: 1,
    insurerName: 'SafeShield General',
    type: 'MOTOR_TWO_WHEELER',
    name: 'SafeShield Two-Wheeler Protect',
    coverage: 100000,
    deductible: 500,
    waitingPeriodMonths: 0,
    basePremium: 1200,
    premiumRulesJson: '{"baseRatePercent": 3.0}',
    underwritingRulesJson: '{}',
    addOns: [],
    active: false,
  },
];

function renderView() {
  return render(
    <BrowserRouter>
      <AuthProvider>
        <InsurerAdminPlansView />
      </AuthProvider>
    </BrowserRouter>
  );
}

describe('InsurerAdminPlansView (INS-F-003, INS-SR-003, TC-Plan-01)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiClient.get.mockResolvedValue({ data: { items: MOCK_PLANS } });
  });

  it('renders plans catalog table with active and inactive plans', async () => {
    renderView();

    await waitFor(() => {
      expect(screen.getByText('PLAN-SAFE-4W-01')).toBeInTheDocument();
      expect(screen.getByText('SafeShield Car Comprehensive')).toBeInTheDocument();
      expect(screen.getByText('PLAN-SAFE-2W-01')).toBeInTheDocument();
      expect(screen.getByText('SafeShield Two-Wheeler Protect')).toBeInTheDocument();
    });

    // Check status badges
    expect(screen.getByText('Active')).toBeInTheDocument();
    expect(screen.getByText('Inactive')).toBeInTheDocument();
  });

  it('TC-Plan-01 Step 1: creates a new plan via POST /plans', async () => {
    apiClient.post.mockResolvedValueOnce({ data: { planId: 12 } });

    renderView();

    await waitFor(() => {
      expect(screen.getByText('PLAN-SAFE-4W-01')).toBeInTheDocument();
    });

    // Open create form
    fireEvent.click(screen.getByRole('button', { name: /\+ Create New Plan/i }));

    // Fill fields
    fireEvent.change(screen.getByLabelText(/Plan Code/i), { target: { value: 'PLAN-SAFE-4W-02' } });
    fireEvent.change(screen.getByLabelText(/Plan Name/i), { target: { value: 'SafeShield Platinum 4W' } });
    fireEvent.change(screen.getByLabelText(/Plan Type/i), { target: { value: 'MOTOR_FOUR_WHEELER' } });
    fireEvent.change(screen.getByLabelText(/Coverage Amount/i), { target: { value: '800000' } });
    fireEvent.change(screen.getByLabelText(/Deductible/i), { target: { value: '1500' } });
    fireEvent.change(screen.getByLabelText(/Base Premium/i), { target: { value: '4200' } });

    // Submit form
    fireEvent.click(screen.getByRole('button', { name: /Create Plan/i }));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith('/plans', expect.objectContaining({
        code: 'PLAN-SAFE-4W-02',
        name: 'SafeShield Platinum 4W',
        type: 'MOTOR_FOUR_WHEELER',
        coverage: 800000,
        deductible: 1500,
        basePremium: 4200,
        active: true,
      }));
      expect(screen.getByText(/Plan PLAN-SAFE-4W-02 created successfully \(ID: 12\)/i)).toBeInTheDocument();
    });
  });

  it('TC-Plan-01 Step 3: deactivates an active plan via PATCH /plans/{id}', async () => {
    apiClient.patch.mockResolvedValueOnce({
      data: { plan: { ...MOCK_PLANS[0], active: false } },
    });

    renderView();

    await waitFor(() => {
      expect(screen.getByText('PLAN-SAFE-4W-01')).toBeInTheDocument();
    });

    const deactivateBtn = screen.getByRole('button', { name: /Deactivate/i });
    fireEvent.click(deactivateBtn);

    await waitFor(() => {
      expect(apiClient.patch).toHaveBeenCalledWith('/plans/10', { active: false });
      expect(screen.getByText(/Plan PLAN-SAFE-4W-01 deactivated/i)).toBeInTheDocument();
    });
  });

  it('TC-Plan-01 Step 4: displays 403 error when updating another insurer plan', async () => {
    const error403 = new Error('Forbidden');
    error403.status = 403;
    error403.errorCode = 'ACCESS-403';
    error403.response = {
      status: 403,
      data: {
        errorCode: 'ACCESS-403',
        message: 'Access denied: Insurer isolation violation. You cannot modify plans belonging to another insurer.',
      },
    };
    apiClient.put.mockRejectedValueOnce(error403);

    renderView();

    await waitFor(() => {
      expect(screen.getByText('PLAN-SAFE-4W-01')).toBeInTheDocument();
    });

    // Click Edit on plan 10
    const editButtons = screen.getAllByRole('button', { name: /Edit/i });
    fireEvent.click(editButtons[0]);

    // Save changes
    fireEvent.click(screen.getByRole('button', { name: /Update Plan/i }));

    await waitFor(() => {
      expect(apiClient.put).toHaveBeenCalledWith('/plans/10', expect.any(Object));
      expect(screen.getByText(/Access denied: Insurer isolation violation/i)).toBeInTheDocument();
    });
  });

  it('TC-Plan-01 Step 5: shows validation error when coverage or basePremium is invalid', async () => {
    renderView();

    await waitFor(() => {
      expect(screen.getByText('PLAN-SAFE-4W-01')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /\+ Create New Plan/i }));

    // Fill invalid coverage (negative)
    fireEvent.change(screen.getByLabelText(/Plan Code/i), { target: { value: 'PLAN-TEST' } });
    fireEvent.change(screen.getByLabelText(/Plan Name/i), { target: { value: 'Test Plan' } });
    fireEvent.change(screen.getByLabelText(/Coverage Amount/i), { target: { value: '-100' } });

    fireEvent.click(screen.getByRole('button', { name: /Create Plan/i }));

    await waitFor(() => {
      expect(screen.getByText(/Coverage amount must be greater than zero/i)).toBeInTheDocument();
      expect(apiClient.post).not.toHaveBeenCalled();
    });
  });
});
