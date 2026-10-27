import React, { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { onUnauthorized } from '../../api/apiClient';
import { ROUTES } from '../../routes/routePaths';

/**
 * Global listener that redirects to /login with an informative flash message
 * whenever any API call returns HTTP 401 Unauthorized (session timeout or invalid token).
 */
export default function AuthSessionListener() {
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const unsubscribe = onUnauthorized(() => {
      if (location.pathname !== ROUTES.LOGIN) {
        navigate(ROUTES.LOGIN, {
          state: {
            from: location,
            message: 'Your session has expired. Please sign in again to continue.',
          },
          replace: true,
        });
      }
    });
    return unsubscribe;
  }, [navigate, location]);

  return null;
}
