import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import PortalLayout from '../../components/layout/PortalLayout';
import { ROUTES } from '../../routes/routePaths';
import apiClient from '../../api/apiClient';

/**
 * Insurer Administration Desk (INS-F-003, INS-SR-003, Decision D-10).
 * Displays live plan count and management links.
 * Hardcoded metrics (1,420, ₹1.18 Cr, 142, 62.4%) and fake records removed per Issue #168.
 * Notice displayed for deferred role dashboards (INS-F-021, D-10).
 */
export default function InsurerAdminDashboardView() {
  const [activeTab, setActiveTab] = useState('OVERVIEW');
  const [plans, setPlans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchPlans = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.get('/plans');
      setPlans(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Unable to load plans catalog.');
      setPlans([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPlans();
  }, []);

  const activePlansCount = plans.filter((p) => p.status === 'ACTIVE' || p.active === true).length;

  return (
    <PortalLayout
      title="Insurer Administration Desk"
      subtitle="Manage insurance plans, underwriting operations, and policy cancellations."
      badgeText="Insurer Admin"
      badgeClass="bg-warning text-dark"
    >

      {/* Tabs */}
      <ul className="nav nav-tabs mb-4">
        <li className="nav-item">
          <button
            className={`nav-link ${activeTab === 'OVERVIEW' ? 'active fw-bold' : ''}`}
            onClick={() => setActiveTab('OVERVIEW')}
            data-testid="tab-overview"
          >
            <i className="bi bi-speedometer2 me-1"></i>Metrics Overview
          </button>
        </li>
        <li className="nav-item">
          <button
            className={`nav-link ${activeTab === 'PLANS' ? 'active fw-bold' : ''}`}
            onClick={() => setActiveTab('PLANS')}
            data-testid="tab-plans"
          >
            <i className="bi bi-card-checklist me-1"></i>Plan Management
          </button>
        </li>
        <li className="nav-item">
          <button
            className={`nav-link ${activeTab === 'CANCELLATIONS' ? 'active fw-bold' : ''}`}
            onClick={() => setActiveTab('CANCELLATIONS')}
            data-testid="tab-cancellations"
          >
            <i className="bi bi-x-circle me-1"></i>Cancellations
          </button>
        </li>
      </ul>

      {/* Overview Tab */}
      {activeTab === 'OVERVIEW' && (
        <div>
          {/* Deferred Metrics Notice */}
          <div className="alert alert-info border-0 shadow-sm mb-4" data-testid="metrics-deferred-notice">
            <div className="d-flex align-items-start">
              <i className="bi bi-info-circle-fill fs-4 me-3 text-info"></i>
              <div>
                <h5 className="alert-heading fw-bold mb-1">
                  Portfolio metrics are not available in this release
                </h5>
                <p className="mb-0 text-muted small">
                  Executive KPI dashboards and aggregated portfolio metrics (Policies Issued, Loss Ratio, Settled Payouts)
                  are scheduled for a future release to maintain core transactional focus.
                  Active plan configurations and underwriting workflows remain live and operational below.
                </p>
              </div>
            </div>
          </div>

          {/* Real Metrics Cards */}
          <div className="row g-3 mb-4">
            <div className="col-12 col-md-6 col-lg-4">
              <div className="dashboard-card h-100">
                <div className="dashboard-card-body">
                  <span className="text-muted small text-uppercase fw-semibold">Configured Plans</span>
                  <div className="d-flex align-items-baseline gap-2 mt-1">
                    {loading ? (
                      <span className="spinner-border spinner-border-sm text-primary" role="status" />
                    ) : (
                      <h3 className="h3 fw-bold text-primary mb-0" data-testid="real-plans-count">
                        {plans.length}
                      </h3>
                    )}
                    <span className="text-muted small">
                      ({activePlansCount} Active)
                    </span>
                  </div>
                  <p className="text-muted small mb-3 mt-2">
                    Insurance plans authored and managed for SafeShield General.
                  </p>
                  <Link to={ROUTES.INSURER_ADMIN_PLANS} className="btn btn-outline-primary btn-sm">
                    <i className="bi bi-pencil-square me-1"></i>Open Plan Editor
                  </Link>
                </div>
              </div>
            </div>

            <div className="col-12 col-md-6 col-lg-8">
              <div className="dashboard-card h-100">
                <div className="dashboard-card-body">
                  <span className="text-muted small text-uppercase fw-semibold">Overview</span>
                  <h5 className="fw-bold mt-1 text-dark">Core Capabilities</h5>
                  <ul className="small text-muted mb-0 ps-3 mt-2">
                    <li><strong>Plan Catalog &amp; Editor:</strong> Authoring motor and health premium rules, add-ons, and coverage boundaries.</li>
                    <li><strong>Underwriting Queue:</strong> Manual review of referred applications exceeding rule thresholds.</li>
                    <li><strong>Policy Lifecycle:</strong> Online issuance, policy document generation, and renewals.</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Plans Tab */}
      {activeTab === 'PLANS' && (
        <div className="dashboard-card">
          <div className="dashboard-card-header d-flex justify-content-between align-items-center">
            <span className="fw-bold">Active Insurance Plans</span>
            <Link to={ROUTES.INSURER_ADMIN_PLANS} className="btn btn-primary btn-sm">
              <i className="bi bi-pencil-square me-1"></i>Open Plan Editor &amp; Management
            </Link>
          </div>
          <div className="dashboard-card-body p-0">
            {loading ? (
              <div className="text-center py-4 text-muted">
                <div className="spinner-border spinner-border-sm me-2" role="status" />
                Loading plans catalog...
              </div>
            ) : error ? (
              <div className="p-3 text-center">
                <div className="alert alert-warning mb-2 py-2 small">{error}</div>
                <button className="btn btn-sm btn-outline-secondary" onClick={fetchPlans}>
                  Retry
                </button>
              </div>
            ) : plans.length === 0 ? (
              <div className="text-center py-4 text-muted">
                No insurance plans configured. Create a plan in the Plan Editor.
              </div>
            ) : (
              <div className="table-responsive">
                <table className="table table-hover mb-0 align-middle">
                  <thead className="table-light">
                    <tr>
                      <th>Plan ID</th>
                      <th>Plan Name</th>
                      <th>Category</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {plans.map((plan) => (
                      <tr key={plan.id}>
                        <td className="fw-semibold">PLAN-{plan.id}</td>
                        <td>{plan.planName || plan.name}</td>
                        <td>{plan.planType || plan.productType || 'General'}</td>
                        <td>
                          <span className={`badge ${plan.status === 'ACTIVE' || plan.active ? 'bg-success' : 'bg-secondary'}`}>
                            {plan.status || (plan.active ? 'ACTIVE' : 'INACTIVE')}
                          </span>
                        </td>
                        <td>
                          <Link to={ROUTES.INSURER_ADMIN_PLANS} className="btn btn-outline-primary btn-sm">
                            Edit in Plan Editor
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Cancellations Tab */}
      {activeTab === 'CANCELLATIONS' && (
        <div className="dashboard-card">
          <div className="dashboard-card-header d-flex justify-content-between align-items-center">
            <span className="fw-bold">Policy Cancellation Requests</span>
            <span className="badge bg-secondary">Queue</span>
          </div>
          <div className="dashboard-card-body p-4 text-center text-muted">
            <i className="bi bi-inbox fs-2 text-secondary d-block mb-2"></i>
            <p className="fw-semibold mb-1">No pending policy cancellation requests</p>
            <p className="small mb-0">
              When customers submit cancellation requests, they will appear here for statutory refund review and approval.
            </p>
          </div>
        </div>
      )}
    </PortalLayout>
  );
}
