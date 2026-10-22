import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import MotorQuoteView from '../views/customer/MotorQuoteView';
import apiClient from '../api/apiClient';

describe('MotorQuoteView Component (P1-08)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders all motor quote form fields with initial defaults', () => {
    render(
      <MemoryRouter initialEntries={['/customer/quote/motor']}>
        <AuthProvider>
          <MotorQuoteView />
        </AuthProvider>
      </MemoryRouter>
    );

    expect(screen.getByText(/Motor Insurance Instant Quote/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Selected Insurance Plan/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Manufacturer \/ Make/i)).toHaveValue('Honda');
    expect(screen.getByLabelText(/Model \/ Variant/i)).toHaveValue('City');
    expect(screen.getByLabelText(/Registration Year/i)).toHaveValue(2022);
    expect(screen.getByLabelText(/Insured Declared Value/i)).toHaveValue(650000);
    expect(screen.getByRole('button', { name: /Calculate Instant Premium Quote/i })).toBeInTheDocument();
  });

  it('pre-populates 2-wheeler defaults when planId=2 is provided in search params', () => {
    render(
      <MemoryRouter initialEntries={['/customer/quote/motor?planId=2']}>
        <AuthProvider>
          <MotorQuoteView />
        </AuthProvider>
      </MemoryRouter>
    );

    expect(screen.getByLabelText(/Selected Insurance Plan/i)).toHaveValue('2');
    expect(screen.getByLabelText(/Manufacturer \/ Make/i)).toHaveValue('Hero');
    expect(screen.getByLabelText(/Model \/ Variant/i)).toHaveValue('Splendor');
    expect(screen.getByLabelText(/Insured Declared Value/i)).toHaveValue(85000);
  });

  it('submits motor quote request and renders calculated itemized breakdown and quoteId', async () => {
    const mockApiResponse = {
      data: {
        quoteId: 5042,
        premium: 8940,
        validUntil: '2026-11-20T10:00:00Z',
        breakdown: {
          basePremium: 6500,
          ageLoading: 325,
          addOns: { ZERO_DEP: 1080 },
          ncbDiscount: 1300,
          netPremium: 6605,
          gst: 1189,
          totalPremium: 8940,
        },
      },
    };

    vi.spyOn(apiClient, 'post').mockResolvedValueOnce(mockApiResponse);

    render(
      <MemoryRouter initialEntries={['/customer/quote/motor?planId=1']}>
        <AuthProvider>
          <MotorQuoteView />
        </AuthProvider>
      </MemoryRouter>
    );

    const calcButton = screen.getByRole('button', { name: /Calculate Instant Premium Quote/i });
    fireEvent.click(calcButton);

    await waitFor(() => {
      expect(screen.getByText(/#QTE-MOTOR-5042/i)).toBeInTheDocument();
    });

    expect(screen.getByText(/₹8,940/i)).toBeInTheDocument();
    expect(screen.getByText(/Base Own Damage/i)).toBeInTheDocument();
    expect(screen.getByText(/Proceed to Application with Quote #5042/i)).toBeInTheDocument();
  });

  it('navigates to customer applications passing quoteId when Proceed to Application is clicked', async () => {
    const mockApiResponse = {
      data: {
        quoteId: 7788,
        premium: 9500,
        validUntil: '2026-11-20T10:00:00Z',
        breakdown: {
          basePremium: 7000,
          ageLoading: 0,
          addOns: { ZERO_DEP: 1080 },
          totalPremium: 9500,
        },
      },
    };

    vi.spyOn(apiClient, 'post').mockResolvedValueOnce(mockApiResponse);

    render(
      <MemoryRouter initialEntries={['/customer/quote/motor']}>
        <AuthProvider>
          <Routes>
            <Route path="/customer/quote/motor" element={<MotorQuoteView />} />
            <Route
              path="/customer/applications"
              element={<div data-testid="applications-page">Applications Landing Screen</div>}
            />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    );

    const calcButton = screen.getByRole('button', { name: /Calculate Instant Premium Quote/i });
    fireEvent.click(calcButton);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Proceed to Application with Quote #7788/i })).toBeInTheDocument();
    });

    const proceedBtn = screen.getByRole('button', { name: /Proceed to Application with Quote #7788/i });
    fireEvent.click(proceedBtn);

    await waitFor(() => {
      expect(screen.getByTestId('applications-page')).toBeInTheDocument();
    });
  });
});
