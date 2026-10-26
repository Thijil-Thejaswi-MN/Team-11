import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import MyPoliciesView from '../views/customer/MyPoliciesView';
import { AuthProvider } from '../context/AuthContext';
import apiClient from '../api/apiClient';

vi.mock('../api/apiClient');

describe('MyPoliciesView Component (P3-06)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders policy portfolio with policies and statutory notice', async () => {
    apiClient.get.mockResolvedValueOnce({
      data: [
        {
          id: 501,
          policyNo: 'POL-2026-000501',
          applicationId: 9021,
          planName: 'SafeShield Motor 4W Comprehensive',
          productType: 'MOTOR',
          status: 'ACTIVE',
          premium: 8450.0,
          startDate: '2026-04-01',
          endDate: '2027-03-31',
          issuedAt: '2026-04-01T10:00:00Z',
          documentId: 501,
        },
      ],
    });

    render(
      <MemoryRouter>
        <AuthProvider>
          <MyPoliciesView />
        </AuthProvider>
      </MemoryRouter>
    );

    // Verify loading or rendered elements
    await waitFor(() => {
      expect(screen.getByText('POL-2026-000501')).toBeInTheDocument();
    });

    expect(screen.getByText('SafeShield Motor 4W Comprehensive')).toBeInTheDocument();
    expect(screen.getByText(/₹8,450.00/)).toBeInTheDocument();
    expect(screen.getByText('IRDAI Policyholder Protection Information')).toBeInTheDocument();
  });

  it('triggers PDF download when Download PDF button is clicked', async () => {
    apiClient.get.mockImplementation((url) => {
      if (url === '/policies') {
        return Promise.resolve({
          data: [
            {
              id: 501,
              policyNo: 'POL-2026-000501',
              applicationId: 9021,
              planName: 'SafeShield Motor 4W Comprehensive',
              productType: 'MOTOR',
              status: 'ACTIVE',
              premium: 8450.0,
              startDate: '2026-04-01',
              endDate: '2027-03-31',
              issuedAt: '2026-04-01T10:00:00Z',
              documentId: 501,
            },
          ],
        });
      }
      if (url === '/policies/501/document') {
        return Promise.resolve({
          data: new Blob(['%PDF-1.4 mock content'], { type: 'application/pdf' }),
        });
      }
      return Promise.reject(new Error('Unknown url'));
    });

    // Mock URL.createObjectURL and URL.revokeObjectURL
    window.URL.createObjectURL = vi.fn(() => 'blob:mock-url');
    window.URL.revokeObjectURL = vi.fn();

    render(
      <MemoryRouter>
        <AuthProvider>
          <MyPoliciesView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('POL-2026-000501')).toBeInTheDocument();
    });

    const downloadBtn = screen.getByRole('button', { name: /Download PDF/i });
    expect(downloadBtn).toBeInTheDocument();

    fireEvent.click(downloadBtn);

    await waitFor(() => {
      expect(apiClient.get).toHaveBeenCalledWith('/policies/501/document', { responseType: 'blob' });
      expect(window.URL.createObjectURL).toHaveBeenCalled();
    });
  });

  it('displays policy detail modal on Details button click', async () => {
    apiClient.get.mockResolvedValueOnce({
      data: [
        {
          id: 501,
          policyNo: 'POL-2026-000501',
          applicationId: 9021,
          planName: 'SafeShield Motor 4W Comprehensive',
          productType: 'MOTOR',
          status: 'ACTIVE',
          premium: 8450.0,
          startDate: '2026-04-01',
          endDate: '2027-03-31',
          issuedAt: '2026-04-01T10:00:00Z',
          documentId: 501,
        },
      ],
    });

    render(
      <MemoryRouter>
        <AuthProvider>
          <MyPoliciesView />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('POL-2026-000501')).toBeInTheDocument();
    });

    const detailsBtn = screen.getByRole('button', { name: 'Details' });
    fireEvent.click(detailsBtn);

    expect(screen.getByText(/Policy Details — POL-2026-000501/)).toBeInTheDocument();
    expect(screen.getByText('15-Day Free-Look / Pro-Rata Refund Applicable')).toBeInTheDocument();
  });
});
