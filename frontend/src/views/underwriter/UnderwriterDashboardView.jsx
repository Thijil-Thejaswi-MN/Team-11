import React from 'react';
import PortalLayout from '../../components/layout/PortalLayout';
import { useAuth } from '../../context/AuthContext';

export default function UnderwriterDashboardView() {
  const { currentUser } = useAuth();

  return (
    <PortalLayout
      title="Underwriter Assessment Queue"
      subtitle="Manual referral decisioning for SafeShield General (SAD Section 4.3.3)"
      badgeText="Underwriter Portal"
      badgeClass="bg-success"
    >
      <div className="dev-hook-box mb-4">
        <span className="dev-hook-badge">Person 1 Ownership (Underwriting Engine C6)</span>
        <p className="mb-0">
          Connects to <code>GET /api/v1/applications/referred</code> and <code>POST /api/v1/applications/&#123;id&#125;/decision</code>.
          Restricted to Underwriters of insurer (<code>INS-SR-004</code> multi-insurer isolation).
        </p>
      </div>

      <div className="dashboard-card">
        <div className="dashboard-card-header">
          <span className="fw-bold">Applications Awaiting Underwriter Review</span>
          <span className="badge bg-warning text-dark">1 Referred</span>
        </div>
        <div className="dashboard-card-body p-0">
          <div className="table-responsive">
            <table className="table table-hover mb-0 align-middle">
              <thead className="table-light">
                <tr>
                  <th>App ID</th>
                  <th>Applicant</th>
                  <th>Plan Type</th>
                  <th>Triggered Referral Rule</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td className="fw-semibold">APP-2026-8812</td>
                  <td>Rajesh Kumar (Age: 62)</td>
                  <td>Personal Health</td>
                  <td>
                    <span className="badge bg-warning-subtle text-dark border">
                      Rule: Applicant Age &gt;= 60 (SRS 4.4.1)
                    </span>
                  </td>
                  <td>
                    <button className="btn btn-outline-success btn-sm me-1" title="Approve Application">
                      Approve
                    </button>
                    <button className="btn btn-outline-danger btn-sm" title="Reject Application">
                      Reject
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
