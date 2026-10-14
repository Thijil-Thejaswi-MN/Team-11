import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth, ROLES } from '../../context/AuthContext';
import { ROUTES } from '../../routes/routePaths';

export default function Header() {
  const { currentUser, switchRole, logout } = useAuth();
  const [isNavCollapsed, setIsNavCollapsed] = useState(true);
  const navigate = useNavigate();

  const handleRoleChange = (e) => {
    const newRole = e.target.value;
    switchRole(newRole);

    // Auto-navigate to appropriate role home for smooth demo
    switch (newRole) {
      case ROLES.CUSTOMER:
        navigate(ROUTES.CUSTOMER_DASHBOARD);
        break;
      case ROLES.INSURER_ADMIN:
        navigate(ROUTES.INSURER_ADMIN_DASHBOARD);
        break;
      case ROLES.UNDERWRITER:
        navigate(ROUTES.UNDERWRITER_DASHBOARD);
        break;
      case ROLES.CLAIMS_OFFICER:
        navigate(ROUTES.CLAIMS_OFFICER_DASHBOARD);
        break;
      case ROLES.SYSTEM_ADMIN:
        navigate(ROUTES.SYSTEM_ADMIN_DASHBOARD);
        break;
      default:
        navigate(ROUTES.HOME);
        break;
    }
  };

  return (
    <header>
      <nav className="navbar navbar-expand-lg navbar-dark app-navbar" id="app-main-navbar">
        <div className="container-fluid px-3">
          {/* Brand Logo & Name */}
          <Link to={ROUTES.HOME} className="brand-logo-text" id="navbar-brand-link">
            <i className="bi bi-shield-check text-info fs-4"></i>
            <span>InsureFlow</span>
            <span className="brand-badge ms-1 d-none d-sm-inline">Team 11</span>
          </Link>

          {/* Mobile Viewport Toggle Button (Accessible & Unique ID) */}
          <button
            className="navbar-toggler"
            type="button"
            id="main-navbar-toggle"
            aria-controls="appNavbarNav"
            aria-expanded={!isNavCollapsed}
            aria-label="Toggle navigation"
            onClick={() => setIsNavCollapsed(!isNavCollapsed)}
          >
            <span className="navbar-toggler-icon"></span>
          </button>

          {/* Collapsible Nav Links & Role Switcher */}
          <div className={`collapse navbar-collapse ${!isNavCollapsed ? 'show' : ''}`} id="appNavbarNav">
            <ul className="navbar-nav me-auto mb-2 mb-lg-0 ms-lg-3">
              <li className="nav-item">
                <Link
                  to={ROUTES.HOME}
                  className="nav-link"
                  id="nav-link-home"
                  onClick={() => setIsNavCollapsed(true)}
                >
                  <i className="bi bi-house me-1"></i>Home
                </Link>
              </li>
              <li className="nav-item">
                <Link
                  to={ROUTES.PLANS}
                  className="nav-link"
                  id="nav-link-plans"
                  onClick={() => setIsNavCollapsed(true)}
                >
                  <i className="bi bi-search me-1"></i>Browse Plans
                </Link>
              </li>
              <li className="nav-item">
                <Link
                  to={ROUTES.PLANS_COMPARE}
                  className="nav-link"
                  id="nav-link-compare"
                  onClick={() => setIsNavCollapsed(true)}
                >
                  <i className="bi bi-arrow-left-right me-1"></i>Compare
                </Link>
              </li>

              {/* Quick links to active portal based on current role */}
              {currentUser.role === ROLES.CUSTOMER && (
                <li className="nav-item">
                  <Link
                    to={ROUTES.CUSTOMER_DASHBOARD}
                    className="nav-link text-info fw-semibold"
                    id="nav-link-portal"
                    onClick={() => setIsNavCollapsed(true)}
                  >
                    <i className="bi bi-speedometer2 me-1"></i>My Portal
                  </Link>
                </li>
              )}
              {currentUser.role === ROLES.INSURER_ADMIN && (
                <li className="nav-item">
                  <Link
                    to={ROUTES.INSURER_ADMIN_DASHBOARD}
                    className="nav-link text-warning fw-semibold"
                    id="nav-link-portal"
                    onClick={() => setIsNavCollapsed(true)}
                  >
                    <i className="bi bi-briefcase me-1"></i>Insurer Admin
                  </Link>
                </li>
              )}
              {currentUser.role === ROLES.UNDERWRITER && (
                <li className="nav-item">
                  <Link
                    to={ROUTES.UNDERWRITER_DASHBOARD}
                    className="nav-link text-warning fw-semibold"
                    id="nav-link-portal"
                    onClick={() => setIsNavCollapsed(true)}
                  >
                    <i className="bi bi-journal-check me-1"></i>Underwriter
                  </Link>
                </li>
              )}
              {currentUser.role === ROLES.CLAIMS_OFFICER && (
                <li className="nav-item">
                  <Link
                    to={ROUTES.CLAIMS_OFFICER_DASHBOARD}
                    className="nav-link text-warning fw-semibold"
                    id="nav-link-portal"
                    onClick={() => setIsNavCollapsed(true)}
                  >
                    <i className="bi bi-file-earmark-medical me-1"></i>Claims Desk
                  </Link>
                </li>
              )}
              {currentUser.role === ROLES.SYSTEM_ADMIN && (
                <li className="nav-item">
                  <Link
                    to={ROUTES.SYSTEM_ADMIN_DASHBOARD}
                    className="nav-link text-danger fw-semibold"
                    id="nav-link-portal"
                    onClick={() => setIsNavCollapsed(true)}
                  >
                    <i className="bi bi-gear me-1"></i>System Admin
                  </Link>
                </li>
              )}
            </ul>

            {/* Right side controls: Role Switcher & User Profile Stub */}
            <div className="d-flex align-items-center flex-wrap gap-2 mt-2 mt-lg-0">
              {/* Role Switcher Toolbar for Team Testing */}
              <div className="d-flex align-items-center gap-1">
                <label htmlFor="role-select" className="text-light small d-none d-md-inline">
                  <i className="bi bi-person-badge me-1"></i>Role:
                </label>
                <select
                  id="role-select"
                  className="role-switcher-select form-select form-select-sm"
                  value={currentUser.role}
                  onChange={handleRoleChange}
                  title="Switch Role Portal (Testing Shell Feature)"
                >
                  <option value={ROLES.CUSTOMER}>Customer (Asha)</option>
                  <option value={ROLES.INSURER_ADMIN}>Insurer Admin (SafeShield)</option>
                  <option value={ROLES.UNDERWRITER}>Underwriter (SafeShield)</option>
                  <option value={ROLES.CLAIMS_OFFICER}>Claims Officer (SafeShield)</option>
                  <option value={ROLES.SYSTEM_ADMIN}>System Admin</option>
                  <option value={ROLES.GUEST}>Guest / Anonymous</option>
                </select>
              </div>

              {/* Login / User Indicator */}
              {currentUser.role === ROLES.GUEST ? (
                <Link
                  to={ROUTES.LOGIN}
                  className="btn btn-outline-info btn-sm ms-1"
                  id="nav-login-btn"
                  onClick={() => setIsNavCollapsed(true)}
                >
                  <i className="bi bi-box-arrow-in-right me-1"></i>Login
                </Link>
              ) : (
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm text-light ms-1"
                  id="nav-logout-btn"
                  onClick={() => {
                    logout();
                    setIsNavCollapsed(true);
                  }}
                  title="Sign out of current session"
                >
                  <i className="bi bi-box-arrow-right"></i>
                </button>
              )}
            </div>
          </div>
        </div>
      </nav>
    </header>
  );
}
