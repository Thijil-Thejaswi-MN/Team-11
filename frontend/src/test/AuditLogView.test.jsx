import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import AuditLogView from '../views/systemAdmin/AuditLogView';
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

function renderAuditLogView() {
  return render(
    <BrowserRouter>
      <AuthProvider>
        <AuditLogView />
      </AuthProvider>
    </BrowserRouter>
  );
}

const mockAuditResponse = {
  items: [
    {
      id: 101,
      userId: 5001,
      action: 'LOGIN_SUCCESS',
      entity: 'User',
      entityId: 5001,
      details: 'Successful login from IP: 127.0.0.1',
      timestamp: '2026-10-28T14:30:00Z',
    },
    {
      id: 102,
      userId: 2001,
      action: 'PLAN_CREATED',
      entity: 'Plan',
      entityId: 5,
      details: 'Plan created: PLAN-GOLD, name: Gold Health',
      timestamp: '2026-10-28T14:35:00Z',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
};

describe('AuditLogView (P2-18 / INS-F-018 / INS-SR-008 / TC-Aud-01)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders filter controls and table columns with accessible labels', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockAuditResponse });
    renderAuditLogView();

    expect(screen.getByText(/System Audit Log/i)).toBeInTheDocument();
    expect(screen.getByText(/Audit Log Search & Filters/i)).toBeInTheDocument();

    expect(screen.getByLabelText(/User ID/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Action/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Entity ID/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/From Date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/To Date/i)).toBeInTheDocument();

    expect(await screen.findByRole('button', { name: /Apply Filters/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Reset Filters/i })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('LOGIN_SUCCESS')).toBeInTheDocument();
      expect(screen.getByText('PLAN_CREATED')).toBeInTheDocument();
      expect(screen.getByText(/User #5001/i)).toBeInTheDocument();
      expect(screen.getByText(/Successful login from IP/i)).toBeInTheDocument();
    });
  });

  it('submits filter criteria and triggers GET /audit with query params', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockAuditResponse });
    renderAuditLogView();

    await waitFor(() => {
      expect(screen.getByText('LOGIN_SUCCESS')).toBeInTheDocument();
    });

    apiClient.get.mockResolvedValueOnce({
      data: {
        items: [mockAuditResponse.items[0]],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      },
    });

    fireEvent.change(screen.getByLabelText(/User ID/i), { target: { value: '5001' } });
    fireEvent.change(screen.getByLabelText(/Action/i), { target: { value: 'LOGIN_SUCCESS' } });
    fireEvent.click(screen.getByRole('button', { name: /Apply Filters/i }));

    await waitFor(() => {
      expect(apiClient.get).toHaveBeenCalledWith('/audit', {
        params: expect.objectContaining({
          userId: 5001,
          action: 'LOGIN_SUCCESS',
          page: 0,
          size: 20,
        }),
      });
    });
  });

  it('resets filters when Reset Filters button is clicked', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockAuditResponse });
    renderAuditLogView();

    await waitFor(() => {
      expect(screen.getByText('LOGIN_SUCCESS')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/User ID/i), { target: { value: '9999' } });
    expect(screen.getByLabelText(/User ID/i)).toHaveValue(9999);

    fireEvent.click(screen.getByRole('button', { name: /Reset Filters/i }));
    expect(screen.getByLabelText(/User ID/i)).toHaveValue(null);
  });

  it('displays error alert when 403 Forbidden is returned', async () => {
    const error403 = new Error('Access denied');
    error403.status = 403;
    error403.response = { status: 403 };
    apiClient.get.mockRejectedValueOnce(error403);

    renderAuditLogView();

    await waitFor(() => {
      expect(screen.getByText(/Access denied: SYSTEM_ADMIN privileges required/i)).toBeInTheDocument();
    });
  });

  it('renders responsive table container suitable for 360px viewport', async () => {
    apiClient.get.mockResolvedValueOnce({ data: mockAuditResponse });
    const { container } = renderAuditLogView();

    const tableContainer = container.querySelector('.table-responsive');
    expect(tableContainer).toBeInTheDocument();
    expect(tableContainer).toHaveAttribute('id', 'audit-table-container');
  });
});
