import React, { useState } from 'react';
import PortalLayout from '../../components/layout/PortalLayout';

export default function ClaimsPlaceholder() {
  const [activeTab, setActiveTab] = useState('TRACK'); // 'TRACK' or 'FILE'

  return (
    <PortalLayout
      title="Claims Tracker & Filing Desk"
      subtitle="Customer claim lifecycle and state progression (SAD Section 4.1.3 & 4.3.5)"
      badgeText="Person 4 Core Module"
      badgeClass="bg-primary"
    >
      <div className="dev-hook-box mb-4">
        <span className="dev-hook-badge">Person 4 Ownership (Claims Workflow C7)</span>
        <p className="mb-0">
          Connects to <code>POST /api/v1/claims</code> (multipart) and <code>GET /api/v1/claims/&#123;id&#125;</code>.
          Enforces the 5-state claim lifecycle:
          <code>Document Verification &rarr; Assessment &rarr; Payment Processing &rarr; Settled / Rejected / Closed</code>.
        </p>
      </div>

      {/* Tabs */}
      <ul className="nav nav-tabs mb-4">
        <li className="nav-item">
          <button
            className={`nav-link ${activeTab === 'TRACK' ? 'active fw-bold' : ''}`}
            onClick={() => setActiveTab('TRACK')}
          >
            <i className="bi bi-clock-history me-1"></i>Track Active Claims
          </button>
        </li>
        <li className="nav-item">
          <button
            className={`nav-link ${activeTab === 'FILE' ? 'active fw-bold' : ''}`}
            onClick={() => setActiveTab('FILE')}
          >
            <i className="bi bi-plus-circle me-1"></i>File New Claim
          </button>
        </li>
      </ul>

      {activeTab === 'TRACK' ? (
        <div className="dashboard-card">
          <div className="dashboard-card-header">
            <span className="fw-bold">Claim Details: CLM-2026-0042</span>
            <span className="status-badge status-assessment">Assessment Stage</span>
          </div>
          <div className="dashboard-card-body">
            <div className="row g-3 mb-4">
              <div className="col-12 col-md-3">
                <span className="text-muted small d-block">Policy Reference</span>
                <span className="fw-semibold">POL-2026-4W-0091</span>
              </div>
              <div className="col-12 col-md-3">
                <span className="text-muted small d-block">Incident Date</span>
                <span className="fw-semibold">05 Oct 2026</span>
              </div>
              <div className="col-12 col-md-3">
                <span className="text-muted small d-block">Claimed Amount</span>
                <span className="fw-semibold text-primary">₹32,000</span>
              </div>
              <div className="col-12 col-md-3">
                <span className="text-muted small d-block">Duplicate Check</span>
                <span className="badge bg-success-subtle text-success">Passed (No Duplicate)</span>
              </div>
            </div>

            {/* State Progression Timeline */}
            <h5 className="h6 fw-bold mb-3">Claim State Machine Progress (ADR-008)</h5>
            <div className="p-3 bg-light rounded-3 mb-3">
              <div className="row text-center g-2 small">
                <div className="col-12 col-sm-4 text-success">
                  <i className="bi bi-check-circle-fill d-block fs-4 mb-1"></i>
                  <span className="fw-bold">1. Document Verification</span>
                  <span className="d-block text-muted small">Completed 06 Oct</span>
                </div>
                <div className="col-12 col-sm-4 text-warning">
                  <i className="bi bi-arrow-repeat d-block fs-4 mb-1"></i>
                  <span className="fw-bold">2. Officer Assessment</span>
                  <span className="d-block text-muted small">Assessing repair bills</span>
                </div>
                <div className="col-12 col-sm-4 text-muted">
                  <i className="bi bi-circle d-block fs-4 mb-1"></i>
                  <span className="fw-bold">3. Payout Processing</span>
                  <span className="d-block text-muted small">Pending decision</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="dashboard-card">
          <div className="dashboard-card-header">
            <span className="fw-bold">File Insurance Claim (Reimbursement)</span>
            <span className="badge bg-secondary">Multipart Form</span>
          </div>
          <div className="dashboard-card-body">
            <form onSubmit={(e) => { e.preventDefault(); alert('Claim submission shell: Person 4 will connect this to POST /api/v1/claims'); }}>
              <div className="row g-3 mb-3">
                <div className="col-12 col-md-6">
                  <label htmlFor="claim-policy" className="form-label small fw-semibold">
                    Select Active Policy
                  </label>
                  <select id="claim-policy" className="form-select form-select-sm" defaultValue="POL-2026-4W-0091">
                    <option value="POL-2026-4W-0091">POL-2026-4W-0091 (SafeShield Motor 4W)</option>
                  </select>
                </div>
                <div className="col-12 col-md-6">
                  <label htmlFor="incident-date" className="form-label small fw-semibold">
                    Incident Date (Must be within 30 days)
                  </label>
                  <input type="date" id="incident-date" className="form-control form-control-sm" defaultValue="2026-10-05" />
                </div>
              </div>

              <div className="mb-3">
                <label htmlFor="claimed-amount" className="form-label small fw-semibold">
                  Estimated Claim Amount (₹)
                </label>
                <input type="number" id="claimed-amount" className="form-control form-control-sm" defaultValue={25000} />
              </div>

              <div className="mb-3">
                <label htmlFor="claim-desc" className="form-label small fw-semibold">
                  Description of Incident
                </label>
                <textarea
                  id="claim-desc"
                  rows="3"
                  className="form-control form-control-sm"
                  placeholder="Describe what occurred, location, and damages..."
                ></textarea>
              </div>

              <div className="mb-3">
                <label htmlFor="claim-files" className="form-label small fw-semibold">
                  Upload Supporting Bills & FIR / Reports (Max 5MB per file)
                </label>
                <input type="file" id="claim-files" className="form-control form-control-sm" multiple />
              </div>

              <button type="submit" className="btn btn-primary btn-sm">
                <i className="bi bi-send me-1"></i>Submit Claim for Verification
              </button>
            </form>
          </div>
        </div>
      )}
    </PortalLayout>
  );
}
