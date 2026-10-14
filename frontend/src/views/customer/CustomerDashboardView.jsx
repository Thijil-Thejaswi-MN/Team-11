import React from 'react';
import { Link } from 'react-router-dom';
import PortalLayout from '../../components/layout/PortalLayout';
import { ROUTES } from '../../routes/routePaths';
import { useAuth } from '../../context/AuthContext';

export default function CustomerDashboardView() {
  const { currentUser } = useAuth();

  return (
    <PortalLayout
      title={`Welcome back, ${currentUser.name}`}
      subtitle="Customer Dashboard (SAD Section 4.5 / GET /api/v1/dashboard)"
      badgeText="Customer Portal"
      badgeClass="bg-primary"
    >
      {/* 3 Metric Cards matching SAD Section 4.3.6 widget requirements */}
      <div className="row g-3 mb-4">
        {/* Active Policies */}
        <div className="col-12 col-sm-6 col-lg-4">
          <div className="dashboard-card h-100">
            <div className="dashboard-card-body d-flex align-items-center justify-content-between">
              <div>
                <span className="text-muted small fw-semibold text-uppercase">Active Policies</span>
                <h2 className="h2 fw-bold text-primary mb-0 mt-1">2</h2>
                <span className="small text-success">
                  <i className="bi bi-shield-fill-check me-1"></i>All covered
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
                <h2 className="h2 fw-bold text-warning mb-0 mt-1">1</h2>
                <span className="small text-muted">
                  <i className="bi bi-clock-history me-1"></i>In Assessment
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
                <h2 className="h2 fw-bold text-danger mb-0 mt-1">1</h2>
                <span className="small text-danger">
                  <i className="bi bi-bell-fill me-1"></i>Expires in 18 days
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
            <div className="col-12 col-sm-6 col-md-3">
              <Link to={ROUTES.CUSTOMER_QUOTE_MOTOR} className="btn btn-outline-primary btn-sm w-100 py-2">
                <i className="bi bi-car-front d-block fs-5 mb-1"></i>
                <span>Get Motor Quote</span>
              </Link>
            </div>
            <div className="col-12 col-sm-6 col-md-3">
              <Link to={ROUTES.CUSTOMER_QUOTE_HEALTH} className="btn btn-outline-primary btn-sm w-100 py-2">
                <i className="bi bi-heart-pulse d-block fs-5 mb-1"></i>
                <span>Get Health Quote</span>
              </Link>
            </div>
            <div className="col-12 col-sm-6 col-md-3">
              <Link to={ROUTES.CUSTOMER_POLICIES} className="btn btn-outline-primary btn-sm w-100 py-2">
                <i className="bi bi-arrow-repeat d-block fs-5 mb-1"></i>
                <span>Renew Policy</span>
              </Link>
            </div>
            <div className="col-12 col-sm-6 col-md-3">
              <Link to={ROUTES.CUSTOMER_CLAIMS} className="btn btn-outline-primary btn-sm w-100 py-2">
                <i className="bi bi-plus-circle d-block fs-5 mb-1"></i>
                <span>File a Claim</span>
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* Recent Policies Preview */}
      <div className="dashboard-card">
        <div className="dashboard-card-header">
          <span className="fw-bold">My Active Insurance Policies</span>
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
                <tr>
                  <td className="fw-semibold">POL-2026-4W-0091</td>
                  <td>SafeShield Motor 4W (SafeShield General)</td>
                  <td>31 Mar 2027</td>
                  <td><span className="status-badge status-active">Active</span></td>
                  <td>
                    <button className="btn btn-outline-secondary btn-sm me-1" title="Download Policy PDF">
                      <i className="bi bi-file-earmark-pdf"></i>
                    </button>
                  </td>
                </tr>
                <tr>
                  <td className="fw-semibold">POL-2025-HL-0012</td>
                  <td>Bharat Care Swasthya (Bharat Care)</td>
                  <td>02 Nov 2026</td>
                  <td><span className="status-badge status-referred">Expiring Soon</span></td>
                  <td>
                    <button className="btn btn-primary btn-sm" title="Quick Renew">
                      Renew
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </PortalLayout>
  );
}
