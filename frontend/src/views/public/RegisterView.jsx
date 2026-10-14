import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import { ROUTES } from '../../routes/routePaths';

export default function RegisterView() {
  const [formData, setFormData] = useState({
    name: 'Asha Verma',
    email: 'asha@example.com',
    phone: '9876543210',
    password: 'password123',
  });
  const navigate = useNavigate();

  const handleSubmit = (e) => {
    e.preventDefault();
    alert('Registration shell: Person 2 will connect this to POST /api/v1/auth/register');
    navigate(ROUTES.LOGIN);
  };

  return (
    <AppLayout>
      <div className="row justify-content-center">
        <div className="col-12 col-sm-10 col-md-8 col-lg-5">
          <div className="dashboard-card my-4">
            <div className="dashboard-card-header">
              <span className="fw-bold">Create New Account</span>
              <span className="badge bg-secondary">Person 2 Hook</span>
            </div>
            <div className="dashboard-card-body">
              <div className="dev-hook-box mb-3">
                <span className="dev-hook-badge">Person 2 Ownership (Registration C3)</span>
                <p className="mb-0">
                  Connects to <code>POST /api/v1/auth/register</code> (<code>INS-F-001</code>).
                  Bcrypt password hashing (<code>INS-SR-002</code>), unique email check (<code>409</code> on duplicate).
                </p>
              </div>

              <form onSubmit={handleSubmit}>
                <div className="mb-2">
                  <label htmlFor="reg-name" className="form-label small fw-semibold">
                    Full Name
                  </label>
                  <input
                    type="text"
                    id="reg-name"
                    className="form-control form-control-sm"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    required
                  />
                </div>

                <div className="mb-2">
                  <label htmlFor="reg-email" className="form-label small fw-semibold">
                    Email Address
                  </label>
                  <input
                    type="email"
                    id="reg-email"
                    className="form-control form-control-sm"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    required
                  />
                </div>

                <div className="mb-2">
                  <label htmlFor="reg-phone" className="form-label small fw-semibold">
                    Mobile Phone
                  </label>
                  <input
                    type="tel"
                    id="reg-phone"
                    className="form-control form-control-sm"
                    value={formData.phone}
                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                    required
                  />
                </div>

                <div className="mb-3">
                  <label htmlFor="reg-password" className="form-label small fw-semibold">
                    Password
                  </label>
                  <input
                    type="password"
                    id="reg-password"
                    className="form-control form-control-sm"
                    value={formData.password}
                    onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                    required
                  />
                </div>

                <button type="submit" className="btn btn-primary btn-sm w-100" id="register-submit-btn">
                  <i className="bi bi-person-plus me-1"></i>Register
                </button>
              </form>

              <div className="mt-3 text-center small text-muted">
                Already registered?{' '}
                <Link to={ROUTES.LOGIN} className="text-primary text-decoration-none">
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
