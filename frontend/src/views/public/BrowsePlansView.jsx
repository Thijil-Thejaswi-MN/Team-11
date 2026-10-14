import React, { useState } from 'react';
import AppLayout from '../../components/layout/AppLayout';

export default function BrowsePlansView() {
  const [filterType, setFilterType] = useState('ALL');

  // Sample static plans demonstrating layout and SAD schema
  const samplePlans = [
    {
      id: 1,
      name: 'SafeShield Motor Comprehensive (4W)',
      insurer: 'SafeShield General (INS-A)',
      type: 'MOTOR_4W',
      coverage: '₹8,00,000 IDV',
      deductible: '₹1,500',
      basePremium: '₹8,450 / yr',
      addOns: ['Zero Depreciation', 'Roadside Assistance'],
    },
    {
      id: 2,
      name: 'Bharat Care Swasthya Health Plan',
      insurer: 'Bharat Care Insurance (INS-B)',
      type: 'HEALTH',
      coverage: '₹5,00,000 Sum Insured',
      deductible: '₹0',
      basePremium: '₹6,200 / yr',
      addOns: ['Critical Illness Cover', 'No Claim Bonus Booster'],
    },
    {
      id: 3,
      name: 'SafeShield Two-Wheeler Protect (2W)',
      insurer: 'SafeShield General (INS-A)',
      type: 'MOTOR_2W',
      coverage: '₹85,000 IDV',
      deductible: '₹500',
      basePremium: '₹1,850 / yr',
      addOns: ['Engine Protection', 'Pillion Rider Cover'],
    },
  ];

  const filtered = filterType === 'ALL' ? samplePlans : samplePlans.filter((p) => p.type.startsWith(filterType));

  return (
    <AppLayout>
      <div className="mb-4">
        <h1 className="h3 fw-bold">Browse Insurance Plans</h1>
        <p className="text-muted small">
          Public Plan Browsing (SRS UC-04 / SAD Section 4.3.2). No authentication required.
        </p>
      </div>

      {/* Developer Integration Hook */}
      <div className="dev-hook-box">
        <span className="dev-hook-badge">Person 1 Ownership (Plan Management C4)</span>
        <p className="mb-0">
          This view connects to <code>GET /api/v1/plans?type=&insurer=&premiumMax=&coverageMin=</code>.
          Person 1 can inject dynamic plan queries and filter state here.
        </p>
      </div>

      {/* Filter Toolbar */}
      <div className="d-flex flex-wrap gap-2 mb-4 align-items-center">
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

      {/* Plan Cards Grid */}
      <div className="row g-3">
        {filtered.map((plan) => (
          <div key={plan.id} className="col-12 col-md-6 col-lg-4">
            <div className="dashboard-card h-100">
              <div className="dashboard-card-header">
                <span className="fw-bold">{plan.name}</span>
                <span className="badge bg-secondary">{plan.type}</span>
              </div>
              <div className="dashboard-card-body d-flex flex-column">
                <div className="text-muted small mb-2">{plan.insurer}</div>
                <div className="mb-2">
                  <span className="fw-bold text-primary fs-5">{plan.basePremium}</span>
                  <span className="text-muted small ms-1">(Base)</span>
                </div>
                <div className="small mb-3">
                  <div><strong>Coverage:</strong> {plan.coverage}</div>
                  <div><strong>Standard Deductible:</strong> {plan.deductible}</div>
                </div>
                <div className="mb-3">
                  <div className="small text-muted mb-1">Available Add-ons:</div>
                  <div className="d-flex flex-wrap gap-1">
                    {plan.addOns.map((addon, idx) => (
                      <span key={idx} className="badge bg-light text-dark border small">{addon}</span>
                    ))}
                  </div>
                </div>
                <div className="mt-auto pt-2 border-top">
                  <button className="btn btn-outline-primary btn-sm w-100">Calculate Instant Quote</button>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </AppLayout>
  );
}
