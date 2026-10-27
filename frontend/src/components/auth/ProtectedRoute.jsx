import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth, ROLES } from '../../context/AuthContext';
import { ROUTES } from '../../routes/routePaths';

/**
 * Client-side route protection guard (INS-F-001 / INS-SR-005).
 * Enforces role access and redirects unauthenticated visitors to /login.
 */
export default function ProtectedRoute({ children, allowedRoles }) {
  const { currentUser, isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated || currentUser.role === ROLES.GUEST) {
    return (
      <Navigate
        to={ROUTES.LOGIN}
        state={{
          from: location,
          message: 'Please sign in to access this page.',
        }}
        replace
      />
    );
  }

  if (allowedRoles && allowedRoles.length > 0 && !allowedRoles.includes(currentUser.role)) {
    return <Navigate to={ROUTES.HOME} replace />;
  }

  return children;
}
