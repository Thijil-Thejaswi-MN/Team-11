import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import PaymentView from '../views/customer/PaymentView';
import apiClient from '../api/apiClient';
import { AuthProvider } from '../context/AuthContext';

describe('PaymentView Component (P4-17)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  const renderWithRouter = (initialRoute = '/customer/payment/9021', state = null) => {
    return render(
      <MemoryRouter initialEntries={[{ pathname: initialRoute, state }]}>
        <AuthProvider>
          <Routes>
            <Route path="/customer/payment/:applicationId" element={<PaymentView />} />
            <Route path="/customer/payment" element={<PaymentView />} />
            <Route path="/customer/policies" element={<div>Customer Policies Page</div>} />
            <Route path="/customer/applications" element={<div>Applications Page</div>} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    );
  };

  it('renders application details and charges ONLY applicationId on Pay button click', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        applicationId: 9021,
        customerId: 1001,
        planId: 10,
        planName: 'SafeShield Motor 4W',
        productType: 'FOUR_WHEELER',
        vehicleRegNo: 'KA22AB1234',
        premium: 8450.0,
        status: 'APPROVED',
      },
    });

    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      data: {
        paymentStatus: 'Success',
        policyNo: 'POL-2026-009021',
        policyStatus: 'ACTIVE',
        transactionReference: 'TXN-APP-9021-1',
        amount: 8450.0,
      },
    });

    renderWithRouter('/customer/payment/9021');

    // Verify application loaded
    expect(await screen.findByText(/SafeShield Motor 4W/i)).toBeInTheDocument();
    expect(screen.getByText('KA22AB1234')).toBeInTheDocument();

    const payButton = screen.getByRole('button', { name: /Pay Premium/i });
    expect(payButton).toBeEnabled();

    // Click Pay Premium
    fireEvent.click(payButton);

    // Verify payload sent to backend: ONLY applicationId! No amount or idempotency key.
    expect(postSpy).toHaveBeenCalledTimes(1);
    expect(postSpy).toHaveBeenCalledWith('/payments', {
      applicationId: 9021,
    });

    // Verify success state displayed
    expect(await screen.findByText(/Payment Successful!/i)).toBeInTheDocument();
    expect(screen.getByText('POL-2026-009021')).toBeInTheDocument();
    expect(screen.getByText(/TXN-APP-9021-1/)).toBeInTheDocument();

    // Pay button is disabled and shows paid
    expect(screen.getByRole('button', { name: /Premium Paid/i })).toBeDisabled();
  });

  it('handles HTTP 402 payment failure, shows "Payment failed — retry" banner, and allows retry', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        applicationId: 9021,
        customerId: 1001,
        planId: 10,
        planName: 'SafeShield Motor 4W',
        premium: 8450.0,
        status: 'APPROVED',
      },
    });

    // 1st attempt: 402 failure
    const postSpy = vi.spyOn(apiClient, 'post')
      .mockRejectedValueOnce({
        status: 402,
        errorCode: 'PAY-402',
        message: 'Payment failed — retry',
      })
      // 2nd attempt: Success
      .mockResolvedValueOnce({
        data: {
          paymentStatus: 'Success',
          policyNo: 'POL-2026-009021',
          policyStatus: 'ACTIVE',
          transactionReference: 'TXN-APP-9021-2',
          amount: 8450.0,
        },
      });

    renderWithRouter('/customer/payment/9021');

    const payButton = await screen.findByRole('button', { name: /Pay Premium/i });
    fireEvent.click(payButton);

    // 402 Failure Banner displayed
    expect(await screen.findByRole('heading', { name: /Payment failed — retry/i })).toBeInTheDocument();
    expect(screen.getByText(/PAY-402/)).toBeInTheDocument();

    // Button allows retry
    const retryButton = screen.getByRole('button', { name: /Retry Payment/i });
    expect(retryButton).toBeEnabled();

    // Retry payment
    fireEvent.click(retryButton);

    // 2nd call sends ONLY applicationId
    expect(postSpy).toHaveBeenCalledTimes(2);
    expect(postSpy).toHaveBeenNthCalledWith(2, '/payments', {
      applicationId: 9021,
    });

    // Succeeds and shows policy number
    expect(await screen.findByText(/Payment Successful!/i)).toBeInTheDocument();
    expect(screen.getByText('POL-2026-009021')).toBeInTheDocument();
  });

  it('handles HTTP 409 conflict and displays the conflict banner', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        applicationId: 9021,
        customerId: 1001,
        premium: 8450.0,
        status: 'APPROVED',
      },
    });

    vi.spyOn(apiClient, 'post').mockRejectedValueOnce({
      status: 409,
      errorCode: 'STATE-409',
      message: 'Application 9021 has already been paid (Policy: POL-2026-009021).',
    });

    renderWithRouter('/customer/payment/9021');

    const payButton = await screen.findByRole('button', { name: /Pay Premium/i });
    fireEvent.click(payButton);

    expect(await screen.findByText(/Application Conflict/i)).toBeInTheDocument();
    expect(screen.getByText(/has already been paid/i)).toBeInTheDocument();
    expect(screen.getByText(/STATE-409/)).toBeInTheDocument();
  });

  it('disables Pay button while submission is in-flight to prevent duplicate double-clicks', async () => {
    let resolvePost;
    const pendingPromise = new Promise((resolve) => {
      resolvePost = resolve;
    });

    vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        applicationId: 9021,
        customerId: 1001,
        premium: 8450.0,
        status: 'APPROVED',
      },
    });

    const postSpy = vi.spyOn(apiClient, 'post').mockReturnValueOnce(pendingPromise);

    renderWithRouter('/customer/payment/9021');

    const payButton = await screen.findByRole('button', { name: /Pay Premium/i });
    fireEvent.click(payButton);

    // Button should be disabled during flight
    expect(payButton).toBeDisabled();
    expect(screen.getByText(/Processing Payment.../i)).toBeInTheDocument();

    // Repeated clicks must be ignored
    fireEvent.click(payButton);
    expect(postSpy).toHaveBeenCalledTimes(1);

    // Resolve in-flight request
    resolvePost({
      data: {
        paymentStatus: 'Success',
        policyNo: 'POL-2026-009021',
        amount: 8450.0,
      },
    });

    expect(await screen.findByText(/Payment Successful!/i)).toBeInTheDocument();
  });

  it('displays network failure banner without falsely indicating success', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        applicationId: 9021,
        customerId: 1001,
        premium: 8450.0,
        status: 'APPROVED',
      },
    });

    vi.spyOn(apiClient, 'post').mockRejectedValueOnce({
      status: 500,
      errorCode: 'SYS-500',
      message: 'Network error or service unavailable.',
    });

    renderWithRouter('/customer/payment/9021');

    const payButton = await screen.findByRole('button', { name: /Pay Premium/i });
    fireEvent.click(payButton);

    expect(await screen.findByText(/Payment Processing Error/i)).toBeInTheDocument();
    expect(screen.queryByText(/Payment Successful!/i)).not.toBeInTheDocument();
  });
});
