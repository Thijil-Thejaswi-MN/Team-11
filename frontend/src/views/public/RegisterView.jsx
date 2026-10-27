import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import { useAuth } from '../../context/AuthContext';
import { ROUTES } from '../../routes/routePaths';

/**
 * Public User Registration View (INS-F-001 / INS-SR-002).
 * Connects to POST /api/v1/auth/register, enforces unique email (409)
 * and validation constraints (422), and redirects to /login on success.
 */
export default function RegisterView() {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: '',
    password: '',
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);

  const { register } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage(null);

    // Basic frontend checks
    if (!formData.name.trim() || !formData.email.trim() || !formData.phone.trim() || !formData.password) {
      setErrorMessage('All fields are required.');
      return;
    }

    if (formData.password.length < 8) {
      setErrorMessage('Password must be at least 8 characters long.');
      return;
    }

    try {
      setIsSubmitting(true);
      await register({
        name: formData.name.trim(),
        email: formData.email.trim(),
        phone: formData.phone.trim(),
        password: formData.password,
      });

      // Navigate to login with success flash message
      navigate(ROUTES.LOGIN, {
        state: {
          message: 'Registration successful! Please sign in with your credentials.',
        },
      });
    } catch (err) {
      if (err.status === 409) {
        setErrorMessage(
          'An account with this email address already exists. Please sign in or use a different email.'
        );
      } else if (err.status === 422) {
        const details =
          Array.isArray(err.details) && err.details.length > 0 ? err.details.join(', ') : err.message;
        setErrorMessage(details || 'Validation failed. Please review your information.');
      } else {
        setErrorMessage(err.message || 'Registration failed. Please try again.');
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
              <span className="fw-bold">Create New Account</span>
              <span className="badge bg-primary">Customer Registration</span>
            </div>
            <div className="dashboard-card-body">
              {/* Error Alert */}
              {errorMessage && (
                <div className="alert alert-danger py-2 small mb-3" role="alert" id="register-error-alert">
                  <i className="bi bi-exclamation-triangle-fill me-1"></i>
                  {errorMessage}
                </div>
              )}

              <form onSubmit={handleSubmit} noValidate>
                <div className="mb-2">
                  <label htmlFor="reg-name" className="form-label small fw-semibold">
                    Full Name
                  </label>
                  <input
                    type="text"
                    id="reg-name"
                    name="name"
                    autoComplete="name"
                    className="form-control form-control-sm"
                    placeholder="Full Name"
                    value={formData.name}
                    onChange={handleChange}
                    required
                    disabled={isSubmitting}
                  />
                </div>

                <div className="mb-2">
                  <label htmlFor="reg-email" className="form-label small fw-semibold">
                    Email Address
                  </label>
                  <input
                    type="email"
                    id="reg-email"
                    name="email"
                    autoComplete="email"
                    className="form-control form-control-sm"
                    placeholder="name@example.com"
                    value={formData.email}
                    onChange={handleChange}
                    required
                    disabled={isSubmitting}
                  />
                </div>

                <div className="mb-2">
                  <label htmlFor="reg-phone" className="form-label small fw-semibold">
                    Mobile Phone
                  </label>
                  <input
                    type="tel"
                    id="reg-phone"
                    name="phone"
                    autoComplete="tel"
                    className="form-control form-control-sm"
                    placeholder="10-digit mobile number"
                    value={formData.phone}
                    onChange={handleChange}
                    required
                    disabled={isSubmitting}
                  />
                </div>

                <div className="mb-3">
                  <label htmlFor="reg-password" className="form-label small fw-semibold">
                    Password
                  </label>
                  <input
                    type="password"
                    id="reg-password"
                    name="password"
                    autoComplete="new-password"
                    className="form-control form-control-sm"
                    placeholder="At least 8 characters"
                    value={formData.password}
                    onChange={handleChange}
                    required
                    disabled={isSubmitting}
                  />
                  <div className="form-text small">Must be at least 8 characters long.</div>
                </div>

                <button
                  type="submit"
                  className="btn btn-primary btn-sm w-100"
                  id="register-submit-btn"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
                      Creating Account...
                    </>
                  ) : (
                    <>
                      <i className="bi bi-person-plus me-1"></i>Register
                    </>
                  )}
                </button>
              </form>

              <div className="mt-3 text-center small text-muted">
                Already registered?{' '}
                <Link to={ROUTES.LOGIN} className="text-primary text-decoration-none" id="register-login-link">
                  Sign in here
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </AppLayout>
  );
}
