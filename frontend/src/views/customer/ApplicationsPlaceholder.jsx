import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import PortalLayout from '../../components/layout/PortalLayout';
import apiClient from '../../api/apiClient';
import { ROUTES } from '../../routes/routePaths';

export default function ApplicationsPlaceholder() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const quoteIdParam = searchParams.get('quoteId');
  const planIdParam = searchParams.get('planId');

  const [vehicleRegNo, setVehicleRegNo] = useState('KA01AB1234');
  const [documentIds, setDocumentIds] = useState('301, 302');
  const [uploadingFile, setUploadingFile] = useState(false);
  const [uploadMessage, setUploadMessage] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [submissionResult, setSubmissionResult] = useState(null);
  const [submissionError, setSubmissionError] = useState(null);

  const [applications, setApplications] = useState([]);
  const [loadingApps, setLoadingApps] = useState(false);
  const [appsError, setAppsError] = useState(null);

  const [selectedTimelineApp, setSelectedTimelineApp] = useState(null);

  const fetchApplications = async () => {
    setLoadingApps(true);
    setAppsError(null);
    try {
      const res = await apiClient.get('/applications');
      setApplications(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      setAppsError(err.response?.data?.message || err.message || 'Failed to load applications.');
    } finally {
      setLoadingApps(false);
    }
  };

  useEffect(() => {
    fetchApplications();
  }, []);

  const handleFileUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploadingFile(true);
    setUploadMessage(null);
    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await apiClient.post('/applications/documents', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      const newDocId = res.data?.documentId;
      if (newDocId != null) {
        setDocumentIds((prev) => (prev && prev.trim() ? `${prev.trim()}, ${newDocId}` : `${newDocId}`));
        setUploadMessage(`Uploaded ${res.data?.filename || file.name} (ID #${newDocId})`);
      }
    } catch (err) {
      setUploadMessage(`Upload failed: ${err.response?.data?.message || err.message || 'Server error'}`);
    } finally {
      setUploadingFile(false);
    }
  };

  const handleSubmitApplication = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setSubmissionError(null);

    const docList = documentIds
      .split(',')
      .map((id) => Number(id.trim()))
      .filter((n) => !isNaN(n) && n > 0);

    const payload = {
      quoteId: Number(quoteIdParam),
      details: {
        vehicleRegNo,
        submittedVia: 'Customer Quoting Portal P1-08',
      },
      documentIds: docList.length > 0 ? docList : [301],
    };

    try {
      const response = await apiClient.post('/applications', payload);
      setSubmissionResult(response.data);
      const isApproved = response.data.status === 'Approved' || response.data.status === 'APPROVED';
      setSelectedTimelineApp({
        id: response.data.applicationId,
        plan: `Plan #${planIdParam || 1}`,
        step: isApproved ? 4 : 3,
        status: response.data.status,
        premium: response.data.premium || null,
      });
      fetchApplications();
    } catch (err) {
      setSubmissionResult(null);
      const status = err.response?.status;
      const msg =
        err.details && err.details.length > 0
          ? err.details.join(', ')
          : err.response?.data?.message || err.message || 'Submission failed. Please check your inputs.';
      if (status === 403) {
        setSubmissionError(`Access Denied (403): ${msg}`);
      } else if (status === 422) {
        setSubmissionError(`Validation / Underwriting Rejection (422): ${msg}`);
      } else {
        setSubmissionError(msg);
      }
    } finally {
      setSubmitting(false);
    }
  };

  const steps = [
    { num: 1, label: 'Quote Calculated', desc: 'Premium & plan selected' },
    { num: 2, label: 'Application Submitted', desc: 'KYC docs & details attached' },
    { num: 3, label: 'Underwriting Review', desc: 'STP auto-approval or appraisal' },
    { num: 4, label: 'Premium Payment', desc: 'Razorpay / Gateway checkout' },
    { num: 5, label: 'Policy Issued', desc: 'OpenPDF digital certificate ready' },
  ];

  return (
    <PortalLayout
      title="Customer Applications & Workflow"
      subtitle="Application lifecycle tracker, underwriting status timeline & payment handoff (INS-F-007 / P3-04a / P3-06)"
      badgeText="Policy Applications"
      badgeClass="bg-info text-dark"
    >
      {import.meta.env.DEV && (
        <div className="dev-hook-box mb-4">
          <span className="dev-hook-badge">Person 3 Ownership (Application Flow C5)</span>
          <p className="mb-0">
            Connects to <code>POST /api/v1/applications</code>. Consumes quotes generated by Person 1's quoting engine
            (<code>/customer/quote/motor</code> & <code>/customer/quote/health</code>) and transitions through underwriting to payment and policy issuance.
          </p>
        </div>
      )}

      {/* Application Lifecycle Timeline Widget */}
      {selectedTimelineApp && (
        <div className="card shadow-sm mb-4 border-0" id="application-lifecycle-timeline">
          <div className="card-header bg-light d-flex justify-content-between align-items-center py-3">
            <div>
              <span className="fw-bold text-dark me-2">
                <i className="bi bi-diagram-3 me-2 text-primary"></i>
                Application Lifecycle Timeline: APP-{selectedTimelineApp.id}
              </span>
              <span className="badge bg-secondary">{selectedTimelineApp.plan}</span>
            </div>
            <div>
              <span className="small text-muted me-2">Current State:</span>
              <span
                className={`badge ${
                  selectedTimelineApp.step === 5
                    ? 'bg-success'
                    : selectedTimelineApp.step === 4
                    ? 'bg-primary'
                    : selectedTimelineApp.status === 'Referred'
                    ? 'bg-warning text-dark'
                    : 'bg-info text-dark'
                }`}
              >
                {selectedTimelineApp.step === 5
                  ? 'Policy Issued & Active'
                  : selectedTimelineApp.step === 4
                  ? 'Ready for Payment'
                  : selectedTimelineApp.status}
              </span>
            </div>
          </div>
          <div className="card-body p-4">
            <div className="row g-2 text-center position-relative">
              {steps.map((st) => {
                const isCompleted = st.num < selectedTimelineApp.step;
                const isCurrent = st.num === selectedTimelineApp.step;
                return (
                  <div key={st.num} className="col">
                    <div className="d-flex flex-column align-items-center">
                      <div
                        className={`rounded-circle d-flex align-items-center justify-content-center shadow-sm mb-2 ${
                          isCompleted
                            ? 'bg-success text-white'
                            : isCurrent
                            ? 'bg-primary text-white border border-3 border-primary-subtle'
                            : 'bg-light text-muted border'
                        }`}
                        style={{ width: '42px', height: '42px', fontSize: '1rem', fontWeight: 'bold' }}
                      >
                        {isCompleted ? <i className="bi bi-check-lg"></i> : st.num}
                      </div>
                      <div className={`fw-semibold small ${isCurrent ? 'text-primary' : 'text-dark'}`}>
                        {st.label}
                      </div>
                      <div className="text-muted extra-small d-none d-md-block mt-1">{st.desc}</div>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Stage Action Prompt */}
            <div className="mt-4 p-3 bg-light rounded d-flex flex-wrap justify-content-between align-items-center gap-2">
              <div>
                {selectedTimelineApp.step === 4 ? (
                  <span className="text-dark small">
                    <i className="bi bi-check-circle-fill text-success me-1"></i>
                    Application is approved! Complete premium payment of{' '}
                    <strong>
                      {selectedTimelineApp.premium != null
                        ? `₹${Number(selectedTimelineApp.premium).toLocaleString('en-IN')}`
                        : 'applicable premium'}
                    </strong>{' '}
                    to immediately issue your policy.
                  </span>
                ) : selectedTimelineApp.step === 5 ? (
                  <span className="text-dark small">
                    <i className="bi bi-shield-fill-check text-success me-1"></i>
                    Premium has been paid and policy schedule is issued. View or download your OpenPDF certificate.
                  </span>
                ) : (
                  <span className="text-dark small">
                    <i className="bi bi-hourglass-split text-warning me-1"></i>
                    Application is undergoing underwriter evaluation. You will be notified upon appraisal.
                  </span>
                )}
              </div>
              <div className="d-flex gap-2">
                {selectedTimelineApp.step === 4 && (
                  <button
                    className="btn btn-primary btn-sm px-3"
                    onClick={() => navigate(`/customer/payment/${selectedTimelineApp.id}`)}
                  >
                    Proceed to Payment &rarr;
                  </button>
                )}
                {selectedTimelineApp.step === 5 && (
                  <Link to={ROUTES.CUSTOMER_POLICIES} className="btn btn-success btn-sm px-3">
                    <i className="bi bi-file-earmark-pdf me-1"></i>View Issued Policy
                  </Link>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Quote Handoff Banner (P1-08 -> P3-04a) */}
      {quoteIdParam && (
        <div className="card border-success shadow-sm mb-4" id="active-quote-banner">
          <div className="card-header bg-success-subtle text-success-emphasis border-success-subtle d-flex justify-content-between align-items-center py-3">
            <div className="d-flex align-items-center gap-2">
              <i className="bi bi-check-circle-fill text-success fs-5"></i>
              <span className="fw-bold">Active Quote Attached for Application</span>
            </div>
            <span className="badge bg-success font-monospace" id="attached-quote-id">
              Quote #{quoteIdParam}
            </span>
          </div>

          <div className="card-body p-4">
            {submissionResult ? (
              <div className="alert alert-success border-0 shadow-sm p-4 text-center" id="application-success-box">
                <i className="bi bi-award-fill text-success fs-1 mb-2 d-block"></i>
                <h5 className="fw-bold text-success mb-1">
                  Application Successfully Submitted!
                </h5>
                <p className="text-muted small mb-3">
                  Reference: <strong>APP-{submissionResult.applicationId}</strong> (Quote #{quoteIdParam})
                </p>
                <div className="d-inline-block px-3 py-1 rounded-pill bg-light border mb-3">
                  Status:{' '}
                  <span
                    className={`fw-bold ${
                      submissionResult.status === 'Approved' ? 'text-success' : 'text-warning-emphasis'
                    }`}
                  >
                    {submissionResult.status}
                  </span>
                </div>
                <p className="small text-secondary mb-3">
                  {submissionResult.details || 'Your application is securely queued for policy issuance and payment.'}
                </p>
                {submissionResult.status === 'Approved' && (
                  <div>
                    <button
                      className="btn btn-primary btn-sm px-4"
                      onClick={() => navigate(`/customer/payment/${submissionResult.applicationId}`)}
                    >
                      Proceed to Payment &rarr;
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <form onSubmit={handleSubmitApplication} id="application-submit-form">
                <div className="row g-3 mb-3">
                  <div className="col-12 col-md-6">
                    <label htmlFor="app-quote-id" className="form-label small fw-semibold text-secondary">
                      Quote Identifier
                    </label>
                    <input
                      type="text"
                      id="app-quote-id"
                      className="form-control form-control-sm bg-light"
                      value={`Quote #${quoteIdParam} (Plan #${planIdParam || 1})`}
                      disabled
                    />
                  </div>
                  <div className="col-12 col-md-6">
                    <label htmlFor="app-reg-no" className="form-label small fw-semibold text-secondary">
                      Vehicle Registration Number / Identification
                    </label>
                    <input
                      type="text"
                      id="app-reg-no"
                      className="form-control form-control-sm"
                      value={vehicleRegNo}
                      onChange={(e) => setVehicleRegNo(e.target.value)}
                      required
                    />
                  </div>
                </div>

                <div className="row g-3 mb-3">
                  <div className="col-12 col-md-6">
                    <label htmlFor="app-doc-upload" className="form-label small fw-semibold text-secondary">
                      Upload Supporting Document
                    </label>
                    <input
                      type="file"
                      id="app-doc-upload"
                      className="form-control form-control-sm"
                      accept=".pdf,.png,.jpg,.jpeg"
                      onChange={handleFileUpload}
                      disabled={uploadingFile}
                    />
                    {uploadMessage && (
                      <span className="text-primary extra-small d-block mt-1">{uploadMessage}</span>
                    )}
                  </div>
                  <div className="col-12 col-md-6">
                    <label htmlFor="app-doc-ids" className="form-label small fw-semibold text-secondary">
                      Uploaded Verification Document IDs (Comma-separated)
                    </label>
                    <input
                      type="text"
                      id="app-doc-ids"
                      className="form-control form-control-sm"
                      value={documentIds}
                      onChange={(e) => setDocumentIds(e.target.value)}
                      required
                    />
                    <span className="text-muted extra-small d-block mt-1">
                      Preloaded verified Aadhaar/PAN and RC documents from KYC store (301, 302).
                    </span>
                  </div>
                </div>

                {submissionError && (
                  <div className="alert alert-danger small py-2 mb-3 d-flex justify-content-between align-items-center" role="alert">
                    <span>{submissionError}</span>
                    <button
                      type="button"
                      className="btn btn-outline-danger btn-sm py-0 ms-2"
                      onClick={handleSubmitApplication}
                    >
                      Retry
                    </button>
                  </div>
                )}

                <div className="d-flex gap-2">
                  <button
                    type="submit"
                    className="btn btn-success btn-sm px-4 fw-semibold shadow-sm"
                    id="submit-application-btn"
                    disabled={submitting}
                  >
                    {submitting ? (
                      <>
                        <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                        Submitting Application...
                      </>
                    ) : (
                      <>
                        <i className="bi bi-file-earmark-check me-2"></i>Submit Application to Underwriting
                      </>
                    )}
                  </button>
                  <Link to={ROUTES.CUSTOMER_QUOTE_MOTOR} className="btn btn-outline-secondary btn-sm">
                    Recalculate Quote
                  </Link>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Existing Feed Card */}
      <div className="dashboard-card">
        <div className="dashboard-card-header d-flex justify-content-between align-items-center">
          <span className="fw-bold">Recent Application Submissions</span>
          <span className="badge bg-secondary">Workflow Feed</span>
        </div>
        <div className="dashboard-card-body p-0">
          <div className="table-responsive">
            <table className="table table-hover mb-0 align-middle">
              <thead className="table-light">
                <tr>
                  <th>Application ID</th>
                  <th>Quote Ref</th>
                  <th>Plan Selected</th>
                  <th>Date Submitted</th>
                  <th>Underwriting Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {loadingApps && (
                  <tr>
                    <td colSpan="6" className="text-center py-4 text-muted">
                      <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                      Loading applications...
                    </td>
                  </tr>
                )}
                {appsError && (
                  <tr>
                    <td colSpan="6" className="p-3">
                      <div className="alert alert-danger mb-0 d-flex justify-content-between align-items-center">
                        <span>{appsError}</span>
                        <button
                          type="button"
                          className="btn btn-outline-danger btn-sm py-0 ms-2"
                          onClick={fetchApplications}
                        >
                          Retry
                        </button>
                      </div>
                    </td>
                  </tr>
                )}
                {!loadingApps && !appsError && applications.length === 0 && (
                  <tr>
                    <td colSpan="6" className="text-center py-4 text-muted">
                      No applications yet.
                    </td>
                  </tr>
                )}
                {!loadingApps &&
                  !appsError &&
                  applications.map((app) => {
                    const isApproved = app.status === 'APPROVED' || app.status === 'Approved';
                    const isPaid = app.status === 'POLICY_ISSUED' || app.policyIssued;
                    const isRejected = app.status === 'REJECTED' || app.status === 'Rejected';
                    return (
                      <tr key={app.id}>
                        <td className="fw-semibold">APP-{app.id}</td>
                        <td>{app.quoteId ? `Quote #${app.quoteId}` : '—'}</td>
                        <td>{app.planName || (app.planId ? `Plan #${app.planId}` : '—')}</td>
                        <td>{app.createdAt ? new Date(app.createdAt).toLocaleDateString() : '—'}</td>
                        <td>
                          <span
                            className={`status-badge ${
                              isPaid
                                ? 'status-active'
                                : isApproved
                                ? 'status-approved'
                                : isRejected
                                ? 'bg-danger text-white'
                                : 'status-referred'
                            }`}
                          >
                            {app.status}
                          </span>
                        </td>
                        <td>
                          {isApproved && !isPaid && (
                            <button
                              id={`pay-premium-${app.id}-btn`}
                              className="btn btn-primary btn-sm me-2"
                              onClick={() => navigate(`/customer/payment/${app.id}`)}
                            >
                              Pay Premium{' '}
                              {app.premium != null ? `(₹${Number(app.premium).toLocaleString('en-IN')})` : ''}
                            </button>
                          )}
                          {isPaid && (
                            <Link to={ROUTES.CUSTOMER_POLICIES} className="btn btn-outline-success btn-sm me-2">
                              <i className="bi bi-file-earmark-pdf me-1"></i>View Policy
                            </Link>
                          )}
                          <button
                            className="btn btn-outline-secondary btn-sm"
                            onClick={() =>
                              setSelectedTimelineApp({
                                id: app.id,
                                plan: app.planName || `Plan #${app.planId || 1}`,
                                step: isPaid ? 5 : isApproved ? 4 : app.timelineStep || 3,
                                status: app.status,
                                premium: app.premium != null ? Number(app.premium) : null,
                              })
                            }
                          >
                            Timeline
                          </button>
                        </td>
                      </tr>
                    );
                  })}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </PortalLayout>
  );
}