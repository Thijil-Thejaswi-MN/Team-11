import React, { useState, useEffect } from 'react';
import PortalLayout from '../../components/layout/PortalLayout';
import apiClient from '../../api/apiClient';

const STAFF_ROLES = ['INSURER_ADMIN', 'UNDERWRITER', 'CLAIMS_OFFICER'];

export default function SystemAdminDashboardView() {
  // Audit log feed state
  const [auditLogs, setAuditLogs] = useState([]);
  const [auditLoading, setAuditLoading] = useState(true);
  const [auditError, setAuditError] = useState(null);

  const fetchAuditLogs = async () => {
    setAuditLoading(true);
    setAuditError(null);
    try {
      const res = await apiClient.get('/audit', { params: { page: 0, size: 5 } });
      const items = res.data?.items || (Array.isArray(res.data) ? res.data : []);
      setAuditLogs(items);
    } catch (err) {
      setAuditLogs([]);
      setAuditError(err.response?.data?.message || err.message || 'Failed to load audit logs.');
    } finally {
      setAuditLoading(false);
    }
  };

  useEffect(() => {
    fetchAuditLogs();
  }, []);

  // Insurer form state
  const [insurerName, setInsurerName] = useState('');
  const [insurerLoading, setInsurerLoading] = useState(false);
  const [insurerSuccess, setInsurerSuccess] = useState('');
  const [insurerError, setInsurerError] = useState('');

  // User form state
  const [userName, setUserName] = useState('');
  const [userEmail, setUserEmail] = useState('');
  const [userPhone, setUserPhone] = useState('');
  const [userPassword, setUserPassword] = useState('');
  const [userRole, setUserRole] = useState('INSURER_ADMIN');
  const [userInsurerId, setUserInsurerId] = useState('');
  const [userLoading, setUserLoading] = useState(false);
  const [userSuccess, setUserSuccess] = useState('');
  const [userError, setUserError] = useState('');

  const isStaffRole = STAFF_ROLES.includes(userRole);

  const handleCreateInsurer = async (e) => {
    e.preventDefault();
    setInsurerSuccess('');
    setInsurerError('');

    if (!insurerName.trim()) {
      setInsurerError('Insurer organization name is required.');
      return;
    }

    try {
      setInsurerLoading(true);
      const res = await apiClient.post('/admin/insurers', { name: insurerName.trim() });
      setInsurerSuccess(`Insurer organization created successfully (ID: ${res.data?.id || 'Created'}).`);
      setInsurerName('');
    } catch (err) {
      if (err.status === 409 || err.response?.status === 409) {
        setInsurerError(`Insurer with name "${insurerName.trim()}" already exists.`);
      } else if (err.status === 403 || err.response?.status === 403) {
        setInsurerError('Access denied: SYSTEM_ADMIN privileges required.');
      } else {
        setInsurerError(err.message || 'Failed to create insurer organization.');
      }
    } finally {
      setInsurerLoading(false);
    }
  };

  const handleCreateUser = async (e) => {
    e.preventDefault();
    setUserSuccess('');
    setUserError('');

    if (!userName.trim() || !userEmail.trim() || !userPhone.trim() || !userPassword) {
      setUserError('All user fields (name, email, phone, password) are required.');
      return;
    }

    if (isStaffRole && (!userInsurerId || Number(userInsurerId) <= 0)) {
      setUserError(`Staff role "${userRole}" must be linked to a valid Insurer ID.`);
      return;
    }

    const payload = {
      name: userName.trim(),
      email: userEmail.trim().toLowerCase(),
      phone: userPhone.trim(),
      password: userPassword,
      role: userRole,
      insurerId: isStaffRole ? Number(userInsurerId) : null,
    };

    try {
      setUserLoading(true);
      const res = await apiClient.post('/admin/users', payload);
      setUserSuccess(`Staff user account created successfully (ID: ${res.data?.id || 'Created'}).`);
      setUserName('');
      setUserEmail('');
      setUserPhone('');
      setUserPassword('');
      setUserInsurerId('');
    } catch (err) {
      if (err.status === 409 || err.response?.status === 409) {
        setUserError(`User with email "${userEmail.trim().toLowerCase()}" already exists.`);
      } else if (err.status === 422 || err.response?.status === 422) {
        setUserError(err.message || 'Validation error: Staff account must be linked to a valid insurer.');
      } else if (err.status === 403 || err.response?.status === 403) {
        setUserError('Access denied: SYSTEM_ADMIN privileges required.');
      } else {
        setUserError(err.message || 'Failed to create user account.');
      }
    } finally {
      setUserLoading(false);
    }
  };

  return (
    <PortalLayout
      title="System Administration & Audit Portal"
      subtitle="Multi-insurer governance, business reporting, and immutable audit logs."
      badgeText="System Admin"
      badgeClass="bg-danger"
    >
      {/* Admin Action Forms */}
      <div className="row g-4 mb-4">
        {/* Onboard Insurer Form */}
        <div className="col-12 col-lg-5">
          <div className="dashboard-card h-100 p-3 p-md-4">
            <div className="dashboard-card-header mb-3 pb-2 border-bottom">
              <span className="fw-bold fs-5">Onboard Insurer Organization</span>
            </div>

            {insurerSuccess && (
              <div className="alert alert-success small py-2" id="insurer-success-alert" role="alert">
                {insurerSuccess}
              </div>
            )}
            {insurerError && (
              <div className="alert alert-danger small py-2" id="insurer-error-alert" role="alert">
                {insurerError}
              </div>
            )}

            <form onSubmit={handleCreateInsurer} noValidate id="create-insurer-form">
              <div className="mb-3">
                <label htmlFor="insurer-name-input" className="form-label small fw-semibold">
                  Insurer Name <span className="text-danger">*</span>
                </label>
                <input
                  id="insurer-name-input"
                  type="text"
                  className="form-control"
                  placeholder="e.g. Apex Mutual Life"
                  value={insurerName}
                  onChange={(e) => setInsurerName(e.target.value)}
                  disabled={insurerLoading}
                  required
                />
                <div className="form-text small text-muted">
                  Organization name must be unique across the platform.
                </div>
              </div>

              <button
                type="submit"
                id="create-insurer-btn"
                className="btn btn-primary w-100"
                disabled={insurerLoading}
              >
                {insurerLoading ? 'Onboarding...' : 'Onboard Insurer'}
              </button>
            </form>
          </div>
        </div>

        {/* Provision User / Staff Account Form */}
        <div className="col-12 col-lg-7">
          <div className="dashboard-card h-100 p-3 p-md-4">
            <div className="dashboard-card-header mb-3 pb-2 border-bottom">
              <span className="fw-bold fs-5">Provision Staff / User Account</span>
            </div>

            {userSuccess && (
              <div className="alert alert-success small py-2" id="user-success-alert" role="alert">
                {userSuccess}
              </div>
            )}
            {userError && (
              <div className="alert alert-danger small py-2" id="user-error-alert" role="alert">
                {userError}
              </div>
            )}

            <form onSubmit={handleCreateUser} noValidate id="create-user-form">
              <div className="row g-2 mb-2">
                <div className="col-12 col-sm-6">
                  <label htmlFor="staff-name-input" className="form-label small fw-semibold">
                    Full Name <span className="text-danger">*</span>
                  </label>
                  <input
                    id="staff-name-input"
                    type="text"
                    className="form-control"
                    placeholder="e.g. Priya Sharma"
                    value={userName}
                    onChange={(e) => setUserName(e.target.value)}
                    disabled={userLoading}
                    required
                  />
                </div>
                <div className="col-12 col-sm-6">
                  <label htmlFor="staff-email-input" className="form-label small fw-semibold">
                    Email Address <span className="text-danger">*</span>
                  </label>
                  <input
                    id="staff-email-input"
                    type="email"
                    className="form-control"
                    placeholder="e.g. priya@safeshield.test"
                    value={userEmail}
                    onChange={(e) => setUserEmail(e.target.value)}
                    disabled={userLoading}
                    required
                  />
                </div>
              </div>

              <div className="row g-2 mb-2">
                <div className="col-12 col-sm-6">
                  <label htmlFor="staff-phone-input" className="form-label small fw-semibold">
                    Phone Number <span className="text-danger">*</span>
                  </label>
                  <input
                    id="staff-phone-input"
                    type="text"
                    className="form-control"
                    placeholder="e.g. 9876543210"
                    value={userPhone}
                    onChange={(e) => setUserPhone(e.target.value)}
                    disabled={userLoading}
                    required
                  />
                </div>
                <div className="col-12 col-sm-6">
                  <label htmlFor="staff-password-input" className="form-label small fw-semibold">
                    Password <span className="text-danger">*</span>
                  </label>
                  <input
                    id="staff-password-input"
                    type="password"
                    className="form-control"
                    placeholder="Min 8 characters"
                    value={userPassword}
                    onChange={(e) => setUserPassword(e.target.value)}
                    disabled={userLoading}
                    required
                  />
                </div>
              </div>

              <div className="row g-2 mb-3">
                <div className="col-12 col-sm-6">
                  <label htmlFor="staff-role-select" className="form-label small fw-semibold">
                    Assigned Role <span className="text-danger">*</span>
                  </label>
                  <select
                    id="staff-role-select"
                    className="form-select"
                    value={userRole}
                    onChange={(e) => setUserRole(e.target.value)}
                    disabled={userLoading}
                  >
                    <option value="INSURER_ADMIN">Insurer Admin</option>
                    <option value="UNDERWRITER">Underwriter</option>
                    <option value="CLAIMS_OFFICER">Claims Officer</option>
                    <option value="SYSTEM_ADMIN">System Admin</option>
                    <option value="CUSTOMER">Customer</option>
                  </select>
                </div>
                <div className="col-12 col-sm-6">
                  <label htmlFor="staff-insurer-id-input" className="form-label small fw-semibold">
                    Insurer ID {isStaffRole ? <span className="text-danger">*</span> : <span className="text-muted">(Optional)</span>}
                  </label>
                  <input
                    id="staff-insurer-id-input"
                    type="number"
                    min="1"
                    className="form-control"
                    placeholder={isStaffRole ? "Required for staff (e.g. 1)" : "N/A for non-staff"}
                    value={userInsurerId}
                    onChange={(e) => setUserInsurerId(e.target.value)}
                    disabled={userLoading || !isStaffRole}
                    required={isStaffRole}
                  />
                  {isStaffRole && (
                    <div className="form-text small text-muted">
                      Staff must belong to exactly one insurer.
                    </div>
                  )}
                </div>
              </div>

              <button
                type="submit"
                id="create-user-btn"
                className="btn btn-success w-100"
                disabled={userLoading}
              >
                {userLoading ? 'Provisioning...' : 'Provision User Account'}
              </button>
            </form>
          </div>
        </div>
      </div>

      {/* Governance & Audit Overview */}
      <div className="row g-3 mb-4">
        <div className="col-12 col-md-6">
          <div className="dashboard-card h-100">
            <div className="dashboard-card-header">
              <span className="fw-bold">Platform Insurers (Multi-Tenancy)</span>
              <span className="badge bg-secondary">Governance</span>
            </div>
            <div className="dashboard-card-body p-0">
              <ul className="list-group list-group-flush small">
                <li className="list-group-item d-flex justify-content-between align-items-center">
                  <div>
                    <span className="fw-semibold">SafeShield General (INS-1)</span>
                    <span className="d-block text-muted">2W & 4W Motor, Comprehensive Health</span>
                  </div>
                  <span className="status-badge status-active">Active</span>
                </li>
                <li className="list-group-item d-flex justify-content-between align-items-center">
                  <div>
                    <span className="fw-semibold">Bharat Care Insurance (INS-2)</span>
                    <span className="d-block text-muted">Family Health & Senior Citizen</span>
                  </div>
                  <span className="status-badge status-active">Active</span>
                </li>
              </ul>
            </div>
          </div>
        </div>

        <div className="col-12 col-md-6">
          <div className="dashboard-card h-100">
            <div className="dashboard-card-header d-flex justify-content-between align-items-center">
              <span className="fw-bold">Audit Log Feed Preview</span>
              <span className="badge bg-dark">Append-Only</span>
            </div>
            <div className="dashboard-card-body p-0">
              {auditLoading && (
                <div className="text-center py-4 text-muted small">
                  <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                  Loading audit logs...
                </div>
              )}
              {auditError && (
                <div className="p-3">
                  <div className="alert alert-danger mb-0 small d-flex justify-content-between align-items-center">
                    <span>{auditError}</span>
                    <button
                      type="button"
                      className="btn btn-outline-danger btn-sm py-0 ms-2"
                      onClick={fetchAuditLogs}
                    >
                      Retry
                    </button>
                  </div>
                </div>
              )}
              {!auditLoading && !auditError && auditLogs.length === 0 && (
                <div className="text-muted text-center py-4 small">
                  No audit logs available.
                </div>
              )}
              {!auditLoading && !auditError && auditLogs.length > 0 && (
                <ul className="list-group list-group-flush small">
                  {auditLogs.map((log) => (
                    <li key={log.id} className="list-group-item">
                      <div className="d-flex justify-content-between text-muted small">
                        <span>user: {log.userId ? `ID ${log.userId}` : 'system'}</span>
                        <span>{log.timestamp ? new Date(log.timestamp).toLocaleString() : '—'}</span>
                      </div>
                      <div className="fw-medium">
                        action: {log.action} [{log.entity}: {log.entityId || 'N/A'}]
                      </div>
                      {log.details && <div className="text-muted extra-small">{log.details}</div>}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </div>
      </div>
    </PortalLayout>
  );
}
