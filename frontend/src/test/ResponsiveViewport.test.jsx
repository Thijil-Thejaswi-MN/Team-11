import React from 'react';
import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import HomeView from '../views/public/HomeView';
import CustomerDashboardView from '../views/customer/CustomerDashboardView';

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
});
