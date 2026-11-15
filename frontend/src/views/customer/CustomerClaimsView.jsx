import React, { useState, useEffect, useMemo } from 'react';
import PortalLayout from '../../components/layout/PortalLayout';
import apiClient from '../../api/apiClient';

/**
 * Authoritative claim status display and badge configurations (SAD §4.1.3, §4.3.5)
 */
export const CLAIM_STATUS_MAP = {
  SUBMITTED: { label: 'Submitted', badge: 'bg-secondary text-white', terminal: false, step: 1 },
  DOCUMENT_VERIFICATION: { label: 'Document Verification', badge: 'bg-warning-subtle text-dark border border-warning', terminal: false, step: 2 },
  ASSESSMENT: { label: 'Loss Assessment', badge: 'bg-primary-subtle text-primary border border-primary', terminal: false, step: 3 },
  APPROVED: { label: 'Approved', badge: 'bg-info-subtle text-dark border', terminal: false, step: 4 },
  PAYMENT_PROCESSING: { label: 'Payment Processing', badge: 'bg-info text-dark', terminal: false, step: 4 },
  SETTLED: { label: 'Settled', badge: 'bg-success text-white', terminal: true, step: 5 },
  REJECTED: { label: 'Rejected', badge: 'bg-danger text-white', terminal: true, step: 5 },
  CLOSED_NO_RESPONSE: { label: 'Closed - No Response', badge: 'bg-dark text-white', terminal: true, step: 5 },
};

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

export function getStatusDisplay(rawStatus) {
  const norm = normalizeStatus(rawStatus);
  return CLAIM_STATUS_MAP[norm] || { label: rawStatus || 'Unknown', badge: 'bg-secondary text-white', terminal: false, step: 1 };
}

/**
 * Customer Claims View (P4-16 / INS-F-012, INS-F-013, INS-F-015, INS-F-023)
 * Provides customer claim filing with real-time validation and claim state machine timeline tracking.
 */
export default function CustomerClaimsView() {
  const [activeTab, setActiveTab] = useState('TRACK'); // 'TRACK' or 'FILE'

  // Policies and claims state
  const [policies, setPolicies] = useState([]);
  const [claimsList, setClaimsList] = useState([]);
  const [selectedClaim, setSelectedClaim] = useState(null);
  const [loadingPolicies, setLoadingPolicies] = useState(true);
  const [loadingClaims, setLoadingClaims] = useState(false);

  // Filing form state
  const [selectedPolicyId, setSelectedPolicyId] = useState('');
  const [incidentDate, setIncidentDate] = useState('');
  const [claimedAmount, setClaimedAmount] = useState('');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [submitResult, setSubmitResult] = useState(null);
  const [claimDetailError, setClaimDetailError] = useState(null);

  const retryLoadClaimDetails = async (claimId, claimNo) => {
    try {
      const claimDetailRes = await apiClient.get(`/claims/${claimId}`);
      const freshClaim = claimDetailRes.data;
      setClaimsList((prev) => [freshClaim, ...prev.filter((c) => c.id !== freshClaim.id)]);
      setSelectedClaim(freshClaim);
      setClaimDetailError(null);
    } catch (err) {
      setClaimDetailError({
        claimId,
        claimNo,
        message: `Claim submitted (${claimNo || claimId}); details could not be loaded - Retry`,
      });
    }
  };

  // Claim Lookup state (for tracking by claim ID)
  const [lookupClaimId, setLookupClaimId] = useState('');
  const [lookupError, setLookupError] = useState(null);
  const [lookupLoading, setLookupLoading] = useState(false);

  // Fetch customer's active policies on mount
  useEffect(() => {
    fetchPolicies();
  }, []);

  const fetchPolicies = async () => {
    setLoadingPolicies(true);
    try {
      const res = await apiClient.get('/policies');
      const data = Array.isArray(res.data) ? res.data : [];
      setPolicies(data);

      // Auto-select first active policy if available
      const active = data.filter((p) => p.status === 'ACTIVE');
      if (active.length > 0) {
        setSelectedPolicyId(String(active[0].id));
      }

      // Fetch claims for user's policies
      if (data.length > 0) {
        fetchAllPolicyClaims(data);
      }
    } catch (err) {
      console.warn('Failed to fetch policies:', err);
      setPolicies([]);
    } finally {
      setLoadingPolicies(false);
    }
  };

  const fetchAllPolicyClaims = async (policyList) => {
    setLoadingClaims(true);
    const accumulated = [];
    const seenIds = new Set();
    for (const pol of policyList) {
      try {
        const res = await apiClient.get(`/claims/policy/${pol.id}`);
        if (Array.isArray(res.data)) {
          for (const item of res.data) {
            if (item && item.id && !seenIds.has(item.id)) {
              seenIds.add(item.id);
              accumulated.push(item);
            }
          }
        }
      } catch {
        // Ignore single policy failures
      }
    }
    setClaimsList(accumulated);
    setSelectedClaim((prev) => {
      if (prev && accumulated.some((c) => c.id === prev.id)) {
        return accumulated.find((c) => c.id === prev.id);
      }
      return accumulated.length > 0 ? accumulated[0] : null;
    });
    setLoadingClaims(false);
  };

  // Active policies only for filing
  const activePolicies = useMemo(() => {
    return policies.filter((p) => p.status === 'ACTIVE');
  }, [policies]);

  const currentSelectedPolicy = useMemo(() => {
    return policies.find((p) => String(p.id) === String(selectedPolicyId)) || null;
  }, [policies, selectedPolicyId]);

  // Form submission handler
  const handleFileClaim = async (e) => {
    e.preventDefault();
    setFormError(null);
    setSubmitResult(null);

    // Validation checks matching backend constraints (ClaimService.java)
    if (!selectedPolicyId) {
      setFormError('Please select an active policy.');
      return;
    }

    if (!incidentDate) {
      setFormError('Incident date is required.');
      return;
    }

    const todayStr = new Date().toISOString().split('T')[0];
    if (incidentDate > todayStr) {
      setFormError('Incident date cannot be in the future.');
      return;
    }

    // 30 days check
    const incidentTime = new Date(incidentDate).getTime();
    const todayTime = new Date(todayStr).getTime();
    const diffDays = Math.floor((todayTime - incidentTime) / (1000 * 60 * 60 * 24));
    if (diffDays > 30) {
      setFormError(`Incident date must be within 30 days of filing (${diffDays} days ago).`);
      return;
    }

    // Check policy coverage bounds
    if (currentSelectedPolicy) {
      if (currentSelectedPolicy.startDate && incidentDate < currentSelectedPolicy.startDate) {
        setFormError(`Incident date (${incidentDate}) is prior to policy start date (${currentSelectedPolicy.startDate}).`);
        return;
      }
      if (currentSelectedPolicy.endDate && incidentDate > currentSelectedPolicy.endDate) {
        setFormError(`Incident date (${incidentDate}) is after policy expiration date (${currentSelectedPolicy.endDate}).`);
        return;
      }
    }

    const amountNum = Number(claimedAmount);
    if (isNaN(amountNum) || amountNum <= 0) {
      setFormError('Claimed amount must be greater than ₹0.');
      return;
    }

    if (!description.trim()) {
      setFormError('Please provide a description of the incident.');
      return;
    }

    setSubmitting(true);

    const payload = {
      policyId: Number(selectedPolicyId),
      incidentDate: incidentDate,
      claimedAmount: amountNum,
      description: description.trim(),
      documentIds: [],
    };

    try {
      const response = await apiClient.post('/claims', payload);
      const data = response.data;

      setSubmitResult({
        success: true,
        claimId: data.claimId,
        claimNo: data.claimNo,
        status: data.status,
        duplicateFlag: data.duplicateFlag,
        message: data.message || 'Claim submitted successfully.',
      });

      // Clear inputs
      setClaimedAmount('');
      setDescription('');
      setIncidentDate('');

      // Refresh claims list and set as selected claim
      try {
        const claimDetailRes = await apiClient.get(`/claims/${data.claimId}`);
        const freshClaim = claimDetailRes.data;
        setClaimsList((prev) => [freshClaim, ...prev.filter((c) => c.id !== freshClaim.id)]);
        setSelectedClaim(freshClaim);
        setClaimDetailError(null);
      } catch {
        setClaimDetailError({
          claimId: data.claimId,
          claimNo: data.claimNo,
          message: `Claim submitted (${data.claimNo || data.claimId}); details could not be loaded - Retry`,
        });
      }
    } catch (err) {
      const status = err.response?.status;
      const serverMsg = err.response?.data?.message || err.message;
      if (status === 409) {
        setFormError(`State Conflict (409): ${serverMsg || 'Policy is not active or eligible.'}`);
      } else if (status === 422 || status === 400) {
        setFormError(`Validation Error (${status}): ${serverMsg}`);
      } else if (status === 403) {
        setFormError(`Access Denied (403): ${serverMsg || 'Policy does not belong to your account.'}`);
      } else {
        setFormError(serverMsg || 'Failed to submit claim. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  // Lookup claim by ID
  const handleLookupClaim = async (e) => {
    e.preventDefault();
    if (!lookupClaimId.trim()) return;

    setLookupLoading(true);
    setLookupError(null);

    // Support entering pure numeric ID or "CLM-..." if matching in list
    const cleanId = lookupClaimId.trim().replace(/^CLM-2026-0*/i, '');
    const numericId = Number(cleanId);

    try {
      let claimToDisplay = null;
      if (!isNaN(numericId) && numericId > 0) {
        const res = await apiClient.get(`/claims/${numericId}`);
        claimToDisplay = res.data;
      } else {
        // Search in already fetched claims
        const found = claimsList.find(
          (c) => c.claimNo?.toLowerCase() === lookupClaimId.trim().toLowerCase()
        );
        if (found) {
          claimToDisplay = found;
        } else {
          throw new Error('Claim reference not found.');
        }
      }

      if (claimToDisplay) {
        setSelectedClaim(claimToDisplay);
        // Add to list if not present
        setClaimsList((prev) => {
          if (!prev.some((c) => c.id === claimToDisplay.id)) {
            return [claimToDisplay, ...prev];
          }
          return prev;
        });
      }
    } catch (err) {
      setLookupError(err.response?.data?.message || err.message || 'Claim not found or access denied.');
    } finally {
      setLookupLoading(false);
    }
  };

  // Timeline helper: determines status for each of the 5 canonical stages
  const getTimelineStepStatus = (stepIndex, claim) => {
    if (!claim) return 'pending';
    const norm = normalizeStatus(claim.status);
    const currentStep = CLAIM_STATUS_MAP[norm]?.step || 1;

    if (norm === 'REJECTED' && stepIndex === 5) return 'rejected';
    if (norm === 'CLOSED_NO_RESPONSE' && stepIndex === 5) return 'closed';
    if (stepIndex < currentStep) return 'completed';
    if (stepIndex === currentStep) return 'active';
    return 'pending';
  };

  return (
    <PortalLayout
      title="Claims Tracker & Filing Desk"
      subtitle="File reimbursement claims and track their review and settlement progress in real time."
      badgeText="Claims Center"
      badgeClass="bg-primary text-white"
    >

      {claimDetailError && (
        <div className="alert alert-warning mb-4 d-flex justify-content-between align-items-center" role="alert">
          <div>
            <i className="bi bi-exclamation-triangle me-2"></i>
            <span>{claimDetailError.message}</span>
          </div>
          <button
            type="button"
            className="btn btn-outline-dark btn-sm ms-2"
            onClick={() => retryLoadClaimDetails(claimDetailError.claimId, claimDetailError.claimNo)}
          >
            Retry
          </button>
        </div>
      )}

      {/* Tabs */}
      <ul className="nav nav-tabs mb-4" role="tablist">
        <li className="nav-item" role="presentation">
          <button
            type="button"
            className={`nav-link ${activeTab === 'TRACK' ? 'active fw-bold' : ''}`}
            data-testid="tab-track-claims"
            onClick={() => {
              setActiveTab('TRACK');
              setFormError(null);
            }}
          >
            <i className="bi bi-clock-history me-1"></i>Track Active Claims &amp; Timeline
          </button>
        </li>
        <li className="nav-item" role="presentation">
          <button
            type="button"
            className={`nav-link ${activeTab === 'FILE' ? 'active fw-bold' : ''}`}
            data-testid="tab-file-claim"
            onClick={() => {
              setActiveTab('FILE');
              setSubmitResult(null);
              setFormError(null);
            }}
          >
            <i className="bi bi-plus-circle me-1"></i>File New Claim
          </button>
        </li>
      </ul>

      {/* =========================================================================
          TAB 1: TRACK ACTIVE CLAIMS & STATE TIMELINE
          ========================================================================= */}
      {activeTab === 'TRACK' && (
        <div>
          {/* Claim Selector & Search Bar */}
          <div className="card border-0 shadow-sm mb-4">
            <div className="card-body p-3">
              <div className="row g-3 align-items-center">
                {/* Select from existing claims */}
                <div className="col-12 col-md-6">
                  <label htmlFor="claimSelector" className="form-label small fw-bold mb-1">
                    Select Your Claim:
                  </label>
                  <select
                    id="claimSelector"
                    data-testid="claim-selector"
                    className="form-select form-select-sm"
                    value={selectedClaim ? String(selectedClaim.id) : ''}
                    onChange={(e) => {
                      const found = claimsList.find((c) => String(c.id) === e.target.value);
                      if (found) setSelectedClaim(found);
                    }}
                    disabled={loadingClaims || claimsList.length === 0}
                  >
                    {claimsList.length === 0 ? (
                      <option value="">(No filed claims found)</option>
                    ) : (
                      claimsList.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.claimNo || `CLM-${c.id}`} — {c.policyNo || `Policy #${c.policyId}`} — {getStatusDisplay(c.status).label}
                        </option>
                      ))
                    )}
                  </select>
                </div>

                {/* Direct Lookup Input */}
                <div className="col-12 col-md-6">
                  <form onSubmit={handleLookupClaim}>
                    <label htmlFor="claimLookupInput" className="form-label small fw-bold mb-1">
                      Lookup by Claim ID / Reference:
                    </label>
                    <div className="input-group input-group-sm">
                      <input
                        type="text"
                        id="claimLookupInput"
                        data-testid="claim-lookup-input"
                        className="form-control"
                        placeholder="e.g. 1 or CLM-2026-000101"
                        value={lookupClaimId}
                        onChange={(e) => setLookupClaimId(e.target.value)}
                      />
                      <button
                        type="submit"
                        data-testid="claim-lookup-btn"
                        className="btn btn-outline-primary"
                        disabled={lookupLoading || !lookupClaimId.trim()}
                      >
                        {lookupLoading ? 'Finding...' : 'Track'}
                      </button>
                    </div>
                  </form>
                </div>
              </div>

              {lookupError && (
                <div className="alert alert-warning py-2 small mt-2 mb-0" data-testid="lookup-error-banner">
                  <i className="bi bi-exclamation-triangle-fill me-1"></i>
                  {lookupError}
                </div>
              )}
            </div>
          </div>

          {/* Details & Timeline Display */}
          {selectedClaim ? (
            <div className="dashboard-card mb-4" data-testid="claim-details-card">
              <div className="dashboard-card-header d-flex flex-wrap justify-content-between align-items-center gap-2">
                <div>
                  <span className="fw-bold me-2">Claim Details: {selectedClaim.claimNo || `CLM-${selectedClaim.id}`}</span>
                  {selectedClaim.duplicateFlag && (
                    <span
                      className="badge bg-danger text-white extra-small"
                      data-testid="claim-duplicate-badge"
                      title="Possible duplicate claim flag"
                    >
                      <i className="bi bi-exclamation-triangle-fill me-1"></i>Possible Duplicate
                    </span>
                  )}
                </div>
                <span className={`badge ${getStatusDisplay(selectedClaim.status).badge}`} data-testid="claim-status-badge">
                  {getStatusDisplay(selectedClaim.status).label}
                </span>
              </div>

              <div className="dashboard-card-body">
                {/* Duplicate Notification Banner */}
                {selectedClaim.duplicateFlag && (
                  <div className="alert alert-danger py-2 small mb-4" data-testid="duplicate-alert-banner">
                    <strong>Possible Duplicate Detected:</strong> This claim was flagged as a potential duplicate (same policy, incident date, and claimed amount). It is currently being reviewed with extra scrutiny by a Claims Officer.
                  </div>
                )}

                {/* Primary Metadata Attributes */}
                <div className="row g-3 mb-4">
                  <div className="col-6 col-md-3">
                    <span className="text-muted small d-block">Policy Reference</span>
                    <span className="fw-semibold text-primary">{selectedClaim.policyNo || `Policy #${selectedClaim.policyId}`}</span>
                  </div>
                  <div className="col-6 col-md-3">
                    <span className="text-muted small d-block">Incident Date</span>
                    <span className="fw-semibold">{selectedClaim.incidentDate || 'N/A'}</span>
                  </div>
                  <div className="col-6 col-md-3">
                    <span className="text-muted small d-block">Claimed Amount</span>
                    <span className="fw-bold text-dark">
                      ₹{Number(selectedClaim.claimedAmount || selectedClaim.claimAmount || 0).toLocaleString('en-IN')}
                    </span>
                  </div>
                  <div className="col-6 col-md-3">
                    <span className="text-muted small d-block">Assessed Sum</span>
                    <span className="fw-semibold text-success">
                      {selectedClaim.assessedAmount != null
                        ? `₹${Number(selectedClaim.assessedAmount).toLocaleString('en-IN')}`
                        : 'Awaiting Assessment'}
                    </span>
                  </div>
                </div>

                {/* Incident Description */}
                <div className="mb-4">
                  <span className="text-muted small d-block fw-semibold mb-1">Incident Description</span>
                  <div className="p-3 bg-light rounded border small">
                    {selectedClaim.description || 'No description recorded.'}
                  </div>
                </div>

                {/* Audit or Rejection Note */}
                {selectedClaim.reason && (
                  <div className="mb-4">
                    <span className="text-muted small d-block fw-semibold mb-1">Claims Officer Adjudication Notes</span>
                    <div className="p-3 bg-light rounded border border-info small font-monospace">
                      {selectedClaim.reason}
                    </div>
                  </div>
                )}

                {/* =========================================================================
                    5-Stage Claim State Machine Progress Timeline (ADR-008 & SAD §4.1.3)
                    ========================================================================= */}
                <h5 className="h6 fw-bold mb-3">Claim Progress Timeline</h5>
                <div className="p-3 bg-light rounded-3 mb-3 border" data-testid="claim-timeline">
                  <div className="row text-center g-3 small">
                    {/* Step 1: Submission */}
                    <div className="col-12 col-sm">
                      <div className="text-success mb-1">
                        <i className="bi bi-check-circle-fill fs-4 d-block"></i>
                      </div>
                      <span className="fw-bold d-block">1. Submitted</span>
                      <span className="text-muted extra-small">Initial Intake</span>
                    </div>

                    {/* Step 2: Document Verification */}
                    <div className="col-12 col-sm">
                      {(() => {
                        const status = getTimelineStepStatus(2, selectedClaim);
                        if (status === 'completed') {
                          return (
                            <div className="text-success mb-1">
                              <i className="bi bi-check-circle-fill fs-4 d-block"></i>
                            </div>
                          );
                        }
                        if (status === 'active') {
                          return (
                            <div className="text-warning mb-1">
                              <i className="bi bi-arrow-repeat fs-4 d-block"></i>
                            </div>
                          );
                        }
                        return (
                          <div className="text-muted mb-1">
                            <i className="bi bi-circle fs-4 d-block"></i>
                          </div>
                        );
                      })()}
                      <span className="fw-bold d-block">2. Doc Verification</span>
                      <span className="text-muted extra-small">Officer Inspection</span>
                    </div>

                    {/* Step 3: Loss Assessment */}
                    <div className="col-12 col-sm">
                      {(() => {
                        const status = getTimelineStepStatus(3, selectedClaim);
                        if (status === 'completed') {
                          return (
                            <div className="text-success mb-1">
                              <i className="bi bi-check-circle-fill fs-4 d-block"></i>
                            </div>
                          );
                        }
                        if (status === 'active') {
                          return (
                            <div className="text-primary mb-1">
                              <i className="bi bi-arrow-repeat fs-4 d-block"></i>
                            </div>
                          );
                        }
                        return (
                          <div className="text-muted mb-1">
                            <i className="bi bi-circle fs-4 d-block"></i>
                          </div>
                        );
                      })()}
                      <span className="fw-bold d-block">3. Assessment</span>
                      <span className="text-muted extra-small">Loss Appraisal</span>
                    </div>

                    {/* Step 4: Payment Processing */}
                    <div className="col-12 col-sm">
                      {(() => {
                        const status = getTimelineStepStatus(4, selectedClaim);
                        if (status === 'completed') {
                          return (
                            <div className="text-success mb-1">
                              <i className="bi bi-check-circle-fill fs-4 d-block"></i>
                            </div>
                          );
                        }
                        if (status === 'active') {
                          return (
                            <div className="text-info mb-1">
                              <i className="bi bi-hourglass-split fs-4 d-block"></i>
                            </div>
                          );
                        }
                        return (
                          <div className="text-muted mb-1">
                            <i className="bi bi-circle fs-4 d-block"></i>
                          </div>
                        );
                      })()}
                      <span className="fw-bold d-block">4. Payout Processing</span>
                      <span className="text-muted extra-small">Gateway Transfer</span>
                    </div>

                    {/* Step 5: Resolution */}
                    <div className="col-12 col-sm">
                      {(() => {
                        const norm = normalizeStatus(selectedClaim.status);
                        if (norm === 'SETTLED') {
                          return (
                            <>
                              <div className="text-success mb-1">
                                <i className="bi bi-check-circle-fill fs-4 d-block"></i>
                              </div>
                              <span className="fw-bold text-success d-block">5. Settled</span>
                              <span className="text-success extra-small">Disbursed</span>
                            </>
                          );
                        }
                        if (norm === 'REJECTED') {
                          return (
                            <>
                              <div className="text-danger mb-1">
                                <i className="bi bi-x-circle-fill fs-4 d-block"></i>
                              </div>
                              <span className="fw-bold text-danger d-block">5. Rejected</span>
                              <span className="text-danger extra-small">Closed with Reason</span>
                            </>
                          );
                        }
                        if (norm === 'CLOSED_NO_RESPONSE') {
                          return (
                            <>
                              <div className="text-dark mb-1">
                                <i className="bi bi-dash-circle-fill fs-4 d-block"></i>
                              </div>
                              <span className="fw-bold text-dark d-block">5. Closed</span>
                              <span className="text-muted extra-small">Stale (15 days)</span>
                            </>
                          );
                        }
                        return (
                          <>
                            <div className="text-muted mb-1">
                              <i className="bi bi-circle fs-4 d-block"></i>
                            </div>
                            <span className="fw-bold text-muted d-block">5. Settlement</span>
                            <span className="text-muted extra-small">Final Decision</span>
                          </>
                        );
                      })()}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            <div className="card border-0 shadow-sm text-center py-5" data-testid="no-claims-state">
              <div className="card-body">
                <i className="bi bi-shield-exclamation text-muted fs-1 mb-2"></i>
                <h5 className="fw-semibold">No Claims on File</h5>
                <p className="text-muted small mb-3">
                  You have not submitted any insurance claims yet, or no claims matched your lookup.
                </p>
                <button
                  type="button"
                  className="btn btn-primary btn-sm px-3"
                  onClick={() => setActiveTab('FILE')}
                >
                  <i className="bi bi-plus-circle me-1"></i>File New Claim
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* =========================================================================
          TAB 2: FILE NEW CLAIM FORM
          ========================================================================= */}
      {activeTab === 'FILE' && (
        <div className="dashboard-card" data-testid="file-claim-card">
          <div className="dashboard-card-header d-flex justify-content-between align-items-center">
            <span className="fw-bold">File Insurance Claim (Reimbursement)</span>
            <span className="badge bg-secondary">IRDAI 30-Day Window</span>
          </div>

          <div className="dashboard-card-body">
            {/* Success Alert Banner */}
            {submitResult && (
              <div className="alert alert-success py-3 mb-4 shadow-sm" role="alert" data-testid="claim-submit-success-alert">
                <div className="d-flex align-items-center mb-1">
                  <i className="bi bi-check-circle-fill fs-5 me-2 text-success"></i>
                  <span className="fw-bold">Claim Submitted Successfully!</span>
                </div>
                <p className="small mb-2">
                  Claim Reference: <strong className="font-monospace">{submitResult.claimNo}</strong>. Current Stage:{' '}
                  <strong>Document Verification</strong>.
                </p>
                {submitResult.duplicateFlag && (
                  <div className="badge bg-danger text-white mb-2 p-2 d-inline-block">
                    <i className="bi bi-exclamation-triangle-fill me-1"></i>
                    Possible Duplicate Detected — Held for Manual Inspection
                  </div>
                )}
                <div className="mt-2">
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-success"
                    onClick={() => setActiveTab('TRACK')}
                    data-testid="view-timeline-after-submit-btn"
                  >
                    View Claim on Timeline &rarr;
                  </button>
                </div>
              </div>
            )}

            {/* Error Alert Banner */}
            {formError && (
              <div className="alert alert-danger py-2 small mb-4 shadow-sm" role="alert" data-testid="claim-form-error-alert">
                <i className="bi bi-exclamation-triangle-fill me-1"></i>
                {formError}
              </div>
            )}

            {/* No Active Policy Warning */}
            {activePolicies.length === 0 && !loadingPolicies && (
              <div className="alert alert-warning py-3 mb-4" data-testid="no-active-policy-warning">
                <strong>No Active Policies Found:</strong> You do not currently have any active insurance policies. Claims can only be filed against an active policy within its coverage period.
              </div>
            )}

            <form onSubmit={handleFileClaim} noValidate>
              {/* Policy Selection and Incident Date */}
              <div className="row g-3 mb-3">
                <div className="col-12 col-md-6">
                  <label htmlFor="claim-policy" className="form-label small fw-semibold">
                    Select Active Policy *
                  </label>
                  <select
                    id="claim-policy"
                    data-testid="claim-policy-select"
                    className="form-select form-select-sm"
                    value={selectedPolicyId}
                    onChange={(e) => setSelectedPolicyId(e.target.value)}
                    required
                    disabled={activePolicies.length === 0 || submitting}
                  >
                    {activePolicies.length === 0 ? (
                      <option value="">No active policies available</option>
                    ) : (
                      activePolicies.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.policyNo || `POL-${p.id}`} {p.vehicleRegNo ? `(${p.vehicleRegNo})` : ''}
                        </option>
                      ))
                    )}
                  </select>
                  {currentSelectedPolicy && (
                    <div className="form-text extra-small">
                      Coverage: {currentSelectedPolicy.startDate} to {currentSelectedPolicy.endDate}
                    </div>
                  )}
                </div>

                <div className="col-12 col-md-6">
                  <label htmlFor="incident-date" className="form-label small fw-semibold">
                    Incident Date * (Must be within 30 days)
                  </label>
                  <input
                    type="date"
                    id="incident-date"
                    data-testid="claim-incident-date-input"
                    className="form-control form-control-sm"
                    value={incidentDate}
                    onChange={(e) => setIncidentDate(e.target.value)}
                    max={new Date().toISOString().split('T')[0]}
                    required
                    disabled={submitting}
                  />
                  <div className="form-text extra-small">
                    Claims must be reported within 30 days of the incident occurrence per IRDAI standards.
                  </div>
                </div>
              </div>

              {/* Claimed Amount */}
              <div className="mb-3">
                <label htmlFor="claimed-amount" className="form-label small fw-semibold">
                  Estimated Claim Amount (₹) *
                </label>
                <input
                  type="number"
                  id="claimed-amount"
                  data-testid="claim-amount-input"
                  className="form-control form-control-sm"
                  placeholder="e.g. 25000"
                  value={claimedAmount}
                  onChange={(e) => setClaimedAmount(e.target.value)}
                  min="1"
                  step="0.01"
                  required
                  disabled={submitting}
                />
                <div className="form-text extra-small">
                  Enter estimated repair or medical expense subject to policy terms and deductible.
                </div>
              </div>

              {/* Description */}
              <div className="mb-3">
                <label htmlFor="claim-desc" className="form-label small fw-semibold">
                  Description of Incident *
                </label>
                <textarea
                  id="claim-desc"
                  data-testid="claim-description-input"
                  rows="3"
                  className="form-control form-control-sm"
                  placeholder="Describe the incident occurrence, damages sustained, location, and surveyor/hospital references..."
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  required
                  disabled={submitting}
                ></textarea>
              </div>

              {/* File Attachment / Notice */}
              <div className="mb-3">
                <label htmlFor="claim-files" className="form-label small fw-semibold">
                  Supporting Bills &amp; FIR / Reports (Max 5MB per file)
                </label>
                <input
                  type="file"
                  id="claim-files"
                  data-testid="claim-files-input"
                  className="form-control form-control-sm"
                  multiple
                  disabled={submitting}
                />
                <div className="form-text extra-small">
                  Original invoices, FIR reports, and medical discharge summaries will be verified by the Claims Officer.
                </div>
              </div>

              {/* Submit Button */}
              <div className="d-flex justify-content-end">
                <button
                  type="submit"
                  className="btn btn-primary btn-sm px-4"
                  data-testid="claim-submit-btn"
                  disabled={submitting || activePolicies.length === 0}
                >
                  {submitting ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                      Submitting Claim...
                    </>
                  ) : (
                    <>
                      <i className="bi bi-send me-1"></i>Submit Claim for Verification
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </PortalLayout>
  );
}
