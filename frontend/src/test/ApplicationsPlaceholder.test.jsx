import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import ApplicationsPlaceholder from '../views/customer/ApplicationsPlaceholder';
import { AuthProvider } from '../context/AuthContext';

describe('ApplicationsPlaceholder Component (P4-17 Navigation)', () => {
  it('navigates to the payment page when Pay Premium button is clicked', () => {
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

    const payBtn = screen.getByRole('button', { name: /Pay Premium \(₹8,450\)/i });
    expect(payBtn).toBeInTheDocument();

    fireEvent.click(payBtn);

    expect(screen.getByTestId('payment-page-destination')).toHaveTextContent('Payment Page Reached');
  });
});
