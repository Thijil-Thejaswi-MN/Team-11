import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth, ROLES } from '../../context/AuthContext';
import { ROUTES } from '../../routes/routePaths';

export default function Sidebar() {
  const { currentUser } = useAuth();
  const role = currentUser.role;

  return (
    <aside className="portal-sidebar" id="portal-main-sidebar">
      <div className="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom">
        <div>
          <div className="fw-bold small text-uppercase text-muted">Portal Navigation</div>
          <div className="badge bg-primary text-wrap text-start mt-1">
            {role.replace('_', ' ')}
          </div>
        </div>
      </div>

      <nav className="portal-sidebar-nav" aria-label="Portal Navigation Links">
        {/* Customer Portal Links */}
        {role === ROLES.CUSTOMER && (
          <>
            <NavLink
              to={ROUTES.CUSTOMER_DASHBOARD}
              end
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <i className="bi bi-speedometer2"></i>
              <span>Dashboard</span>
            </NavLink>
            <NavLink
              to={ROUTES.CUSTOMER_KYC}
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
              id="sidebar-link-kyc"
            >
              <i className="bi bi-person-check"></i>
              <span>KYC Verification</span>
            </NavLink>
            <NavLink
              to={ROUTES.CUSTOMER_QUOTE_MOTOR}
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <i className="bi bi-car-front"></i>
              <span>Motor Quote</span>
            </NavLink>
            <NavLink
              to={ROUTES.CUSTOMER_QUOTE_HEALTH}
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <i className="bi bi-heart-pulse"></i>
              <span>Health Quote</span>
            </NavLink>
            <NavLink
              to={ROUTES.CUSTOMER_APPLICATIONS}
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <i className="bi bi-file-earmark-text"></i>
              <span>Applications</span>
            </NavLink>
            <NavLink
              to={ROUTES.CUSTOMER_POLICIES}
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <i className="bi bi-shield-check"></i>
              <span>My Policies</span>
            </NavLink>
            <NavLink
              to={ROUTES.CUSTOMER_CLAIMS}
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <i className="bi bi-clipboard2-pulse"></i>
              <span>Claims Tracker</span>
            </NavLink>
          </>
        )}

        {/* Insurer Admin Portal Links */}
        {role === ROLES.INSURER_ADMIN && (
          <>
            <NavLink
              to={ROUTES.INSURER_ADMIN_DASHBOARD}
              end
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <i className="bi bi-speedometer2"></i>
              <span>Admin Overview</span>
            </NavLink>
            <NavLink
              to={ROUTES.INSURER_ADMIN_PLANS}
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <i className="bi bi-card-checklist"></i>
              <span>Plan Management</span>
            </NavLink>
            <NavLink
              to={ROUTES.INSURER_ADMIN_CANCELLATIONS}
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <i className="bi bi-x-circle"></i>
              <span>Cancellations</span>
            </NavLink>
          </>
        )}

        {/* Underwriter Portal Links */}
        {role === ROLES.UNDERWRITER && (
          <>
            <NavLink
              to={ROUTES.UNDERWRITER_DASHBOARD}
              end
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <i className="bi bi-inbox"></i>
              <span>Referred Applications</span>
            </NavLink>
          </>
        )}

        {/* Claims Officer Portal Links */}
        {role === ROLES.CLAIMS_OFFICER && (
          <>
            <NavLink
              to={ROUTES.CLAIMS_OFFICER_DASHBOARD}
              end
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <i className="bi bi-file-earmark-medical"></i>
              <span>Assessment Queue</span>
            </NavLink>
          </>
        )}

        {/* System Admin Portal Links */}
        {role === ROLES.SYSTEM_ADMIN && (
          <>
            <NavLink
              to={ROUTES.SYSTEM_ADMIN_DASHBOARD}
              end
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <i className="bi bi-gear"></i>
              <span>System Overview</span>
            </NavLink>
            <NavLink
              to={ROUTES.SYSTEM_ADMIN_REPORTS}
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <i className="bi bi-bar-chart-line"></i>
              <span>Reports</span>
            </NavLink>
            <NavLink
              to={ROUTES.SYSTEM_ADMIN_AUDIT}
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <i className="bi bi-shield-lock"></i>
              <span>Audit Log</span>
            </NavLink>
          </>
        )}

        {/* Public / Guest Links */}
        {role === ROLES.GUEST && (
          <>
            <NavLink
              to={ROUTES.PLANS}
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <i className="bi bi-search"></i>
              <span>Browse Plans</span>
            </NavLink>
            <NavLink
              to={ROUTES.PLANS_COMPARE}
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <i className="bi bi-arrow-left-right"></i>
              <span>Compare Plans</span>
            </NavLink>
            <NavLink
              to={ROUTES.LOGIN}
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <i className="bi bi-box-arrow-in-right"></i>
              <span>Sign In</span>
            </NavLink>
          </>
        )}
      </nav>
    </aside>
  );
}
