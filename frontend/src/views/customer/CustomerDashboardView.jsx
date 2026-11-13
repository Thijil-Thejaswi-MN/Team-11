import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import PortalLayout from '../../components/layout/PortalLayout';
import { ROUTES } from '../../routes/routePaths';
import { useAuth } from '../../context/AuthContext';
import apiClient from '../../api/apiClient';

export default function CustomerDashboardView() {
  const { currentUser } = useAuth();
  const [policies, setPolicies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchPolicies = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiClient.get('/policies');
      setPolicies(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      setPolicies([]);
      setError(err.response?.data?.message || err.message || 'Failed to load policies.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPolicies();
  }, []);

  const activePolicies = policies.filter((p) => p.status === 'ACTIVE');
  const expiringSoonPolicies = policies.filter((p) => p.status === 'EXPIRING_SOON');

  return (
    <PortalLayout
      title={`Welcome back, ${currentUser?.name || 'Customer'}`}
      subtitle="Customer Dashboard (SAD Section 4.5 / GET /api/v1/policies)"
      badgeText="Customer Portal"
      badgeClass="bg-primary"
    >
      {/* Error Alert with Retry */}
      {error && (
        <div className="alert alert-danger mb-4 d-flex justify-content-between align-items-center" role="alert">
          <div>
            <i className="bi bi-exclamation-triangle-fill me-2"></i>
            <span>{error}</span>
          </div>
          <button type="button" className="btn btn-outline-danger btn-sm" onClick={fetchPolicies}>
            Retry
          </button>
        </div>
      )}

      {/* 3 Metric Cards matching SAD Section 4.3.6 widget requirements */}
      <div className="row g-3 mb-4">
        {/* Active Policies */}
        <div className="col-12 col-sm-6 col-lg-4">
          <div className="dashboard-card h-100">
            <div className="dashboard-card-body d-flex align-items-center justify-content-between">
              <div>
                <span className="text-muted small fw-semibold text-uppercase">Active Policies</span>
                <h2 className="h2 fw-bold text-primary mb-0 mt-1">
                  {loading ? '—' : activePolicies.length}
                </h2>
                <span className="small text-success">
                  <i className="bi bi-shield-fill-check me-1"></i>
                  {activePolicies.length > 0 ? 'Coverage active' : 'No active coverage'}
                </span>
              </div>
              <div className="bg-primary-subtle text-primary p-3 rounded-circle fs-4">
                <i className="bi bi-shield-check"></i>
              </div>
            </div>
          </div>
        </div>

        {/* Claims Under Process */}
        <div className="col-12 col-sm-6 col-lg-4">
          <div className="dashboard-card h-100">
            <div className="dashboard-card-body d-flex align-items-center justify-content-between">
              <div>
                <span className="text-muted small fw-semibold text-uppercase">Active Claims</span>
                <h2 className="h2 fw-bold text-warning mb-0 mt-1">0</h2>
                <span className="small text-muted">
                  <i className="bi bi-clock-history me-1"></i>Track via Claims Desk
                </span>
              </div>
              <div className="bg-warning-subtle text-warning p-3 rounded-circle fs-4">
                <i className="bi bi-clipboard2-pulse"></i>
              </div>
            </div>
          </div>
        </div>

        {/* Renewal Alerts */}
        <div className="col-12 col-sm-6 col-lg-4">
          <div className="dashboard-card h-100">
            <div className="dashboard-card-body d-flex align-items-center justify-content-between">
              <div>
                <span className="text-muted small fw-semibold text-uppercase">Renewal Alerts</span>
                <h2 className="h2 fw-bold text-danger mb-0 mt-1">
                  {loading ? '—' : expiringSoonPolicies.length}
                </h2>
                <span className="small text-danger">
                  <i className="bi bi-bell-fill me-1"></i>
                  {expiringSoonPolicies.length > 0 ? 'Requires attention' : 'None expiring soon'}
                </span>
              </div>
              <div className="bg-danger-subtle text-danger p-3 rounded-circle fs-4">
                <i className="bi bi-exclamation-circle"></i>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Action Buttons */}
      <div className="dashboard-card mb-4">
        <div className="dashboard-card-header">
          <span className="fw-bold">Frequent Actions</span>
          <span className="text-muted small">Common customer workflows</span>
        </div>
        <div className="dashboard-card-body">
          <div className="row g-2">
            <div className="col-6 col-md">
              <Link to={ROUTES.CUSTOMER_KYC} className="btn btn-outline-primary btn-sm w-100 py-2" id="dashboard-kyc-btn">
                <i className="bi bi-person-check d-block fs-5 mb-1"></i>
                <span>Verify KYC</span>
              </Link>
            </div>
            <div className="col-6 col-md">
              <Link to={ROUTES.CUSTOMER_QUOTE_MOTOR} className="btn btn-outline-primary btn-sm w-100 py-2">
                <i className="bi bi-car-front d-block fs-5 mb-1"></i>
                <span>Motor Quote</span>
              </Link>
            </div>
            <div className="col-6 col-md">
              <Link to={ROUTES.CUSTOMER_QUOTE_HEALTH} className="btn btn-outline-primary btn-sm w-100 py-2">
                <i className="bi bi-heart-pulse d-block fs-5 mb-1"></i>
                <span>Health Quote</span>
              </Link>
            </div>
            <div className="col-6 col-md">
              <Link to={ROUTES.CUSTOMER_POLICIES} className="btn btn-outline-primary btn-sm w-100 py-2">
                <i className="bi bi-arrow-repeat d-block fs-5 mb-1"></i>
                <span>My Policies</span>
              </Link>
            </div>
            <div className="col-6 col-md">
              <Link to={ROUTES.CUSTOMER_CLAIMS} className="btn btn-outline-primary btn-sm w-100 py-2">
                <i className="bi bi-plus-circle d-block fs-5 mb-1"></i>
                <span>File Claim</span>
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* Recent Policies Preview */}
      <div className="dashboard-card">
        <div className="dashboard-card-header d-flex justify-content-between align-items-center">
          <span className="fw-bold">Current Policy Portfolio</span>
          <Link to={ROUTES.CUSTOMER_POLICIES} className="btn btn-link btn-sm text-decoration-none p-0">
            View All Policies &rarr;
          </Link>
        </div>
        <div className="dashboard-card-body p-0">
          <div className="table-responsive">
            <table className="table table-hover mb-0 align-middle">
              <thead className="table-light">
                <tr>
                  <th>Policy No</th>
                  <th>Plan & Insurer</th>
                  <th>Valid Until</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {loading && (
                  <tr>
                    <td colSpan="5" className="text-center py-4 text-muted">
                      <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                      Loading policies...
                    </td>
                  </tr>
                )}
                {!loading && policies.length === 0 && (
                  <tr>
                    <td colSpan="5" className="text-center py-4 text-muted">
                      No policies yet.
                    </td>
                  </tr>
                )}
                {!loading &&
                  policies.map((policy) => (
                    <tr key={policy.id || policy.policyNo}>
                      <td className="fw-semibold font-monospace text-primary">{policy.policyNo}</td>
                      <td>{policy.planName || (policy.productType === 'MOTOR' ? 'Motor Insurance' : 'Health Insurance')}</td>
                      <td>{policy.endDate ? new Date(policy.endDate).toLocaleDateString() : '—'}</td>
                      <td>
                        <span
                          className={`status-badge ${
                            policy.status === 'ACTIVE'
                              ? 'status-active'
                              : policy.status === 'EXPIRING_SOON'
                              ? 'status-referred'
                              : 'badge bg-secondary'
                          }`}
                        >
                          {policy.status}
                        </span>
                      </td>
                      <td>
                        <Link to={ROUTES.CUSTOMER_POLICIES} className="btn btn-outline-secondary btn-sm me-1" title="View in Portfolio">
                          <i className="bi bi-eye"></i>
                        </Link>
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </PortalLayout>
  );
}
