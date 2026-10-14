import React from 'react';
import PortalLayout from '../../components/layout/PortalLayout';

export default function SystemAdminDashboardView() {
  return (
    <PortalLayout
      title="System Administration & Audit Portal"
      subtitle="Multi-insurer governance, business reporting, and immutable audit logs (SAD Section 4.3.6)"
      badgeText="System Admin"
      badgeClass="bg-danger"
    >
      <div className="dev-hook-box mb-4">
        <span className="dev-hook-badge">Shared Administration Module C11</span>
        <p className="mb-0">
          Connects to <code>GET /api/v1/reports/&#123;type&#125;</code> and <code>GET /api/v1/audit</code>.
          Enforces 5-year retention (<code>INS-NF-007</code>) and separate audit-reader database role (<code>INS-SR-008</code>).
        </p>
      </div>

      <div className="row g-3 mb-4">
        <div className="col-12 col-md-6">
          <div className="dashboard-card h-100">
            <div className="dashboard-card-header">
              <span className="fw-bold">Platform Insurers (Multi-Tenancy)</span>
              <span className="badge bg-secondary">2 Insurers Onboarded</span>
            </div>
            <div className="dashboard-card-body p-0">
              <ul className="list-group list-group-flush small">
                <li className="list-group-item d-flex justify-content-between align-items-center">
                  <div>
                    <span className="fw-semibold">SafeShield General (INS-A)</span>
                    <span className="d-block text-muted">2W & 4W Motor, Health</span>
                  </div>
                  <span className="status-badge status-active">Active</span>
                </li>
                <li className="list-group-item d-flex justify-content-between align-items-center">
                  <div>
                    <span className="fw-semibold">Bharat Care Insurance (INS-B)</span>
                    <span className="d-block text-muted">Personal Health Insurance</span>
                  </div>
                  <span className="status-badge status-active">Active</span>
                </li>
              </ul>
            </div>
          </div>
        </div>

        <div className="col-12 col-md-6">
          <div className="dashboard-card h-100">
            <div className="dashboard-card-header">
              <span className="fw-bold">Audit Log Feed Preview</span>
              <span className="badge bg-dark">Append-Only</span>
            </div>
            <div className="dashboard-card-body p-0">
              <ul className="list-group list-group-flush small">
                <li className="list-group-item">
                  <div className="d-flex justify-content-between text-muted small">
                    <span>user: underwriter.priya</span>
                    <span>13 Oct 2026 14:22</span>
                  </div>
                  <div className="fw-medium">action: DECIDE_APPLICATION [APP-2026-9021: APPROVED]</div>
                </li>
                <li className="list-group-item">
                  <div className="d-flex justify-content-between text-muted small">
                    <span>user: customer.asha</span>
                    <span>13 Oct 2026 14:15</span>
                  </div>
                  <div className="fw-medium">action: CHARGE_PREMIUM [POL-2026-4W-0091: ₹8,450]</div>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </PortalLayout>
  );
}
