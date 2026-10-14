import React from 'react';
import PortalLayout from '../../components/layout/PortalLayout';

export default function ClaimsOfficerDashboardView() {
  return (
    <PortalLayout
      title="Claims Officer Queue"
      subtitle="Verification, assessment & duplicate check desk (SAD Section 4.3.5)"
      badgeText="Person 4 Core Module"
      badgeClass="bg-info text-dark"
    >
      <div className="dev-hook-box mb-4">
        <span className="dev-hook-badge">Person 4 Ownership (Claims Workflow C7)</span>
        <p className="mb-0">
          Connects to <code>GET /api/v1/claims?status=Assessment</code>, <code>POST /api/v1/claims/&#123;id&#125;/verify</code>,
          and <code>POST /api/v1/claims/&#123;id&#125;/decision</code>. Enforces multi-insurer scoping.
        </p>
      </div>

      <div className="dashboard-card">
        <div className="dashboard-card-header">
          <span className="fw-bold">Active Claim Assessment Queue</span>
          <span className="badge bg-primary">1 Pending</span>
        </div>
        <div className="dashboard-card-body p-0">
          <div className="table-responsive">
            <table className="table table-hover mb-0 align-middle">
              <thead className="table-light">
                <tr>
                  <th>Claim ID</th>
                  <th>Policy No</th>
                  <th>Claimed Amount</th>
                  <th>Duplicate Check</th>
                  <th>Stage</th>
                  <th>Officer Action</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td className="fw-semibold">CLM-2026-0042</td>
                  <td>POL-2026-4W-0091</td>
                  <td>₹32,000</td>
                  <td>
                    <span className="badge bg-success-subtle text-success">
                      Clear (INS-F-023)
                    </span>
                  </td>
                  <td>
                    <span className="status-badge status-assessment">Assessment</span>
                  </td>
                  <td>
                    <button className="btn btn-outline-primary btn-sm me-1">Review Bills</button>
                    <button className="btn btn-success btn-sm">Authorize Payout</button>
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
