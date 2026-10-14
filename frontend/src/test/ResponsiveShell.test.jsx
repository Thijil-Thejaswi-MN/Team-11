import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider, ROLES } from '../context/AuthContext';
import Header from '../components/common/Header';

describe('Responsive Application Shell (SRS INS-NF-005: 360px+ Viewport)', () => {
  it('renders mobile navigation toggle button with accessible aria attributes', () => {
    render(
      <BrowserRouter>
        <AuthProvider>
          <Header />
        </AuthProvider>
      </BrowserRouter>
    );

    const toggleBtn = screen.getByRole('button', { name: /toggle navigation/i });
    expect(toggleBtn).toBeInTheDocument();
    expect(toggleBtn).toHaveAttribute('id', 'main-navbar-toggle');
    expect(toggleBtn).toHaveAttribute('aria-controls', 'appNavbarNav');
  });

  it('renders role switcher dropdown supporting all 6 application personas', () => {
    render(
      <BrowserRouter>
        <AuthProvider>
          <Header />
        </AuthProvider>
      </BrowserRouter>
    );

    const roleSelect = screen.getByRole('combobox', { name: /role/i });
    expect(roleSelect).toBeInTheDocument();

    // Verify option values
    expect(screen.getByRole('option', { name: /Customer/i })).toHaveValue(ROLES.CUSTOMER);
    expect(screen.getByRole('option', { name: /Insurer Admin/i })).toHaveValue(ROLES.INSURER_ADMIN);
    expect(screen.getByRole('option', { name: /Underwriter/i })).toHaveValue(ROLES.UNDERWRITER);
    expect(screen.getByRole('option', { name: /Claims Officer/i })).toHaveValue(ROLES.CLAIMS_OFFICER);
    expect(screen.getByRole('option', { name: /System Admin/i })).toHaveValue(ROLES.SYSTEM_ADMIN);
    expect(screen.getByRole('option', { name: /Guest/i })).toHaveValue(ROLES.GUEST);
  });

  it('switches navigation links dynamically when role changes', () => {
    render(
      <BrowserRouter>
        <AuthProvider>
          <Header />
        </AuthProvider>
      </BrowserRouter>
    );

    const roleSelect = screen.getByRole('combobox', { name: /role/i });

    // Switch to Underwriter
    fireEvent.change(roleSelect, { target: { value: ROLES.UNDERWRITER } });
    const uwLink = screen.getByRole('link', { name: /underwriter/i });
    expect(uwLink).toBeInTheDocument();
    expect(uwLink).toHaveAttribute('href', '/underwriter');

    // Switch to Claims Officer
    fireEvent.change(roleSelect, { target: { value: ROLES.CLAIMS_OFFICER } });
    const coLink = screen.getByRole('link', { name: /claims desk/i });
    expect(coLink).toBeInTheDocument();
    expect(coLink).toHaveAttribute('href', '/claims-officer');
  });
});
