import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import UnderwriterDashboardView from '../views/underwriter/UnderwriterDashboardView';
import { AuthProvider } from '../context/AuthContext';
import apiClient from '../api/apiClient';

vi.mock('../api/apiClient');

const mockReferredApps = [
  {
    id: 8812,
    customerId: 1001,
    insurerId: 1,
    planId: 2,
    quoteId: 501,
    productType: 'HEALTH',
    vehicleRegNo: null,
    premium: 12500,
    reason: 'Applicant age is 62 years (exceeds auto-approval limit of 60 years)',
    detailsJson: '{"age":62,"applicantAge":62,"hasPreExisting":true,"sumInsured":1000000}',
    status: 'REFERRED',
    createdAt: '2026-10-26T08:00:00Z',
  },
  {
    id: 8815,
    customerId: 1002,
    insurerId: 1,
    planId: 1,
    quoteId: 502,
    productType: 'MOTOR_FOUR_WHEELER',
    vehicleRegNo: 'KA03HA9988',
    premium: 18450,
    reason: 'Vehicle age is 12 years (exceeds auto-approve threshold of 10 years)',
    detailsJson: '{"vehicleAge":12,"idv":750000,"make":"Honda","model":"City","city":"Bangalore"}',
    status: 'REFERRED',
    createdAt: '2026-10-26T07:00:00Z',
  },
];

describe('UnderwriterDashboardView Component (P1-11)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders queue with referred applications and displays triggered referral rules', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockReferredApps });

    render(
      <MemoryRouter>
        <AuthProvider>
          <UnderwriterDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('APP-8812')).toBeInTheDocument();
      expect(screen.getByText('APP-8815')).toBeInTheDocument();
    });

    // Check referral trigger reasons
    expect(
      screen.getByText(/Applicant age is 62 years \(exceeds auto-approval limit of 60 years\)/)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Vehicle age is 12 years \(exceeds auto-approve threshold of 10 years\)/)
    ).toBeInTheDocument();

    // Check action buttons exist
    expect(screen.getByTestId('approve-btn-8812')).toBeInTheDocument();
    expect(screen.getByTestId('reject-btn-8812')).toBeInTheDocument();
  });

  it('filters referred applications by search query', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockReferredApps });

    render(
      <MemoryRouter>
        <AuthProvider>
          <UnderwriterDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('APP-8812')).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText(/Search by App ID, Vehicle Reg, Rule.../i);
    fireEvent.change(searchInput, { target: { value: 'KA03HA' } });

    expect(screen.queryByText('APP-8812')).not.toBeInTheDocument();
    expect(screen.getByText('APP-8815')).toBeInTheDocument();
  });

  it('submits manual APPROVE decision with required reason and updates queue', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockReferredApps });
    apiClient.post.mockResolvedValueOnce({
      data: {
        applicationId: 8812,
        status: 'APPROVED',
        reason: 'Senior medical underwriter review passed with standard loading.',
      },
    });

    render(
      <MemoryRouter>
        <AuthProvider>
          <UnderwriterDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('APP-8812')).toBeInTheDocument();
    });

    // Click Approve on application 8812
    const approveBtn = screen.getByTestId('approve-btn-8812');
    fireEvent.click(approveBtn);

    // Modal should open
    expect(screen.getByText(/Underwriter Review — Application APP-8812/i)).toBeInTheDocument();

    // Fill in required justification reason
    const reasonTextarea = screen.getByTestId('reason-textarea');
    fireEvent.change(reasonTextarea, {
      target: { value: 'Senior medical underwriter review passed with standard loading.' },
    });

    // Submit decision
    const submitBtn = screen.getByTestId('submit-decision-btn');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith('/applications/8812/decision', {
        decision: 'APPROVE',
        reason: 'Senior medical underwriter review passed with standard loading.',
      });
    });

    // Application 8812 should be removed from queue
    await waitFor(() => {
      expect(screen.queryByText('APP-8812')).not.toBeInTheDocument();
    });
    expect(screen.getByText(/Application APP-8812 successfully APPROVED/i)).toBeInTheDocument();
  });

  it('submits manual REJECT decision with reason', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockReferredApps });
    apiClient.post.mockResolvedValueOnce({
      data: {
        applicationId: 8815,
        status: 'REJECTED',
        reason: 'Vehicle age exceeds allowable commercial risk parameter.',
      },
    });

    render(
      <MemoryRouter>
        <AuthProvider>
          <UnderwriterDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('APP-8815')).toBeInTheDocument();
    });

    // Click Reject on application 8815
    const rejectBtn = screen.getByTestId('reject-btn-8815');
    fireEvent.click(rejectBtn);

    const reasonTextarea = screen.getByTestId('reason-textarea');
    fireEvent.change(reasonTextarea, {
      target: { value: 'Vehicle age exceeds allowable commercial risk parameter.' },
    });

    const submitBtn = screen.getByTestId('submit-decision-btn');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith('/applications/8815/decision', {
        decision: 'REJECT',
        reason: 'Vehicle age exceeds allowable commercial risk parameter.',
      });
    });

    await waitFor(() => {
      expect(screen.queryByText('APP-8815')).not.toBeInTheDocument();
    });
    expect(screen.getByText(/Application APP-8815 successfully REJECTED/i)).toBeInTheDocument();
  });

  it('displays validation error banner (422) when reason is empty', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockReferredApps });

    render(
      <MemoryRouter>
        <AuthProvider>
          <UnderwriterDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('APP-8812')).toBeInTheDocument();
    });

    const approveBtn = screen.getByTestId('approve-btn-8812');
    fireEvent.click(approveBtn);

    // Leave reason empty and submit
    const submitBtn = screen.getByTestId('submit-decision-btn');
    fireEvent.click(submitBtn);

    // 422 banner should appear
    await waitFor(() => {
      expect(screen.getByText(/Validation Error \(HTTP 422\)/i)).toBeInTheDocument();
    });
    expect(
      screen.getByText(/Underwriter justification reason is strictly required/i)
    ).toBeInTheDocument();
    expect(apiClient.post).not.toHaveBeenCalled();
  });

  it('displays state conflict error banner (409) when application is no longer in REFERRED state', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockReferredApps });
    apiClient.post.mockRejectedValueOnce({
      status: 409,
      errorCode: 'STATE_CONFLICT',
      response: {
        status: 409,
        data: {
          errorCode: 'STATE_CONFLICT',
          message: 'Invalid application state transition: Application is already APPROVED.',
        },
      },
    });

    render(
      <MemoryRouter>
        <AuthProvider>
          <UnderwriterDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('APP-8812')).toBeInTheDocument();
    });

    const approveBtn = screen.getByTestId('approve-btn-8812');
    fireEvent.click(approveBtn);

    const reasonTextarea = screen.getByTestId('reason-textarea');
    fireEvent.change(reasonTextarea, { target: { value: 'Approved by desk officer' } });

    const submitBtn = screen.getByTestId('submit-decision-btn');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/State Conflict \(HTTP 409\)/i)).toBeInTheDocument();
    });
    expect(
      screen.getByText(/Invalid application state transition: Application is already APPROVED/i)
    ).toBeInTheDocument();
  });

  it('displays access forbidden error banner (403) on tenant isolation violation', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockReferredApps });
    apiClient.post.mockRejectedValueOnce({
      status: 403,
      errorCode: 'ACCESS_FORBIDDEN',
      response: {
        status: 403,
        data: {
          errorCode: 'ACCESS_FORBIDDEN',
          message: 'Tenant Isolation Violation: Access denied for another insurer tenant.',
        },
      },
    });

    render(
      <MemoryRouter>
        <AuthProvider>
          <UnderwriterDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('APP-8812')).toBeInTheDocument();
    });

    const approveBtn = screen.getByTestId('approve-btn-8812');
    fireEvent.click(approveBtn);

    const reasonTextarea = screen.getByTestId('reason-textarea');
    fireEvent.change(reasonTextarea, { target: { value: 'Approved by cross tenant' } });

    const submitBtn = screen.getByTestId('submit-decision-btn');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/Access Forbidden \(HTTP 403\)/i)).toBeInTheDocument();
    });
    expect(
      screen.getByText(/Tenant Isolation Violation: Access denied for another insurer tenant/i)
    ).toBeInTheDocument();
  });
});
