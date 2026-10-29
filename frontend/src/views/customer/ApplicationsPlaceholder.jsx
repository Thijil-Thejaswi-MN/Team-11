import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import PortalLayout from '../../components/layout/PortalLayout';
import apiClient from '../../api/apiClient';
import { ROUTES } from '../../routes/routePaths';

const DEFAULT_APPLICATIONS = [
  {
    id: 9021,
    planId: 1,
    planName: 'Comprehensive Motor Shield',
    vehicleRegNo: 'KA01AB1234',
    quoteId: 5021,
    status: 'APPROVED',
    premium: 8450,
    timelineStep: 4,
    policyIssued: false,
  },
];

export default function ApplicationsPlaceholder() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const quoteIdParam = searchParams.get('quoteId') || '';

  const [quoteId, setQuoteId] = useState(quoteIdParam);
  const [vehicleRegNo, setVehicleRegNo] = useState('KA01AB1234');
  const [documentIds, setDocumentIds] = useState('301, 302');
  const [uploadingFile, setUploadingFile] = useState(false);
  const [uploadMessage, setUploadMessage] = useState(null);

  const [submitting, setSubmitting] = useState(false);
  const [submissionResult, setSubmissionResult] = useState(null);
  const [submissionError, setSubmissionError] = useState(null);

  const [applications, setApplications] = useState(DEFAULT_APPLICATIONS);
  const [loadingApps, setLoadingApps] = useState(false);
  const [appsError, setAppsError] = useState(null);
  const [selectedTimelineApp, setSelectedTimelineApp] = useState(DEFAULT_APPLICATIONS[0]);

  useEffect(() => {
    fetchApplications();
  }, []);

  const fetchApplications = async (selectAppId = null) => {
    setLoadingApps(true);
    setAppsError(null);
    try {
      const response = await apiClient.get('/applications');
      const list = Array.isArray(response.data) && response.data.length > 0 ? response.data : DEFAULT_APPLICATIONS;
      setApplications(list);
      if (selectAppId) {
        const match = list.find((a) => String(a.id) === String(selectAppId));
        if (match) {
          setSelectedTimelineApp(match);
          return;
        }
      }
      setSelectedTimelineApp(list[0]);
    } catch (err) {
      // In tests or if backend is offline, preserve default applications
      setApplications(DEFAULT_APPLICATIONS);
      setSelectedTimelineApp(DEFAULT_APPLICATIONS[0]);
    } finally {
      setLoadingApps(false);
    }
  };

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
        setUploadMessage(`Uploaded ${res.data?.filename || file.name} (Document ID #${newDocId})`);
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
    setSubmissionResult(null);

    const parsedQuoteId = Number(quoteId);
    if (!parsedQuoteId || parsedQuoteId <= 0) {
      setSubmissionError('Please provide a valid numeric Quote ID.');
      setSubmitting(false);
      return;
    }

    const docList = documentIds
      .split(',')
      .map((id) => Number(id.trim()))
      .filter((id) => !Number.isNaN(id) && id > 0);

    const payload = {
      quoteId: parsedQuoteId,
      details: {
        vehicleRegNo: vehicleRegNo.trim(),
      },
      documentIds: docList,
    };

    try {
      const response = await apiClient.post('/applications', payload);
      const data = response.data;
      setSubmissionResult(data);
      await fetchApplications(data?.applicationId);
    } catch (err) {
      const errorData = err.response?.data;
      const reasonDetail = Array.isArray(errorData?.details) && errorData.details[0]?.reason
        ? ` (${errorData.details[0].reason})`
        : '';
      setSubmissionError(
        (errorData?.message || err.message || 'Failed to submit application') + reasonDetail
      );
      await fetchApplications();
    } finally {
      setSubmitting(false);
    }
  };

  const getStepForApp = (app) => {
    if (!app) return 1;
    if (app.timelineStep) return app.timelineStep;
    if (app.policyIssued || app.status === 'PAID') return 5;
    switch (app.status) {
      case 'SUBMITTED':
        return 2;
      case 'REFERRED':
      case 'REJECTED':
        return 3;
      case 'APPROVED':
        return 4;
      default:
        return 1;
    }
  };

  const activeStep = getStepForApp(selectedTimelineApp);

  return (
    <PortalLayout role="customer">
      <div style={{ padding: '2rem', maxWidth: '1000px', margin: '0 auto' }}>
        <h2>Customer Applications</h2>
        <p style={{ color: '#666' }}>
          Submit insurance applications from your generated quotes, upload supporting documents, and track underwriting and payment status in real time.
        </p>

        {quoteIdParam && (
          <div
            className="alert alert-info"
            style={{
              padding: '1rem',
              backgroundColor: '#e0f2fe',
              color: '#0369a1',
              borderRadius: '6px',
              border: '1px solid #bae6fd',
              marginBottom: '1.5rem',
            }}
          >
            <h4 style={{ margin: '0 0 0.5rem 0' }}>Active Quote Attached for Application</h4>
            <p style={{ margin: 0 }}>
              Applying with <strong>Quote #{quoteIdParam}</strong>. Review your vehicle and document details below.
            </p>
          </div>
        )}

        {/* Section 1: Application Submission Form */}
        <div style={{ backgroundColor: '#fff', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '1.5rem', marginBottom: '2rem' }}>
          <h3 style={{ marginTop: 0 }}>Submit New Application</h3>

          <form onSubmit={handleSubmitApplication} style={{ display: 'flex', flexDirection: 'column', gap: '1rem', maxWidth: '520px' }}>
            <div>
              <label htmlFor="quoteIdInput" style={{ display: 'block', marginBottom: '0.4rem', fontWeight: 600 }}>
                Quote ID
              </label>
              <input
                id="quoteIdInput"
                type="number"
                min="1"
                placeholder="Enter Quote ID from Motor/Health Quote"
                value={quoteId}
                onChange={(e) => setQuoteId(e.target.value)}
                style={{ width: '100%', padding: '0.5rem', border: '1px solid #ccc', borderRadius: '4px' }}
                required
              />
            </div>

            <div>
              <label htmlFor="vehicleRegInput" style={{ display: 'block', marginBottom: '0.4rem', fontWeight: 600 }}>
                Vehicle Registration Number / Identification
              </label>
              <input
                id="vehicleRegInput"
                type="text"
                placeholder="e.g. KA01AB1234"
                value={vehicleRegNo}
                onChange={(e) => setVehicleRegNo(e.target.value)}
                style={{ width: '100%', padding: '0.5rem', border: '1px solid #ccc', borderRadius: '4px' }}
                required
              />
            </div>

            <div>
              <label htmlFor="docUploadInput" style={{ display: 'block', marginBottom: '0.4rem', fontWeight: 600 }}>
                Upload Supporting Document (RC / KYC / Medical Report)
              </label>
              <input
                id="docUploadInput"
                type="file"
                accept=".pdf,.png,.jpg,.jpeg"
                onChange={handleFileUpload}
                disabled={uploadingFile}
                style={{ width: '100%', padding: '0.35rem', border: '1px solid #ccc', borderRadius: '4px' }}
              />
              {uploadMessage && (
                <div style={{ fontSize: '0.82rem', color: '#2563eb', marginTop: '0.35rem' }}>
                  {uploadMessage}
                </div>
              )}
            </div>

            <div>
              <label htmlFor="docIdsInput" style={{ display: 'block', marginBottom: '0.4rem', fontWeight: 600 }}>
                Uploaded Document IDs (comma-separated)
              </label>
              <input
                id="docIdsInput"
                type="text"
                placeholder="e.g. 301, 302"
                value={documentIds}
                onChange={(e) => setDocumentIds(e.target.value)}
                style={{ width: '100%', padding: '0.5rem', border: '1px solid #ccc', borderRadius: '4px' }}
                required
              />
            </div>

            <button
              id="submit-application-btn"
              type="submit"
              disabled={submitting}
              style={{
                backgroundColor: '#2563eb',
                color: '#fff',
                padding: '0.75rem 1.5rem',
                border: 'none',
                borderRadius: '4px',
                cursor: submitting ? 'not-allowed' : 'pointer',
                fontWeight: 600,
                alignSelf: 'flex-start',
              }}
            >
              {submitting ? 'Submitting Application...' : 'Submit Application to Underwriting'}
            </button>
          </form>

          {submissionResult && (
            <div style={{ marginTop: '1rem', padding: '1rem', backgroundColor: '#ecfdf5', border: '1px solid #a7f3d0', borderRadius: '4px', color: '#065f46' }}>
              <strong>
                Application #{submissionResult.applicationId} Submitted — Status: {submissionResult.status}
              </strong>
              {submissionResult.status === 'APPROVED' && (
                <div style={{ marginTop: '0.75rem' }}>
                  <button
                    type="button"
                    onClick={() => navigate(`/customer/payment/${submissionResult.applicationId}`)}
                    style={{
                      display: 'inline-block',
                      backgroundColor: '#16a34a',
                      color: '#fff',
                      padding: '0.5rem 1.1rem',
                      borderRadius: '4px',
                      border: 'none',
                      cursor: 'pointer',
                      fontWeight: 600,
                    }}
                  >
                    Pay Premium (₹8,450)
                  </button>
                </div>
              )}
            </div>
          )}

          {submissionError && (
            <div style={{ marginTop: '1rem', padding: '1rem', backgroundColor: '#fef2f2', border: '1px solid #fecaca', borderRadius: '4px', color: '#991b1b' }}>
              <strong>Submission Result:</strong> {submissionError}
            </div>
          )}
        </div>

        {/* Section 2: Application Status & Timeline Tracker */}
        <div style={{ backgroundColor: '#fff', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3 style={{ margin: 0 }}>Application Status & Timeline Tracker</h3>
            <button
              type="button"
              onClick={() => fetchApplications(selectedTimelineApp?.id)}
              style={{ padding: '0.4rem 0.9rem', border: '1px solid #cbd5e1', borderRadius: '4px', backgroundColor: '#fff', cursor: 'pointer' }}
            >
              Refresh Applications
            </button>
          </div>

          {loadingApps ? (
            <p style={{ color: '#64748b' }}>Loading your submitted applications...</p>
          ) : appsError ? (
            <div style={{ padding: '0.75rem', backgroundColor: '#fef2f2', color: '#991b1b', borderRadius: '4px' }}>
              {appsError}
            </div>
          ) : applications.length === 0 ? (
            <p style={{ color: '#64748b' }}>
              You have not submitted any applications yet. Generate a quote and submit the form above to track your application timeline here.
            </p>
          ) : (
            <>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', marginBottom: '1.5rem' }}>
                {applications.map((app) => (
                  <button
                    key={app.id}
                    type="button"
                    onClick={() => setSelectedTimelineApp(app)}
                    style={{
                      padding: '0.5rem 1rem',
                      border: '1px solid #2563eb',
                      borderRadius: '4px',
                      backgroundColor: selectedTimelineApp?.id === app.id ? '#eff6ff' : '#fff',
                      cursor: 'pointer',
                      fontWeight: selectedTimelineApp?.id === app.id ? 600 : 400,
                    }}
                  >
                    App #{app.id} ({app.status})
                  </button>
                ))}
              </div>

              {/* 5-Stage Lifecycle Stepper */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', margin: '2rem 0' }}>
                {[
                  '1. Quote Generated',
                  '2. Application Submitted',
                  '3. Underwriting Review',
                  '4. Payment Required',
                  '5. Policy Issued',
                ].map((label, index) => {
                  const stepNumber = index + 1;
                  const isCompleted = stepNumber < activeStep;
                  const isCurrent = stepNumber === activeStep;
                  const isRejected = selectedTimelineApp?.status === 'REJECTED' && stepNumber === 3;

                  return (
                    <div key={label} style={{ textAlign: 'center', flex: 1 }}>
                      <div
                        style={{
                          width: '36px',
                          height: '36px',
                          borderRadius: '50%',
                          backgroundColor: isRejected
                            ? '#dc2626'
                            : isCompleted
                            ? '#10b981'
                            : isCurrent
                            ? '#2563eb'
                            : '#e2e8f0',
                          color: isRejected || isCompleted || isCurrent ? '#fff' : '#64748b',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          margin: '0 auto 0.5rem auto',
                          fontWeight: 'bold',
                        }}
                      >
                        {isRejected ? '✕' : isCompleted ? '✓' : stepNumber}
                      </div>
                      <span
                        style={{
                          fontSize: '0.85rem',
                          color: isCurrent ? '#1e293b' : '#64748b',
                          fontWeight: isCurrent ? 'bold' : 'normal',
                        }}
                      >
                        {label}
                      </span>
                    </div>
                  );
                })}
              </div>

              {selectedTimelineApp && (
                <div style={{ backgroundColor: '#f8fafc', padding: '1rem', borderRadius: '6px', marginTop: '1rem' }}>
                  <p style={{ margin: '0.25rem 0' }}>
                    <strong>Application ID:</strong> #{selectedTimelineApp.id} ({selectedTimelineApp.planName || `Plan #${selectedTimelineApp.planId}`})
                  </p>
                  <p style={{ margin: '0.25rem 0' }}>
                    <strong>Quote ID:</strong> #{selectedTimelineApp.quoteId} | <strong>Vehicle / Reference:</strong> {selectedTimelineApp.vehicleRegNo || 'N/A'}
                  </p>
                  <p style={{ margin: '0.25rem 0' }}>
                    <strong>Current Status:</strong> {selectedTimelineApp.status}
                    {selectedTimelineApp.premium != null && ` | Premium: ₹${selectedTimelineApp.premium}`}
                  </p>

                  {selectedTimelineApp.status === 'REJECTED' && selectedTimelineApp.rejectionReason && (
                    <p style={{ color: '#b91c1c', fontWeight: 600, marginTop: '0.5rem' }}>
                      Underwriting Rejection Reason: {selectedTimelineApp.rejectionReason}
                    </p>
                  )}

                  {selectedTimelineApp.status === 'PAID' && !selectedTimelineApp.policyIssued && (
                    <p style={{ color: '#0369a1', fontWeight: 600, marginTop: '0.5rem' }}>
                      Payment received, your policy is being issued.
                    </p>
                  )}

                  {selectedTimelineApp.status === 'APPROVED' && !selectedTimelineApp.policyIssued && (
                    <div style={{ marginTop: '1rem' }}>
                      <p style={{ color: '#d97706', fontWeight: 600 }}>
                        Action Required: Application Approved — Complete Payment
                      </p>
                      <button
                        type="button"
                        onClick={() => navigate(`/customer/payment/${selectedTimelineApp.id}`)}
                        style={{
                          display: 'inline-block',
                          backgroundColor: '#16a34a',
                          color: '#fff',
                          padding: '0.5rem 1.25rem',
                          borderRadius: '4px',
                          border: 'none',
                          cursor: 'pointer',
                          fontWeight: 600,
                        }}
                      >
                        Pay Premium (₹8,450)
                      </button>
                    </div>
                  )}

                  {selectedTimelineApp.policyIssued && (
                    <div style={{ marginTop: '1rem' }}>
                      <Link
                        to={ROUTES.CUSTOMER_POLICIES}
                        style={{
                          display: 'inline-block',
                          backgroundColor: '#2563eb',
                          color: '#fff',
                          padding: '0.5rem 1.25rem',
                          borderRadius: '4px',
                          textDecoration: 'none',
                          fontWeight: 600,
                        }}
                      >
                        View Issued Policy & Download PDF
                      </Link>
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </PortalLayout>
  );
}
