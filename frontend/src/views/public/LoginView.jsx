import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import { useAuth, ROLES } from '../../context/AuthContext';
import { ROUTES } from '../../routes/routePaths';

/**
 * Public User Login View (INS-F-001 / INS-SR-005).
 * Connects to POST /api/v1/auth/login, receives JWT Bearer token in memory,
 * and redirects user to their appropriate role portal.
 */
export default function LoginView() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Flash message passed via navigation state (e.g., registration success or session timeout)
  const flashMessage = location.state?.message || null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(false);
    setErrorMessage(null);

    if (!email || !password) {
      setErrorMessage('Please enter both email and password.');
      return;
    }

    try {
      setIsSubmitting(true);
      const user = await login(email.trim(), password);

      // Navigate to target route if user was redirected from a protected route
      if (location.state?.from?.pathname) {
        navigate(location.state.from.pathname, { replace: true });
        return;
      }

      // Default role routing
      switch (user.role) {
        case ROLES.INSURER_ADMIN:
          navigate(ROUTES.INSURER_ADMIN_DASHBOARD);
          break;
        case ROLES.UNDERWRITER:
          navigate(ROUTES.UNDERWRITER_DASHBOARD);
          break;
        case ROLES.CLAIMS_OFFICER:
          navigate(ROUTES.CLAIMS_OFFICER_DASHBOARD);
          break;
        case ROLES.SYSTEM_ADMIN:
          navigate(ROUTES.SYSTEM_ADMIN_DASHBOARD);
          break;
        case ROLES.CUSTOMER:
        default:
          navigate(ROUTES.CUSTOMER_DASHBOARD);
          break;
      }
    } catch (err) {
      if (err.status === 401) {
        // Indistinguishable 401 error message per security baseline (INS-SR-005 / OWASP ASVS)
        setErrorMessage('Invalid email or password.');
      } else if (err.status === 423) {
        setErrorMessage(
          err.message ||
            'Account is locked due to multiple failed login attempts. Please try again after 15 minutes.'
        );
      } else {
        setErrorMessage(err.message || 'Login failed. Please check your credentials and network connection.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AppLayout>
      <div className="row justify-content-center">
        <div className="col-12 col-sm-10 col-md-8 col-lg-5">
          <div className="dashboard-card my-4">
            <div className="dashboard-card-header">
              <span className="fw-bold">Sign In to Your Account</span>
              <span className="badge bg-primary">Secure Auth</span>
            </div>
            <div className="dashboard-card-body">
              {/* Flash alert banner (e.g. session timeout redirect or registration success) */}
              {flashMessage && (
                <div className="alert alert-info py-2 small mb-3" role="alert" id="login-flash-message">
                  <i className="bi bi-info-circle me-1"></i>
                  {flashMessage}
                </div>
              )}

              {/* Error banner */}
              {errorMessage && (
                <div className="alert alert-danger py-2 small mb-3" role="alert" id="login-error-alert">
                  <i className="bi bi-exclamation-triangle-fill me-1"></i>
                  {errorMessage}
                </div>
              )}

              <form onSubmit={handleSubmit} noValidate>
                <div className="mb-3">
                  <label htmlFor="login-email" className="form-label small fw-semibold">
                    Email Address
                  </label>
                  <input
                    type="email"
                    id="login-email"
                    name="email"
                    autoComplete="email"
                    className="form-control form-control-sm"
                    placeholder="name@example.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    disabled={isSubmitting}
                  />
                </div>

                <div className="mb-3">
                  <label htmlFor="login-password" className="form-label small fw-semibold">
                    Password
                  </label>
                  <input
                    type="password"
                    id="login-password"
                    name="password"
                    autoComplete="current-password"
                    className="form-control form-control-sm"
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    disabled={isSubmitting}
                  />
                </div>

                <button
                  type="submit"
                  className="btn btn-primary btn-sm w-100"
                  id="login-submit-btn"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
                      Signing In...
                    </>
                  ) : (
                    <>
                      <i className="bi bi-box-arrow-in-right me-1"></i>Sign In
                    </>
                  )}
                </button>
              </form>

              <div className="mt-3 text-center small text-muted">
                Don't have an account?{' '}
                <Link to={ROUTES.REGISTER} className="text-primary text-decoration-none" id="login-register-link">
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
