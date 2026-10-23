import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import HealthQuoteView from '../views/customer/HealthQuoteView';
import apiClient from '../api/apiClient';

describe('HealthQuoteView Component (P1-08)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders health quote form with plans, coverage types, and sum insured', () => {
    render(
      <MemoryRouter initialEntries={['/customer/quote/health']}>
        <AuthProvider>
          <HealthQuoteView />
        </AuthProvider>
      </MemoryRouter>
    );

    expect(screen.getByText(/Health Insurance Instant Quote/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Selected Health Plan/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Coverage Policy Type/i)).toHaveValue('INDIVIDUAL');
    expect(screen.getByLabelText(/Sum Insured/i)).toHaveValue('500000');
    expect(screen.getByLabelText(/Primary Insured Age/i)).toHaveValue(32);
    expect(screen.getByRole('button', { name: /Calculate Instant Health Quote/i })).toBeInTheDocument();
  });

  it('submits health quote request and renders calculated itemized medical breakdown and quoteId', async () => {
    const mockApiResponse = {
      data: {
        quoteId: 6012,
        premium: 7480,
        validUntil: '2026-11-20T10:00:00Z',
        breakdown: {
          basePremium: 5200,
          ageLoading: 0,
          addOns: { CRITICAL_ILLNESS: 1200 },
          netPremium: 6400,
          gst: 1080,
          totalPremium: 7480,
        },
      },
    };

    vi.spyOn(apiClient, 'post').mockResolvedValueOnce(mockApiResponse);

    render(
      <MemoryRouter initialEntries={['/customer/quote/health?planId=3']}>
        <AuthProvider>
          <HealthQuoteView />
        </AuthProvider>
      </MemoryRouter>
    );

    const calcButton = screen.getByRole('button', { name: /Calculate Instant Health Quote/i });
    fireEvent.click(calcButton);

    await waitFor(() => {
      expect(screen.getByText(/#QTE-HEALTH-6012/i)).toBeInTheDocument();
    });

    expect(screen.getByText(/₹7,480/i)).toBeInTheDocument();
    expect(screen.getByText(/Base Medical Cover/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Proceed to Application with Quote #6012/i })).toBeInTheDocument();
  });

  it('navigates to customer applications passing quoteId when Proceed to Application is clicked', async () => {
    const mockApiResponse = {
      data: {
        quoteId: 8899,
        premium: 8200,
        validUntil: '2026-11-20T10:00:00Z',
        breakdown: {
          basePremium: 6000,
          ageLoading: 0,
          addOns: {},
          totalPremium: 8200,
        },
      },
    };

    vi.spyOn(apiClient, 'post').mockResolvedValueOnce(mockApiResponse);

    render(
      <MemoryRouter initialEntries={['/customer/quote/health']}>
        <AuthProvider>
          <Routes>
            <Route path="/customer/quote/health" element={<HealthQuoteView />} />
            <Route
              path="/customer/applications"
              element={<div data-testid="applications-page">Applications Landing Screen</div>}
            />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    );

    const calcButton = screen.getByRole('button', { name: /Calculate Instant Health Quote/i });
    fireEvent.click(calcButton);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Proceed to Application with Quote #8899/i })).toBeInTheDocument();
    });

    const proceedBtn = screen.getByRole('button', { name: /Proceed to Application with Quote #8899/i });
    fireEvent.click(proceedBtn);

    await waitFor(() => {
      expect(screen.getByTestId('applications-page')).toBeInTheDocument();
    });
  });
});
