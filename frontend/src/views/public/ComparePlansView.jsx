import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import apiClient from '../../api/apiClient';
import { ROUTES } from '../../routes/routePaths';

export default function ComparePlansView() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const idsParam = searchParams.get('ids') || '';

  const [plans, setPlans] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchComparison = async () => {
    if (!idsParam.trim()) {
      setPlans([]);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.get('/plans/compare', {
        params: { ids: idsParam.trim() },
      });
      const items = response.data?.plans || (Array.isArray(response.data) ? response.data : []);
      setPlans(items);
    } catch (err) {
      setPlans([]);
      const status = err.response?.status;
      const apiMsg = err.response?.data?.message || err.message;
      if (status === 400 || status === 422) {
        setError(`Invalid comparison request (${status}): ${apiMsg || 'Maximum 3 plans can be compared at once.'}`);
      } else if (status === 404) {
        setError(`Plan not found (404): ${apiMsg || 'One or more selected plans could not be found.'}`);
      } else {
        setError(apiMsg || 'Failed to compare plans.');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchComparison();
  }, [idsParam]);

  return (
    <AppLayout>
      <div className="mb-4">
        <h1 className="h3 fw-bold">Compare Insurance Plans</h1>
        <p className="text-muted small">
          Side-by-side comparison for up to 3 plans (SAD Section 4.3.2 / <code>GET /api/v1/plans/compare?ids=...</code>).
        </p>
      </div>

      {/* Developer Integration Hook */}
      {import.meta.env.DEV && (
        <div className="dev-hook-box mb-4">
          <span className="dev-hook-badge">Person 1 Ownership (Plan Comparison C4)</span>
          <p className="mb-0">
            This view connects to <code>GET /api/v1/plans/compare?ids=...</code>.
            Enforces maximum 3 plans boundary (returns <code>VALID-422 / 400</code> if more than 3 selected).
          </p>
        </div>
      )}

      {/* Error Alert with Retry */}
      {error && (
        <div className="alert alert-danger mb-4 d-flex justify-content-between align-items-center" role="alert">
          <div>
            <i className="bi bi-exclamation-triangle-fill me-2"></i>
            <span>{error}</span>
          </div>
          <button type="button" className="btn btn-outline-danger btn-sm ms-2" onClick={fetchComparison}>
            Retry
          </button>
        </div>
      )}

      {/* Comparison Matrix Container */}
      <div className="dashboard-card">
        <div className="dashboard-card-header d-flex justify-content-between align-items-center">
          <span className="fw-bold">
            Comparison Matrix {plans.length > 0 ? `(${plans.length} Plans)` : ''}
          </span>
          <Link to={ROUTES.PLANS} className="btn btn-outline-secondary btn-sm">
            &larr; Back to Browse Plans
          </Link>
        </div>
        <div className="dashboard-card-body p-0">
          <div className="table-responsive">
            {loading && (
              <div className="text-center py-5">
                <div className="spinner-border text-primary" role="status"></div>
                <div className="mt-2 text-muted small">Loading side-by-side plan comparison...</div>
              </div>
            )}

            {!loading && !idsParam.trim() && (
              <div className="text-center py-5 p-4">
                <i className="bi bi-layout-split fs-1 text-muted mb-2 d-block"></i>
                <h5 className="fw-bold">No Plans Selected for Comparison</h5>
                <p className="text-muted small mb-3">
                  Please browse active insurance plans and select up to 3 plans to compare side by side.
                </p>
                <Link to={ROUTES.PLANS} className="btn btn-primary btn-sm">
                  <i className="bi bi-search me-1"></i>Browse Insurance Plans
                </Link>
              </div>
            )}

            {!loading && idsParam.trim() && !error && plans.length === 0 && (
              <div className="text-center py-5 p-4">
                <i className="bi bi-exclamation-circle fs-1 text-muted mb-2 d-block"></i>
                <h5 className="fw-bold">No Plan Details Available</h5>
                <p className="text-muted small mb-3">
                  The requested plan comparison could not be loaded. Please return to browse plans.
                </p>
                <Link to={ROUTES.PLANS} className="btn btn-primary btn-sm">
                  Browse Plans
                </Link>
              </div>
            )}

            {!loading && !error && plans.length > 0 && (
              <table className="table table-hover mb-0 align-middle">
                <thead className="table-light">
                  <tr>
                    <th style={{ minWidth: '160px' }}>Plan Attribute</th>
                    {plans.map((p) => (
                      <th key={p.planId || p.id} style={{ minWidth: '200px' }}>
                        <div className="fw-bold text-dark">{p.name}</div>
                        <span className="badge bg-secondary extra-small">{p.type}</span>
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td className="fw-semibold">Insurer</td>
                    {plans.map((p) => (
                      <td key={p.planId || p.id}>{p.insurer || '—'}</td>
                    ))}
                  </tr>
                  <tr>
                    <td className="fw-semibold">Category</td>
                    {plans.map((p) => (
                      <td key={p.planId || p.id}>{p.type || '—'}</td>
                    ))}
                  </tr>
                  <tr>
                    <td className="fw-semibold">Maximum Coverage</td>
                    {plans.map((p) => (
                      <td key={p.planId || p.id}>
                        {p.coverage != null ? `₹${Number(p.coverage).toLocaleString('en-IN')}` : '—'}
                      </td>
                    ))}
                  </tr>
                  <tr>
                    <td className="fw-semibold">Standard Deductible</td>
                    {plans.map((p) => (
                      <td key={p.planId || p.id}>
                        {p.deductible != null ? `₹${Number(p.deductible).toLocaleString('en-IN')}` : '—'}
                      </td>
                    ))}
                  </tr>
                  <tr>
                    <td className="fw-semibold">Waiting Period</td>
                    {plans.map((p) => (
                      <td key={p.planId || p.id}>
                        {p.waitingPeriodMonths != null ? `${p.waitingPeriodMonths} months` : 'None (Immediate)'}
                      </td>
                    ))}
                  </tr>
                  <tr>
                    <td className="fw-semibold">Key Add-ons</td>
                    {plans.map((p) => (
                      <td key={p.planId || p.id}>
                        {Array.isArray(p.addOns) && p.addOns.length > 0 ? p.addOns.join(', ') : 'None'}
                      </td>
                    ))}
                  </tr>
                  <tr>
                    <td className="fw-semibold">Estimated Base</td>
                    {plans.map((p) => (
                      <td key={p.planId || p.id} className="text-primary fw-bold">
                        {p.basePremium != null ? `₹${Number(p.basePremium).toLocaleString('en-IN')} / yr` : '—'}
                      </td>
                    ))}
                  </tr>
                  <tr>
                    <td className="fw-semibold">Action</td>
                    {plans.map((p) => {
                      const planId = p.planId || p.id;
                      const isHealth = p.type && p.type.toUpperCase().startsWith('HEALTH');
                      return (
                        <td key={planId}>
                          <button
                            className="btn btn-outline-primary btn-sm w-100"
                            onClick={() => {
                              const target = isHealth
                                ? `${ROUTES.CUSTOMER_QUOTE_HEALTH}?planId=${planId}`
                                : `${ROUTES.CUSTOMER_QUOTE_MOTOR}?planId=${planId}`;
                              navigate(target);
                            }}
                          >
                            <i className="bi bi-calculator me-1"></i>Calculate Instant Quote
                          </button>
                        </td>
                      );
                    })}
                  </tr>
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>
    </AppLayout>
  );
}
