import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import ApplicationsPlaceholder from '../views/customer/ApplicationsPlaceholder';
import { AuthProvider } from '../context/AuthContext';
import apiClient from '../api/apiClient';

describe('ApplicationsPlaceholder Component (P4-17 Navigation & API rendering)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders applications from API and navigates to payment page when Pay Premium button is clicked', async () => {
    vi.spyOn(apiClient, 'get').mockImplementation((url) => {
      if (url === '/applications') {
        return Promise.resolve({
          data: [
            {
              id: 9021,
              quoteId: 5021,
              planId: 1,
              planName: 'SafeShield Motor 4W',
              premium: 8450,
              status: 'APPROVED',
              createdAt: '2026-10-12T10:00:00Z',
            },
          ],
        });
      }
      return Promise.reject(new Error(`Unhandled GET ${url}`));
    });

    render(
      <MemoryRouter initialEntries={['/customer/applications']}>
        <AuthProvider>
          <Routes>
            <Route path="/customer/applications" element={<ApplicationsPlaceholder />} />
            <Route
              path="/customer/payment/:applicationId"
              element={<div data-testid="payment-page-destination">Payment Page Reached</div>}
            />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    );

    const payBtn = await screen.findByRole('button', { name: /Pay Premium \(₹8,450\)/i });
    expect(payBtn).toBeInTheDocument();

    fireEvent.click(payBtn);

    expect(screen.getByTestId('payment-page-destination')).toHaveTextContent('Payment Page Reached');
  });

  it('renders error state and Retry button when GET /applications fails', async () => {
    vi.spyOn(apiClient, 'get').mockImplementation((url) => {
      if (url === '/applications') {
        return Promise.reject({
          response: { status: 500, data: { message: 'Database connection failed' } },
        });
      }
      return Promise.reject(new Error(`Unhandled GET ${url}`));
    });

    render(
      <MemoryRouter initialEntries={['/customer/applications']}>
        <AuthProvider>
          <ApplicationsPlaceholder />
        </AuthProvider>
      </MemoryRouter>
    );

    expect(await screen.findByText(/Database connection failed/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Retry/i })).toBeInTheDocument();
  });

  it('forwards regYear and age in details payload upon application submission (Issue #198)', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValue({ data: [] });
    let capturedPayload = null;
    vi.spyOn(apiClient, 'post').mockImplementation((url, payload) => {
      if (url === '/applications') {
        capturedPayload = payload;
        return Promise.resolve({
          data: {
            applicationId: 405,
            status: 'Approved',
            details: 'Auto-approved',
          },
        });
      }
      return Promise.reject(new Error(`Unhandled POST ${url}`));
    });

    render(
      <MemoryRouter initialEntries={['/customer/applications?quoteId=505&planId=1&regYear=2021&age=34&vehicleRegNo=KA05CD5678']}>
        <AuthProvider>
          <ApplicationsPlaceholder />
        </AuthProvider>
      </MemoryRouter>
    );

    const submitBtn = await screen.findByRole('button', { name: /Submit Application to Underwriting/i });
    expect(submitBtn).toBeInTheDocument();

    fireEvent.click(submitBtn);

    expect(capturedPayload).not.toBeNull();
    expect(capturedPayload.quoteId).toBe(505);
    expect(capturedPayload.details.regYear).toBe(2021);
    expect(capturedPayload.details.age).toBe(34);
    expect(capturedPayload.details.vehicleRegNo).toBe('KA05CD5678');
  });
});
