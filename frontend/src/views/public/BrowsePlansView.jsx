import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import apiClient from '../../api/apiClient';
import { ROUTES } from '../../routes/routePaths';

export default function BrowsePlansView() {
  const navigate = useNavigate();
  const [filterType, setFilterType] = useState('ALL');
  const [plans, setPlans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedPlanIds, setSelectedPlanIds] = useState([]);

  const fetchPlans = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.get('/plans');
      const items = response.data?.items || (Array.isArray(response.data) ? response.data : []);
      setPlans(items);
    } catch (err) {
      setPlans([]);
      setError(err.response?.data?.message || err.message || 'Failed to load insurance plans.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPlans();
  }, []);

  const handleToggleSelectForCompare = (planId) => {
    setSelectedPlanIds((prev) => {
      if (prev.includes(planId)) {
        return prev.filter((id) => id !== planId);
      }
      if (prev.length >= 3) {
        return prev;
      }
      return [...prev, planId];
    });
  };

  const handleCompareNow = () => {
    if (selectedPlanIds.length > 0) {
      navigate(`${ROUTES.PLANS_COMPARE}?ids=${selectedPlanIds.join(',')}`);
    }
  };

  const filtered =
    filterType === 'ALL'
      ? plans
      : plans.filter((p) => (p.type ? p.type.toUpperCase().startsWith(filterType) : false));

  return (
    <AppLayout>
      <div className="mb-4">
        <h1 className="h3 fw-bold">Browse Insurance Plans</h1>
        <p className="text-muted small">
          Explore and compare verified motor and health insurance plans.
        </p>
      </div>

      {/* Filter and Compare Toolbar */}
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-4">
        <div className="d-flex flex-wrap gap-2 align-items-center">
          <span className="small fw-semibold text-muted">Filter By Category:</span>
          <button
            className={`btn btn-sm ${filterType === 'ALL' ? 'btn-primary' : 'btn-outline-secondary'}`}
            onClick={() => setFilterType('ALL')}
          >
            All Plans
          </button>
          <button
            className={`btn btn-sm ${filterType === 'MOTOR' ? 'btn-primary' : 'btn-outline-secondary'}`}
            onClick={() => setFilterType('MOTOR')}
          >
            Motor (2W / 4W)
          </button>
          <button
            className={`btn btn-sm ${filterType === 'HEALTH' ? 'btn-primary' : 'btn-outline-secondary'}`}
            onClick={() => setFilterType('HEALTH')}
          >
            Personal Health
          </button>
        </div>

        {selectedPlanIds.length > 0 && (
          <div className="d-flex align-items-center gap-2">
            <span className="badge bg-primary">{selectedPlanIds.length} / 3 selected</span>
            <button
              className="btn btn-primary btn-sm"
              id="compare-selected-plans-btn"
              onClick={handleCompareNow}
            >
              Compare Plans &rarr;
            </button>
            <button
              className="btn btn-outline-secondary btn-sm"
              onClick={() => setSelectedPlanIds([])}
            >
              Clear
            </button>
          </div>
        )}
      </div>

      {/* Error Alert with Retry */}
      {error && (
        <div className="alert alert-danger mb-4 d-flex justify-content-between align-items-center" role="alert">
          <div>
            <i className="bi bi-exclamation-triangle-fill me-2"></i>
            <span>{error}</span>
          </div>
          <button type="button" className="btn btn-outline-danger btn-sm" onClick={fetchPlans}>
            Retry
          </button>
        </div>
      )}

      {/* Loading State */}
      {loading && (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status"></div>
          <div className="mt-2 text-muted small">Loading available plans...</div>
        </div>
      )}

      {/* Empty State */}
      {!loading && !error && filtered.length === 0 && (
        <div className="text-center py-5 dashboard-card">
          <div className="p-4">
            <i className="bi bi-shield-slash fs-1 text-muted mb-2 d-block"></i>
            <h5 className="fw-bold">No Plans Available</h5>
            <p className="text-muted small mb-0">
              {filterType !== 'ALL'
                ? `No plans matching filter "${filterType}". Try selecting "All Plans".`
                : 'No insurance plans currently active.'}
            </p>
          </div>
        </div>
      )}

      {/* Plan Cards Grid */}
      {!loading && !error && filtered.length > 0 && (
        <div className="row g-3">
          {filtered.map((plan) => {
            const planId = plan.planId || plan.id;
            const isSelected = selectedPlanIds.includes(planId);
            const isHealth = plan.type && plan.type.toUpperCase().startsWith('HEALTH');
            return (
              <div key={planId} className="col-12 col-md-6 col-lg-4">
                <div className={`dashboard-card h-100 ${isSelected ? 'border border-primary' : ''}`}>
                  <div className="dashboard-card-header d-flex justify-content-between align-items-center">
                    <span className="fw-bold">{plan.name}</span>
                    <span className="badge bg-secondary">{plan.type}</span>
                  </div>
                  <div className="dashboard-card-body d-flex flex-column">
                    <div className="text-muted small mb-2">{plan.insurer || 'SafeShield General'}</div>
                    <div className="mb-2">
                      <span className="fw-bold text-primary fs-5">
                        {plan.basePremium != null
                          ? `₹${Number(plan.basePremium).toLocaleString('en-IN')} / yr`
                          : 'Custom Quote'}
                      </span>
                      <span className="text-muted small ms-1">(Base)</span>
                    </div>
                    <div className="small mb-3">
                      <div>
                        <strong>Coverage:</strong>{' '}
                        {plan.coverage != null ? `₹${Number(plan.coverage).toLocaleString('en-IN')}` : '—'}
                      </div>
                      <div>
                        <strong>Standard Deductible:</strong>{' '}
                        {plan.deductible != null ? `₹${Number(plan.deductible).toLocaleString('en-IN')}` : '—'}
                      </div>
                      {plan.waitingPeriodMonths != null && (
                        <div>
                          <strong>Waiting Period:</strong> {plan.waitingPeriodMonths} months
                        </div>
                      )}
                    </div>
                    {Array.isArray(plan.addOns) && plan.addOns.length > 0 && (
                      <div className="mb-3">
                        <div className="small text-muted mb-1">Available Add-ons:</div>
                        <div className="d-flex flex-wrap gap-1">
                          {plan.addOns.map((addon, idx) => (
                            <span key={idx} className="badge bg-light text-dark border small">
                              {addon}
                            </span>
                          ))}
                        </div>
                      </div>
                    )}
                    <div className="mt-auto pt-2 border-top d-flex gap-2">
                      <button
                        className={`btn btn-sm ${isSelected ? 'btn-primary' : 'btn-outline-secondary'}`}
                        onClick={() => handleToggleSelectForCompare(planId)}
                        title={isSelected ? 'Remove from comparison' : 'Select for comparison (max 3)'}
                      >
                        {isSelected ? '✓ Selected' : 'Compare'}
                      </button>
                      <button
                        className="btn btn-outline-primary btn-sm flex-grow-1"
                        id={`calculate-quote-plan-${planId}`}
                        onClick={() => {
                          const target = isHealth
                            ? `${ROUTES.CUSTOMER_QUOTE_HEALTH}?planId=${planId}`
                            : `${ROUTES.CUSTOMER_QUOTE_MOTOR}?planId=${planId}`;
                          navigate(target);
                        }}
                      >
                        <i className="bi bi-calculator me-1"></i>Calculate Instant Quote
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </AppLayout>
  );
}
