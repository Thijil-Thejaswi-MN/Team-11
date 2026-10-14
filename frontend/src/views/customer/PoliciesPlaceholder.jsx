import React from 'react';
import PortalLayout from '../../components/layout/PortalLayout';

export default function PoliciesPlaceholder() {
  return (
    <PortalLayout
      title="My Policies & Lifecycle"
      subtitle="Active policies, OpenPDF certificates, renewals, and cancellations (SAD Section 4.3.4)"
      badgeText="Person 3 Ownership"
      badgeClass="bg-info text-dark"
    >
      <div className="dev-hook-box mb-4">
        <span className="dev-hook-badge">Person 3 Ownership (Policy Lifecycle C5)</span>
        <p className="mb-0">
          Connects to <code>GET /api/v1/policies</code>, <code>GET /api/v1/policies/&#123;id&#125;/document</code> (OpenPDF),
          <code>POST /api/v1/policies/&#123;id&#125;/renew</code>, and <code>POST /api/v1/policies/&#123;id&#125;/cancellation</code>.
        </p>
      </div>

      <div className="dashboard-card">
        <div className="dashboard-card-header">
          <span className="fw-bold">Policy Portfolio</span>
          <span className="badge bg-success">2 Active</span>
        </div>
        <div className="dashboard-card-body p-0">
          <div className="table-responsive">
            <table className="table table-hover mb-0 align-middle">
              <thead className="table-light">
                <tr>
                  <th>Policy Number</th>
                  <th>Plan & Insurer</th>
                  <th>Coverage Valid Until</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td className="fw-semibold">POL-2026-4W-0091</td>
                  <td>SafeShield Motor 4W (SafeShield)</td>
                  <td>31 Mar 2027</td>
                  <td><span className="status-badge status-active">Active</span></td>
                  <td>
                    <button className="btn btn-outline-secondary btn-sm me-1" title="Download Policy PDF">
                      <i className="bi bi-file-earmark-pdf me-1"></i>PDF
                    </button>
                    <button className="btn btn-outline-danger btn-sm" title="Request Cancellation">
                      Cancel
                    </button>
                  </td>
                </tr>
                <tr>
                  <td className="fw-semibold">POL-2025-HL-0012</td>
                  <td>Bharat Care Swasthya (Bharat Care)</td>
                  <td>02 Nov 2026</td>
                  <td><span className="status-badge status-referred">Expiring Soon</span></td>
                  <td>
                    <button className="btn btn-primary btn-sm me-1" title="Renew Policy">
                      <i className="bi bi-arrow-repeat me-1"></i>Renew
                    </button>
                    <button className="btn btn-outline-secondary btn-sm" title="Download Document">
                      <i className="bi bi-file-earmark-pdf"></i>
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
