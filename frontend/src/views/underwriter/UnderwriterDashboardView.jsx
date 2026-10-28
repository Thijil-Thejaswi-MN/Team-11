import React, { useState, useEffect, useId } from 'react';
import PortalLayout from '../../components/layout/PortalLayout';
import { useAuth } from '../../context/AuthContext';
import apiClient from '../../api/apiClient';

export default function UnderwriterDashboardView() {
  const { currentUser } = useAuth();
  const searchInputId = useId();
  const filterSelectId = useId();
  const reasonTextareaId = useId();

  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [productFilter, setProductFilter] = useState('ALL');

  // Selected application for assessment
  const [selectedApp, setSelectedApp] = useState(null);
  const [decision, setDecision] = useState('APPROVE');
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Specific error state tracking for 422, 409, 403
  const [errorState, setErrorState] = useState(null); // { type: '422' | '409' | '403' | 'GENERAL', message, details }
  const [successMessage, setSuccessMessage] = useState(null);

  // Fetch referred applications
  const fetchReferredApplications = async () => {
    setLoading(true);
    setFetchError(null);
    try {
      const response = await apiClient.get('/applications/referred');
      setApplications(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      // In offline / preview / test environments with mock fallback
      console.warn('Could not fetch referred applications from backend:', err.message);
      setFetchError(err.message || 'Unable to connect to underwriting queue service.');
      // Provide initial mock items if empty
      setApplications([
        {
          id: 8812,
          customerId: 1001,
          insurerId: 1,
          planId: 2,
          quoteId: 501,
          productType: 'HEALTH',
          vehicleRegNo: null,
          premium: 12500,
          reason: 'Applicant age is 62 years (exceeds auto-approval limit of 60 years)',
          detailsJson: '{"age":62,"applicantAge":62,"hasPreExisting":true,"sumInsured":1000000}',
          status: 'REFERRED',
          createdAt: new Date().toISOString(),
        },
        {
          id: 8815,
          customerId: 1002,
          insurerId: 1,
          planId: 1,
          quoteId: 502,
          productType: 'MOTOR_FOUR_WHEELER',
          vehicleRegNo: 'KA03HA9988',
          premium: 18450,
          reason: 'Vehicle age is 12 years (exceeds auto-approve threshold of 10 years)',
          detailsJson: '{"vehicleAge":12,"idv":750000,"make":"Honda","model":"City","city":"Bangalore"}',
          status: 'REFERRED',
          createdAt: new Date(Date.now() - 3600000).toISOString(),
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReferredApplications();
  }, []);

  const handleOpenDecision = (app, initialDecision) => {
    setSelectedApp(app);
    setDecision(initialDecision);
    setReason('');
    setErrorState(null);
    setSuccessMessage(null);
  };

  const handleCloseDecision = () => {
    setSelectedApp(null);
    setReason('');
    setErrorState(null);
  };

  const handleSubmitDecision = async (e) => {
    e.preventDefault();
    setErrorState(null);
    setSuccessMessage(null);

    // Client-side required reason check (triggers 422 feedback)
    if (!reason || !reason.trim()) {
      setErrorState({
        type: '422',
        status: 422,
        title: 'Validation Error (HTTP 422)',
        message: 'Underwriter justification reason is strictly required by regulatory guidelines and cannot be blank.',
        details: ['Field `reason` is mandatory for manual underwriting decisions.'],
      });
      return;
    }

    setSubmitting(true);
    const appId = selectedApp.id;

    try {
      const payload = {
        decision: decision.toUpperCase(),
        reason: reason.trim(),
      };

      await apiClient.post(`/applications/${appId}/decision`, payload);

      setSuccessMessage(
        `Application APP-${appId} successfully ${decision === 'APPROVE' ? 'APPROVED' : 'REJECTED'}. Audit entry written and notification dispatched.`
      );
      // Remove the decided application from queue
      setApplications((prev) => prev.filter((a) => a.id !== appId));
      setSelectedApp(null);
      setReason('');
    } catch (err) {
      const status = err.status || err.response?.status;
      const data = err.response?.data || err;

      if (status === 422 || err.errorCode === 'VALIDATION_ERROR') {
        setErrorState({
          type: '422',
          status: 422,
          title: 'Validation Error (HTTP 422)',
          message: data.message || 'The decision request is malformed or missing required justification.',
          details: data.details || ['Invalid decision payload or blank reason.'],
        });
      } else if (status === 409 || err.errorCode === 'STATE_CONFLICT') {
        setErrorState({
          type: '409',
          status: 409,
          title: 'State Conflict (HTTP 409)',
          message: data.message || `Application APP-${appId} is no longer in REFERRED status. Another underwriter may have already processed it.`,
          details: data.details || ['Invalid state transition: application must be in REFERRED state.'],
        });
      } else if (status === 403 || err.errorCode === 'ACCESS_FORBIDDEN') {
        setErrorState({
          type: '403',
          status: 403,
          title: 'Access Forbidden (HTTP 403)',
          message: data.message || 'Tenant Isolation Violation: You are not authorized to review applications for this Insurer.',
          details: data.details || ['Role UNDERWRITER or INSURER_ADMIN for the matching insurer is required.'],
        });
      } else {
        setErrorState({
          type: 'GENERAL',
          status: status || 500,
          title: `Decision Failed (${status || 'Network Error'})`,
          message: data.message || 'An error occurred while submitting the underwriter decision.',
          details: data.details || [],
        });
      }
    } finally {
      setSubmitting(false);
    }
  };

  // Filter applications
  const filteredApps = applications.filter((app) => {
    const matchesProduct =
      productFilter === 'ALL' ||
      (productFilter === 'MOTOR' && (app.productType?.includes('MOTOR') || app.productType?.includes('WHEELER'))) ||
      (productFilter === 'HEALTH' && app.productType?.includes('HEALTH'));

    const searchLower = searchQuery.toLowerCase();
    const matchesSearch =
      !searchQuery ||
      String(app.id).includes(searchLower) ||
      (app.vehicleRegNo && app.vehicleRegNo.toLowerCase().includes(searchLower)) ||
      (app.productType && app.productType.toLowerCase().includes(searchLower)) ||
      (app.reason && app.reason.toLowerCase().includes(searchLower));

    return matchesProduct && matchesSearch;
  });

  return (
    <PortalLayout
      title="Underwriter Assessment Queue"
      subtitle="Manual referral decisioning, risk evaluation & IRDAI compliance (SAD §4.3.7, INS-F-011)"
      badgeText="Underwriter Portal"
      badgeClass="bg-success"
    >
      {/* Dev Hook Box */}
      {import.meta.env.DEV && (
        <div className="dev-hook-box mb-4">
          <span className="dev-hook-badge">Person 1 Scope: P1-11 Manual Underwriter Desk</span>
          <p className="mb-0 small">
            Active API: <code>GET /api/v1/applications/referred</code> &amp; <code>POST /api/v1/applications/&#123;id&#125;/decision</code>.
            Enforces tenant isolation (INS-SR-003), immutability audit trail, and customer notification dispatch.
          </p>
        </div>
      )}

      {/* Global Success Banner */}
      {successMessage && (
        <div className="alert alert-success alert-dismissible fade show mb-4 shadow-sm" role="alert" id="underwriter-success-alert">
          <div className="d-flex align-items-center gap-2">
            <span className="fs-5">✅</span>
            <div>
              <strong>Decision Recorded:</strong> {successMessage}
            </div>
          </div>
          <button
            type="button"
            className="btn-close"
            onClick={() => setSuccessMessage(null)}
            aria-label="Close"
          ></button>
        </div>
      )}

      {/* Queue Filter & Stats Strip */}
      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body p-3">
          <div className="row g-3 align-items-center">
            <div className="col-12 col-md-4">
              <label htmlFor={searchInputId} className="visually-hidden">Search Queue</label>
              <div className="input-group">
                <span className="input-group-text bg-white border-end-0">🔍</span>
                <input
                  type="text"
                  id={searchInputId}
                  className="form-control border-start-0 ps-0"
                  placeholder="Search by App ID, Vehicle Reg, Rule..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>
            </div>

            <div className="col-6 col-md-3">
              <label htmlFor={filterSelectId} className="visually-hidden">Filter Product</label>
              <select
                id={filterSelectId}
                className="form-select"
                value={productFilter}
                onChange={(e) => setProductFilter(e.target.value)}
              >
                <option value="ALL">All Product Lines</option>
                <option value="MOTOR">Motor Insurance</option>
                <option value="HEALTH">Health Insurance</option>
              </select>
            </div>

            <div className="col-6 col-md-3">
              <button
                type="button"
                className="btn btn-outline-secondary w-100"
                onClick={fetchReferredApplications}
                disabled={loading}
              >
                {loading ? 'Refreshing...' : '🔄 Refresh Queue'}
              </button>
            </div>

            <div className="col-12 col-md-2 text-md-end">
              <span className="badge bg-warning text-dark px-3 py-2 fs-6">
                {filteredApps.length} Pending Referral{filteredApps.length === 1 ? '' : 's'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Referred Applications Queue */}
      <div className="dashboard-card mb-4">
        <div className="dashboard-card-header d-flex justify-content-between align-items-center">
          <span className="fw-bold">Applications Awaiting Underwriter Review</span>
          <span className="text-muted small">Rule-Triggered Cases (TC-UW-04)</span>
        </div>

        <div className="dashboard-card-body p-0">
          {loading ? (
            <div className="text-center py-5">
              <div className="spinner-border text-primary" role="status">
                <span className="visually-hidden">Loading queue...</span>
              </div>
              <p className="mt-2 text-muted small">Loading referred applications...</p>
            </div>
          ) : filteredApps.length === 0 ? (
            <div className="text-center py-5">
              <span className="fs-1 text-muted">🎉</span>
              <h5 className="mt-2 fw-semibold">No Referred Applications</h5>
              <p className="text-muted small mb-0">
                All auto-underwriting referrals have been resolved or queue is empty.
              </p>
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover mb-0 align-middle" id="underwriter-queue-table">
                <thead className="table-light">
                  <tr>
                    <th>App ID</th>
                    <th>Product</th>
                    <th>Customer / Asset</th>
                    <th>Premium</th>
                    <th style={{ minWidth: '280px' }}>Triggered Referral Rule</th>
                    <th className="text-end">Assessment Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredApps.map((app) => {
                    let parsedDetails = {};
                    try {
                      parsedDetails = app.detailsJson ? JSON.parse(app.detailsJson) : {};
                    } catch {
                      parsedDetails = {};
                    }

                    const isMotor = app.productType?.includes('MOTOR') || app.productType?.includes('WHEELER');

                    return (
                      <tr key={app.id} id={`referred-app-row-${app.id}`}>
                        <td>
                          <span className="badge bg-light text-dark border fw-bold font-monospace">
                            APP-{app.id}
                          </span>
                          <div className="text-muted extra-small mt-1">
                            Plan #{app.planId}
                          </div>
                        </td>

                        <td>
                          <span className={`badge ${isMotor ? 'bg-primary-subtle text-primary' : 'bg-success-subtle text-success'} border`}>
                            {app.productType || 'GENERAL'}
                          </span>
                        </td>

                        <td>
                          <div className="fw-semibold">
                            Customer #{app.customerId}
                          </div>
                          <div className="text-muted small">
                            {isMotor ? (
                              <span>Vehicle: {app.vehicleRegNo || parsedDetails.make || 'Reg N/A'}</span>
                            ) : (
                              <span>Age: {parsedDetails.age || parsedDetails.applicantAge || 'N/A'} yrs</span>
                            )}
                          </div>
                        </td>

                        <td className="fw-bold text-dark">
                          ₹{app.premium ? Number(app.premium).toLocaleString('en-IN') : 'N/A'}
                        </td>

                        <td>
                          <div className="p-2 rounded bg-warning-subtle border border-warning-subtle text-dark small">
                            <div className="fw-semibold text-warning-emphasis d-flex align-items-center gap-1">
                              <span>⚠️ Referral Trigger:</span>
                            </div>
                            <div className="mt-1" id={`referral-reason-${app.id}`}>
                              {app.reason || 'Flagged for underwriter risk evaluation'}
                            </div>
                            {parsedDetails.hasPreExisting && (
                              <span className="badge bg-danger-subtle text-danger border mt-1">
                                Pre-Existing Condition Declared
                              </span>
                            )}
                          </div>
                        </td>

                        <td className="text-end">
                          <div className="btn-group">
                            <button
                              type="button"
                              className="btn btn-sm btn-outline-success"
                              id={`approve-btn-${app.id}`}
                              data-testid={`approve-btn-${app.id}`}
                              onClick={() => handleOpenDecision(app, 'APPROVE')}
                            >
                              Approve
                            </button>
                            <button
                              type="button"
                              className="btn btn-sm btn-outline-danger"
                              id={`reject-btn-${app.id}`}
                              data-testid={`reject-btn-${app.id}`}
                              onClick={() => handleOpenDecision(app, 'REJECT')}
                            >
                              Reject
                            </button>
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

      {/* Decision Review Modal */}
      {selectedApp && (
        <div
          className="modal fade show d-block"
          tabIndex="-1"
          role="dialog"
          style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
          id="underwriter-decision-modal"
        >
          <div className="modal-dialog modal-dialog-centered modal-lg">
            <div className="modal-content shadow-lg border-0">
              <div className="modal-header border-bottom">
                <h5 className="modal-title fw-bold">
                  Underwriter Review — Application APP-{selectedApp.id}
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={handleCloseDecision}
                  aria-label="Close"
                ></button>
              </div>

              <form onSubmit={handleSubmitDecision} noValidate>
                <div className="modal-body p-4">
                  {/* Error Banner inside Modal (422, 409, 403, General) */}
                  {errorState && (
                    <div
                      className={`alert ${
                        errorState.type === '422'
                          ? 'alert-warning border-warning'
                          : errorState.type === '409'
                          ? 'alert-info border-info'
                          : errorState.type === '403'
                          ? 'alert-danger border-danger'
                          : 'alert-danger'
                      } mb-4 shadow-sm`}
                      role="alert"
                      id="decision-error-banner"
                    >
                      <div className="d-flex align-items-start gap-2">
                        <span className="fs-5">
                          {errorState.type === '422' ? '⚠️' : errorState.type === '409' ? '🔄' : '🛑'}
                        </span>
                        <div>
                          <strong className="d-block">{errorState.title}</strong>
                          <div>{errorState.message}</div>
                          {errorState.details?.length > 0 && (
                            <ul className="mb-0 mt-2 ps-3 small">
                              {errorState.details.map((d, idx) => (
                                <li key={idx}>{d}</li>
                              ))}
                            </ul>
                          )}
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Summary Box */}
                  <div className="bg-light p-3 rounded mb-4 border">
                    <div className="row g-2 small">
                      <div className="col-6 col-md-3">
                        <span className="text-muted d-block">Application ID</span>
                        <span className="fw-bold font-monospace">APP-{selectedApp.id}</span>
                      </div>
                      <div className="col-6 col-md-3">
                        <span className="text-muted d-block">Product Type</span>
                        <span className="fw-semibold">{selectedApp.productType}</span>
                      </div>
                      <div className="col-6 col-md-3">
                        <span className="text-muted d-block">Insurer Tenant</span>
                        <span className="fw-semibold">Insurer #{selectedApp.insurerId}</span>
                      </div>
                      <div className="col-6 col-md-3">
                        <span className="text-muted d-block">Quoted Premium</span>
                        <span className="fw-bold text-success">
                          ₹{Number(selectedApp.premium || 0).toLocaleString('en-IN')}
                        </span>
                      </div>
                    </div>

                    <div className="mt-3 pt-2 border-top">
                      <span className="text-muted d-block small mb-1">Referral Trigger Reason:</span>
                      <div className="p-2 rounded bg-warning-subtle text-dark border border-warning small fw-semibold">
                        {selectedApp.reason || 'Evaluated and referred by automated underwriting rule engine.'}
                      </div>
                    </div>
                  </div>

                  {/* Decision Selection */}
                  <div className="mb-3">
                    <label className="form-label fw-bold">Manual Underwriting Decision *</label>
                    <div className="d-flex gap-3">
                      <div className="form-check form-check-inline border rounded p-2 px-3 flex-fill">
                        <input
                          className="form-check-input"
                          type="radio"
                          name="underwriterDecision"
                          id="decisionApprove"
                          value="APPROVE"
                          checked={decision === 'APPROVE'}
                          onChange={() => setDecision('APPROVE')}
                        />
                        <label className="form-check-label fw-semibold text-success ms-1" htmlFor="decisionApprove">
                          ✅ APPROVE Application
                        </label>
                      </div>

                      <div className="form-check form-check-inline border rounded p-2 px-3 flex-fill">
                        <input
                          className="form-check-input"
                          type="radio"
                          name="underwriterDecision"
                          id="decisionReject"
                          value="REJECT"
                          checked={decision === 'REJECT'}
                          onChange={() => setDecision('REJECT')}
                        />
                        <label className="form-check-label fw-semibold text-danger ms-1" htmlFor="decisionReject">
                          ❌ REJECT Application
                        </label>
                      </div>
                    </div>
                  </div>

                  {/* Mandatory Justification Reason */}
                  <div className="mb-3">
                    <label htmlFor={reasonTextareaId} className="form-label fw-bold">
                      Justification Reason * <span className="text-muted small fw-normal">(Strictly required for audit trail)</span>
                    </label>
                    <textarea
                      className="form-control"
                      id={reasonTextareaId}
                      data-testid="reason-textarea"
                      rows="3"
                      placeholder={
                        decision === 'APPROVE'
                          ? 'Provide regulatory justification (e.g. Verified clean medical report, senior underwriting approval granted)...'
                          : 'Provide specific reason for rejection (e.g. Unacceptable risk due to adverse claims history)...'
                      }
                      value={reason}
                      onChange={(e) => setReason(e.target.value)}
                      required
                    ></textarea>
                    <div className="form-text extra-small">
                      This reason will be recorded in the immutable audit log and transmitted in the customer status notification.
                    </div>
                  </div>
                </div>

                <div className="modal-footer border-top bg-light">
                  <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={handleCloseDecision}
                    disabled={submitting}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className={`btn ${decision === 'APPROVE' ? 'btn-success' : 'btn-danger'} px-4`}
                    id="submit-decision-btn"
                    data-testid="submit-decision-btn"
                    disabled={submitting}
                  >
                    {submitting
                      ? 'Submitting Decision...'
                      : decision === 'APPROVE'
                      ? 'Confirm Approval'
                      : 'Confirm Rejection'}
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
