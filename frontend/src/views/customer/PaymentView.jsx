import React, { useState, useEffect } from 'react';
import { useParams, useSearchParams, Link } from 'react-router-dom';
import PortalLayout from '../../components/layout/PortalLayout';
import apiClient from '../../api/apiClient';
import { ROUTES } from '../../routes/routePaths';

/**
 * Customer Payment View (P4-17 / INS-F-009).
 * <p>
 * Security Invariants:
 * 1. The browser MUST send ONLY applicationId when starting a payment.
 * 2. Amount and idempotency keys are derived strictly server-side.
 * 3. Never fabricate an application or amount if loading fails or data is missing.
 * 4. Pay button is strictly disabled while loading, when load failed, when no authoritative
 *    application data exists, or when the application status is not APPROVED.
 */
export default function PaymentView() {
  const { applicationId: routeAppId } = useParams();
  const [searchParams] = useSearchParams();

  // Extract numeric application ID from route param or query param
  const rawId = routeAppId || searchParams.get('applicationId') || '';
  const parsedNumericMatch = rawId.match(/\d+$/);
  const cleanId = parsedNumericMatch ? parseInt(parsedNumericMatch[0], 10) : null;

  const [application, setApplication] = useState(null);
  const [loadingApp, setLoadingApp] = useState(true);
  const [loadError, setLoadError] = useState(null);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [paymentState, setPaymentState] = useState('idle'); // 'idle' | 'success' | 'failed' | 'conflict' | 'error'
  const [result, setResult] = useState(null);
  const [errorMessage, setErrorMessage] = useState(null);
  const [errorCode, setErrorCode] = useState(null);

  useEffect(() => {
    if (!cleanId) {
      setLoadError('No valid application ID provided.');
      setApplication(null);
      setLoadingApp(false);
      return;
    }

    setLoadingApp(true);
    setLoadError(null);

    // Fetch authoritative application summary strictly from backend
    apiClient
      .get(`/payments/applications/${cleanId}`)
      .then((res) => {
        setApplication(res.data);
        setLoadError(null);
      })
      .catch((err) => {
        // Authoritative load failed: keep application unset, do NOT fabricate fallback
        const msg = err.message || err.response?.data?.message || 'Unable to load application details.';
        setLoadError(msg);
        setApplication(null);
      })
      .finally(() => {
        setLoadingApp(false);
      });
  }, [cleanId]);

  const isApproved = application?.status === 'APPROVED';
  const isPaid = application?.status === 'PAID' || paymentState === 'success';

  // Strict disable rule per P4-17 requirement 1
  const isPayDisabled =
    loadingApp ||
    !!loadError ||
    !application ||
    !isApproved ||
    isSubmitting ||
    isPaid ||
    !cleanId;

  const handlePay = async () => {
    if (isPayDisabled || !cleanId) return;

    setIsSubmitting(true);
    setErrorMessage(null);
    setErrorCode(null);

    try {
      // Security Invariant: Send ONLY applicationId. Never send amount or client idempotency key.
      const response = await apiClient.post('/payments', {
        applicationId: cleanId,
      });

      setResult(response.data);
      setPaymentState('success');
      if (application) {
        setApplication((prev) => (prev ? { ...prev, status: 'PAID' } : null));
      }
    } catch (err) {
      const status = err.status || err.response?.status;
      const code = err.errorCode || err.response?.data?.errorCode;
      const msg = err.message || err.response?.data?.message;

      setErrorCode(code);

      if (status === 402 || code === 'PAY-402') {
        // HTTP 402: Payment failed — retry allowed
        setPaymentState('failed');
        setErrorMessage(msg || 'Payment failed — retry');
      } else if (status === 409 || code === 'STATE-409') {
        // HTTP 409: Conflict state
        setPaymentState('conflict');
        setErrorMessage(msg || 'Operation not allowed in current application state.');
      } else {
        // General error or network failure
        setPaymentState('error');
        setErrorMessage(msg || 'Network error or service unavailable. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const displayPremium = application?.premium ? Number(application.premium) : null;

  const getButtonText = () => {
    if (isSubmitting) {
      return (
        <>
          <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
          Processing Payment...
        </>
      );
    }
    if (isPaid) {
      return 'Premium Paid';
    }
    if (loadingApp) {
      return 'Loading Application...';
    }
    if (loadError || !application) {
      return 'Payment Unavailable';
    }
    if (!isApproved) {
      return `Application ${application.status}`;
    }
    if (paymentState === 'failed') {
      return `Retry Payment (₹${displayPremium?.toLocaleString('en-IN')})`;
    }
    return `Pay Premium (₹${displayPremium?.toLocaleString('en-IN')})`;
  };

  return (
    <PortalLayout
      title="Premium Payment"
      subtitle="Complete payment to issue your insurance policy (INS-F-009 / SAD Section 4.3.4)"
      badgeText="Secure Checkout"
      badgeClass="bg-primary text-white"
    >
      {import.meta.env.DEV && (
        <div className="dev-hook-box mb-4">
          <span className="dev-hook-badge">Person 4 Payment Adapter (INS-F-009)</span>
          <p className="mb-0">
            Connects to <code>POST /api/v1/payments</code> with payload <code>&#123; "applicationId": {cleanId || '...'} &#125;</code>.
            Amount and idempotency keys are derived strictly server-side.
          </p>
        </div>
      )}

      <div className="row g-4">
        {/* Left Column: Application Details & States */}
        <div className="col-lg-7">
          {/* Application Load Error Banner */}
          {loadError && (
            <div id="application-load-error-banner" className="alert alert-danger d-flex align-items-start mb-4" role="alert">
              <div className="me-3 fs-3">✕</div>
              <div className="flex-grow-1">
                <h5 className="alert-heading fw-bold mb-1">Unable to Load Application</h5>
                <p className="mb-0">{loadError}</p>
              </div>
            </div>
          )}

          {/* Success Banner */}
          {paymentState === 'success' && result && (
            <div id="payment-success-banner" className="alert alert-success d-flex align-items-start mb-4" role="alert">
              <div className="me-3 fs-3">✓</div>
              <div className="flex-grow-1">
                <h5 className="alert-heading fw-bold mb-1">Payment Successful!</h5>
                <p className="mb-2">Your insurance application has been paid and your policy is now active.</p>
                <hr className="my-2" />
                <div className="small">
                  <div><strong>Policy Number:</strong> <span id="policy-number-display" className="font-monospace fw-bold">{result.policyNo}</span></div>
                  {result.transactionReference && (
                    <div><strong>Transaction Ref:</strong> <span className="font-monospace">{result.transactionReference}</span></div>
                  )}
                  {result.amount && (
                    <div><strong>Amount Paid:</strong> ₹{Number(result.amount).toLocaleString('en-IN')}</div>
                  )}
                </div>
                <div className="mt-3">
                  <Link to={ROUTES.CUSTOMER_POLICIES} className="btn btn-sm btn-success me-2">
                    View My Policies
                  </Link>
                  <Link to={ROUTES.CUSTOMER_APPLICATIONS} className="btn btn-sm btn-outline-secondary">
                    Back to Applications
                  </Link>
                </div>
              </div>
            </div>
          )}

          {/* 402 Payment Failed Banner */}
          {paymentState === 'failed' && (
            <div id="payment-failed-banner" className="alert alert-danger d-flex align-items-start mb-4" role="alert">
              <div className="me-3 fs-3">⚠️</div>
              <div className="flex-grow-1">
                <h5 className="alert-heading fw-bold mb-1">Payment failed — retry</h5>
                <p className="mb-1">
                  {errorMessage && errorMessage !== 'Payment failed — retry'
                    ? errorMessage
                    : 'Your transaction was declined or timed out. You may retry payment.'}
                </p>
                <p className="mb-0 small text-muted">
                  Error Code: <code>{errorCode || 'PAY-402'}</code>. No charges have been deducted.
                </p>
              </div>
            </div>
          )}

          {/* 409 Conflict Banner */}
          {paymentState === 'conflict' && (
            <div id="payment-conflict-banner" className="alert alert-warning d-flex align-items-start mb-4" role="alert">
              <div className="me-3 fs-3">ℹ️</div>
              <div className="flex-grow-1">
                <h5 className="alert-heading fw-bold mb-1">Application Conflict</h5>
                <p className="mb-1">{errorMessage || 'This application cannot be paid in its current status.'}</p>
                <p className="mb-0 small text-muted">
                  Error Code: <code>{errorCode || 'STATE-409'}</code>
                </p>
              </div>
            </div>
          )}

          {/* Network / Unexpected Error Banner */}
          {paymentState === 'error' && (
            <div id="payment-error-banner" className="alert alert-danger d-flex align-items-start mb-4" role="alert">
              <div className="me-3 fs-3">✕</div>
              <div className="flex-grow-1">
                <h5 className="alert-heading fw-bold mb-1">Payment Processing Error</h5>
                <p className="mb-1">{errorMessage || 'A system or network error occurred while contacting the payment gateway.'}</p>
                <p className="mb-0 small text-muted">
                  Error Code: <code>{errorCode || 'SYS-500'}</code>. Please try again shortly.
                </p>
              </div>
            </div>
          )}

          {/* Application Summary Card */}
          <div className="dashboard-card mb-4">
            <div className="dashboard-card-header d-flex justify-content-between align-items-center">
              <span className="fw-bold">Application Summary</span>
              {application && (
                <span className="badge bg-secondary">
                  {application.displayId || `APP-2026-${cleanId || 'N/A'}`}
                </span>
              )}
            </div>
            <div className="dashboard-card-body">
              {loadingApp ? (
                <div className="text-center py-4">
                  <div className="spinner-border text-primary" role="status"></div>
                  <div className="mt-2 text-muted">Loading application details...</div>
                </div>
              ) : loadError || !application ? (
                <div className="text-center py-3 text-muted">
                  <p className="mb-1 fw-semibold text-danger">No authoritative application data available.</p>
                  <p className="small mb-0">Please verify the application ID or check back once underwriter review is complete.</p>
                </div>
              ) : (
                <div className="table-responsive">
                  <table className="table table-borderless align-middle mb-0">
                    <tbody>
                      <tr>
                        <th className="text-muted w-40">Plan Selected:</th>
                        <td className="fw-semibold">{application.planName || 'Insurance Plan'}</td>
                      </tr>
                      {application.productType && (
                        <tr>
                          <th className="text-muted">Product Type:</th>
                          <td>{application.productType}</td>
                        </tr>
                      )}
                      {application.vehicleRegNo && (
                        <tr>
                          <th className="text-muted">Vehicle Reg No:</th>
                          <td><code>{application.vehicleRegNo}</code></td>
                        </tr>
                      )}
                      <tr>
                        <th className="text-muted">Status:</th>
                        <td>
                          {isPaid ? (
                            <span className="badge bg-success">PAID</span>
                          ) : isApproved ? (
                            <span className="badge bg-primary">APPROVED</span>
                          ) : (
                            <span className="badge bg-warning text-dark">{application.status}</span>
                          )}
                        </td>
                      </tr>
                      <tr>
                        <th className="text-muted">Payable Premium:</th>
                        <td className="fs-5 fw-bold text-primary">
                          ₹{displayPremium?.toLocaleString('en-IN')}
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right Column: Checkout & Action */}
        <div className="col-lg-5">
          <div className="dashboard-card">
            <div className="dashboard-card-header">
              <span className="fw-bold">Payment Checkout</span>
            </div>
            <div className="dashboard-card-body">
              <div className="mb-3 text-muted small">
                Payment is processed through the secure payment gateway adapter. Server generates unique idempotency keys for protection against duplicate charges.
              </div>

              <div className="p-3 bg-light rounded mb-4">
                <div className="d-flex justify-content-between mb-2">
                  <span>Base Premium:</span>
                  <span>{displayPremium ? `₹${Math.round(displayPremium / 1.18).toLocaleString('en-IN')}` : '—'}</span>
                </div>
                <div className="d-flex justify-content-between mb-2">
                  <span>GST (18%):</span>
                  <span>{displayPremium ? `₹${(displayPremium - Math.round(displayPremium / 1.18)).toLocaleString('en-IN')}` : '—'}</span>
                </div>
                <hr className="my-2" />
                <div className="d-flex justify-content-between fw-bold fs-5">
                  <span>Total Due:</span>
                  <span className="text-primary" id="total-premium-amount">
                    {displayPremium ? `₹${displayPremium.toLocaleString('en-IN')}` : '—'}
                  </span>
                </div>
              </div>

              <button
                id="submit-payment-btn"
                className={`btn btn-lg w-100 ${paymentState === 'failed' ? 'btn-warning' : 'btn-primary'}`}
                onClick={handlePay}
                disabled={isPayDisabled}
              >
                {getButtonText()}
              </button>

              <div className="mt-3 text-center">
                <Link to={ROUTES.CUSTOMER_APPLICATIONS} className="text-decoration-none small text-muted">
                  &larr; Cancel and return to applications
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </PortalLayout>
  );
}
