import React, { useState, useEffect, useCallback } from 'react';
import PortalLayout from '../../components/layout/PortalLayout';
import apiClient from '../../api/apiClient';

/**
 * System Administration Audit Trail View (P2-18 / INS-F-018 / INS-SR-008 / TC-Aud-01).
 * Displays immutable audit logs with multi-field filtering and pagination.
 * Responsive design verified from 360px mobile viewports up to desktop.
 */
export default function AuditLogView() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Pagination state
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filter form state
  const [userIdFilter, setUserIdFilter] = useState('');
  const [actionFilter, setActionFilter] = useState('');
  const [entityFilter, setEntityFilter] = useState('');
  const [entityIdFilter, setEntityIdFilter] = useState('');
  const [fromDateFilter, setFromDateFilter] = useState('');
  const [toDateFilter, setToDateFilter] = useState('');

  // Applied filter state (triggers API queries)
  const [appliedFilters, setAppliedFilters] = useState({});

  const fetchAuditLogs = useCallback(async (targetPage, targetSize, filters) => {
    setLoading(true);
    setError('');

    const params = {
      page: targetPage,
      size: targetSize,
    };

    if (filters.userId && Number(filters.userId) > 0) {
      params.userId = Number(filters.userId);
    }
    if (filters.action && filters.action.trim()) {
      params.action = filters.action.trim();
    }
    if (filters.entity && filters.entity.trim()) {
      params.entity = filters.entity.trim();
    }
    if (filters.entityId && Number(filters.entityId) > 0) {
      params.entityId = Number(filters.entityId);
    }
    if (filters.from && filters.from.trim()) {
      params.from = filters.from.trim();
    }
    if (filters.to && filters.to.trim()) {
      params.to = filters.to.trim();
    }

    try {
      const response = await apiClient.get('/audit', { params });
      const data = response.data || {};
      setLogs(data.items || []);
      setPage(data.page || 0);
      setSize(data.size || targetSize);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      if (err.status === 403 || err.response?.status === 403) {
        setError('Access denied: SYSTEM_ADMIN privileges required to inspect audit trail (INS-SR-003).');
      } else if (err.status === 401 || err.response?.status === 401) {
        setError('Authentication required: please log in with a System Admin account.');
      } else {
        setError(err.message || 'Failed to retrieve audit trail records.');
      }
      setLogs([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAuditLogs(page, size, appliedFilters);
  }, [page, size, appliedFilters, fetchAuditLogs]);

  const handleApplyFilters = (e) => {
    e.preventDefault();
    const newFilters = {
      userId: userIdFilter,
      action: actionFilter,
      entity: entityFilter,
      entityId: entityIdFilter,
      from: fromDateFilter,
      to: toDateFilter,
    };
    setPage(0);
    setAppliedFilters(newFilters);
  };

  const handleResetFilters = () => {
    setUserIdFilter('');
    setActionFilter('');
    setEntityFilter('');
    setEntityIdFilter('');
    setFromDateFilter('');
    setToDateFilter('');
    setPage(0);
    setAppliedFilters({});
  };

  const formatTimestamp = (isoString) => {
    if (!isoString) return '—';
    try {
      const d = new Date(isoString);
      return d.toLocaleString('en-IN', {
        year: 'numeric',
        month: 'short',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false,
      });
    } catch {
      return isoString;
    }
  };

  return (
    <PortalLayout
      title="System Audit Log"
      subtitle="Immutable, append-only security and workflow audit trail (INS-F-018, INS-SR-008, SAD §4.3.6)"
      badgeText="System Admin"
      badgeClass="bg-danger"
    >
      {/* Dev Hook Information Banner */}
      {import.meta.env.DEV && (
        <div className="dev-hook-box mb-4">
          <span className="dev-hook-badge">P2-18 Audit Hardening</span>
          <p className="mb-0">
            Connects to <code>GET /api/v1/audit</code>. Enforces <code>SYSTEM_ADMIN</code> RBAC (<code>ACCESS-403</code>),
            server-side PII scrubbing, and append-only database semantics (<code>TC-Aud-01</code>, <code>TC-Sec-08</code>).
          </p>
        </div>
      )}

      {/* Filter Card */}
      <div className="dashboard-card mb-4 p-3 p-md-4">
        <div className="dashboard-card-header mb-3 pb-2 border-bottom d-flex justify-content-between align-items-center">
          <span className="fw-bold fs-5">Audit Log Search & Filters</span>
          <span className="badge bg-secondary">Total Records: {totalElements}</span>
        </div>

        <form onSubmit={handleApplyFilters} id="audit-filter-form" noValidate>
          <div className="row g-2 mb-3">
            {/* User ID Filter */}
            <div className="col-12 col-sm-6 col-md-4 col-lg-2">
              <label htmlFor="audit-filter-user-id" className="form-label small fw-semibold">
                User ID
              </label>
              <input
                id="audit-filter-user-id"
                type="number"
                min="1"
                className="form-control form-control-sm"
                placeholder="e.g. 5001"
                value={userIdFilter}
                onChange={(e) => setUserIdFilter(e.target.value)}
              />
            </div>

            {/* Action Filter */}
            <div className="col-12 col-sm-6 col-md-4 col-lg-2">
              <label htmlFor="audit-filter-action" className="form-label small fw-semibold">
                Action
              </label>
              <input
                id="audit-filter-action"
                type="text"
                className="form-control form-control-sm"
                placeholder="e.g. LOGIN_SUCCESS"
                value={actionFilter}
                onChange={(e) => setActionFilter(e.target.value)}
              />
            </div>

            {/* Entity Filter */}
            <div className="col-12 col-sm-6 col-md-4 col-lg-2">
              <label htmlFor="audit-filter-entity" className="form-label small fw-semibold">
                Entity
              </label>
              <input
                id="audit-filter-entity"
                type="text"
                className="form-control form-control-sm"
                placeholder="e.g. User, Policy"
                value={entityFilter}
                onChange={(e) => setEntityFilter(e.target.value)}
              />
            </div>

            {/* Entity ID Filter */}
            <div className="col-12 col-sm-6 col-md-4 col-lg-2">
              <label htmlFor="audit-filter-entity-id" className="form-label small fw-semibold">
                Entity ID
              </label>
              <input
                id="audit-filter-entity-id"
                type="number"
                min="1"
                className="form-control form-control-sm"
                placeholder="e.g. 101"
                value={entityIdFilter}
                onChange={(e) => setEntityIdFilter(e.target.value)}
              />
            </div>

            {/* From Date Filter */}
            <div className="col-12 col-sm-6 col-md-4 col-lg-2">
              <label htmlFor="audit-filter-from-date" className="form-label small fw-semibold">
                From Date
              </label>
              <input
                id="audit-filter-from-date"
                type="date"
                className="form-control form-control-sm"
                value={fromDateFilter}
                onChange={(e) => setFromDateFilter(e.target.value)}
              />
            </div>

            {/* To Date Filter */}
            <div className="col-12 col-sm-6 col-md-4 col-lg-2">
              <label htmlFor="audit-filter-to-date" className="form-label small fw-semibold">
                To Date
              </label>
              <input
                id="audit-filter-to-date"
                type="date"
                className="form-control form-control-sm"
                value={toDateFilter}
                onChange={(e) => setToDateFilter(e.target.value)}
              />
            </div>
          </div>

          <div className="d-flex flex-wrap gap-2 justify-content-end">
            <button
              type="button"
              id="audit-reset-filters-btn"
              className="btn btn-sm btn-outline-secondary"
              onClick={handleResetFilters}
              disabled={loading}
            >
              Reset Filters
            </button>
            <button
              type="submit"
              id="audit-apply-filters-btn"
              className="btn btn-sm btn-primary"
              disabled={loading}
            >
              {loading ? 'Searching...' : 'Apply Filters'}
            </button>
          </div>
        </form>
      </div>

      {/* Error Alert */}
      {error && (
        <div className="alert alert-danger mb-4" id="audit-error-alert" role="alert">
          {error}
        </div>
      )}

      {/* Audit Log Table Container */}
      <div className="dashboard-card p-3 p-md-4 mb-4">
        <div className="d-flex flex-wrap justify-content-between align-items-center mb-3">
          <h2 className="fs-5 fw-bold mb-0">Audit Records</h2>
          <div className="d-flex align-items-center gap-2 mt-2 mt-sm-0">
            <label htmlFor="audit-page-size-select" className="small text-muted mb-0">
              Per Page:
            </label>
            <select
              id="audit-page-size-select"
              className="form-select form-select-sm"
              style={{ width: 'auto' }}
              value={size}
              onChange={(e) => {
                setSize(Number(e.target.value));
                setPage(0);
              }}
            >
              <option value="10">10</option>
              <option value="20">20</option>
              <option value="50">50</option>
              <option value="100">100</option>
            </select>
          </div>
        </div>

        {/* Responsive Table Container (scrolls within container on 360px without expanding page) */}
        <div className="table-responsive" id="audit-table-container">
          <table className="table table-hover table-striped align-middle small mb-0" id="audit-table">
            <thead className="table-light">
              <tr>
                <th scope="col" style={{ minWidth: '70px' }}>ID</th>
                <th scope="col" style={{ minWidth: '160px' }}>Timestamp</th>
                <th scope="col" style={{ minWidth: '90px' }}>User ID</th>
                <th scope="col" style={{ minWidth: '180px' }}>Action</th>
                <th scope="col" style={{ minWidth: '110px' }}>Entity</th>
                <th scope="col" style={{ minWidth: '90px' }}>Entity ID</th>
                <th scope="col" style={{ minWidth: '220px' }}>Details / Metadata</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="7" className="text-center py-4 text-muted" id="audit-loading-row">
                    <div className="spinner-border spinner-border-sm me-2" role="status" />
                    Loading audit trail records...
                  </td>
                </tr>
              ) : logs.length === 0 ? (
                <tr>
                  <td colSpan="7" className="text-center py-4 text-muted" id="audit-empty-row">
                    No audit records found matching the active criteria.
                  </td>
                </tr>
              ) : (
                logs.map((item) => (
                  <tr key={item.id} id={`audit-row-${item.id}`}>
                    <td className="fw-semibold text-muted">#{item.id}</td>
                    <td className="text-nowrap">{formatTimestamp(item.timestamp)}</td>
                    <td>
                      <span className="badge bg-light text-dark border">
                        {item.userId === 0 ? 'SYSTEM (0)' : `User #${item.userId}`}
                      </span>
                    </td>
                    <td>
                      <span className="fw-semibold text-primary">{item.action}</span>
                    </td>
                    <td>
                      <span className="badge bg-secondary">{item.entity}</span>
                    </td>
                    <td className="text-muted">#{item.entityId}</td>
                    <td>
                      <div
                        className="text-break font-monospace small"
                        style={{ maxWidth: '380px' }}
                      >
                        {item.details || <span className="text-muted">—</span>}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Navigation */}
        <div className="d-flex flex-wrap justify-content-between align-items-center mt-3 pt-3 border-top gap-2">
          <div className="small text-muted" id="audit-pagination-info">
            Showing Page <strong>{totalPages > 0 ? page + 1 : 0}</strong> of <strong>{totalPages}</strong> ({totalElements} total entries)
          </div>
          <div className="btn-group" role="group" aria-label="Audit pagination controls">
            <button
              type="button"
              id="audit-prev-page-btn"
              className="btn btn-sm btn-outline-primary"
              disabled={page <= 0 || loading}
              onClick={() => setPage((prev) => Math.max(0, prev - 1))}
            >
              &larr; Previous
            </button>
            <button
              type="button"
              id="audit-next-page-btn"
              className="btn btn-sm btn-outline-primary"
              disabled={page >= totalPages - 1 || loading}
              onClick={() => setPage((prev) => prev + 1)}
            >
              Next &rarr;
            </button>
          </div>
        </div>
      </div>
    </PortalLayout>
  );
}
