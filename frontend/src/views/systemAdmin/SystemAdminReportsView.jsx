import React from 'react';
import PortalLayout from '../../components/layout/PortalLayout';

/**
 * System Administration Reports View (INS-F-025 / Decision D-10).
 * Formal cross-insurer reporting was deferred to Sprint 3 under D-10.
 */
export default function SystemAdminReportsView() {
  return (
    <PortalLayout
      title="System & Platform Reports"
      subtitle="Multi-insurer portfolio analytics, regulatory summaries, and financial reports."
      badgeText="System Admin"
      badgeClass="bg-danger"
    >
      <div className="alert alert-info border-0 shadow-sm my-4" data-testid="reports-deferred-notice">
        <div className="d-flex align-items-start">
          <i className="bi bi-info-circle-fill fs-4 me-3 text-info"></i>
          <div>
            <h5 className="alert-heading fw-bold mb-1">
              Aggregated Reports are not available in this release
            </h5>
            <p className="mb-2 text-muted small">
              Executive business reporting and cross-insurer analytics (INS-F-025) were formally deferred to Sprint 3 under <strong>Team Decision D-10</strong>.
            </p>
            <p className="mb-0 text-muted small">
              For transactional verification and compliance events, please check the live <strong>Audit Log</strong> in the navigation sidebar.
            </p>
          </div>
        </div>
      </div>
    </PortalLayout>
  );
}
