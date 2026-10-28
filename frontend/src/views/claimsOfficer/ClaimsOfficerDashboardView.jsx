import React, { useState, useEffect, useMemo } from 'react';
import PortalLayout from '../../components/layout/PortalLayout';
import apiClient from '../../api/apiClient';

/**
 * Standard claim status mapping for display, styling and terminal state detection
 */
export const CLAIM_STATUS_MAP = {
  SUBMITTED: { label: 'Submitted', badge: 'bg-secondary text-white', terminal: false },
  DOCUMENT_VERIFICATION: { label: 'Document Verification', badge: 'bg-warning-subtle text-dark border border-warning', terminal: false },
  ASSESSMENT: { label: 'Assessment', badge: 'bg-primary-subtle text-primary border border-primary', terminal: false },
  APPROVED: { label: 'Approved', badge: 'bg-info-subtle text-dark border', terminal: false },
  PAYMENT_PROCESSING: { label: 'Payment Processing', badge: 'bg-info text-dark', terminal: false },
  SETTLED: { label: 'Settled', badge: 'bg-success text-white', terminal: true },
  REJECTED: { label: 'Rejected', badge: 'bg-danger text-white', terminal: true },
  CLOSED_NO_RESPONSE: { label: 'Closed - No Response', badge: 'bg-dark text-white', terminal: true },
};

/**
 * Normalizes backend enum strings (e.g. "DOCUMENT_VERIFICATION") or display strings ("Document Verification")
 */
export function normalizeStatus(rawStatus) {
  if (!rawStatus) return 'SUBMITTED';
  const upper = rawStatus.toString().trim().toUpperCase().replace(/[\s-]+/g, '_');
  if (upper === 'CLOSED_NO_RESPONSE' || upper === 'CLOSED___NO_RESPONSE') return 'CLOSED_NO_RESPONSE';
  if (upper === 'DOCUMENT_VERIFICATION') return 'DOCUMENT_VERIFICATION';
  if (upper === 'ASSESSMENT') return 'ASSESSMENT';
  if (upper === 'PAYMENT_PROCESSING') return 'PAYMENT_PROCESSING';
  if (upper === 'SETTLED') return 'SETTLED';
  if (upper === 'REJECTED') return 'REJECTED';
  if (upper === 'APPROVED') return 'APPROVED';
  if (upper === 'SUBMITTED') return 'SUBMITTED';
  return upper;
}

export function isTerminalStatus(rawStatus) {
  const norm = normalizeStatus(rawStatus);
  return CLAIM_STATUS_MAP[norm]?.terminal ?? false;
}

export function getStatusDisplay(rawStatus) {
  const norm = normalizeStatus(rawStatus);
  return CLAIM_STATUS_MAP[norm] || { label: rawStatus || 'Unknown', badge: 'bg-secondary text-white', terminal: false };
}

export default function ClaimsOfficerDashboardView() {
  const [claims, setClaims] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState(null);
  const [successBanner, setSuccessBanner] = useState(null);

  // Filters: default to ACTIVE (exclude Settled, Rejected, Closed - No Response)
  const [statusFilter, setStatusFilter] = useState('ACTIVE');
  const [searchQuery, setSearchQuery] = useState('');

  // Modals state
  const [detailClaim, setDetailClaim] = useState(null);
  const [verifyModalClaim, setVerifyModalClaim] = useState(null);
  const [decisionModalClaim, setDecisionModalClaim] = useState(null);

  // Verification form state
  const [documentsComplete, setDocumentsComplete] = useState(true);
  const [missingDocsInput, setMissingDocsInput] = useState('');
  const [verifyNotes, setVerifyNotes] = useState('');
  const [verifySubmitting, setVerifySubmitting] = useState(false);
  const [verifyError, setVerifyError] = useState(null);

  // Decision form state
  const [decisionType, setDecisionType] = useState('Approve'); // 'Approve' | 'Reject'
  const [assessedAmountInput, setAssessedAmountInput] = useState('');
  const [decisionReason, setDecisionReason] = useState('');
  const [decisionSubmitting, setDecisionSubmitting] = useState(false);
  const [decisionError, setDecisionError] = useState(null);

  // Fetch claims from backend
  const fetchClaims = async () => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const response = await apiClient.get('/claims');
      const data = Array.isArray(response.data) ? response.data : [];
      setClaims(data);
    } catch (err) {
      const msg = err.message || 'Failed to load claims queue from server.';
      setErrorMessage(msg);
      setClaims([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchClaims();
  }, []);

  // Filtered claims logic: excludes Settled, Rejected, Closed by default (ACTIVE queue)
  const filteredClaims = useMemo(() => {
    return claims.filter((claim) => {
      const norm = normalizeStatus(claim.status);

      // Status filtering
      if (statusFilter === 'ACTIVE') {
        if (isTerminalStatus(norm)) return false;
      } else if (statusFilter !== 'ALL') {
        if (norm !== statusFilter) return false;
      }

      // Search query filter
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const matchesNo = claim.claimNo?.toLowerCase().includes(q);
        const matchesPol = claim.policyNo?.toLowerCase().includes(q);
        const matchesDesc = claim.description?.toLowerCase().includes(q);
        const matchesCust = claim.customerId?.toString().includes(q);
        if (!matchesNo && !matchesPol && !matchesDesc && !matchesCust) {
          return false;
        }
      }

      return true;
    });
  }, [claims, statusFilter, searchQuery]);

  // Metric counts
  const metrics = useMemo(() => {
    let activeCount = 0;
    let verifyCount = 0;
    let assessCount = 0;
    let paymentCount = 0;
    let duplicateCount = 0;

    claims.forEach((c) => {
      const norm = normalizeStatus(c.status);
      if (!isTerminalStatus(norm)) activeCount++;
      if (norm === 'DOCUMENT_VERIFICATION') verifyCount++;
      if (norm === 'ASSESSMENT') assessCount++;
      if (norm === 'PAYMENT_PROCESSING') paymentCount++;
      if (c.duplicateFlag) duplicateCount++;
    });

    return { activeCount, verifyCount, assessCount, paymentCount, duplicateCount };
  }, [claims]);

  // Open Document Verification Modal
  const handleOpenVerify = (claim) => {
    setVerifyModalClaim(claim);
    setDocumentsComplete(true);
    setMissingDocsInput('');
    setVerifyNotes('');
    setVerifyError(null);
  };

  // Submit Document Verification
  const handleSubmitVerify = async (e) => {
    e.preventDefault();
    if (!verifyModalClaim) return;

    setVerifySubmitting(true);
    setVerifyError(null);

    const missingList = documentsComplete
      ? []
      : missingDocsInput
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean);

    const payload = {
      documentsComplete: Boolean(documentsComplete),
      missing: missingList,
      notes: verifyNotes.trim() || (documentsComplete ? 'Documents verified complete' : 'Additional documents requested'),
    };

    try {
      const res = await apiClient.post(`/claims/${verifyModalClaim.id}/verify`, payload);
      const updatedStatus = res.data?.status || (documentsComplete ? 'ASSESSMENT' : 'DOCUMENT_VERIFICATION');

      setSuccessBanner(
        `Claim ${verifyModalClaim.claimNo} successfully updated: ${
          documentsComplete ? 'Documents verified -> Moved to Assessment.' : 'Additional documents requested -> Remains in Document Verification.'
        }`
      );

      // Update local state
      setClaims((prev) =>
        prev.map((c) =>
          c.id === verifyModalClaim.id
            ? { ...c, status: updatedStatus, reason: payload.notes }
            : c
        )
      );

      setVerifyModalClaim(null);
    } catch (err) {
      const status = err.status || 500;
      let title = 'Verification Failed';
      let msg = err.message || 'An error occurred during verification.';

      if (status === 403) {
        title = 'Access Forbidden (403)';
        msg = 'You do not have permission to verify claims belonging to another insurer.';
      } else if (status === 409) {
        title = 'State Conflict (409)';
        msg = 'This claim is not in the Document Verification stage.';
      } else if (status === 422) {
        title = 'Validation Error (422)';
        msg = err.message || 'Invalid verification request parameters.';
      }

      setVerifyError({ title, message: msg });
    } finally {
      setVerifySubmitting(false);
    }
  };

  // Open Decision Modal
  const handleOpenDecision = (claim) => {
    setDecisionModalClaim(claim);
    setDecisionType('Approve');
    setAssessedAmountInput(claim.assessedAmount || claim.claimedAmount || '');
    setDecisionReason('');
    setDecisionError(null);
  };

  // Submit Decision
  const handleSubmitDecision = async (e) => {
    e.preventDefault();
    if (!decisionModalClaim) return;

    // Reason is mandatory per P1 contract & IRDAI compliance
    if (!decisionReason.trim()) {
      setDecisionError({
        title: 'Validation Error (422)',
        message: 'A detailed assessment/decision reason is strictly mandatory for the audit log.',
      });
      return;
    }

    if (decisionType === 'Approve' && (!assessedAmountInput || Number(assessedAmountInput) <= 0)) {
      setDecisionError({
        title: 'Validation Error (422)',
        message: 'A valid assessed payout amount greater than ₹0 is required for approval.',
      });
      return;
    }

    setDecisionSubmitting(true);
    setDecisionError(null);

    const payload = {
      decision: decisionType, // "Approve" | "Reject"
      assessedAmount: decisionType === 'Approve' ? Number(assessedAmountInput) : null,
      reason: decisionReason.trim(),
    };

    try {
      const res = await apiClient.post(`/claims/${decisionModalClaim.id}/decision`, payload);
      const updatedStatus = res.data?.status || (decisionType === 'Approve' ? 'PAYMENT_PROCESSING' : 'REJECTED');
      const updatedAssessed = res.data?.assessedAmount ?? (decisionType === 'Approve' ? Number(assessedAmountInput) : null);

      setSuccessBanner(
        `Claim ${decisionModalClaim.claimNo} successfully adjudicated: ${
          decisionType === 'Approve' ? 'Approved -> Moved to Payment Processing.' : 'Rejected with audit justification.'
        }`
      );

      // Update local state
      setClaims((prev) =>
        prev.map((c) =>
          c.id === decisionModalClaim.id
            ? {
                ...c,
                status: updatedStatus,
                assessedAmount: updatedAssessed,
                reason: payload.reason,
              }
            : c
        )
      );

      setDecisionModalClaim(null);
    } catch (err) {
      const status = err.status || 500;
      let title = 'Adjudication Failed';
      let msg = err.message || 'An error occurred while recording the decision.';

      if (status === 422) {
        title = 'Validation Error (422)';
        msg = err.message || 'Decision reason is strictly required.';
      } else if (status === 409) {
        title = 'State Conflict (409)';
        msg = 'Claim must be in Assessment status to record a decision.';
      } else if (status === 403) {
        title = 'Access Forbidden (403)';
        msg = 'You do not have permission to adjudicate claims for another insurer.';
      }

      setDecisionError({ title, message: msg });
    } finally {
      setDecisionSubmitting(false);
    }
  };

  return (
    <PortalLayout
      title="Claims Officer Queue & Appraisal Desk"
      subtitle="Verification, loss assessment, adjudication, and decisioning (SAD §4.1.3, §4.3.5, INS-F-014)"
      badgeText="Claims Officer Desk"
      badgeClass="bg-info text-dark"
    >
      {/* Dev Hook Box */}
      <div className="dev-hook-box mb-4">
        <span className="dev-hook-badge">Person 1 Ownership: Claims Officer Screens (P1-12, Decision D-11)</span>
        <p className="mb-0 small">
          Agreed in Decision D-11: Raghunandan (P4) owns the backend claims entities and service APIs.
          Person 1 (Thijil) owns the appraisal screens (P1-12), duplicate claim detection review banner (INS-F-023),
          stale claim auto-close scheduler (P1-19, TC-Clm-06), and STP independent test verification (TC-Clm-01 to TC-Clm-04).
        </p>
      </div>

      {/* Success Notification Banner */}
      {successBanner && (
        <div className="alert alert-success alert-dismissible fade show mb-4 shadow-sm" role="alert">
          <div className="d-flex align-items-center">
            <span className="fs-5 me-2">✅</span>
            <div>{successBanner}</div>
          </div>
          <button
            type="button"
            className="btn-close"
            onClick={() => setSuccessBanner(null)}
            aria-label="Close"
          ></button>
        </div>
      )}

      {/* Error Banner */}
      {errorMessage && (
        <div className="alert alert-warning alert-dismissible fade show mb-4 shadow-sm" role="alert" data-testid="claims-error-banner">
          <div className="d-flex align-items-center justify-content-between flex-wrap gap-2">
            <div className="d-flex align-items-center">
              <span className="fs-5 me-2">⚠️</span>
              <div>{errorMessage}</div>
            </div>
            <button
              type="button"
              className="btn btn-sm btn-outline-dark"
              onClick={fetchClaims}
              data-testid="error-retry-btn"
            >
              🔄 Retry
            </button>
          </div>
          <button
            type="button"
            className="btn-close"
            onClick={() => setErrorMessage(null)}
            aria-label="Close"
          ></button>
        </div>
      )}

      {/* KPI Metric Summary Strip */}
      <div className="row g-3 mb-4">
        <div className="col-6 col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-3">
              <span className="text-muted extra-small d-block text-uppercase fw-bold">Active Queue</span>
              <h3 className="fw-bold mb-0 text-primary mt-1">{metrics.activeCount}</h3>
              <span className="text-muted extra-small">Excludes settled/closed</span>
            </div>
          </div>
        </div>

        <div className="col-6 col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-3">
              <span className="text-muted extra-small d-block text-uppercase fw-bold">Doc Verification</span>
              <h3 className="fw-bold mb-0 text-warning mt-1">{metrics.verifyCount}</h3>
              <span className="text-muted extra-small">Awaiting document check</span>
            </div>
          </div>
        </div>

        <div className="col-6 col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-3">
              <span className="text-muted extra-small d-block text-uppercase fw-bold">Assessment</span>
              <h3 className="fw-bold mb-0 text-info mt-1">{metrics.assessCount}</h3>
              <span className="text-muted extra-small">Ready for decision</span>
            </div>
          </div>
        </div>

        <div className="col-6 col-md-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body p-3">
              <span className="text-muted extra-small d-block text-uppercase fw-bold">Duplicate Flags</span>
              <h3 className="fw-bold mb-0 text-danger mt-1">{metrics.duplicateCount}</h3>
              <span className="text-muted extra-small">INS-F-023 Held for review</span>
            </div>
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body p-3">
          <div className="row g-3 align-items-center">
            {/* Search Input */}
            <div className="col-12 col-md-5">
              <div className="input-group">
                <span className="input-group-text bg-white border-end-0">🔍</span>
                <input
                  type="text"
                  className="form-control border-start-0"
                  id="claimSearchInput"
                  data-testid="claim-search-input"
                  placeholder="Search by Claim No, Policy No, Customer ID..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>
            </div>

            {/* Stage Selector */}
            <div className="col-7 col-md-4">
              <select
                className="form-select"
                id="claimStageFilter"
                data-testid="claim-stage-filter"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <option value="ACTIVE">Pending Action Queue (Default)</option>
                <option value="ALL">All Claims ({claims.length})</option>
                <option value="DOCUMENT_VERIFICATION">Document Verification</option>
                <option value="ASSESSMENT">Assessment</option>
                <option value="PAYMENT_PROCESSING">Payment Processing</option>
                <option value="SETTLED">Settled</option>
                <option value="REJECTED">Rejected</option>
                <option value="CLOSED_NO_RESPONSE">Closed - No Response</option>
              </select>
            </div>

            {/* Refresh Button */}
            <div className="col-5 col-md-3 text-end">
              <button
                type="button"
                className="btn btn-outline-secondary w-100"
                onClick={fetchClaims}
                disabled={loading}
              >
                {loading ? 'Refreshing...' : '🔄 Refresh Queue'}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Queue Table Card */}
      <div className="dashboard-card mb-4">
        <div className="dashboard-card-header d-flex flex-wrap justify-content-between align-items-center gap-2">
          <div>
            <span className="fw-bold me-2">Claims Adjudication Queue</span>
            <span className="badge bg-primary px-2 py-1">
              {filteredClaims.length} Claim{filteredClaims.length === 1 ? '' : 's'}
            </span>
          </div>
          <span className="text-muted extra-small">
            Showing: {statusFilter === 'ACTIVE' ? 'Active Worklist (excludes Settled/Rejected)' : statusFilter}
          </span>
        </div>

        <div className="dashboard-card-body p-0">
          {loading ? (
            <div className="text-center py-5">
              <div className="spinner-border text-info" role="status">
                <span className="visually-hidden">Loading claims...</span>
              </div>
              <p className="mt-2 text-muted small">Loading insurer claims queue...</p>
            </div>
          ) : errorMessage ? (
            <div className="text-center py-5" data-testid="claims-error-state">
              <span className="fs-1 text-danger">⚠️</span>
              <h5 className="mt-2 fw-semibold text-danger">Failed to Load Claims Queue</h5>
              <p className="text-muted small mb-3">{errorMessage}</p>
              <button
                type="button"
                className="btn btn-primary btn-sm"
                onClick={fetchClaims}
                data-testid="retry-btn"
              >
                🔄 Retry
              </button>
            </div>
          ) : filteredClaims.length === 0 ? (
            <div className="text-center py-5">
              <span className="fs-1 text-muted">📋</span>
              <h5 className="mt-2 fw-semibold">No Claims in Queue</h5>
              <p className="text-muted small mb-0">
                {statusFilter === 'ACTIVE'
                  ? 'All active claims have been processed or settled.'
                  : 'No claims match the selected filter criteria.'}
              </p>
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover mb-0 align-middle" data-testid="claims-table">
                <thead className="table-light">
                  <tr>
                    <th>Claim No &amp; Date</th>
                    <th>Policy &amp; Customer</th>
                    <th>Incident Summary</th>
                    <th>Amount (Claimed / Assessed)</th>
                    <th>Current Stage</th>
                    <th className="text-end">Appraisal Action</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredClaims.map((claim) => {
                    const statusInfo = getStatusDisplay(claim.status);
                    const norm = normalizeStatus(claim.status);

                    return (
                      <tr key={claim.id} data-testid={`claim-row-${claim.id}`}>
                        {/* Claim No & Incident Date */}
                        <td>
                          <div className="d-flex align-items-center gap-1 flex-wrap">
                            <span className="badge bg-light text-dark border fw-bold font-monospace">
                              {claim.claimNo || `CLM-${claim.id}`}
                            </span>
                            {claim.duplicateFlag && (
                              <span
                                className="badge bg-danger text-white extra-small"
                                data-testid={`duplicate-badge-${claim.id}`}
                                title="INS-F-023: Duplicate claim flag (same policy, incident date and claimed amount)"
                              >
                                ⚠️ Possible Duplicate
                              </span>
                            )}
                          </div>
                          <div className="text-muted extra-small mt-1">
                            Incident: {claim.incidentDate || 'N/A'}
                          </div>
                        </td>

                        {/* Policy & Customer */}
                        <td>
                          <div className="fw-semibold text-primary">
                            {claim.policyNo || `Policy #${claim.policyId}`}
                          </div>
                          <div className="text-muted extra-small">Customer #{claim.customerId}</div>
                        </td>

                        {/* Summary */}
                        <td>
                          <div
                            className="small text-truncate"
                            style={{ maxWidth: '240px' }}
                            title={claim.description}
                          >
                            {claim.description || 'No description recorded'}
                          </div>
                          <div className="text-muted extra-small mt-1">
                            📎 {claim.documentIds?.length || 0} Incident Document{claim.documentIds?.length === 1 ? '' : 's'}
                          </div>
                        </td>

                        {/* Amounts */}
                        <td>
                          <div className="fw-bold text-dark">
                            ₹{Number(claim.claimedAmount || claim.claimAmount || 0).toLocaleString('en-IN')}
                          </div>
                          {claim.assessedAmount != null && (
                            <div className="text-success extra-small fw-semibold">
                              Assessed: ₹{Number(claim.assessedAmount).toLocaleString('en-IN')}
                            </div>
                          )}
                        </td>

                        {/* Stage Badge */}
                        <td>
                          <span className={`badge ${statusInfo.badge}`} data-testid={`status-badge-${claim.id}`}>
                            {statusInfo.label}
                          </span>
                        </td>

                        {/* Action Buttons */}
                        <td className="text-end">
                          <div className="d-flex justify-content-end gap-1 flex-wrap">
                            <button
                              type="button"
                              className="btn btn-sm btn-outline-secondary"
                              onClick={() => setDetailClaim(claim)}
                              data-testid={`view-details-btn-${claim.id}`}
                              title="View full claim details & history"
                            >
                              Details
                            </button>

                            {norm === 'DOCUMENT_VERIFICATION' && (
                              <button
                                type="button"
                                className="btn btn-sm btn-outline-warning"
                                onClick={() => handleOpenVerify(claim)}
                                data-testid={`verify-btn-${claim.id}`}
                              >
                                Verify Docs
                              </button>
                            )}

                            {norm === 'ASSESSMENT' && (
                              <button
                                type="button"
                                className="btn btn-sm btn-outline-primary"
                                onClick={() => handleOpenDecision(claim)}
                                data-testid={`decide-btn-${claim.id}`}
                              >
                                Assess &amp; Decide
                              </button>
                            )}

                            {norm === 'PAYMENT_PROCESSING' && (
                              <button
                                type="button"
                                className="btn btn-sm btn-outline-info"
                                onClick={() => setDetailClaim(claim)}
                                data-testid={`payout-info-btn-${claim.id}`}
                                title="Awaiting settlement payout trigger"
                              >
                                In Settlement
                              </button>
                            )}

                            {norm === 'SETTLED' && (
                              <span className="text-success small fw-semibold align-self-center px-1">
                                ✓ Settled
                              </span>
                            )}

                            {norm === 'REJECTED' && (
                              <span className="text-danger small fw-semibold align-self-center px-1">
                                ✗ Rejected
                              </span>
                            )}

                            {norm === 'CLOSED_NO_RESPONSE' && (
                              <span className="text-muted small fw-semibold align-self-center px-1">
                                Closed (Stale)
                              </span>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* =========================================================================
          Claim Detail & History Modal
          ========================================================================= */}
      {detailClaim && (
        <div
          className="modal fade show d-block"
          tabIndex="-1"
          role="dialog"
          style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
          data-testid="claim-detail-modal"
        >
          <div className="modal-dialog modal-dialog-centered modal-lg">
            <div className="modal-content shadow-lg border-0">
              <div className="modal-header border-bottom">
                <div>
                  <h5 className="modal-title fw-bold mb-0">
                    Claim Detail — {detailClaim.claimNo || `CLM-${detailClaim.id}`}
                  </h5>
                  <span className="text-muted extra-small">
                    Insurer #{detailClaim.insurerId} · Filed: {detailClaim.createdAt ? new Date(detailClaim.createdAt).toLocaleString('en-IN') : 'N/A'}
                  </span>
                </div>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setDetailClaim(null)}
                  aria-label="Close"
                ></button>
              </div>

              <div className="modal-body p-4">
                {/* Duplicate Claim Warning Banner (INS-F-023) */}
                {detailClaim.duplicateFlag && (
                  <div
                    className="alert alert-danger border-danger d-flex align-items-start gap-2 mb-4"
                    role="alert"
                    data-testid="duplicate-warning-banner"
                  >
                    <span className="fs-4">⚠️</span>
                    <div>
                      <strong className="d-block">Possible Duplicate Claim Detected (INS-F-023)</strong>
                      <div className="small">
                        This claim has the same policy reference, incident date, and claimed amount as an earlier claim filing.
                        It is held for mandatory manual appraisal and fraud scrutiny.
                      </div>
                    </div>
                  </div>
                )}

                {/* Primary Metadata Strip */}
                <div className="bg-light p-3 rounded mb-4 border">
                  <div className="row g-3 small">
                    <div className="col-6 col-sm-3">
                      <span className="text-muted d-block">Policy Reference:</span>
                      <span className="fw-bold">{detailClaim.policyNo || `Policy #${detailClaim.policyId}`}</span>
                    </div>

                    <div className="col-6 col-sm-3">
                      <span className="text-muted d-block">Customer ID:</span>
                      <span className="fw-semibold">User #{detailClaim.customerId}</span>
                    </div>

                    <div className="col-6 col-sm-3">
                      <span className="text-muted d-block">Claimed Amount:</span>
                      <span className="fw-bold text-dark">
                        ₹{Number(detailClaim.claimedAmount || detailClaim.claimAmount || 0).toLocaleString('en-IN')}
                      </span>
                    </div>

                    <div className="col-6 col-sm-3">
                      <span className="text-muted d-block">Current Stage:</span>
                      <span className={`badge ${getStatusDisplay(detailClaim.status).badge}`}>
                        {getStatusDisplay(detailClaim.status).label}
                      </span>
                    </div>
                  </div>

                  <div className="row g-3 small mt-1 pt-2 border-top">
                    <div className="col-6 col-sm-3">
                      <span className="text-muted d-block">Incident Date:</span>
                      <span className="fw-semibold">{detailClaim.incidentDate || 'N/A'}</span>
                    </div>

                    <div className="col-6 col-sm-3">
                      <span className="text-muted d-block">Assessed Amount:</span>
                      <span className="fw-bold text-success">
                        {detailClaim.assessedAmount != null
                          ? `₹${Number(detailClaim.assessedAmount).toLocaleString('en-IN')}`
                          : 'Not assessed yet'}
                      </span>
                    </div>

                    <div className="col-12 col-sm-6">
                      <span className="text-muted d-block">Attached Documents:</span>
                      <span className="fw-semibold">
                        {detailClaim.documentIds?.length
                          ? `${detailClaim.documentIds.length} document(s) uploaded (IDs: ${detailClaim.documentIds.join(', ')})`
                          : 'No documents attached'}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Incident Description */}
                <div className="mb-4">
                  <h6 className="fw-bold text-dark mb-1">Incident Report &amp; Description</h6>
                  <div className="p-3 bg-white border rounded small">
                    {detailClaim.description || 'No incident summary recorded.'}
                  </div>
                </div>

                {/* Decision / History Audit Note */}
                {detailClaim.reason && (
                  <div className="mb-4">
                    <h6 className="fw-bold text-dark mb-1">Audit Trail &amp; Adjudication Reason</h6>
                    <div className="p-3 bg-light border rounded small font-monospace">
                      {detailClaim.reason}
                    </div>
                  </div>
                )}

                {/* Payment Processing Notice */}
                {normalizeStatus(detailClaim.status) === 'PAYMENT_PROCESSING' && (
                  <div className="alert alert-info border-info small mb-0">
                    <strong>ℹ️ Settlement Payout Notice:</strong> Claim has been approved for ₹
                    {Number(detailClaim.assessedAmount).toLocaleString('en-IN')}. Awaiting disbursement by payment gateway.
                    <br />
                    <em>
                      (Note: The backend payout trigger endpoint <code>POST /claims/&#123;id&#125;/payout</code> is owned by
                      Person 4 and pending API controller mapping per Decision D-11).
                    </em>
                  </div>
                )}
              </div>

              <div className="modal-footer border-top bg-light">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setDetailClaim(null)}
                >
                  Close
                </button>
                {normalizeStatus(detailClaim.status) === 'DOCUMENT_VERIFICATION' && (
                  <button
                    type="button"
                    className="btn btn-warning"
                    onClick={() => {
                      const c = detailClaim;
                      setDetailClaim(null);
                      handleOpenVerify(c);
                    }}
                  >
                    Verify Documents Now
                  </button>
                )}
                {normalizeStatus(detailClaim.status) === 'ASSESSMENT' && (
                  <button
                    type="button"
                    className="btn btn-primary"
                    onClick={() => {
                      const c = detailClaim;
                      setDetailClaim(null);
                      handleOpenDecision(c);
                    }}
                  >
                    Assess &amp; Decide Now
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* =========================================================================
          Document Verification Modal (POST /claims/{id}/verify)
          ========================================================================= */}
      {verifyModalClaim && (
        <div
          className="modal fade show d-block"
          tabIndex="-1"
          role="dialog"
          style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
          data-testid="verify-modal"
        >
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content shadow-lg border-0">
              <div className="modal-header border-bottom">
                <h5 className="modal-title fw-bold">
                  Document Verification — {verifyModalClaim.claimNo}
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setVerifyModalClaim(null)}
                  aria-label="Close"
                  disabled={verifySubmitting}
                ></button>
              </div>

              <form onSubmit={handleSubmitVerify} noValidate>
                <div className="modal-body p-4">
                  {/* Verification Error Alert */}
                  {verifyError && (
                    <div className="alert alert-danger mb-3" role="alert" data-testid="verify-error-banner">
                      <strong>{verifyError.title}: </strong>
                      <span>{verifyError.message}</span>
                    </div>
                  )}

                  {/* Duplicate warning if applicable */}
                  {verifyModalClaim.duplicateFlag && (
                    <div className="alert alert-danger py-2 small mb-3">
                      ⚠️ <strong>INS-F-023:</strong> Possible duplicate claim. Verify documentation with extra scrutiny.
                    </div>
                  )}

                  {/* Summary */}
                  <div className="bg-light p-3 rounded mb-3 border small">
                    <div><strong>Claimed:</strong> ₹{Number(verifyModalClaim.claimedAmount || verifyModalClaim.claimAmount || 0).toLocaleString('en-IN')}</div>
                    <div><strong>Policy:</strong> {verifyModalClaim.policyNo}</div>
                    <div><strong>Incident:</strong> {verifyModalClaim.incidentDate}</div>
                    <div className="mt-1 text-muted text-truncate">{verifyModalClaim.description}</div>
                  </div>

                  {/* Documents Complete Radio Group */}
                  <div className="mb-3">
                    <label className="form-label fw-bold">Document Status *</label>
                    <div className="d-flex flex-column gap-2">
                      <div className="form-check border rounded p-2 ps-4">
                        <input
                          className="form-check-input"
                          type="radio"
                          name="docsCompleteRadio"
                          id="docsCompleteTrue"
                          data-testid="docs-complete-true"
                          checked={documentsComplete === true}
                          onChange={() => setDocumentsComplete(true)}
                        />
                        <label className="form-check-label fw-semibold text-success ms-1" htmlFor="docsCompleteTrue">
                          ✅ Documents Complete — Advance to Assessment
                        </label>
                        <div className="form-text extra-small ps-1">
                          All mandatory bills, FIR, and medical certificates are attached and verified.
                        </div>
                      </div>

                      <div className="form-check border rounded p-2 ps-4">
                        <input
                          className="form-check-input"
                          type="radio"
                          name="docsCompleteRadio"
                          id="docsCompleteFalse"
                          data-testid="docs-complete-false"
                          checked={documentsComplete === false}
                          onChange={() => setDocumentsComplete(false)}
                        />
                        <label className="form-check-label fw-semibold text-warning-emphasis ms-1" htmlFor="docsCompleteFalse">
                          📑 Request Additional Documents — Remain in Verification
                        </label>
                        <div className="form-text extra-small ps-1">
                          Emits CLAIM_DOCUMENTS_REQUESTED notification and starts 15-day auto-close clock (TC-Clm-06).
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Missing documents list (only if incomplete) */}
                  {!documentsComplete && (
                    <div className="mb-3">
                      <label htmlFor="missingDocsInput" className="form-label fw-bold">
                        Missing Documents (comma-separated) *
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        id="missingDocsInput"
                        data-testid="missing-docs-input"
                        placeholder="e.g. Original garage repair invoice, Police FIR copy"
                        value={missingDocsInput}
                        onChange={(e) => setMissingDocsInput(e.target.value)}
                        required={!documentsComplete}
                      />
                      <div className="form-text extra-small">
                        The customer will be notified of these specific missing files.
                      </div>
                    </div>
                  )}

                  {/* Verification Notes */}
                  <div className="mb-3">
                    <label htmlFor="verifyNotesInput" className="form-label fw-bold">
                      Verification Notes &amp; Observations
                    </label>
                    <textarea
                      className="form-control"
                      id="verifyNotesInput"
                      data-testid="verify-notes-input"
                      rows="3"
                      placeholder="Record inspection appraisal justification, surveyor notes, or missing reasons..."
                      value={verifyNotes}
                      onChange={(e) => setVerifyNotes(e.target.value)}
                    ></textarea>
                  </div>
                </div>

                <div className="modal-footer border-top bg-light">
                  <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={() => setVerifyModalClaim(null)}
                    disabled={verifySubmitting}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-warning px-4"
                    data-testid="submit-verify-btn"
                    disabled={verifySubmitting}
                  >
                    {verifySubmitting
                      ? 'Submitting...'
                      : documentsComplete
                      ? 'Confirm & Move to Assessment'
                      : 'Request Documents & Save'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* =========================================================================
          Assessment Decision Modal (POST /claims/{id}/decision)
          ========================================================================= */}
      {decisionModalClaim && (
        <div
          className="modal fade show d-block"
          tabIndex="-1"
          role="dialog"
          style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
          data-testid="decision-modal"
        >
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content shadow-lg border-0">
              <div className="modal-header border-bottom">
                <h5 className="modal-title fw-bold">
                  Claim Adjudication — {decisionModalClaim.claimNo}
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setDecisionModalClaim(null)}
                  aria-label="Close"
                  disabled={decisionSubmitting}
                ></button>
              </div>

              <form onSubmit={handleSubmitDecision} noValidate>
                <div className="modal-body p-4">
                  {/* Decision Error Alert (surfacing 422, 409, 403) */}
                  {decisionError && (
                    <div className="alert alert-danger mb-3" role="alert" data-testid="decision-error-banner">
                      <strong>{decisionError.title}: </strong>
                      <span>{decisionError.message}</span>
                    </div>
                  )}

                  {/* Duplicate warning if applicable */}
                  {decisionModalClaim.duplicateFlag && (
                    <div className="alert alert-danger py-2 small mb-3">
                      ⚠️ <strong>INS-F-023:</strong> Potential duplicate claim. Verify loss assessment carefully before approval.
                    </div>
                  )}

                  {/* Summary Strip */}
                  <div className="bg-light p-3 rounded mb-3 border small">
                    <div className="d-flex justify-content-between">
                      <span><strong>Policy:</strong> {decisionModalClaim.policyNo}</span>
                      <span>
                        <strong>Claimed:</strong> ₹{Number(decisionModalClaim.claimedAmount || decisionModalClaim.claimAmount || 0).toLocaleString('en-IN')}
                      </span>
                    </div>
                    <div className="text-muted mt-1 text-truncate">
                      <strong>Incident:</strong> {decisionModalClaim.description}
                    </div>
                  </div>

                  {/* Decision Selection: Approve vs Reject */}
                  <div className="mb-3">
                    <label className="form-label fw-bold">Adjudication Decision *</label>
                    <div className="d-flex gap-2">
                      <div className="form-check border rounded p-2 ps-4 flex-fill">
                        <input
                          className="form-check-input"
                          type="radio"
                          name="claimDecisionRadio"
                          id="decisionApproveRadio"
                          data-testid="decision-approve-radio"
                          value="Approve"
                          checked={decisionType === 'Approve'}
                          onChange={() => setDecisionType('Approve')}
                        />
                        <label className="form-check-label fw-semibold text-success ms-1" htmlFor="decisionApproveRadio">
                          ✅ APPROVE Claim
                        </label>
                      </div>

                      <div className="form-check border rounded p-2 ps-4 flex-fill">
                        <input
                          className="form-check-input"
                          type="radio"
                          name="claimDecisionRadio"
                          id="decisionRejectRadio"
                          data-testid="decision-reject-radio"
                          value="Reject"
                          checked={decisionType === 'Reject'}
                          onChange={() => setDecisionType('Reject')}
                        />
                        <label className="form-check-label fw-semibold text-danger ms-1" htmlFor="decisionRejectRadio">
                          ❌ REJECT Claim
                        </label>
                      </div>
                    </div>
                  </div>

                  {/* Assessed Amount (Only if Approve) */}
                  {decisionType === 'Approve' && (
                    <div className="mb-3">
                      <label htmlFor="assessedAmountInput" className="form-label fw-bold">
                        Approved Assessment Amount (₹) *
                      </label>
                      <input
                        type="number"
                        className="form-control"
                        id="assessedAmountInput"
                        data-testid="assessed-amount-input"
                        placeholder="Enter approved payout sum"
                        value={assessedAmountInput}
                        onChange={(e) => setAssessedAmountInput(e.target.value)}
                        min="1"
                        step="0.01"
                        required
                      />
                      <div className="form-text extra-small">
                        Claimed amount: ₹{Number(decisionModalClaim.claimedAmount || decisionModalClaim.claimAmount || 0).toLocaleString('en-IN')}.
                        Approved sum moves claim to Payment Processing.
                      </div>
                    </div>
                  )}

                  {/* Mandatory Justification Reason */}
                  <div className="mb-3">
                    <label htmlFor="decisionReasonInput" className="form-label fw-bold">
                      {decisionType === 'Approve' ? 'Assessment Rationale & Surveyor Reference *' : 'Rejection Reason *'}
                    </label>
                    <textarea
                      className="form-control"
                      id="decisionReasonInput"
                      data-testid="decision-reason-input"
                      rows="3"
                      placeholder={
                        decisionType === 'Approve'
                          ? 'Provide loss assessment justification, surveyor inspection reference, or deductible deduction...'
                          : 'Provide specific reason for rejection (e.g. Incident outside policy coverage period, pre-existing condition exclusion)...'
                      }
                      value={decisionReason}
                      onChange={(e) => setDecisionReason(e.target.value)}
                      required
                    ></textarea>
                    <div className="form-text extra-small">
                      Strictly mandatory per P1 contract &amp; SAD §4.3.5. Surfaced as 422 if empty.
                    </div>
                  </div>
                </div>

                <div className="modal-footer border-top bg-light">
                  <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={() => setDecisionModalClaim(null)}
                    disabled={decisionSubmitting}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className={`btn ${decisionType === 'Approve' ? 'btn-success' : 'btn-danger'} px-4`}
                    data-testid="submit-decision-btn"
                    disabled={decisionSubmitting}
                  >
                    {decisionSubmitting
                      ? 'Recording Decision...'
                      : decisionType === 'Approve'
                      ? 'Confirm Approval & Advance'
                      : 'Confirm Claim Rejection'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </PortalLayout>
  );
}
