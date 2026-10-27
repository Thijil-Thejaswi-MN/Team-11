import React from 'react';
import { Link } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import { ROUTES } from '../../routes/routePaths';
import { useAuth, ROLES } from '../../context/AuthContext';

export default function HomeView() {
  const { switchRole } = useAuth();

  return (
    <AppLayout>
      {/* Hero Section */}
      <div className="p-4 p-md-5 mb-4 rounded-3 text-white" style={{ background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)' }}>
        <div className="container-fluid py-2">
          <div className="d-flex align-items-center gap-2 mb-2">
            <span className="badge bg-info text-dark fw-bold">Team 11</span>
            <span className="badge bg-secondary">Deliverable 2</span>
          </div>
          <h1 className="display-6 fw-bold">Insurance Workflow Automation System</h1>
          <p className="col-md-9 fs-6 text-light mb-4">
            End-to-end automation for 2-wheeler, 4-wheeler, and personal health insurance workflows:
            from automated rule-based underwriting to instant policy issuance, renewals, and claims processing.
          </p>
          <div className="d-flex flex-wrap gap-2">
            <Link to={ROUTES.PLANS} className="btn btn-primary btn-sm px-3" id="home-browse-btn">
              <i className="bi bi-search me-1"></i>Browse Insurance Plans
            </Link>
            <Link to={ROUTES.PLANS_COMPARE} className="btn btn-outline-light btn-sm px-3" id="home-compare-btn">
              <i className="bi bi-arrow-left-right me-1"></i>Compare Plans
            </Link>
          </div>
        </div>
      </div>

      {/* Role Portal Cards (Easy Testing for Reviewers & Team Members - Dev demo only) */}
      {import.meta.env.DEV && (
        <section aria-labelledby="dev-portals-heading" className="dev-portals-wrapper mb-4">
          <div className="d-flex align-items-center gap-2 mb-3">
            <h2 className="h4 fw-bold mb-0" id="dev-portals-heading">Role-Based Portals (SAD Section 4.5)</h2>
            <span className="badge bg-warning text-dark">Dev demo only</span>
          </div>
          <div className="row g-3">
            {/* Customer Portal */}
            <div className="col-12 col-md-6 col-lg-4">
              <div className="dashboard-card h-100">
                <div className="dashboard-card-header">
                  <span className="fw-bold">Customer Portal</span>
                  <span className="badge bg-primary">Person 1, 3, 4</span>
                </div>
                <div className="dashboard-card-body d-flex flex-column">
                  <p className="text-muted small">
                    Explore plans, calculate motor/health quotes, submit KYC, pay premiums, download policy PDFs, and track claims.
                  </p>
                  <div className="mt-auto">
                    <Link
                      to={ROUTES.CUSTOMER_DASHBOARD}
                      className="btn btn-outline-primary btn-sm w-100"
                      onClick={() => switchRole(ROLES.CUSTOMER)}
                    >
                      Enter as Customer (Asha)
                    </Link>
                  </div>
                </div>
              </div>
            </div>

            {/* Insurer Admin Portal */}
            <div className="col-12 col-md-6 col-lg-4">
              <div className="dashboard-card h-100">
                <div className="dashboard-card-header">
                  <span className="fw-bold">Insurer Admin</span>
                  <span className="badge bg-warning text-dark">Person 1 & 3</span>
                </div>
                <div className="dashboard-card-body d-flex flex-column">
                  <p className="text-muted small">
                    Configure insurance plans, premium rating rules, underwriting parameters, add-ons, and review cancellation requests.
                  </p>
                  <div className="mt-auto">
                    <Link
                      to={ROUTES.INSURER_ADMIN_DASHBOARD}
                      className="btn btn-outline-warning btn-sm w-100 text-dark"
                      onClick={() => switchRole(ROLES.INSURER_ADMIN)}
                    >
                      Enter as Insurer Admin
                    </Link>
                  </div>
                </div>
              </div>
            </div>

            {/* Underwriter Portal */}
            <div className="col-12 col-md-6 col-lg-4">
              <div className="dashboard-card h-100">
                <div className="dashboard-card-header">
                  <span className="fw-bold">Underwriter Portal</span>
                  <span className="badge bg-success">Person 1</span>
                </div>
                <div className="dashboard-card-body d-flex flex-column">
                  <p className="text-muted small">
                    Review referred applications (e.g. vehicle age &gt; 15, customer age &gt;= 60, pre-existing diseases) and record decisions.
                  </p>
                  <div className="mt-auto">
                    <Link
                      to={ROUTES.UNDERWRITER_DASHBOARD}
                      className="btn btn-outline-success btn-sm w-100"
                      onClick={() => switchRole(ROLES.UNDERWRITER)}
                    >
                      Enter as Underwriter (Priya)
                    </Link>
                  </div>
                </div>
              </div>
            </div>

            {/* Claims Officer Portal */}
            <div className="col-12 col-md-6 col-lg-4">
              <div className="dashboard-card h-100">
                <div className="dashboard-card-header">
                  <span className="fw-bold">Claims Officer</span>
                  <span className="badge bg-info text-dark">Person 4</span>
                </div>
                <div className="dashboard-card-body d-flex flex-column">
                  <p className="text-muted small">
                    Enforce claim state machine, document verification, duplicate detection check, assessment, and claim payout authorization.
                  </p>
                  <div className="mt-auto">
                    <Link
                      to={ROUTES.CLAIMS_OFFICER_DASHBOARD}
                      className="btn btn-outline-info btn-sm w-100 text-dark"
                      onClick={() => switchRole(ROLES.CLAIMS_OFFICER)}
                    >
                      Enter as Claims Officer (Manoj)
                    </Link>
                  </div>
                </div>
              </div>
            </div>

            {/* System Admin Portal */}
            <div className="col-12 col-md-6 col-lg-4">
              <div className="dashboard-card h-100">
                <div className="dashboard-card-header">
                  <span className="fw-bold">System Admin</span>
                  <span className="badge bg-danger">Shared</span>
                </div>
                <div className="dashboard-card-body d-flex flex-column">
                  <p className="text-muted small">
                    System health overview, multi-insurer marketplace management, aggregated business reports, and immutable audit logs.
                  </p>
                  <div className="mt-auto">
                    <Link
                      to={ROUTES.SYSTEM_ADMIN_DASHBOARD}
                      className="btn btn-outline-danger btn-sm w-100"
                      onClick={() => switchRole(ROLES.SYSTEM_ADMIN)}
                    >
                      Enter as System Admin
                    </Link>
                  </div>
                </div>
              </div>
            </div>

            {/* Team Architecture Specs */}
            <div className="col-12 col-md-6 col-lg-4">
              <div className="dashboard-card h-100 bg-light">
                <div className="dashboard-card-header">
                  <span className="fw-bold">Architecture Specs</span>
                  <span className="badge bg-secondary">SAD v1.1</span>
                </div>
                <div className="dashboard-card-body small">
                  <ul className="list-unstyled mb-0 text-muted">
                    <li><i className="bi bi-check2 text-success me-1"></i>Modular Monolith (ADR-001)</li>
                    <li><i className="bi bi-check2 text-success me-1"></i>REST /api/v1 (ADR-002)</li>
                    <li><i className="bi bi-check2 text-success me-1"></i>MySQL 8 InnoDB (ADR-003)</li>
                    <li><i className="bi bi-check2 text-success me-1"></i>State Machines (ADR-008)</li>
                    <li><i className="bi bi-check2 text-success me-1"></i>360px Mobile Layout (INS-NF-005)</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </section>
      )}
    </AppLayout>
  );
}
