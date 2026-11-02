import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import CustomerClaimsView from '../views/customer/CustomerClaimsView';
import apiClient from '../api/apiClient';

vi.mock('../api/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
  onUnauthorized: vi.fn(() => () => {}),
  setAuthToken: vi.fn(),
  getAuthToken: vi.fn(),
}));

const mockPolicies = [
  {
    id: 101,
    policyNo: 'POL-2026-4W-0091',
    planName: 'SafeShield Motor Comprehensive',
    vehicleRegNo: 'KA-01-AB-1234',
    status: 'ACTIVE',
    premiumPaid: 12000.0,
    startDate: '2026-01-01',
    endDate: '2026-12-31',
  },
  {
    id: 102,
    policyNo: 'POL-2026-HL-0092',
    planName: 'HealthCare Plus Floater',
    vehicleRegNo: null,
    status: 'EXPIRED',
    premiumPaid: 15000.0,
    startDate: '2025-01-01',
    endDate: '2025-12-31',
  },
];

const mockClaimInVerification = {
  id: 1,
  claimNo: 'CLM-2026-000001',
  policyId: 101,
  policyNo: 'POL-2026-4W-0091',
  customerId: 1001,
  insurerId: 1,
  incidentDate: '2026-10-20',
  claimedAmount: 25000.0,
  assessedAmount: null,
  status: 'DOCUMENT_VERIFICATION',
  description: 'Front bumper damage from parking post collision.',
  reason: null,
  duplicateFlag: false,
  documentIds: [],
  createdAt: '2026-10-21T10:00:00Z',
};

const mockClaimDuplicate = {
  id: 2,
  claimNo: 'CLM-2026-000002',
  policyId: 101,
  policyNo: 'POL-2026-4W-0091',
  customerId: 1001,
  insurerId: 1,
  incidentDate: '2026-10-20',
  claimedAmount: 25000.0,
  assessedAmount: null,
  status: 'DOCUMENT_VERIFICATION',
  description: 'Second claim submission for same damage.',
  reason: null,
  duplicateFlag: true, // INS-F-023
  documentIds: [],
  createdAt: '2026-10-21T11:00:00Z',
};

const mockClaimAssessment = {
  id: 3,
  claimNo: 'CLM-2026-000003',
  policyId: 101,
  policyNo: 'POL-2026-4W-0091',
  customerId: 1001,
  insurerId: 1,
  incidentDate: '2026-10-15',
  claimedAmount: 40000.0,
  assessedAmount: 38000.0,
  status: 'ASSESSMENT',
  description: 'Windshield crack from highway flying stone.',
  reason: 'Surveyor assigned: Rajesh Kumar (Ref #SRV-991)',
  duplicateFlag: false,
  documentIds: [],
  createdAt: '2026-10-16T09:00:00Z',
};

const mockClaimSettled = {
  id: 4,
  claimNo: 'CLM-2026-000004',
  policyId: 101,
  policyNo: 'POL-2026-4W-0091',
  customerId: 1001,
  insurerId: 1,
  incidentDate: '2026-09-10',
  claimedAmount: 18000.0,
  assessedAmount: 17500.0,
  status: 'SETTLED',
  description: 'Minor rear side dent repaired at authorized network garage.',
  reason: 'Approved by Claims Officer; NEFT disbursement completed.',
  duplicateFlag: false,
  documentIds: [],
  createdAt: '2026-09-11T09:00:00Z',
};

const mockClaimRejected = {
  id: 5,
  claimNo: 'CLM-2026-000005',
  policyId: 101,
  policyNo: 'POL-2026-4W-0091',
  customerId: 1001,
  insurerId: 1,
  incidentDate: '2026-08-01',
  claimedAmount: 50000.0,
  assessedAmount: null,
  status: 'REJECTED',
  description: 'Flood damage claim.',
  reason: 'Incident occurred outside territorial limits specified in Section 2(b).',
  duplicateFlag: false,
  documentIds: [],
  createdAt: '2026-08-05T09:00:00Z',
};

function renderComponent() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <CustomerClaimsView />
      </AuthProvider>
    </MemoryRouter>
  );
}

describe('CustomerClaimsView Component (P4-16 / INS-F-012, INS-F-013, INS-F-015, INS-F-023)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders Claims Tracker & Filing Desk with Track and File tabs', async () => {
    apiClient.get.mockImplementation((url) => {
      if (url === '/policies') return Promise.resolve({ data: mockPolicies });
      if (url.startsWith('/claims/policy/')) return Promise.resolve({ data: [mockClaimInVerification] });
      return Promise.resolve({ data: [] });
    });

    renderComponent();

    expect(screen.getByText('Claims Tracker & Filing Desk')).toBeInTheDocument();
    expect(screen.getByTestId('tab-track-claims')).toBeInTheDocument();
    expect(screen.getByTestId('tab-file-claim')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByTestId('claim-details-card')).toBeInTheDocument();
    });
  });

  it('displays claim details and state machine progress timeline for in-flight claim', async () => {
    apiClient.get.mockImplementation((url) => {
      if (url === '/policies') return Promise.resolve({ data: mockPolicies });
      if (url.startsWith('/claims/policy/')) return Promise.resolve({ data: [mockClaimInVerification] });
      return Promise.resolve({ data: [] });
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getAllByText(/CLM-2026-000001/i).length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText('POL-2026-4W-0091').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('₹25,000')).toBeInTheDocument();
      expect(screen.getByTestId('claim-status-badge')).toHaveTextContent('Document Verification');
    });

    // Check timeline is rendered
    expect(screen.getByTestId('claim-timeline')).toBeInTheDocument();
    expect(screen.getByText('1. Submitted')).toBeInTheDocument();
    expect(screen.getByText('2. Doc Verification')).toBeInTheDocument();
    expect(screen.getByText('3. Assessment')).toBeInTheDocument();
    expect(screen.getByText('4. Payout Processing')).toBeInTheDocument();
    expect(screen.getByText('5. Settlement')).toBeInTheDocument();
  });

  it('displays duplicate claim badge and warning banner when duplicateFlag is true (INS-F-023)', async () => {
    apiClient.get.mockImplementation((url) => {
      if (url === '/policies') return Promise.resolve({ data: mockPolicies });
      if (url.startsWith('/claims/policy/')) return Promise.resolve({ data: [mockClaimDuplicate] });
      return Promise.resolve({ data: [] });
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByTestId('claim-duplicate-badge')).toBeInTheDocument();
      expect(screen.getByTestId('duplicate-alert-banner')).toBeInTheDocument();
      expect(screen.getByTestId('duplicate-alert-banner')).toHaveTextContent(/Possible Duplicate Detected \(INS-F-023\)/i);
    });
  });

  it('renders timeline with Assessment stage and surveyor reason', async () => {
    apiClient.get.mockImplementation((url) => {
      if (url === '/policies') return Promise.resolve({ data: mockPolicies });
      if (url.startsWith('/claims/policy/')) return Promise.resolve({ data: [mockClaimAssessment] });
      return Promise.resolve({ data: [] });
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getAllByText(/CLM-2026-000003/i).length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('₹38,000')).toBeInTheDocument();
      expect(screen.getByText(/Surveyor assigned: Rajesh Kumar/i)).toBeInTheDocument();
    });
  });

  it('renders Settled status resolution on timeline', async () => {
    apiClient.get.mockImplementation((url) => {
      if (url === '/policies') return Promise.resolve({ data: mockPolicies });
      if (url.startsWith('/claims/policy/')) return Promise.resolve({ data: [mockClaimSettled] });
      return Promise.resolve({ data: [] });
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('5. Settled')).toBeInTheDocument();
      expect(screen.getByText('Disbursed')).toBeInTheDocument();
    });
  });

  it('renders Rejected status resolution and officer note on timeline', async () => {
    apiClient.get.mockImplementation((url) => {
      if (url === '/policies') return Promise.resolve({ data: mockPolicies });
      if (url.startsWith('/claims/policy/')) return Promise.resolve({ data: [mockClaimRejected] });
      return Promise.resolve({ data: [] });
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('5. Rejected')).toBeInTheDocument();
      expect(screen.getByText(/Incident occurred outside territorial limits/i)).toBeInTheDocument();
    });
  });

  it('allows looking up a claim by ID', async () => {
    apiClient.get.mockImplementation((url) => {
      if (url === '/policies') return Promise.resolve({ data: mockPolicies });
      if (url.startsWith('/claims/policy/')) return Promise.resolve({ data: [] });
      if (url === '/claims/3') return Promise.resolve({ data: mockClaimAssessment });
      return Promise.resolve({ data: [] });
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByTestId('no-claims-state')).toBeInTheDocument();
    });

    const input = screen.getByTestId('claim-lookup-input');
    const button = screen.getByTestId('claim-lookup-btn');

    fireEvent.change(input, { target: { value: '3' } });
    fireEvent.click(button);

    await waitFor(() => {
      expect(apiClient.get).toHaveBeenCalledWith('/claims/3');
      expect(screen.getAllByText(/CLM-2026-000003/i).length).toBeGreaterThanOrEqual(1);
    });
  });

  it('switches to File New Claim tab and loads active policies', async () => {
    apiClient.get.mockImplementation((url) => {
      if (url === '/policies') return Promise.resolve({ data: mockPolicies });
      return Promise.resolve({ data: [] });
    });

    renderComponent();

    fireEvent.click(screen.getByTestId('tab-file-claim'));

    await waitFor(() => {
      expect(screen.getByTestId('file-claim-card')).toBeInTheDocument();
    });

    const select = screen.getByTestId('claim-policy-select');
    // Only ACTIVE policy (101) should be present, not EXPIRED (102)
    expect(select).toBeInTheDocument();
    expect(screen.getByText(/POL-2026-4W-0091 \(KA-01-AB-1234\)/i)).toBeInTheDocument();
  });

  it('validates required fields and boundary rules on filing submission', async () => {
    apiClient.get.mockImplementation((url) => {
      if (url === '/policies') return Promise.resolve({ data: mockPolicies });
      return Promise.resolve({ data: [] });
    });

    renderComponent();

    fireEvent.click(screen.getByTestId('tab-file-claim'));

    await waitFor(() => {
      expect(screen.getByTestId('claim-submit-btn')).toBeInTheDocument();
    });

    // Try submitting without incident date
    fireEvent.click(screen.getByTestId('claim-submit-btn'));
    expect(screen.getByTestId('claim-form-error-alert')).toHaveTextContent(/Incident date is required/i);

    // Try submitting with future incident date
    const futureDate = new Date(Date.now() + 86400000 * 5).toISOString().split('T')[0];
    fireEvent.change(screen.getByTestId('claim-incident-date-input'), { target: { value: futureDate } });
    fireEvent.click(screen.getByTestId('claim-submit-btn'));
    expect(screen.getByTestId('claim-form-error-alert')).toHaveTextContent(/cannot be in the future/i);

    // Try submitting with incident date > 30 days ago
    const past45Date = new Date(Date.now() - 86400000 * 45).toISOString().split('T')[0];
    fireEvent.change(screen.getByTestId('claim-incident-date-input'), { target: { value: past45Date } });
    fireEvent.click(screen.getByTestId('claim-submit-btn'));
    expect(screen.getByTestId('claim-form-error-alert')).toHaveTextContent(/within 30 days of filing/i);

    // Enter valid date within 30 days
    const validDate = new Date(Date.now() - 86400000 * 5).toISOString().split('T')[0];
    fireEvent.change(screen.getByTestId('claim-incident-date-input'), { target: { value: validDate } });

    // Try submitting without amount
    fireEvent.click(screen.getByTestId('claim-submit-btn'));
    expect(screen.getByTestId('claim-form-error-alert')).toHaveTextContent(/greater than ₹0/i);

    // Enter valid amount
    fireEvent.change(screen.getByTestId('claim-amount-input'), { target: { value: '25000' } });

    // Try submitting without description
    fireEvent.click(screen.getByTestId('claim-submit-btn'));
    expect(screen.getByTestId('claim-form-error-alert')).toHaveTextContent(/description/i);
  });

  it('submits claim successfully via POST /claims and displays confirmation banner', async () => {
    apiClient.get.mockImplementation((url) => {
      if (url === '/policies') return Promise.resolve({ data: mockPolicies });
      if (url === '/claims/99') {
        return Promise.resolve({
          data: {
            id: 99,
            claimNo: 'CLM-2026-000099',
            policyId: 101,
            policyNo: 'POL-2026-4W-0091',
            status: 'DOCUMENT_VERIFICATION',
            claimedAmount: 25000,
            incidentDate: '2026-10-25',
            description: 'Rear bumper scratch while reversing.',
            duplicateFlag: false,
          },
        });
      }
      return Promise.resolve({ data: [] });
    });

    apiClient.post.mockResolvedValueOnce({
      data: {
        claimId: 99,
        claimNo: 'CLM-2026-000099',
        policyId: 101,
        status: 'DOCUMENT_VERIFICATION',
        claimedAmount: 25000.0,
        assessedAmount: null,
        duplicateFlag: false,
        message: 'Claim submitted successfully and under document verification',
      },
    });

    renderComponent();

    fireEvent.click(screen.getByTestId('tab-file-claim'));

    await waitFor(() => {
      expect(screen.getByTestId('claim-submit-btn')).toBeInTheDocument();
    });

    const validDate = new Date(Date.now() - 86400000 * 3).toISOString().split('T')[0];
    fireEvent.change(screen.getByTestId('claim-incident-date-input'), { target: { value: validDate } });
    fireEvent.change(screen.getByTestId('claim-amount-input'), { target: { value: '25000' } });
    fireEvent.change(screen.getByTestId('claim-description-input'), {
      target: { value: 'Rear bumper scratch while reversing.' },
    });

    fireEvent.click(screen.getByTestId('claim-submit-btn'));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith('/claims', {
        policyId: 101,
        incidentDate: validDate,
        claimedAmount: 25000,
        description: 'Rear bumper scratch while reversing.',
        documentIds: [],
      });
    });

    await waitFor(() => {
      expect(screen.getByTestId('claim-submit-success-alert')).toBeInTheDocument();
      expect(screen.getByTestId('claim-submit-success-alert')).toHaveTextContent(/CLM-2026-000099/i);
      expect(screen.getByTestId('claim-submit-success-alert')).toHaveTextContent(/Document Verification/i);
    });

    // Clicking 'View Claim on Timeline' switches back to TRACK tab
    fireEvent.click(screen.getByTestId('view-timeline-after-submit-btn'));
    expect(screen.getByTestId('claim-details-card')).toBeInTheDocument();
  });

  it('handles backend conflict or validation errors on submission', async () => {
    apiClient.get.mockImplementation((url) => {
      if (url === '/policies') return Promise.resolve({ data: mockPolicies });
      return Promise.resolve({ data: [] });
    });

    apiClient.post.mockRejectedValueOnce({
      response: {
        status: 409,
        data: { message: 'Policy is not active (current status: SUSPENDED).' },
      },
    });

    renderComponent();

    fireEvent.click(screen.getByTestId('tab-file-claim'));

    await waitFor(() => {
      expect(screen.getByTestId('claim-submit-btn')).toBeInTheDocument();
    });

    const validDate = new Date(Date.now() - 86400000 * 2).toISOString().split('T')[0];
    fireEvent.change(screen.getByTestId('claim-incident-date-input'), { target: { value: validDate } });
    fireEvent.change(screen.getByTestId('claim-amount-input'), { target: { value: '15000' } });
    fireEvent.change(screen.getByTestId('claim-description-input'), {
      target: { value: 'Collision with streetlight post.' },
    });

    fireEvent.click(screen.getByTestId('claim-submit-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('claim-form-error-alert')).toBeInTheDocument();
      expect(screen.getByText(/State Conflict \(409\): Policy is not active/i)).toBeInTheDocument();
    });
  });
});
