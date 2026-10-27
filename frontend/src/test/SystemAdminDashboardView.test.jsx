import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import SystemAdminDashboardView from '../views/systemAdmin/SystemAdminDashboardView';
import apiClient from '../api/apiClient';

vi.mock('../api/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
  setAuthToken: vi.fn(),
  getAuthToken: vi.fn(),
  clearAuthToken: vi.fn(),
  onUnauthorized: vi.fn(),
}));

function renderSystemAdminDashboard() {
  return render(
    <BrowserRouter>
      <AuthProvider>
        <SystemAdminDashboardView />
      </AuthProvider>
    </BrowserRouter>
  );
}

describe('SystemAdminDashboardView (P2-11 / SAD §4.3.6 / INS-SR-003)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders onboarding and user provisioning forms with required fields', () => {
    renderSystemAdminDashboard();

    expect(screen.getByText(/System Administration & Audit Portal/i)).toBeInTheDocument();
    expect(screen.getByText(/Onboard Insurer Organization/i)).toBeInTheDocument();
    expect(screen.getByText(/Provision Staff \/ User Account/i)).toBeInTheDocument();

    expect(screen.getByLabelText(/Insurer Name/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Onboard Insurer/i })).toBeInTheDocument();

    expect(screen.getByLabelText(/Full Name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Email Address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Phone Number/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Assigned Role/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Insurer ID/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Provision User Account/i })).toBeInTheDocument();
  });

  it('successfully onboards an insurer when form is submitted', async () => {
    apiClient.post.mockResolvedValueOnce({ data: { id: 101 } });

    renderSystemAdminDashboard();

    fireEvent.change(screen.getByLabelText(/Insurer Name/i), {
      target: { value: 'Apex Mutual Assurance' },
    });
    fireEvent.click(screen.getByRole('button', { name: /Onboard Insurer/i }));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith('/admin/insurers', {
        name: 'Apex Mutual Assurance',
      });
      expect(screen.getByText(/Insurer organization created successfully \(ID: 101\)/i)).toBeInTheDocument();
    });
  });

  it('displays conflict error when insurer name is duplicated (409 Conflict)', async () => {
    const error409 = new Error('Conflict');
    error409.status = 409;
    error409.response = { status: 409 };
    apiClient.post.mockRejectedValueOnce(error409);

    renderSystemAdminDashboard();

    fireEvent.change(screen.getByLabelText(/Insurer Name/i), {
      target: { value: 'Duplicate Insurer' },
    });
    fireEvent.click(screen.getByRole('button', { name: /Onboard Insurer/i }));

    await waitFor(() => {
      expect(screen.getByText(/Insurer with name "Duplicate Insurer" already exists/i)).toBeInTheDocument();
    });
  });

  it('successfully creates staff user with insurer ID', async () => {
    apiClient.post.mockResolvedValueOnce({ data: { id: 501 } });

    renderSystemAdminDashboard();

    fireEvent.change(screen.getByLabelText(/Full Name/i), { target: { value: 'Priya Sharma' } });
    fireEvent.change(screen.getByLabelText(/Email Address/i), { target: { value: 'priya@safeshield.test' } });
    fireEvent.change(screen.getByLabelText(/Phone Number/i), { target: { value: '9876543210' } });
    fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: 'SecurePass123!' } });
    fireEvent.change(screen.getByLabelText(/Assigned Role/i), { target: { value: 'UNDERWRITER' } });
    fireEvent.change(screen.getByLabelText(/Insurer ID/i), { target: { value: '1' } });

    fireEvent.click(screen.getByRole('button', { name: /Provision User Account/i }));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith('/admin/users', {
        name: 'Priya Sharma',
        email: 'priya@safeshield.test',
        phone: '9876543210',
        password: 'SecurePass123!',
        role: 'UNDERWRITER',
        insurerId: 1,
      });
      expect(screen.getByText(/Staff user account created successfully \(ID: 501\)/i)).toBeInTheDocument();
    });
  });

  it('validates that staff roles require an Insurer ID before submitting', async () => {
    renderSystemAdminDashboard();

    fireEvent.change(screen.getByLabelText(/Full Name/i), { target: { value: 'Staff User' } });
    fireEvent.change(screen.getByLabelText(/Email Address/i), { target: { value: 'staff@test.invalid' } });
    fireEvent.change(screen.getByLabelText(/Phone Number/i), { target: { value: '9876543210' } });
    fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: 'SecurePass123!' } });
    fireEvent.change(screen.getByLabelText(/Assigned Role/i), { target: { value: 'INSURER_ADMIN' } });
    // Keep Insurer ID empty

    fireEvent.click(screen.getByRole('button', { name: /Provision User Account/i }));

    await waitFor(() => {
      expect(screen.getByText(/Staff role "INSURER_ADMIN" must be linked to a valid Insurer ID/i)).toBeInTheDocument();
      expect(apiClient.post).not.toHaveBeenCalled();
    });
  });

  it('disables Insurer ID field when role is non-staff (CUSTOMER or SYSTEM_ADMIN)', () => {
    renderSystemAdminDashboard();

    const insurerInput = screen.getByLabelText(/Insurer ID/i);
    expect(insurerInput).not.toBeDisabled();

    fireEvent.change(screen.getByLabelText(/Assigned Role/i), { target: { value: 'CUSTOMER' } });
    expect(insurerInput).toBeDisabled();

    fireEvent.change(screen.getByLabelText(/Assigned Role/i), { target: { value: 'SYSTEM_ADMIN' } });
    expect(insurerInput).toBeDisabled();

    fireEvent.change(screen.getByLabelText(/Assigned Role/i), { target: { value: 'CLAIMS_OFFICER' } });
    expect(insurerInput).not.toBeDisabled();
  });

  it('displays conflict error when user email is duplicated (409 Conflict)', async () => {
    const error409 = new Error('Conflict');
    error409.status = 409;
    error409.response = { status: 409 };
    apiClient.post.mockRejectedValueOnce(error409);

    renderSystemAdminDashboard();

    fireEvent.change(screen.getByLabelText(/Full Name/i), { target: { value: 'Duplicate User' } });
    fireEvent.change(screen.getByLabelText(/Email Address/i), { target: { value: 'dupe@safeshield.test' } });
    fireEvent.change(screen.getByLabelText(/Phone Number/i), { target: { value: '9876543210' } });
    fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: 'SecurePass123!' } });
    fireEvent.change(screen.getByLabelText(/Assigned Role/i), { target: { value: 'CUSTOMER' } });

    fireEvent.click(screen.getByRole('button', { name: /Provision User Account/i }));

    await waitFor(() => {
      expect(screen.getByText(/User with email "dupe@safeshield.test" already exists/i)).toBeInTheDocument();
    });
  });
});
