import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import AppRoutes from '../routes/AppRoutes';
import apiClient from '../api/apiClient';

describe('End-to-End Quote Flow Integration (P1-08 -> P3-04a)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('allows user to browse plans, navigate to quote calculation, see breakdown, and proceed to application with quoteId', async () => {
    const mockQuoteResponse = {
      data: {
        quoteId: 99123,
        premium: 8640,
        validUntil: '2026-11-20T10:00:00Z',
        breakdown: {
          basePremium: 6800,
          ageLoading: 340,
          addOns: { ZERO_DEP: 1080 },
          netPremium: 7322,
          gst: 1318,
          totalPremium: 8640,
        },
      },
    };

    vi.spyOn(apiClient, 'post').mockImplementation((url) => {
      if (url === '/quotes/motor') {
        return Promise.resolve(mockQuoteResponse);
      }
      return Promise.reject(new Error(`Unhandled URL ${url}`));
    });

    render(
      <MemoryRouter initialEntries={['/plans']}>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </MemoryRouter>
    );

    // Step 1: User arrives at /plans
    expect(screen.getByText(/Browse Insurance Plans/i)).toBeInTheDocument();
    const calculateButtons = screen.getAllByRole('button', { name: /Calculate Instant Quote/i });
    expect(calculateButtons.length).toBeGreaterThan(0);

    // Step 2: User clicks "Calculate Instant Quote" on the first plan (Motor 4W)
    fireEvent.click(calculateButtons[0]);

    // Step 3: Verify navigated to Motor Quote screen
    await waitFor(() => {
      expect(screen.getByText(/Motor Insurance Instant Quote/i)).toBeInTheDocument();
    });

    // Step 4: Click "Calculate Instant Premium Quote"
    const calcSubmitBtn = screen.getByRole('button', { name: /Calculate Instant Premium Quote/i });
    fireEvent.click(calcSubmitBtn);

    // Step 5: Verify itemized breakdown and generated Quote ID
    await waitFor(() => {
      expect(screen.getByText(/#QTE-MOTOR-99123/i)).toBeInTheDocument();
      expect(screen.getByText(/₹8,640/i)).toBeInTheDocument();
    });

    // Step 6: Click "Proceed to Application with Quote #99123"
    const proceedBtn = screen.getByRole('button', { name: /Proceed to Application with Quote #99123/i });
    fireEvent.click(proceedBtn);

    // Step 7: Verify landed on Applications page with active quote banner
    await waitFor(() => {
      expect(screen.getByText(/Customer Applications/i)).toBeInTheDocument();
      expect(screen.getByText(/Active Quote Attached for Application/i)).toBeInTheDocument();
      expect(screen.getByText(/Quote #99123/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Submit Application to Underwriting/i })).toBeInTheDocument();
    });
  });
});
