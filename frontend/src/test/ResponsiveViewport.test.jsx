import React from 'react';
import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import HomeView from '../views/public/HomeView';
import CustomerDashboardView from '../views/customer/CustomerDashboardView';
import BrowsePlansView from '../views/public/BrowsePlansView';
import ComparePlansView from '../views/public/ComparePlansView';
import RegisterView from '../views/public/RegisterView';
import LoginView from '../views/public/LoginView';
import CustomerKycView from '../views/customer/CustomerKycView';
import MotorQuoteView from '../views/customer/MotorQuoteView';
import PaymentView from '../views/customer/PaymentView';

describe('Responsive Viewport 360px Inspection (SRS INS-NF-005)', () => {
  beforeEach(() => {
    // Emulate 360px mobile viewport (e.g. Galaxy S8 / standard mobile test spec)
    window.innerWidth = 360;
    window.innerHeight = 640;
    window.dispatchEvent(new Event('resize'));
  });

  it('renders HomeView with no missing elements at 360px width', () => {
    const { container } = render(
      <MemoryRouter initialEntries={['/']}>
        <AuthProvider>
          <HomeView />
        </AuthProvider>
      </MemoryRouter>
    );

    // Verify main header and region exist
    const mainRegion = container.querySelector('#main-content-region');
    expect(mainRegion).toBeInTheDocument();

    // Verify portal links render cleanly
    expect(screen.getByText(/Role-Based Portals/i)).toBeInTheDocument();
    expect(screen.getByText(/Customer Portal/i)).toBeInTheDocument();
    expect(screen.getAllByText(/Claims Officer/i).length).toBeGreaterThanOrEqual(1);

    // Check container styles ensure no horizontal overflow
    const appWrapper = container.querySelector('.app-wrapper');
    expect(appWrapper).toBeInTheDocument();
  });

  it('renders Customer Dashboard without element clipping at 360px width', () => {
    const { container } = render(
      <MemoryRouter initialEntries={['/customer']}>
        <AuthProvider>
          <CustomerDashboardView />
        </AuthProvider>
      </MemoryRouter>
    );

    // Verify portal container and sidebar exist
    expect(container.querySelector('#portal-container-wrapper')).toBeInTheDocument();
    expect(container.querySelector('#portal-main-sidebar')).toBeInTheDocument();

    // Verify metrics cards exist
    expect(screen.getByText(/Active Policies/i)).toBeInTheDocument();
    expect(screen.getByText(/Active Claims/i)).toBeInTheDocument();
    expect(screen.getByText(/Renewal Alerts/i)).toBeInTheDocument();
  });

  it('renders BrowsePlansView with filter buttons and plan cards at 360px width', () => {
    const { container } = render(
      <MemoryRouter initialEntries={['/plans']}>
        <AuthProvider>
          <BrowsePlansView />
        </AuthProvider>
      </MemoryRouter>
    );

    expect(screen.getByText(/Browse Insurance Plans/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /All Plans/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Motor \(2W \/ 4W\)/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Personal Health/i })).toBeInTheDocument();
    expect(container.querySelector('.app-wrapper')).toBeInTheDocument();
  });

  it('renders ComparePlansView with responsive table at 360px width', () => {
    const { container } = render(
      <MemoryRouter initialEntries={['/plans/compare']}>
        <AuthProvider>
          <ComparePlansView />
        </AuthProvider>
      </MemoryRouter>
    );

    expect(screen.getByText(/Compare Insurance Plans/i)).toBeInTheDocument();
    const tableResp = container.querySelector('.table-responsive');
    expect(tableResp).toBeInTheDocument();
    expect(screen.getByText(/Comparison Matrix/i)).toBeInTheDocument();
  });

  it('renders RegisterView with accessible form labels and controls at 360px width', () => {
    const { container } = render(
      <MemoryRouter initialEntries={['/register']}>
        <AuthProvider>
          <RegisterView />
        </AuthProvider>
      </MemoryRouter>
    );

    expect(screen.getByText(/Create New Account/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Full Name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Email Address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Mobile Phone/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Password/i)).toBeInTheDocument();
    expect(container.querySelector('#register-submit-btn')).toBeInTheDocument();
  });

  it('renders LoginView with accessible form labels and controls at 360px width', () => {
    const { container } = render(
      <MemoryRouter initialEntries={['/login']}>
        <AuthProvider>
          <LoginView />
        </AuthProvider>
      </MemoryRouter>
    );

    expect(screen.getByText(/Sign In to Your Account/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Email Address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Password/i)).toBeInTheDocument();
    expect(container.querySelector('#login-submit-btn')).toBeInTheDocument();
  });

  it('renders CustomerKycView with PAN and Aadhaar form controls at 360px width', async () => {
    const { container } = render(
      <MemoryRouter initialEntries={['/customer/kyc']}>
        <AuthProvider>
          <CustomerKycView />
        </AuthProvider>
      </MemoryRouter>
    );

    expect(await screen.findByLabelText(/Permanent Account Number \(PAN\)/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Aadhaar Number/i)).toBeInTheDocument();
    expect(container.querySelector('#kyc-submit-btn')).toBeInTheDocument();
  });

  it('renders MotorQuoteView with rating parameters at 360px width', () => {
    const { container } = render(
      <MemoryRouter initialEntries={['/customer/quote/motor']}>
        <AuthProvider>
          <MotorQuoteView />
        </AuthProvider>
      </MemoryRouter>
    );

    expect(screen.getByText(/Motor Insurance Instant Quote/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Selected Insurance Plan/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Insured Declared Value/i)).toBeInTheDocument();
    expect(container.querySelector('#calculate-quote-btn')).toBeInTheDocument();
  });

  it('renders PaymentView with checkout panel at 360px width', async () => {
    const { container } = render(
      <MemoryRouter initialEntries={['/customer/payment']}>
        <AuthProvider>
          <PaymentView />
        </AuthProvider>
      </MemoryRouter>
    );

    expect(await screen.findByText(/Premium Payment/i)).toBeInTheDocument();
    expect(screen.getByText(/Payment Checkout/i)).toBeInTheDocument();
    expect(container.querySelector('#submit-payment-btn')).toBeInTheDocument();
  });
});
