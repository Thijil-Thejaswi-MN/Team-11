import React, { useState, useEffect } from 'react';
import PortalLayout from '../../components/layout/PortalLayout';
import apiClient from '../../api/apiClient';
import { useAuth } from '../../context/AuthContext';

const AVAILABLE_ADD_ONS = [
  { id: 'ZERO_DEP', label: 'Zero Depreciation (Motor)' },
  { id: 'ENGINE_PROTECT', label: 'Engine Protector (Motor)' },
  { id: 'ROADSIDE_ASSISTANCE', label: '24x7 Roadside Assistance (Motor)' },
  { id: 'CONSUMABLES', label: 'Consumables Cover (Motor)' },
  { id: 'NCB_PROTECT', label: 'No-Claim Bonus Protection (Motor)' },
  { id: 'MATERNITY_COVER', label: 'Maternity Cover (Health)' },
  { id: 'CRITICAL_ILLNESS', label: 'Critical Illness Rider (Health)' },
];

const INITIAL_FORM = {
  code: '',
  name: '',
  type: 'MOTOR_FOUR_WHEELER',
  coverage: '',
  deductible: '0',
  waitingPeriodMonths: '0',
  basePremium: '',
  premiumRulesJson: '{"baseRatePercent": 2.5}',
  underwritingRulesJson: '{}',
  addOns: ['ZERO_DEP'],
  active: true,
};

export default function InsurerAdminPlansView() {
  const { currentUser } = useAuth();
  const effectiveInsurerId = currentUser?.insurerId || 1;

  const [plans, setPlans] = useState([]);
  const [loading, setLoading] = useState(false);
  const [fetchError, setFetchError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  // Form mode: null (hidden/closed), 'CREATE', or 'EDIT'
  const [formMode, setFormMode] = useState(null);
  const [editingPlanId, setEditingPlanId] = useState(null);
  const [formData, setFormData] = useState(INITIAL_FORM);
  const [formSubmitting, setFormSubmitting] = useState(false);

  // Fetch plans on mount
  useEffect(() => {
    fetchPlans();
  }, []);

  const fetchPlans = async () => {
    setLoading(true);
    setFetchError('');
    try {
      const res = await apiClient.get('/plans');
      const items = res.data?.items || [];
      // Filter to current insurer if insurerId is present
      const insurerPlans = effectiveInsurerId
        ? items.filter((p) => p.insurerId === effectiveInsurerId || !p.insurerId)
        : items;
      setPlans(insurerPlans.length > 0 ? insurerPlans : items);
    } catch (err) {
      setFetchError(err.message || 'Failed to fetch insurance plans.');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenCreate = () => {
    setFormData(INITIAL_FORM);
    setEditingPlanId(null);
    setFormMode('CREATE');
    setSuccessMessage('');
    setErrorMessage('');
  };

  const handleOpenEdit = (plan) => {
    setFormData({
      code: plan.code || '',
      name: plan.name || '',
      type: plan.type || 'MOTOR_FOUR_WHEELER',
      coverage: plan.coverage?.toString() || '',
      deductible: plan.deductible != null ? plan.deductible.toString() : '0',
      waitingPeriodMonths: plan.waitingPeriodMonths != null ? plan.waitingPeriodMonths.toString() : '0',
      basePremium: plan.basePremium != null ? plan.basePremium.toString() : '',
      premiumRulesJson: plan.premiumRulesJson || '{"baseRatePercent": 2.5}',
      underwritingRulesJson: plan.underwritingRulesJson || '{}',
      addOns: Array.isArray(plan.addOns) ? plan.addOns : [],
      active: plan.active !== false,
    });
    setEditingPlanId(plan.planId || plan.id);
    setFormMode('EDIT');
    setSuccessMessage('');
    setErrorMessage('');
  };

  const handleCancelForm = () => {
    setFormMode(null);
    setEditingPlanId(null);
    setFormData(INITIAL_FORM);
    setErrorMessage('');
  };

  const handleToggleAddOn = (addOnId) => {
    setFormData((prev) => {
      const exists = prev.addOns.includes(addOnId);
      const nextAddOns = exists
        ? prev.addOns.filter((id) => id !== addOnId)
        : [...prev.addOns, addOnId];
      return { ...prev, addOns: nextAddOns };
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSuccessMessage('');
    setErrorMessage('');

    // Client-side quick validations
    if (!formData.name.trim()) {
      setErrorMessage("Field 'name' is required.");
      return;
    }
    if (formMode === 'CREATE' && !formData.code.trim()) {
      setErrorMessage("Field 'code' is required.");
      return;
    }
    const coverageNum = Number(formData.coverage);
    if (!formData.coverage || isNaN(coverageNum) || coverageNum <= 0) {
      setErrorMessage('Coverage amount must be greater than zero.');
      return;
    }
    const deductibleNum = Number(formData.deductible);
    if (isNaN(deductibleNum) || deductibleNum < 0) {
      setErrorMessage('Deductible cannot be negative.');
      return;
    }
    const basePremiumNum = Number(formData.basePremium);
    if (isNaN(basePremiumNum) || basePremiumNum < 0) {
      setErrorMessage('Base premium cannot be negative.');
      return;
    }

    try {
      setFormSubmitting(true);

      if (formMode === 'CREATE') {
        const payload = {
          code: formData.code.trim().toUpperCase(),
          name: formData.name.trim(),
          type: formData.type,
          coverage: coverageNum,
          deductible: deductibleNum,
          waitingPeriodMonths: Number(formData.waitingPeriodMonths) || 0,
          basePremium: basePremiumNum,
          premiumRulesJson: formData.premiumRulesJson,
          underwritingRulesJson: formData.underwritingRulesJson,
          addOns: formData.addOns,
          active: formData.active,
        };

        const res = await apiClient.post('/plans', payload);
        const newId = res.data?.planId || res.data?.id;
        setSuccessMessage(`Plan ${payload.code} created successfully (ID: ${newId}).`);
        setFormMode(null);
        await fetchPlans();
      } else if (formMode === 'EDIT') {
        const payload = {
          name: formData.name.trim(),
          type: formData.type,
          coverage: coverageNum,
          deductible: deductibleNum,
          waitingPeriodMonths: Number(formData.waitingPeriodMonths) || 0,
          basePremium: basePremiumNum,
          premiumRulesJson: formData.premiumRulesJson,
          underwritingRulesJson: formData.underwritingRulesJson,
          addOns: formData.addOns,
          active: formData.active,
        };

        await apiClient.put(`/plans/${editingPlanId}`, payload);
        setSuccessMessage(`Plan ${formData.code || editingPlanId} updated successfully.`);
        setFormMode(null);
        await fetchPlans();
      }
    } catch (err) {
      handleApiError(err);
    } finally {
      setFormSubmitting(false);
    }
  };

  const handleToggleActive = async (plan) => {
    setSuccessMessage('');
    setErrorMessage('');
    const id = plan.planId || plan.id;
    const newActiveState = !plan.active;

    try {
      await apiClient.patch(`/plans/${id}`, { active: newActiveState });
      setSuccessMessage(
        newActiveState
          ? `Plan ${plan.code} activated. It will now appear in public plan browsing.`
          : `Plan ${plan.code} deactivated. It will no longer appear in public plan browsing.`
      );
      await fetchPlans();
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleApiError = (err) => {
    const status = err.status || err.response?.status;
    const errorCode = err.errorCode || err.response?.data?.errorCode;
    const serverMessage = err.response?.data?.message || err.message;

    if (status === 403 || errorCode === 'ACCESS-403') {
      setErrorMessage(
        'Access denied: Insurer isolation violation. You cannot modify plans belonging to another insurer (403 ACCESS-403).'
      );
    } else if (status === 409 || errorCode === 'STATE-409') {
      setErrorMessage(serverMessage || 'Plan with this code already exists (409 Conflict).');
    } else if (status === 422 || errorCode === 'VALID-422' || status === 400) {
      const details = err.response?.data?.details;
      if (Array.isArray(details) && details.length > 0) {
        setErrorMessage(`Validation error: ${details.join(', ')}`);
      } else {
        setErrorMessage(serverMessage || 'Validation error: Please verify plan inputs.');
      }
    } else {
      setErrorMessage(serverMessage || 'An unexpected error occurred while saving the plan.');
    }
  };

  const formatCurrency = (val) => {
    if (val == null) return '₹0';
    return `₹${Number(val).toLocaleString('en-IN')}`;
  };

  const formatPlanType = (type) => {
    switch (type) {
      case 'MOTOR_FOUR_WHEELER':
      case '4W':
        return '4-Wheeler Motor';
      case 'MOTOR_TWO_WHEELER':
      case '2W':
        return '2-Wheeler Motor';
      case 'HEALTH':
        return 'Health Insurance';
      default:
        return type;
    }
  };

  return (
    <PortalLayout
      title="Plan Management & Rating Rules"
      subtitle="Configure insurance plans, premium rating formulas, underwriting rules, and add-ons (INS-F-003, SAD §4.3.2)"
      badgeText="Insurer Admin"
      badgeClass="bg-warning text-dark"
    >
      <div className="dev-hook-box mb-4">
        <span className="dev-hook-badge">Person 1 Module C4 (INS-F-003)</span>
        <p className="mb-0">
          Enforces insurer isolation (<code>INS-SR-003</code>). Insurer Admins can create and edit plans for their own insurer
          (<code>POST /api/v1/plans</code>, <code>PUT /api/v1/plans/&#123;id&#125;</code>) and deactivate them (<code>PATCH /api/v1/plans/&#123;id&#125;</code>).
          Cross-insurer modifications are rejected with <code>403 Forbidden</code>.
        </p>
      </div>

      {/* Global Alerts */}
      {successMessage && (
        <div className="alert alert-success alert-dismissible fade show mb-4" id="plan-success-alert" role="alert">
          <i className="bi bi-check-circle-fill me-2"></i>
          {successMessage}
          <button type="button" className="btn-close" onClick={() => setSuccessMessage('')} aria-label="Close"></button>
        </div>
      )}
      {errorMessage && (
        <div className="alert alert-danger alert-dismissible fade show mb-4" id="plan-error-alert" role="alert">
          <i className="bi bi-exclamation-triangle-fill me-2"></i>
          {errorMessage}
          <button type="button" className="btn-close" onClick={() => setErrorMessage('')} aria-label="Close"></button>
        </div>
      )}

      {/* Action Header */}
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <div>
          <h2 className="h5 fw-bold mb-0">Insurer Portfolio Plans</h2>
          <span className="text-muted small">
            Managing catalog for Insurer #{effectiveInsurerId} ({plans.length} total plan{plans.length === 1 ? '' : 's'})
          </span>
        </div>
        {formMode === null && (
          <button
            type="button"
            id="create-new-plan-btn"
            className="btn btn-primary btn-sm"
            onClick={handleOpenCreate}
          >
            <i className="bi bi-plus-circle me-1"></i>+ Create New Plan
          </button>
        )}
      </div>

      {/* Plan Form Modal/Card */}
      {formMode !== null && (
        <div className="dashboard-card mb-4 border border-primary shadow-sm" id="plan-editor-card">
          <div className="dashboard-card-header bg-light d-flex justify-content-between align-items-center">
            <span className="fw-bold fs-6">
              {formMode === 'CREATE' ? 'Create New Insurance Plan (INS-F-003)' : `Edit Plan: ${formData.code}`}
            </span>
            <button
              type="button"
              className="btn btn-sm btn-outline-secondary"
              id="cancel-edit-btn"
              onClick={handleCancelForm}
              disabled={formSubmitting}
            >
              Cancel
            </button>
          </div>
          <div className="dashboard-card-body p-3 p-md-4">
            <form onSubmit={handleSubmit} noValidate id="plan-editor-form">
              <div className="row g-3 mb-3">
                <div className="col-12 col-md-4">
                  <label htmlFor="plan-code-input" className="form-label small fw-semibold">
                    Plan Code <span className="text-danger">*</span>
                  </label>
                  <input
                    id="plan-code-input"
                    type="text"
                    className="form-control"
                    placeholder="e.g. PLAN-SAFE-4W-01"
                    value={formData.code}
                    onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })}
                    disabled={formMode === 'EDIT' || formSubmitting}
                    required
                  />
                  {formMode === 'EDIT' && (
                    <div className="form-text small text-muted">Plan code is permanent and cannot be changed.</div>
                  )}
                </div>

                <div className="col-12 col-md-5">
                  <label htmlFor="plan-name-input" className="form-label small fw-semibold">
                    Plan Name <span className="text-danger">*</span>
                  </label>
                  <input
                    id="plan-name-input"
                    type="text"
                    className="form-control"
                    placeholder="e.g. SafeShield Comprehensive 4W"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    disabled={formSubmitting}
                    required
                  />
                </div>

                <div className="col-12 col-md-3">
                  <label htmlFor="plan-type-select" className="form-label small fw-semibold">
                    Plan Type <span className="text-danger">*</span>
                  </label>
                  <select
                    id="plan-type-select"
                    className="form-select"
                    value={formData.type}
                    onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                    disabled={formSubmitting}
                  >
                    <option value="MOTOR_FOUR_WHEELER">4-Wheeler Motor</option>
                    <option value="MOTOR_TWO_WHEELER">2-Wheeler Motor</option>
                    <option value="HEALTH">Health Insurance</option>
                  </select>
                </div>
              </div>

              <div className="row g-3 mb-3">
                <div className="col-12 col-sm-6 col-md-3">
                  <label htmlFor="plan-coverage-input" className="form-label small fw-semibold">
                    Coverage Amount (₹) <span className="text-danger">*</span>
                  </label>
                  <input
                    id="plan-coverage-input"
                    type="number"
                    min="1"
                    className="form-control"
                    placeholder="e.g. 500000"
                    value={formData.coverage}
                    onChange={(e) => setFormData({ ...formData, coverage: e.target.value })}
                    disabled={formSubmitting}
                    required
                  />
                </div>

                <div className="col-12 col-sm-6 col-md-3">
                  <label htmlFor="plan-deductible-input" className="form-label small fw-semibold">
                    Deductible (₹) <span className="text-danger">*</span>
                  </label>
                  <input
                    id="plan-deductible-input"
                    type="number"
                    min="0"
                    className="form-control"
                    placeholder="e.g. 1000"
                    value={formData.deductible}
                    onChange={(e) => setFormData({ ...formData, deductible: e.target.value })}
                    disabled={formSubmitting}
                    required
                  />
                </div>

                <div className="col-12 col-sm-6 col-md-3">
                  <label htmlFor="plan-base-premium-input" className="form-label small fw-semibold">
                    Base Premium (₹) <span className="text-danger">*</span>
                  </label>
                  <input
                    id="plan-base-premium-input"
                    type="number"
                    min="0"
                    className="form-control"
                    placeholder="e.g. 3500"
                    value={formData.basePremium}
                    onChange={(e) => setFormData({ ...formData, basePremium: e.target.value })}
                    disabled={formSubmitting}
                    required
                  />
                </div>

                <div className="col-12 col-sm-6 col-md-3">
                  <label htmlFor="plan-waiting-period-input" className="form-label small fw-semibold">
                    Waiting Period (Months)
                  </label>
                  <input
                    id="plan-waiting-period-input"
                    type="number"
                    min="0"
                    className="form-control"
                    placeholder="0"
                    value={formData.waitingPeriodMonths}
                    onChange={(e) => setFormData({ ...formData, waitingPeriodMonths: e.target.value })}
                    disabled={formSubmitting}
                  />
                  <div className="form-text small text-muted">Health pre-existing disease period.</div>
                </div>
              </div>

              {/* JSON Rules */}
              <div className="row g-3 mb-3">
                <div className="col-12 col-md-6">
                  <label htmlFor="plan-premium-rules-input" className="form-label small fw-semibold">
                    Premium Rating Rules JSON
                  </label>
                  <textarea
                    id="plan-premium-rules-input"
                    rows="3"
                    className="form-control font-monospace small"
                    value={formData.premiumRulesJson}
                    onChange={(e) => setFormData({ ...formData, premiumRulesJson: e.target.value })}
                    disabled={formSubmitting}
                  ></textarea>
                </div>
                <div className="col-12 col-md-6">
                  <label htmlFor="plan-uw-rules-input" className="form-label small fw-semibold">
                    Underwriting Rules JSON
                  </label>
                  <textarea
                    id="plan-uw-rules-input"
                    rows="3"
                    className="form-control font-monospace small"
                    value={formData.underwritingRulesJson}
                    onChange={(e) => setFormData({ ...formData, underwritingRulesJson: e.target.value })}
                    disabled={formSubmitting}
                  ></textarea>
                </div>
              </div>

              {/* Add-Ons Checkboxes */}
              <div className="mb-3">
                <label className="form-label small fw-semibold d-block mb-2">Supported Add-Ons</label>
                <div className="row g-2">
                  {AVAILABLE_ADD_ONS.map((addOn) => (
                    <div key={addOn.id} className="col-12 col-sm-6 col-lg-4">
                      <div className="form-check">
                        <input
                          type="checkbox"
                          id={`addon-${addOn.id}`}
                          className="form-check-input"
                          checked={formData.addOns.includes(addOn.id)}
                          onChange={() => handleToggleAddOn(addOn.id)}
                          disabled={formSubmitting}
                        />
                        <label htmlFor={`addon-${addOn.id}`} className="form-check-label small">
                          {addOn.label}
                        </label>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Active Toggle Switch */}
              <div className="form-check form-switch mb-4">
                <input
                  type="checkbox"
                  id="plan-active-switch"
                  className="form-check-input"
                  checked={formData.active}
                  onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                  disabled={formSubmitting}
                />
                <label htmlFor="plan-active-switch" className="form-check-label small fw-semibold">
                  Publish to Public Browse (Active)
                </label>
              </div>

              <div className="d-flex gap-2 justify-content-end">
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={handleCancelForm}
                  disabled={formSubmitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  id="save-plan-btn"
                  className="btn btn-primary px-4"
                  disabled={formSubmitting}
                >
                  {formSubmitting ? 'Saving...' : formMode === 'CREATE' ? 'Create Plan' : 'Update Plan'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Plans List Table */}
      <div className="dashboard-card" id="plans-table-card">
        <div className="dashboard-card-header d-flex justify-content-between align-items-center">
          <span className="fw-bold">Active & Managed Insurance Plans</span>
          <button
            type="button"
            className="btn btn-sm btn-outline-primary"
            onClick={fetchPlans}
            disabled={loading}
          >
            <i className="bi bi-arrow-clockwise me-1"></i>Refresh
          </button>
        </div>

        {fetchError && (
          <div className="alert alert-danger m-3 small" role="alert">
            {fetchError}
          </div>
        )}

        <div className="dashboard-card-body p-0">
          {loading ? (
            <div className="text-center py-5">
              <div className="spinner-border text-primary" role="status">
                <span className="visually-hidden">Loading plans...</span>
              </div>
              <p className="text-muted small mt-2 mb-0">Loading insurer plans catalog...</p>
            </div>
          ) : plans.length === 0 ? (
            <div className="text-center py-5">
              <i className="bi bi-inbox fs-2 text-muted"></i>
              <p className="text-muted small mt-2 mb-0">No insurance plans found for this insurer.</p>
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0" id="plans-table">
                <thead className="table-light">
                  <tr>
                    <th>Plan Code</th>
                    <th>Name</th>
                    <th>Category</th>
                    <th>Coverage</th>
                    <th>Deductible</th>
                    <th>Base Premium</th>
                    <th>Status</th>
                    <th className="text-end">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {plans.map((p) => {
                    const id = p.planId || p.id;
                    const isActive = p.active !== false;

                    return (
                      <tr key={id} id={`plan-row-${id}`}>
                        <td className="fw-semibold text-primary">{p.code}</td>
                        <td>
                          <div className="fw-medium">{p.name}</div>
                          {Array.isArray(p.addOns) && p.addOns.length > 0 && (
                            <div className="mt-1 d-flex flex-wrap gap-1">
                              {p.addOns.map((ao) => (
                                <span key={ao} className="badge bg-light text-dark border small">
                                  {ao}
                                </span>
                              ))}
                            </div>
                          )}
                        </td>
                        <td>
                          <span className="badge bg-info text-dark">{formatPlanType(p.type)}</span>
                        </td>
                        <td className="fw-semibold">{formatCurrency(p.coverage)}</td>
                        <td>{formatCurrency(p.deductible)}</td>
                        <td>{formatCurrency(p.basePremium)}</td>
                        <td>
                          {isActive ? (
                            <span className="badge bg-success" id={`status-badge-active-${id}`}>
                              Active
                            </span>
                          ) : (
                            <span className="badge bg-secondary" id={`status-badge-inactive-${id}`}>
                              Inactive
                            </span>
                          )}
                        </td>
                        <td className="text-end">
                          <button
                            type="button"
                            id={`edit-plan-btn-${id}`}
                            className="btn btn-outline-primary btn-sm me-1"
                            onClick={() => handleOpenEdit(p)}
                            title="Edit plan rating rules and coverage"
                          >
                            <i className="bi bi-pencil me-1"></i>Edit
                          </button>
                          <button
                            type="button"
                            id={`toggle-active-btn-${id}`}
                            className={`btn btn-sm ${isActive ? 'btn-outline-danger' : 'btn-outline-success'}`}
                            onClick={() => handleToggleActive(p)}
                            title={isActive ? 'Deactivate plan from public browse' : 'Activate plan'}
                          >
                            {isActive ? 'Deactivate' : 'Activate'}
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </PortalLayout>
  );
}
