import React from 'react';
import { Link } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import { ROUTES } from '../../routes/routePaths';

export default function HomeView() {
  return (
    <AppLayout>
      {/* Hero Section */}
      <div
        className="p-4 p-md-5 mb-4 rounded-3 text-white"
        style={{ background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)' }}
      >
        <div className="container-fluid py-2">
          <h1 className="display-6 fw-bold">InsureFlow</h1>
          <p className="col-md-9 fs-6 text-light mb-4">
            Smart digital insurance management — explore plans, calculate instant quotes, and manage policies and claims in one place.
          </p>
          <div className="d-flex flex-wrap gap-2">
            <Link to={ROUTES.PLANS} className="btn btn-primary btn-sm px-3" id="home-browse-btn">
              <i className="bi bi-search me-1"></i>Browse plans
            </Link>
            <Link to={ROUTES.PLANS_COMPARE} className="btn btn-outline-light btn-sm px-3" id="home-compare-btn">
              <i className="bi bi-arrow-left-right me-1"></i>Compare plans
            </Link>
          </div>
        </div>
      </div>

      {/* What You Can Do Section */}
      <section aria-labelledby="features-heading" className="mb-4">
        <h2 className="h4 fw-bold mb-3" id="features-heading">What you can do</h2>
        <div className="row g-3">
          {/* Card 1: Compare plans */}
          <div className="col-12 col-md-6 col-lg-3">
            <div className="dashboard-card h-100 p-3 d-flex flex-column">
              <div className="text-primary fs-3 mb-2">
                <i className="bi bi-layout-split"></i>
              </div>
              <h3 className="h6 fw-bold">Compare plans</h3>
              <p className="text-muted small mb-0">
                Browse policies across top insurers and compare coverage, deductibles, and benefits side by side.
              </p>
            </div>
          </div>

          {/* Card 2: Instant quotes */}
          <div className="col-12 col-md-6 col-lg-3">
            <div className="dashboard-card h-100 p-3 d-flex flex-column">
              <div className="text-success fs-3 mb-2">
                <i className="bi bi-calculator"></i>
              </div>
              <h3 className="h6 fw-bold">Instant motor & health quotes</h3>
              <p className="text-muted small mb-0">
                Get real-time premium estimates for two-wheeler, four-wheeler, and comprehensive health insurance.
              </p>
            </div>
          </div>

          {/* Card 3: Buy & download policy */}
          <div className="col-12 col-md-6 col-lg-3">
            <div className="dashboard-card h-100 p-3 d-flex flex-column">
              <div className="text-info fs-3 mb-2">
                <i className="bi bi-file-earmark-check"></i>
              </div>
              <h3 className="h6 fw-bold">Buy and download your policy</h3>
              <p className="text-muted small mb-0">
                Complete application verification, pay securely, and immediately download your official certified policy schedule.
              </p>
            </div>
          </div>

          {/* Card 4: File & track claims */}
          <div className="col-12 col-md-6 col-lg-3">
            <div className="dashboard-card h-100 p-3 d-flex flex-column">
              <div className="text-warning fs-3 mb-2">
                <i className="bi bi-clipboard2-pulse"></i>
              </div>
              <h3 className="h6 fw-bold">File and track claims</h3>
              <p className="text-muted small mb-0">
                Submit claims with required documentation and monitor progress through verification, loss assessment, and payout.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Call to Action */}
      <section className="dashboard-card p-4 text-center my-4">
        <h3 className="h5 fw-bold mb-2">Ready to get started?</h3>
        <p className="text-muted small mb-3">
          Sign in to access your portal, view your policy portfolio, or file a claim.
        </p>
        <div className="d-flex justify-content-center gap-2">
          <Link to={ROUTES.LOGIN} className="btn btn-primary btn-sm px-4">
            Sign in
          </Link>
          <Link to={ROUTES.REGISTER} className="btn btn-outline-secondary btn-sm px-4">
            Create account
          </Link>
        </div>
      </section>
    </AppLayout>
  );
}
