import React from 'react';
import { useNavigate } from 'react-router-dom';
import PortalLayout from '../../components/layout/PortalLayout';

export default function ApplicationsPlaceholder() {
  const navigate = useNavigate();

  return (
    <PortalLayout
      title="My Applications"
      subtitle="Application lifecycle tracking (SAD Section 4.1.2 & 4.3.3)"
      badgeText="Person 3 Ownership"
      badgeClass="bg-info text-dark"
    >
      <div className="dev-hook-box mb-4">
        <span className="dev-hook-badge">Person 3 Ownership (Application Flow C5)</span>
        <p className="mb-0">
          Connects to <code>POST /api/v1/applications</code>. Drives states:
          <code>Submitted &rarr; Underwriting (Auto-Approved / Referred / Rejected) &rarr; Payment &rarr; Issued</code>.
        </p>
      </div>

      <div className="dashboard-card">
        <div className="dashboard-card-header">
          <span className="fw-bold">Recent Application Submissions</span>
          <span className="badge bg-secondary">Sample Feed</span>
        </div>
        <div className="dashboard-card-body p-0">
          <div className="table-responsive">
            <table className="table table-hover mb-0 align-middle">
              <thead className="table-light">
                <tr>
                  <th>Application ID</th>
                  <th>Plan Selected</th>
                  <th>Date Submitted</th>
                  <th>Underwriting Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td className="fw-semibold">APP-2026-9021</td>
                  <td>SafeShield Motor 4W</td>
                  <td>12 Oct 2026</td>
                  <td><span className="status-badge status-approved">Approved</span></td>
                  <td>
                    <button
                      id="pay-premium-9021-btn"
                      className="btn btn-primary btn-sm"
                      onClick={() => navigate('/customer/payment/9021')}
                    >
                      Pay Premium (₹8,450)
                    </button>
                  </td>
                </tr>
                <tr>
                  <td className="fw-semibold">APP-2026-8812</td>
                  <td>Bharat Care Swasthya</td>
                  <td>08 Oct 2026</td>
                  <td><span className="status-badge status-referred">Referred to Underwriter</span></td>
                  <td>
                    <span className="text-muted small">Under Review</span>
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
