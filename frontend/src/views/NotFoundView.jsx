import React from 'react';
import { Link } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import { ROUTES } from '../routes/routePaths';

export default function NotFoundView() {
  return (
    <AppLayout>
      <div className="text-center py-5">
        <div className="display-1 fw-bold text-muted mb-2">404</div>
        <h1 className="h3 fw-bold mb-3">Page Not Found</h1>
        <p className="text-muted col-md-6 mx-auto mb-4">
          The requested page or resource could not be found. Please check the URL or navigate back to the home portal.
        </p>
        <Link to={ROUTES.HOME} className="btn btn-primary btn-sm px-4">
          <i className="bi bi-house me-1"></i>Back to Home
        </Link>
      </div>
    </AppLayout>
  );
}
