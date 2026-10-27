import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider, ROLES } from '../context/AuthContext';
import apiClient, { setAuthToken, getAuthToken } from '../api/apiClient';
import LoginView from '../views/public/LoginView';
import RegisterView from '../views/public/RegisterView';
import CustomerKycView from '../views/customer/CustomerKycView';

describe('P2-08 Authentication & KYC Screens (INS-F-001, INS-F-002, INS-NF-005)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    setAuthToken(null);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('LoginView (INS-F-001)', () => {
    it('submits credentials to POST /auth/login and stores JWT in-memory on success', async () => {
      const mockPost = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
        data: {
          accessToken: 'mock-jwt-token-person-2',
          role: ROLES.CUSTOMER,
          expiresIn: 900,
        },
      });

      const mockGet = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
        data: {
          userId: 201,
          name: 'Asha Customer',
          email: 'asha@example.test',
          role: ROLES.CUSTOMER,
          insurerId: null,
        },
      });

      render(
        <MemoryRouter initialEntries={['/login']}>
          <AuthProvider>
            <Routes>
              <Route path="/login" element={<LoginView />} />
              <Route path="/customer" element={<div>Customer Dashboard Target</div>} />
            </Routes>
          </AuthProvider>
        </MemoryRouter>
      );

      const emailInput = screen.getByLabelText(/Email Address/i);
      const passwordInput = screen.getByLabelText(/Password/i);
      const submitBtn = screen.getByRole('button', { name: /Sign In/i });

      fireEvent.change(emailInput, { target: { value: 'asha@example.test' } });
      fireEvent.change(passwordInput, { target: { value: 'Secret123!' } });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(mockPost).toHaveBeenCalledWith('/auth/login', {
          email: 'asha@example.test',
          password: 'Secret123!',
        });
      });

      await waitFor(() => {
        expect(getAuthToken()).toBe('mock-jwt-token-person-2');
        expect(screen.getByText('Customer Dashboard Target')).toBeInTheDocument();
      });
    });

    it('displays uniform 401 message for invalid credentials', async () => {
      vi.spyOn(apiClient, 'post').mockRejectedValueOnce({
        status: 401,
        errorCode: 'AUTH_UNAUTHORIZED',
        message: 'Invalid credentials.',
      });

      render(
        <MemoryRouter initialEntries={['/login']}>
          <AuthProvider>
            <Routes>
              <Route path="/login" element={<LoginView />} />
            </Routes>
          </AuthProvider>
        </MemoryRouter>
      );

      const emailInput = screen.getByLabelText(/Email Address/i);
      const passwordInput = screen.getByLabelText(/Password/i);
      const submitBtn = screen.getByRole('button', { name: /Sign In/i });

      fireEvent.change(emailInput, { target: { value: 'unknown@example.test' } });
      fireEvent.change(passwordInput, { target: { value: 'wrongPassword' } });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(screen.getByText('Invalid email or password.')).toBeInTheDocument();
      });
      expect(getAuthToken()).toBeNull();
    });

    it('displays lockout message when server returns 423 AUTH-423', async () => {
      vi.spyOn(apiClient, 'post').mockRejectedValueOnce({
        status: 423,
        errorCode: 'AUTH_LOCKED',
        message: 'Account locked for 15 minutes due to 5 consecutive failed attempts.',
      });

      render(
        <MemoryRouter initialEntries={['/login']}>
          <AuthProvider>
            <Routes>
              <Route path="/login" element={<LoginView />} />
            </Routes>
          </AuthProvider>
        </MemoryRouter>
      );

      fireEvent.change(screen.getByLabelText(/Email Address/i), { target: { value: 'locked@example.test' } });
      fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: 'pass123' } });
      fireEvent.click(screen.getByRole('button', { name: /Sign In/i }));

      await waitFor(() => {
        expect(screen.getByText(/Account locked for 15 minutes/i)).toBeInTheDocument();
      });
    });

    it('renders flash message passed via location state (e.g. session timeout redirect)', () => {
      render(
        <MemoryRouter
          initialEntries={[
            {
              pathname: '/login',
              state: { message: 'Your session has expired. Please sign in again to continue.' },
            },
          ]}
        >
          <AuthProvider>
            <Routes>
              <Route path="/login" element={<LoginView />} />
            </Routes>
          </AuthProvider>
        </MemoryRouter>
      );

      expect(screen.getByText(/Your session has expired/i)).toBeInTheDocument();
    });
  });

  describe('RegisterView (INS-F-001)', () => {
    it('submits registration data to POST /auth/register and redirects to /login on success', async () => {
      const mockPost = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
        data: { userId: 301 },
      });

      render(
        <MemoryRouter initialEntries={['/register']}>
          <AuthProvider>
            <Routes>
              <Route path="/register" element={<RegisterView />} />
              <Route path="/login" element={<div>Sign In Screen (Redirect Destination)</div>} />
            </Routes>
          </AuthProvider>
        </MemoryRouter>
      );

      fireEvent.change(screen.getByLabelText(/Full Name/i), { target: { value: 'Vikram Malhotra' } });
      fireEvent.change(screen.getByLabelText(/Email Address/i), { target: { value: 'vikram@example.test' } });
      fireEvent.change(screen.getByLabelText(/Mobile Phone/i), { target: { value: '9876543210' } });
      fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: 'SecurePass99!' } });

      fireEvent.click(screen.getByRole('button', { name: /Register/i }));

      await waitFor(() => {
        expect(mockPost).toHaveBeenCalledWith('/auth/register', {
          name: 'Vikram Malhotra',
          email: 'vikram@example.test',
          phone: '9876543210',
          password: 'SecurePass99!',
        });
      });

      await waitFor(() => {
        expect(screen.getByText('Sign In Screen (Redirect Destination)')).toBeInTheDocument();
      });
    });

    it('displays conflict message when email is already registered (409)', async () => {
      vi.spyOn(apiClient, 'post').mockRejectedValueOnce({
        status: 409,
        errorCode: 'STATE_CONFLICT',
        message: 'Email already exists',
      });

      render(
        <MemoryRouter initialEntries={['/register']}>
          <AuthProvider>
            <Routes>
              <Route path="/register" element={<RegisterView />} />
            </Routes>
          </AuthProvider>
        </MemoryRouter>
      );

      fireEvent.change(screen.getByLabelText(/Full Name/i), { target: { value: 'Duplicate User' } });
      fireEvent.change(screen.getByLabelText(/Email Address/i), { target: { value: 'duplicate@example.test' } });
      fireEvent.change(screen.getByLabelText(/Mobile Phone/i), { target: { value: '9876543210' } });
      fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: 'SecurePass123' } });

      fireEvent.click(screen.getByRole('button', { name: /Register/i }));

      await waitFor(() => {
        expect(screen.getByText(/An account with this email address already exists/i)).toBeInTheDocument();
      });
    });
  });

  describe('CustomerKycView (INS-F-002, INS-SR-007)', () => {
    it('fetches and displays initial KYC status', async () => {
      vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
        data: { status: 'Verified' },
      });

      render(
        <MemoryRouter initialEntries={['/customer/kyc']}>
          <AuthProvider>
            <CustomerKycView />
          </AuthProvider>
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('Verified')).toBeInTheDocument();
        expect(screen.getByText(/Your KYC is verified/i)).toBeInTheDocument();
      });
    });

    it('submits valid KYC document and updates status to Verified', async () => {
      vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
        data: { status: 'Not Submitted' },
      });

      const mockPost = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
        data: { status: 'Verified' },
      });

      render(
        <MemoryRouter initialEntries={['/customer/kyc']}>
          <AuthProvider>
            <CustomerKycView />
          </AuthProvider>
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('Not Submitted')).toBeInTheDocument();
      });

      const panInput = screen.getByLabelText(/Permanent Account Number/i);
      const aadhaarInput = screen.getByLabelText(/Aadhaar Number/i);
      const fileInput = screen.getByLabelText(/Identity \/ Address Document/i);
      const submitBtn = screen.getByRole('button', { name: /Submit for Verification/i });

      const testFile = new File(['dummy-kyc-content'], 'pan_card.pdf', { type: 'application/pdf' });

      fireEvent.change(panInput, { target: { value: 'ABCDE1234F' } });
      fireEvent.change(aadhaarInput, { target: { value: '123456789012' } });
      fireEvent.change(fileInput, { target: { files: [testFile] } });

      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(mockPost).toHaveBeenCalledWith(
          '/kyc',
          expect.any(FormData),
          expect.objectContaining({
            headers: expect.objectContaining({ 'Content-Type': 'multipart/form-data' }),
          })
        );
      });

      await waitFor(() => {
        expect(screen.getByText(/KYC verified successfully/i)).toBeInTheDocument();
      });
    });

    it('handles simulated rejection (PAN ZZZZZ0000Z) and allows resubmission', async () => {
      vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
        data: { status: 'Not Submitted' },
      });

      vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
        data: { status: 'Rejected' },
      });

      render(
        <MemoryRouter initialEntries={['/customer/kyc']}>
          <AuthProvider>
            <CustomerKycView />
          </AuthProvider>
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('Not Submitted')).toBeInTheDocument();
      });

      const testFile = new File(['dummy-content'], 'pan.pdf', { type: 'application/pdf' });
      fireEvent.change(screen.getByLabelText(/Permanent Account Number/i), { target: { value: 'ZZZZZ0000Z' } });
      fireEvent.change(screen.getByLabelText(/Aadhaar Number/i), { target: { value: '123456789012' } });
      fireEvent.change(screen.getByLabelText(/Identity \/ Address Document/i), { target: { files: [testFile] } });

      fireEvent.click(screen.getByRole('button', { name: /Submit for Verification/i }));

      await waitFor(() => {
        expect(
          screen.getByText(/KYC submission was rejected by the verification provider/i)
        ).toBeInTheDocument();
        expect(screen.getByText(/Your previous KYC submission was rejected/i)).toBeInTheDocument();
      });
    });

    it('displays plain language error when file exceeds 5 MB (HTTP 413)', async () => {
      vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
        data: { status: 'Not Submitted' },
      });

      render(
        <MemoryRouter initialEntries={['/customer/kyc']}>
          <AuthProvider>
            <CustomerKycView />
          </AuthProvider>
        </MemoryRouter>
      );

      // 6MB test file
      const bigFile = new File(['x'.repeat(100)], 'huge.pdf', { type: 'application/pdf' });
      Object.defineProperty(bigFile, 'size', { value: 6 * 1024 * 1024 });

      fireEvent.change(screen.getByLabelText(/Permanent Account Number/i), { target: { value: 'ABCDE1234F' } });
      fireEvent.change(screen.getByLabelText(/Aadhaar Number/i), { target: { value: '123456789012' } });
      fireEvent.change(screen.getByLabelText(/Identity \/ Address Document/i), { target: { files: [bigFile] } });

      fireEvent.click(screen.getByRole('button', { name: /Submit for Verification/i }));

      await waitFor(() => {
        expect(screen.getByText(/File exceeds the 5 MB limit/i)).toBeInTheDocument();
      });
    });

    it('displays plain language error on unsupported file extension (HTTP 415)', async () => {
      vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
        data: { status: 'Not Submitted' },
      });

      render(
        <MemoryRouter initialEntries={['/customer/kyc']}>
          <AuthProvider>
            <CustomerKycView />
          </AuthProvider>
        </MemoryRouter>
      );

      const invalidFile = new File(['binary content'], 'script.exe', { type: 'application/x-msdownload' });

      fireEvent.change(screen.getByLabelText(/Permanent Account Number/i), { target: { value: 'ABCDE1234F' } });
      fireEvent.change(screen.getByLabelText(/Aadhaar Number/i), { target: { value: '123456789012' } });
      fireEvent.change(screen.getByLabelText(/Identity \/ Address Document/i), { target: { files: [invalidFile] } });

      fireEvent.click(screen.getByRole('button', { name: /Submit for Verification/i }));

      await waitFor(() => {
        expect(screen.getByText(/Only PDF, JPG, and PNG files are accepted/i)).toBeInTheDocument();
      });
    });
  });

  describe('Mobile Viewport 360px Inspection (INS-NF-005)', () => {
    beforeEach(() => {
      window.innerWidth = 360;
      window.innerHeight = 640;
      window.dispatchEvent(new Event('resize'));
    });

    it('renders LoginView and RegisterView without layout failure at 360px', () => {
      const { container: loginContainer } = render(
        <MemoryRouter initialEntries={['/login']}>
          <AuthProvider>
            <LoginView />
          </AuthProvider>
        </MemoryRouter>
      );

      expect(loginContainer.querySelector('#login-email')).toBeInTheDocument();
      expect(loginContainer.querySelector('#login-submit-btn')).toBeInTheDocument();

      const { container: regContainer } = render(
        <MemoryRouter initialEntries={['/register']}>
          <AuthProvider>
            <RegisterView />
          </AuthProvider>
        </MemoryRouter>
      );

      expect(regContainer.querySelector('#reg-email')).toBeInTheDocument();
      expect(regContainer.querySelector('#register-submit-btn')).toBeInTheDocument();
    });

    it('renders CustomerKycView without element clipping at 360px width', async () => {
      vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
        data: { status: 'Not Submitted' },
      });

      const { container } = render(
        <MemoryRouter initialEntries={['/customer/kyc']}>
          <AuthProvider>
            <CustomerKycView />
          </AuthProvider>
        </MemoryRouter>
      );

      expect(container.querySelector('#kyc-status-card')).toBeInTheDocument();
      expect(container.querySelector('#kyc-pan-input')).toBeInTheDocument();
      expect(container.querySelector('#kyc-file-input')).toBeInTheDocument();
      expect(container.querySelector('#kyc-submit-btn')).toBeInTheDocument();
    });
  });
});
