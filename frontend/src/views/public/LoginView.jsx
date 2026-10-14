import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import { useAuth, ROLES } from '../../context/AuthContext';
import { ROUTES } from '../../routes/routePaths';

export default function LoginView() {
  const [email, setEmail] = useState('asha@example.com');
  const [password, setPassword] = useState('password123');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    await login(email, password);
    navigate(ROUTES.CUSTOMER_DASHBOARD);
  };

  return (
    <AppLayout>
      <div className="row justify-content-center">
        <div className="col-12 col-sm-10 col-md-8 col-lg-5">
          <div className="dashboard-card my-4">
            <div className="dashboard-card-header">
              <span className="fw-bold">Sign In to Your Account</span>
              <span className="badge bg-secondary">Person 2 Hook</span>
            </div>
            <div className="dashboard-card-body">
              <div className="dev-hook-box mb-3">
                <span className="dev-hook-badge">Person 2 Ownership (Auth C3)</span>
                <p className="mb-0">
                  Connects to <code>POST /api/v1/auth/login</code>. Returns JWT Bearer token and role.
                  Enforces lockout after 5 failed attempts (<code>AUTH-423</code>) and 15 min idle session.
                </p>
              </div>

              <form onSubmit={handleSubmit}>
                <div className="mb-3">
                  <label htmlFor="login-email" className="form-label small fw-semibold">
                    Email Address
                  </label>
                  <input
                    type="email"
                    id="login-email"
                    className="form-control form-control-sm"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                  />
                </div>

                <div className="mb-3">
                  <label htmlFor="login-password" className="form-label small fw-semibold">
                    Password
                  </label>
                  <input
                    type="password"
                    id="login-password"
                    className="form-control form-control-sm"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                  />
                </div>

                <button type="submit" className="btn btn-primary btn-sm w-100" id="login-submit-btn">
                  <i className="bi bi-box-arrow-in-right me-1"></i>Sign In (Shell Demo)
                </button>
              </form>

              <div className="mt-3 text-center small text-muted">
                Don't have an account?{' '}
                <Link to={ROUTES.REGISTER} className="text-primary text-decoration-none">
                  Register here
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </AppLayout>
  );
}
