import React from 'react';
import AppLayout from '../../components/layout/AppLayout';

export default function ComparePlansView() {
  return (
    <AppLayout>
      <div className="mb-4">
        <h1 className="h3 fw-bold">Compare Insurance Plans</h1>
        <p className="text-muted small">
          Side-by-side comparison for up to 3 plans (SAD Section 4.3.2 / <code>GET /api/v1/plans/compare?ids=...</code>).
        </p>
      </div>

      {import.meta.env.DEV && (
        <div className="dev-hook-box">
          <span className="dev-hook-badge">Person 1 Ownership (Plan Comparison C4)</span>
          <p className="mb-0">
            This view connects to <code>GET /api/v1/plans/compare?ids=1,2,3</code>.
            Enforces maximum 3 plans boundary (returns <code>VALID-422 / 400</code> if more than 3 selected).
          </p>
        </div>
      )}

      <div className="dashboard-card">
        <div className="dashboard-card-header">
          <span className="fw-bold">Comparison Matrix</span>
          <span className="badge bg-info text-dark">Sample 3-Plan Comparison</span>
        </div>
        <div className="dashboard-card-body p-0">
          <div className="table-responsive">
            <table className="table table-hover mb-0 align-middle">
              <thead className="table-light">
                <tr>
                  <th style={{ minWidth: '160px' }}>Plan Attribute</th>
                  <th style={{ minWidth: '180px' }}>SafeShield Motor (4W)</th>
                  <th style={{ minWidth: '180px' }}>Bharat Care Swasthya</th>
                  <th style={{ minWidth: '180px' }}>SafeShield Two-Wheeler</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td className="fw-semibold">Insurer</td>
                  <td>SafeShield General</td>
                  <td>Bharat Care Insurance</td>
                  <td>SafeShield General</td>
                </tr>
                <tr>
                  <td className="fw-semibold">Category</td>
                  <td>4-Wheeler Motor</td>
                  <td>Personal Health</td>
                  <td>2-Wheeler Motor</td>
                </tr>
                <tr>
                  <td className="fw-semibold">Maximum Coverage</td>
                  <td>₹8,00,000 IDV</td>
                  <td>₹5,00,000 Sum Insured</td>
                  <td>₹85,000 IDV</td>
                </tr>
                <tr>
                  <td className="fw-semibold">Standard Deductible</td>
                  <td>₹1,500</td>
                  <td>₹0</td>
                  <td>₹500</td>
                </tr>
                <tr>
                  <td className="fw-semibold">Waiting Period</td>
                  <td>None (Immediate)</td>
                  <td>30 days (Waiting period)</td>
                  <td>None (Immediate)</td>
                </tr>
                <tr>
                  <td className="fw-semibold">Key Add-ons</td>
                  <td>Zero Depreciation, RSA</td>
                  <td>Critical Illness Booster</td>
                  <td>Pillion Rider Cover</td>
                </tr>
                <tr>
                  <td className="fw-semibold">Estimated Base</td>
                  <td className="text-primary fw-bold">₹8,450 / yr</td>
                  <td className="text-primary fw-bold">₹6,200 / yr</td>
                  <td className="text-primary fw-bold">₹1,850 / yr</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </AppLayout>
  );
}
