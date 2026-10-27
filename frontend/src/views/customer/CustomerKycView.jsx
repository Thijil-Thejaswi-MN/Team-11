import React, { useState, useEffect } from 'react';
import PortalLayout from '../../components/layout/PortalLayout';
import apiClient from '../../api/apiClient';

/**
 * Customer KYC Verification View (INS-F-002, INS-SR-007, SAD §4.3.1, §4.5).
 * Allows customers to inspect their current KYC verification status and submit
 * identity documents (PAN, Aadhaar, and identity document upload up to 5 MB).
 */
export default function CustomerKycView() {
  const [kycStatus, setKycStatus] = useState('Loading...');
  const [loadingStatus, setLoadingStatus] = useState(true);

  const [panNumber, setPanNumber] = useState('');
  const [aadhaarNumber, setAadhaarNumber] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Fetch initial KYC status on mount
  useEffect(() => {
    fetchKycStatus();
  }, []);

  const fetchKycStatus = async () => {
    try {
      setLoadingStatus(true);
      const res = await apiClient.get('/kyc/status');
      setKycStatus(res.data.status || 'Not Submitted');
    } catch (err) {
      if (err.status === 404) {
        setKycStatus('Not Submitted');
      } else {
        setKycStatus('Not Submitted');
      }
    } finally {
      setLoadingStatus(false);
    }
  };

  const handleFileChange = (e) => {
    const file = e.target.files?.[0] || null;
    setSelectedFile(file);
    setErrorMessage(null);

    if (file && file.size > 5 * 1024 * 1024) {
      setErrorMessage('File exceeds the 5 MB limit. Please upload a smaller file.');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    const cleanPan = panNumber.trim().toUpperCase();
    const cleanAadhaar = aadhaarNumber.trim();

    // Client-side format validations
    const panRegex = /^[A-Z]{5}[0-9]{4}[A-Z]$/;
    if (!panRegex.test(cleanPan)) {
      setErrorMessage('Invalid PAN format. Must be 5 letters, 4 digits, and 1 letter (e.g., ABCDE1234F).');
      return;
    }

    const aadhaarRegex = /^[0-9]{12}$/;
    if (!aadhaarRegex.test(cleanAadhaar)) {
      setErrorMessage('Invalid Aadhaar format. Aadhaar number must be exactly 12 digits.');
      return;
    }

    if (!selectedFile) {
      setErrorMessage('Please select a KYC document file to upload (PDF, JPG, or PNG).');
      return;
    }

    if (selectedFile.size > 5 * 1024 * 1024) {
      setErrorMessage('File exceeds the 5 MB limit. Please upload a smaller file.');
      return;
    }

    // Supported extensions / MIME types check
    const allowedExtensions = ['pdf', 'jpg', 'jpeg', 'png'];
    const fileExt = selectedFile.name.split('.').pop()?.toLowerCase();
    if (!allowedExtensions.includes(fileExt)) {
      setErrorMessage('Only PDF, JPG, and PNG files are accepted. Please select a valid document format.');
      return;
    }

    try {
      setIsSubmitting(true);
      const formData = new FormData();
      formData.append('panNumber', cleanPan);
      formData.append('aadhaarNumber', cleanAadhaar);
      formData.append('files[]', selectedFile);
      formData.append('files', selectedFile);
      formData.append('file', selectedFile);

      const res = await apiClient.post('/kyc', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      const updatedStatus = res.data?.status || 'Pending';
      setKycStatus(updatedStatus);

      if (updatedStatus === 'Verified') {
        setSuccessMessage('KYC verified successfully! Your identity documents have been approved.');
      } else if (updatedStatus === 'Rejected') {
        setErrorMessage(
          'KYC submission was rejected by the verification provider. Please check your details and try again.'
        );
      } else {
        setSuccessMessage('KYC documents submitted successfully. Status is currently pending.');
      }

      // Reset file input safely
      setSelectedFile(null);
      try {
        const fileInput = document.getElementById('kyc-file-input');
        if (fileInput) fileInput.value = '';
      } catch (e) {
        // Ignore file input reset quirks in test environment
      }
    } catch (err) {
      if (err.status === 413 || err.errorCode === 'FILE_413') {
        setErrorMessage('File exceeds the 5 MB limit. Please upload a smaller file.');
      } else if (err.status === 415 || err.errorCode === 'FILE_415') {
        setErrorMessage('Only PDF, JPG, and PNG files are accepted. Please select a valid document format.');
      } else if (err.status === 422 || err.errorCode === 'VALID_422') {
        setErrorMessage('Invalid PAN or Aadhaar format. Please check your entered details and try again.');
      } else if (err.status === 403) {
        setErrorMessage('Access forbidden: only customer accounts are authorized to submit KYC.');
      } else {
        setErrorMessage(err.message || 'Failed to submit KYC documents. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const getStatusBadge = () => {
    switch (kycStatus) {
      case 'Verified':
        return (
          <span className="badge bg-success fs-6" id="kyc-status-badge">
            <i className="bi bi-patch-check-fill me-1"></i>Verified
          </span>
        );
      case 'Pending':
        return (
          <span className="badge bg-warning text-dark fs-6" id="kyc-status-badge">
            <i className="bi bi-hourglass-split me-1"></i>Pending Verification
          </span>
        );
      case 'Rejected':
        return (
          <span className="badge bg-danger fs-6" id="kyc-status-badge">
            <i className="bi bi-x-circle-fill me-1"></i>Rejected
          </span>
        );
      case 'Not Submitted':
      default:
        return (
          <span className="badge bg-secondary fs-6" id="kyc-status-badge">
            <i className="bi bi-dash-circle me-1"></i>Not Submitted
          </span>
        );
    }
  };

  return (
    <PortalLayout
      title="KYC Verification"
      subtitle="Submit regulatory identity documentation (INS-F-002, INS-SR-007, SAD §4.3.1)"
      badgeText="Customer Portal"
      badgeClass="bg-primary"
    >
      <div className="row g-4">
        {/* Status Overview Card */}
        <div className="col-12">
          <div className="dashboard-card" id="kyc-status-card">
            <div className="dashboard-card-body d-flex flex-wrap align-items-center justify-content-between gap-3">
              <div>
                <span className="text-muted small fw-semibold text-uppercase d-block mb-1">
                  Current Verification Status
                </span>
                <div className="d-flex align-items-center gap-2">
                  {loadingStatus ? (
                    <span className="spinner-border spinner-border-sm text-primary" role="status"></span>
                  ) : (
                    getStatusBadge()
                  )}
                </div>
              </div>
              <div className="text-muted small text-sm-end">
                <div><i className="bi bi-shield-check text-success me-1"></i>Encrypted File Storage (AES-256)</div>
                <div><i className="bi bi-file-earmark-check text-info me-1"></i>PDF, JPG, PNG (Max 5 MB)</div>
              </div>
            </div>
          </div>
        </div>

        {/* Verification Form Card */}
        <div className="col-12 col-lg-8">
          <div className="dashboard-card">
            <div className="dashboard-card-header">
              <span className="fw-bold">
                {kycStatus === 'Rejected' ? 'Resubmit KYC Documents' : 'Submit Identity Documents'}
              </span>
              <span className="badge bg-secondary">Self-Service</span>
            </div>
            <div className="dashboard-card-body">
              {/* Informative alerts */}
              {kycStatus === 'Verified' && (
                <div className="alert alert-success py-2 small mb-3" role="alert" id="kyc-verified-banner">
                  <i className="bi bi-check-circle-fill me-2"></i>
                  Your KYC is verified. You are eligible to purchase insurance policies and file claims.
                </div>
              )}

              {kycStatus === 'Rejected' && (
                <div className="alert alert-warning py-2 small mb-3" role="alert" id="kyc-rejected-banner">
                  <i className="bi bi-exclamation-triangle-fill me-2"></i>
                  Your previous KYC submission was rejected. You may resubmit with corrected PAN or documents.
                </div>
              )}

              {successMessage && (
                <div className="alert alert-success py-2 small mb-3" role="alert" id="kyc-success-alert">
                  <i className="bi bi-check-circle-fill me-2"></i>
                  {successMessage}
                </div>
              )}

              {errorMessage && (
                <div className="alert alert-danger py-2 small mb-3" role="alert" id="kyc-error-alert">
                  <i className="bi bi-exclamation-octagon-fill me-2"></i>
                  {errorMessage}
                </div>
              )}

              <form onSubmit={handleSubmit} noValidate>
                {/* PAN Number Input */}
                <div className="mb-3">
                  <label htmlFor="kyc-pan-input" className="form-label small fw-semibold">
                    Permanent Account Number (PAN) <span className="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    id="kyc-pan-input"
                    name="panNumber"
                    className="form-control form-control-sm text-uppercase"
                    placeholder="ABCDE1234F"
                    maxLength={10}
                    value={panNumber}
                    onChange={(e) => setPanNumber(e.target.value.toUpperCase())}
                    required
                    disabled={isSubmitting}
                  />
                  <div className="form-text small">
                    Must be 10 characters: 5 uppercase letters, 4 digits, 1 uppercase letter.
                  </div>
                </div>

                {/* Aadhaar Number Input */}
                <div className="mb-3">
                  <label htmlFor="kyc-aadhaar-input" className="form-label small fw-semibold">
                    Aadhaar Number <span className="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    id="kyc-aadhaar-input"
                    name="aadhaarNumber"
                    className="form-control form-control-sm"
                    placeholder="123456789012"
                    maxLength={12}
                    value={aadhaarNumber}
                    onChange={(e) => setAadhaarNumber(e.target.value.replace(/\D/g, ''))}
                    required
                    disabled={isSubmitting}
                  />
                  <div className="form-text small">12-digit unique Aadhaar identification number.</div>
                </div>

                {/* File Upload Input */}
                <div className="mb-4">
                  <label htmlFor="kyc-file-input" className="form-label small fw-semibold">
                    Identity / Address Document <span className="text-danger">*</span>
                  </label>
                  <input
                    type="file"
                    id="kyc-file-input"
                    name="file"
                    className="form-control form-control-sm"
                    accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
                    onChange={handleFileChange}
                    required
                    disabled={isSubmitting}
                  />
                  <div className="form-text small">
                    Accepted formats: PDF, JPG, PNG. Maximum allowed file size: 5 MB.
                  </div>
                </div>

                {/* Submit Button */}
                <button
                  type="submit"
                  className="btn btn-primary btn-sm px-4"
                  id="kyc-submit-btn"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
                      Uploading & Verifying...
                    </>
                  ) : (
                    <>
                      <i className="bi bi-cloud-arrow-up me-1"></i>
                      {kycStatus === 'Rejected' ? 'Resubmit KYC Documents' : 'Submit for Verification'}
                    </>
                  )}
                </button>
              </form>
            </div>
          </div>
        </div>

        {/* Regulatory & Security Guidelines Sidebar */}
        <div className="col-12 col-lg-4">
          <div className="dashboard-card bg-light">
            <div className="dashboard-card-header">
              <span className="fw-bold">Compliance Guidelines</span>
              <span className="badge bg-info text-dark">IRDAI</span>
            </div>
            <div className="dashboard-card-body small text-muted">
              <p className="mb-2">
                Under IRDAI Master Guidelines, full KYC verification is mandatory before issuing insurance policies:
              </p>
              <ul className="ps-3 mb-3">
                <li>Government-issued PAN and Aadhaar records are required.</li>
                <li>Uploaded files must clearly show name, photo, and identification numbers.</li>
                <li>Documents are encrypted at rest using AES-256 (Component C12).</li>
                <li>Simulation rule: PAN <code>ZZZZZ0000Z</code> triggers rejection for test workflows.</li>
              </ul>
              <div className="p-2 border rounded bg-white small">
                <i className="bi bi-lock-fill text-success me-1"></i>
                Personally Identifiable Information (PII) is masked in system logs.
              </div>
            </div>
          </div>
        </div>
      </div>
    </PortalLayout>
  );
}
