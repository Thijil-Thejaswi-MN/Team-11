import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import ClaimsOfficerDashboardView from '../views/claimsOfficer/ClaimsOfficerDashboardView';
import apiClient from '../api/apiClient';

vi.mock('../api/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

const mockClaimsList = [
  {
    id: 101,
    claimNo: 'CLM-2026-000101',
    policyId: 10,
    policyNo: 'POL-2026-4W-0091',
    customerId: 1001,
    insurerId: 1,
    claimedAmount: 32000,
    assessedAmount: null,
    incidentDate: '2026-10-18',
    description: 'Front bumper damage due to parking collision with bollard.',
    status: 'DOCUMENT_VERIFICATION',
    duplicateFlag: true, // INS-F-023
    documentIds: [1, 2],
    createdAt: '2026-10-19T10:30:00Z',
    updatedAt: '2026-10-19T10:30:00Z',
  },
  {
    id: 102,
    claimNo: 'CLM-2026-000102',
    policyId: 11,
    policyNo: 'POL-2026-4W-0092',
    customerId: 1002,
    insurerId: 1,
    claimedAmount: 18000,
    assessedAmount: null,
    incidentDate: '2026-10-20',
    description: 'Rear windshield crack from road gravel impact on expressway.',
    status: 'ASSESSMENT',
    duplicateFlag: false,
    documentIds: [3],
    createdAt: '2026-10-21T14:15:00Z',
    updatedAt: '2026-10-22T09:00:00Z',
  },
  {
    id: 103,
    claimNo: 'CLM-2026-000103',
    policyId: 12,
    policyNo: 'POL-2026-4W-0093',
    customerId: 1003,
    insurerId: 1,
    claimedAmount: 45000,
    assessedAmount: 42000,
    incidentDate: '2026-10-15',
    description: 'Emergency appendectomy hospitalization surgery bill.',
    status: 'PAYMENT_PROCESSING',
    duplicateFlag: false,
    documentIds: [4, 5],
    createdAt: '2026-10-16T09:00:00Z',
    updatedAt: '2026-10-23T11:20:00Z',
  },
  {
    id: 104,
    claimNo: 'CLM-2026-000104',
    policyId: 13,
    policyNo: 'POL-2026-4W-0094',
    customerId: 1004,
    insurerId: 1,
    claimedAmount: 12000,
    assessedAmount: 12000,
    incidentDate: '2026-10-10',
    description: 'Minor dental trauma emergency claim.',
    status: 'SETTLED',
    duplicateFlag: false,
    documentIds: [6],
    createdAt: '2026-10-11T08:00:00Z',
    updatedAt: '2026-10-24T16:00:00Z',
  },
  {
    id: 105,
    claimNo: 'CLM-2026-000105',
    policyId: 14,
    policyNo: 'POL-2026-4W-0095',
    customerId: 1005,
    insurerId: 1,
    claimedAmount: 9000,
    assessedAmount: null,
    incidentDate: '2026-09-10',
    description: 'Uncovered pre-existing cosmetic condition.',
    status: 'REJECTED',
    duplicateFlag: false,
    documentIds: [],
    createdAt: '2026-09-12T08:00:00Z',
    updatedAt: '2026-09-15T16:00:00Z',
  },
];

describe('ClaimsOfficerDashboardView (INS-F-014, TC-Clm-03, P1-12)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders claims queue excluding Settled and Rejected claims by default', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockClaimsList });

    render(
      <MemoryRouter>
        <AuthProvider>
          <ClaimsOfficerDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    // Wait for queue to load
    await waitFor(() => {
      expect(screen.getByText('CLM-2026-000101')).toBeInTheDocument();
      expect(screen.getByText('CLM-2026-000102')).toBeInTheDocument();
      expect(screen.getByText('CLM-2026-000103')).toBeInTheDocument();
    });

    // Settled (#104) and Rejected (#105) should be excluded in default ACTIVE view
    expect(screen.queryByText('CLM-2026-000104')).not.toBeInTheDocument();
    expect(screen.queryByText('CLM-2026-000105')).not.toBeInTheDocument();

    // Verify Active Queue count indicator (3 active)
    expect(screen.getByText(/3 Claims/i)).toBeInTheDocument();
  });

  it('displays "Possible duplicate" badge for duplicate claims (INS-F-023)', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockClaimsList });

    render(
      <MemoryRouter>
        <AuthProvider>
          <ClaimsOfficerDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('duplicate-badge-101')).toBeInTheDocument();
    });

    expect(screen.getByTestId('duplicate-badge-101')).toHaveTextContent(/Possible Duplicate/i);
    expect(screen.queryByTestId('duplicate-badge-102')).not.toBeInTheDocument();
  });

  it('shows all claims including Settled and Rejected when filter is changed to ALL', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockClaimsList });

    render(
      <MemoryRouter>
        <AuthProvider>
          <ClaimsOfficerDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('CLM-2026-000101')).toBeInTheDocument();
    });

    // Change stage filter to ALL
    const filterSelect = screen.getByTestId('claim-stage-filter');
    fireEvent.change(filterSelect, { target: { value: 'ALL' } });

    await waitFor(() => {
      expect(screen.getByText('CLM-2026-000104')).toBeInTheDocument();
      expect(screen.getByText('CLM-2026-000105')).toBeInTheDocument();
    });
  });

  it('filters claims by search query (Claim No / Description / Policy)', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockClaimsList });

    render(
      <MemoryRouter>
        <AuthProvider>
          <ClaimsOfficerDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('CLM-2026-000101')).toBeInTheDocument();
    });

    const searchInput = screen.getByTestId('claim-search-input');
    fireEvent.change(searchInput, { target: { value: 'windshield' } });

    expect(screen.queryByText('CLM-2026-000101')).not.toBeInTheDocument();
    expect(screen.getByText('CLM-2026-000102')).toBeInTheDocument();
  });

  it('opens claim detail modal with history and duplicate warning banner', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockClaimsList });

    render(
      <MemoryRouter>
        <AuthProvider>
          <ClaimsOfficerDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('view-details-btn-101')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('view-details-btn-101'));

    // Modal opens
    expect(screen.getByTestId('claim-detail-modal')).toBeInTheDocument();
    expect(screen.getByText(/Claim Detail — CLM-2026-000101/i)).toBeInTheDocument();

    // Prominent duplicate warning banner per INS-F-023
    expect(screen.getByTestId('duplicate-warning-banner')).toBeInTheDocument();
    expect(
      screen.getByText(/Possible Duplicate Claim Detected \(INS-F-023\)/i)
    ).toBeInTheDocument();
  });

  it('submits verification with documents complete -> moves claim to Assessment', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockClaimsList });
    apiClient.post.mockResolvedValueOnce({
      data: {
        claimId: 101,
        claimNo: 'CLM-2026-000101',
        status: 'ASSESSMENT',
        message: 'Claim documents verified; moved to Assessment',
      },
    });

    render(
      <MemoryRouter>
        <AuthProvider>
          <ClaimsOfficerDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('verify-btn-101')).toBeInTheDocument();
    });

    // Open verify modal
    fireEvent.click(screen.getByTestId('verify-btn-101'));
    expect(screen.getByTestId('verify-modal')).toBeInTheDocument();

    // Fill notes
    fireEvent.change(screen.getByTestId('verify-notes-input'), {
      target: { value: 'All garage bills and inspection photos verified.' },
    });

    // Submit
    fireEvent.click(screen.getByTestId('submit-verify-btn'));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith('/claims/101/verify', {
        documentsComplete: true,
        missing: [],
        notes: 'All garage bills and inspection photos verified.',
      });
    });

    // Success banner appears
    await waitFor(() => {
      expect(screen.getByText(/Documents verified -> Moved to Assessment/i)).toBeInTheDocument();
    });
  });

  it('submits verification requesting additional documents -> remains in Document Verification', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockClaimsList });
    apiClient.post.mockResolvedValueOnce({
      data: {
        claimId: 101,
        claimNo: 'CLM-2026-000101',
        status: 'DOCUMENT_VERIFICATION',
        message: 'Additional documents requested; remains in Document Verification',
      },
    });

    render(
      <MemoryRouter>
        <AuthProvider>
          <ClaimsOfficerDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('verify-btn-101')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('verify-btn-101'));

    // Switch to Request Additional Documents
    fireEvent.click(screen.getByTestId('docs-complete-false'));

    // Enter missing files
    fireEvent.change(screen.getByTestId('missing-docs-input'), {
      target: { value: 'Police FIR copy, Original surveyor report' },
    });

    fireEvent.change(screen.getByTestId('verify-notes-input'), {
      target: { value: 'Awaiting original police statement.' },
    });

    fireEvent.click(screen.getByTestId('submit-verify-btn'));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith('/claims/101/verify', {
        documentsComplete: false,
        missing: ['Police FIR copy', 'Original surveyor report'],
        notes: 'Awaiting original police statement.',
      });
    });

    await waitFor(() => {
      expect(
        screen.getByText(/Additional documents requested -> Remains in Document Verification/i)
      ).toBeInTheDocument();
    });
  });

  it('submits Approve decision with assessed amount & mandatory reason -> moves to Payment Processing', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockClaimsList });
    apiClient.post.mockResolvedValueOnce({
      data: {
        claimId: 102,
        claimNo: 'CLM-2026-000102',
        status: 'PAYMENT_PROCESSING',
        assessedAmount: 16500.0,
        message: 'Claim approved and moved to Payment Processing',
      },
    });

    render(
      <MemoryRouter>
        <AuthProvider>
          <ClaimsOfficerDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('decide-btn-102')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('decide-btn-102'));
    expect(screen.getByTestId('decision-modal')).toBeInTheDocument();

    // Fill assessed amount
    fireEvent.change(screen.getByTestId('assessed-amount-input'), {
      target: { value: '16500' },
    });

    // Fill mandatory reason
    fireEvent.change(screen.getByTestId('decision-reason-input'), {
      target: { value: 'Surveyor verified windshield damage; deductible applied.' },
    });

    fireEvent.click(screen.getByTestId('submit-decision-btn'));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith('/claims/102/decision', {
        decision: 'Approve',
        assessedAmount: 16500,
        reason: 'Surveyor verified windshield damage; deductible applied.',
      });
    });

    await waitFor(() => {
      expect(screen.getByText(/Approved -> Moved to Payment Processing/i)).toBeInTheDocument();
    });
  });

  it('submits Reject decision with reason -> moves to REJECTED', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockClaimsList });
    apiClient.post.mockResolvedValueOnce({
      data: {
        claimId: 102,
        claimNo: 'CLM-2026-000102',
        status: 'REJECTED',
        assessedAmount: null,
        message: 'Claim rejected',
      },
    });

    render(
      <MemoryRouter>
        <AuthProvider>
          <ClaimsOfficerDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('decide-btn-102')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('decide-btn-102'));

    // Switch to Reject
    fireEvent.click(screen.getByTestId('decision-reject-radio'));

    // Fill reason
    fireEvent.change(screen.getByTestId('decision-reason-input'), {
      target: { value: 'Claim incident occurred prior to policy inception date.' },
    });

    fireEvent.click(screen.getByTestId('submit-decision-btn'));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith('/claims/102/decision', {
        decision: 'Reject',
        assessedAmount: null,
        reason: 'Claim incident occurred prior to policy inception date.',
      });
    });

    await waitFor(() => {
      expect(screen.getByText(/Rejected with audit justification/i)).toBeInTheDocument();
    });
  });

  it('displays validation error banner (422) when decision reason is empty', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockClaimsList });

    render(
      <MemoryRouter>
        <AuthProvider>
          <ClaimsOfficerDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('decide-btn-102')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('decide-btn-102'));

    // Submit without reason
    fireEvent.click(screen.getByTestId('submit-decision-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('decision-error-banner')).toBeInTheDocument();
    });
    expect(screen.getByText(/decision reason is strictly mandatory/i)).toBeInTheDocument();
    expect(apiClient.post).not.toHaveBeenCalled();
  });

  it('displays state conflict (409) error banner from backend', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockClaimsList });
    apiClient.post.mockRejectedValueOnce({
      status: 409,
      message: 'Claim cannot transition from DOCUMENT_VERIFICATION to PAYMENT_PROCESSING',
    });

    render(
      <MemoryRouter>
        <AuthProvider>
          <ClaimsOfficerDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('decide-btn-102')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('decide-btn-102'));

    fireEvent.change(screen.getByTestId('decision-reason-input'), {
      target: { value: 'Approved prematurely.' },
    });

    fireEvent.click(screen.getByTestId('submit-decision-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('decision-error-banner')).toBeInTheDocument();
    });
    expect(screen.getByText(/State Conflict \(409\)/i)).toBeInTheDocument();
  });

  it('displays access forbidden (403) error banner on cross-insurer isolation failure', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockClaimsList });
    apiClient.post.mockRejectedValueOnce({
      status: 403,
      message: 'Access denied: insurer isolation violation.',
    });

    render(
      <MemoryRouter>
        <AuthProvider>
          <ClaimsOfficerDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('verify-btn-101')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('verify-btn-101'));
    fireEvent.click(screen.getByTestId('submit-verify-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('verify-error-banner')).toBeInTheDocument();
    });
    expect(screen.getByText(/Access Forbidden \(403\)/i)).toBeInTheDocument();
  });
});
