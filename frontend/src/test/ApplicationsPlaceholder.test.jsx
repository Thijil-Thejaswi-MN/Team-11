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
});
